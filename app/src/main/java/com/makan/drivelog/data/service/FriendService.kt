package com.makan.drivelog.data.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.makan.drivelog.domain.model.FriendRequest
import com.makan.drivelog.domain.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUid get() = auth.currentUser?.uid ?: ""

    suspend fun searchUsers(query: String): List<UserProfile> {
        if (query.isBlank()) return emptyList()
        val end = query + ''
        return runCatching {
            firestore.collection("users")
                .whereGreaterThanOrEqualTo("displayName", query)
                .whereLessThanOrEqualTo("displayName", end)
                .get().await()
                .documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    val uid = d["uid"] as? String ?: return@mapNotNull null
                    if (uid == currentUid) return@mapNotNull null
                    UserProfile(
                        uid         = uid,
                        displayName = d["displayName"] as? String ?: "",
                        email       = d["email"] as? String ?: "",
                        photoUrl    = d["photoUrl"] as? String,
                        createdAt   = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
                    )
                }
        }.getOrDefault(emptyList())
    }

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
    }

    suspend fun declineRequest(requestId: String): Result<Unit> = runCatching {
        firestore.collection("friendRequests").document(requestId)
            .update("status", "declined").await()
    }

    fun getFriends(): Flow<List<UserProfile>> = callbackFlow {
        val selfListener = firestore.collection("users").document(currentUid)
            .addSnapshotListener { snap, _ ->
                @Suppress("UNCHECKED_CAST")
                val friendIds = snap?.get("friendIds") as? List<String> ?: emptyList()
                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Fetch each profile (Firestore 'in' supports up to 30 items)
                firestore.collection("users")
                    .whereIn("uid", friendIds.take(30))
                    .get()
                    .addOnSuccessListener { result ->
                        trySend(result.documents.mapNotNull { doc ->
                            val d = doc.data ?: return@mapNotNull null
                            UserProfile(
                                uid         = d["uid"] as? String ?: "",
                                displayName = d["displayName"] as? String ?: "",
                                email       = d["email"] as? String ?: "",
                                photoUrl    = d["photoUrl"] as? String,
                                createdAt   = (d["createdAt"] as? Timestamp)?.toDate()?.time ?: 0L
                            )
                        })
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
}
