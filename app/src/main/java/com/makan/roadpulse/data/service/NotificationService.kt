package com.makan.roadpulse.data.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.makan.roadpulse.domain.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    companion object {
        const val TYPE_FRIEND_REQUEST    = "friend_request"
        const val TYPE_FRIEND_ACCEPTED   = "friend_accepted"
        const val TYPE_GROUP_INVITE      = "group_invite"
        const val TYPE_GROUP_DRIVE_STARTED = "group_drive_started"
        const val TYPE_ACHIEVEMENT       = "achievement"
    }

    private fun notifCol(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    fun getNotifications(): Flow<List<AppNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); return@callbackFlow }
        val listener = notifCol(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.documents?.mapNotNull { doc ->
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        AppNotification(
                            id         = doc.id,
                            type       = doc.getString("type") ?: "",
                            title      = doc.getString("title") ?: "",
                            body       = doc.getString("body") ?: "",
                            isRead     = doc.getBoolean("isRead") ?: false,
                            createdAt  = doc.getLong("createdAt") ?: 0L,
                            actionData = (doc.get("actionData") as? Map<*, *>)
                                ?.entries
                                ?.associate { it.key.toString() to it.value.toString() }
                                ?: emptyMap()
                        )
                    }.getOrNull()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notifId: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching { notifCol(uid).document(notifId).update("isRead", true).await() }
    }

    suspend fun markAllAsRead() {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val unread = notifCol(uid).whereEqualTo("isRead", false).get().await()
            if (unread.isEmpty) return@runCatching
            val batch = firestore.batch()
            unread.documents.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
        }
    }

    suspend fun createNotification(
        targetUid: String,
        type: String,
        title: String,
        body: String,
        actionData: Map<String, String> = emptyMap()
    ) {
        runCatching {
            val ref = notifCol(targetUid).document()
            ref.set(
                hashMapOf(
                    "id"         to ref.id,
                    "type"       to type,
                    "title"      to title,
                    "body"       to body,
                    "isRead"     to false,
                    "createdAt"  to System.currentTimeMillis(),
                    "actionData" to actionData
                )
            ).await()
        }
    }
}
