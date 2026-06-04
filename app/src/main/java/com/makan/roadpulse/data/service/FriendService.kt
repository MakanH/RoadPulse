package com.makan.roadpulse.data.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.makan.roadpulse.domain.model.FriendRequest
import com.makan.roadpulse.domain.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationService: NotificationService
) {
    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ── User lookup ───────────────────────────────────────────────────────────

    /** Fetch up to any number of user profiles, chunking to satisfy Firestore's 30-item `in` limit. */
    suspend fun getUserProfiles(uids: List<String>): List<UserProfile> {
        if (uids.isEmpty()) return emptyList()
        return uids.chunked(30).flatMap { chunk ->
            runCatching {
                firestore.collection("users")
                    .whereIn("uid", chunk)
                    .get().await()
                    .documents.mapNotNull { doc -> doc.data?.toUserProfile() }
            }.getOrDefault(emptyList())
        }
    }

    /** Search users by display name prefix. Throws on network error — callers should catch. */
    suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) return emptyList()
        val end = query + ''
        @Suppress("UNCHECKED_CAST")
        val myFriendIds = runCatching {
            firestore.collection("users").document(currentUid).get().await()
                .get("friendIds") as? List<String>
        }.getOrNull() ?: emptyList()
        return firestore.collection("users")
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", end)
            .get().await()
            .documents.mapNotNull { doc ->
                val d = doc.data ?: return@mapNotNull null
                val uid = d["uid"] as? String ?: return@mapNotNull null
                if (uid == currentUid) return@mapNotNull null
                val visibility = d["visibility"] as? String ?: "everyone"
                if (visibility == "friends_only" && uid !in myFriendIds) return@mapNotNull null
                d.toUserProfile()
            }
    }

    // ── Friend requests ───────────────────────────────────────────────────────

    suspend fun sendFriendRequest(toUid: String): Result<Unit> = runCatching {
        val existing = firestore.collection("friendRequests")
            .whereEqualTo("fromUid", currentUid)
            .whereEqualTo("toUid", toUid)
            .whereEqualTo("status", "pending")
            .get().await()
        if (!existing.isEmpty) return@runCatching

        val user = auth.currentUser!!
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@') ?: "Driver"
        firestore.collection("friendRequests").add(
            hashMapOf(
                "fromUid"         to currentUid,
                "toUid"           to toUid,
                "fromDisplayName" to displayName,
                "fromPhotoUrl"    to user.photoUrl?.toString(),
                "status"          to "pending",
                "createdAt"       to Timestamp.now()
            )
        ).await()
        notificationService.createNotification(
            targetUid  = toUid,
            type       = NotificationService.TYPE_FRIEND_REQUEST,
            title      = "New friend request",
            body       = "$displayName wants to be your friend",
            actionData = mapOf("fromUid" to currentUid)
        )
    }

    fun getIncomingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    FriendRequest(
                        id              = doc.id,
                        fromUid         = d["fromUid"] as? String ?: "",
                        toUid           = d["toUid"] as? String ?: "",
                        fromDisplayName = d["fromDisplayName"] as? String ?: "",
                        fromPhotoUrl    = d["fromPhotoUrl"] as? String,
                        status          = d["status"] as? String ?: "",
                        createdAt       = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
                    )
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getOutgoingRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val listener = firestore.collection("friendRequests")
            .whereEqualTo("fromUid", currentUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    FriendRequest(
                        id              = doc.id,
                        fromUid         = d["fromUid"] as? String ?: "",
                        toUid           = d["toUid"] as? String ?: "",
                        fromDisplayName = d["fromDisplayName"] as? String ?: "",
                        fromPhotoUrl    = d["fromPhotoUrl"] as? String,
                        status          = d["status"] as? String ?: "",
                        createdAt       = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
                    )
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun acceptRequest(requestId: String, fromUid: String): Result<Unit> = runCatching {
        val batch = firestore.batch()
        batch.update(
            firestore.collection("friendRequests").document(requestId),
            "status", "accepted"
        )
        batch.update(
            firestore.collection("users").document(currentUid),
            "friendIds", FieldValue.arrayUnion(fromUid)
        )
        batch.update(
            firestore.collection("users").document(fromUid),
            "friendIds", FieldValue.arrayUnion(currentUid)
        )
        batch.commit().await()
        val accepterName = auth.currentUser?.let {
            it.displayName?.takeIf { n -> n.isNotBlank() }
                ?: it.email?.substringBefore('@') ?: "Someone"
        } ?: "Someone"
        notificationService.createNotification(
            targetUid  = fromUid,
            type       = NotificationService.TYPE_FRIEND_ACCEPTED,
            title      = "Friend request accepted",
            body       = "$accepterName accepted your friend request",
            actionData = mapOf("uid" to currentUid)
        )
    }

    suspend fun declineRequest(requestId: String): Result<Unit> = runCatching {
        firestore.collection("friendRequests").document(requestId)
            .update("status", "declined").await()
    }

    // ── Friends list ──────────────────────────────────────────────────────────

    /** Live friends list. Handles any number of friends by chunking Firestore `in` queries. */
    fun getFriends(): Flow<List<UserProfile>> = callbackFlow {
        val selfListener = firestore.collection("users").document(currentUid)
            .addSnapshotListener { snap, _ ->
                @Suppress("UNCHECKED_CAST")
                val friendIds = snap?.get("friendIds") as? List<String> ?: emptyList()
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                launch {
                    val profiles = friendIds.chunked(30).flatMap { chunk ->
                        runCatching {
                            firestore.collection("users")
                                .whereIn("uid", chunk)
                                .get().await()
                                .documents.mapNotNull { doc -> doc.data?.toUserProfile() }
                        }.getOrDefault(emptyList())
                    }
                    trySend(profiles)
                }
            }
        awaitClose { selfListener.remove() }
    }

    suspend fun removeFriend(friendUid: String): Result<Unit> = runCatching {
        val batch = firestore.batch()
        batch.update(
            firestore.collection("users").document(currentUid),
            "friendIds", FieldValue.arrayRemove(friendUid)
        )
        batch.update(
            firestore.collection("users").document(friendUid),
            "friendIds", FieldValue.arrayRemove(currentUid)
        )
        batch.commit().await()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun Map<String, Any>.toUserProfile() = UserProfile(
        uid         = this["uid"] as? String ?: "",
        displayName = this["displayName"] as? String ?: "",
        email       = this["email"] as? String ?: "",
        photoUrl    = this["photoUrl"] as? String,
        createdAt   = (this["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
    )
}
