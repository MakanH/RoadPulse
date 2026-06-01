package com.makan.drivelog.data.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.makan.drivelog.domain.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun createOrUpdateProfile(user: FirebaseUser, displayNameOverride: String? = null) {
        val displayName = displayNameOverride?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: "Driver"
        val data = hashMapOf(
            "uid"         to user.uid,
            "displayName" to displayName,
            "email"       to (user.email ?: ""),
            "photoUrl"    to user.photoUrl?.toString(),
            "createdAt"   to Timestamp.now(),
            "friendIds"   to emptyList<String>()
        )
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge()).await()
    }

    fun getProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                val d = snap?.data
                trySend(if (d == null) null else UserProfile(
                    uid         = d["uid"] as? String ?: "",
                    displayName = d["displayName"] as? String ?: "",
                    email       = d["email"] as? String ?: "",
                    photoUrl    = d["photoUrl"] as? String,
                    createdAt   = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
                ))
            }
        awaitClose { listener.remove() }
    }

    suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            firestore.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", query + '')
                .get().await()
                .documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    UserProfile(
                        uid         = d["uid"] as? String ?: "",
                        displayName = d["displayName"] as? String ?: "",
                        email       = d["email"] as? String ?: "",
                        photoUrl    = d["photoUrl"] as? String,
                        createdAt   = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
                    )
                }
        }.getOrDefault(emptyList())
    }

    suspend fun updateVisibility(uid: String, visibility: String) {
        runCatching {
            firestore.collection("users").document(uid)
                .update("visibility", visibility).await()
        }
    }
}
