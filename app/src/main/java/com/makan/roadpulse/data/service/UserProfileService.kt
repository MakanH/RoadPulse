package com.makan.roadpulse.data.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.makan.roadpulse.domain.model.UserProfile
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
    suspend fun createOrUpdateProfile(
        user: FirebaseUser,
        displayNameOverride: String? = null,
        tosAccepted: Boolean = false
    ) {
        val displayName = displayNameOverride?.takeIf { it.isNotBlank() }
            ?: user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: "Driver"
        val data = hashMapOf<String, Any?>(
            "uid"         to user.uid,
            "displayName" to displayName,
            "email"       to (user.email ?: ""),
            "photoUrl"    to user.photoUrl?.toString(),
            "createdAt"   to Timestamp.now(),
            "friendIds"   to emptyList<String>()
        )
        if (tosAccepted) data["tosAcceptedAt"] = Timestamp.now()
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

    suspend fun deleteUserData(uid: String): Result<Unit> = runCatching {
        // 1. Remove uid from all friends' friendIds arrays
        val usersWithFriend = firestore.collection("users")
            .whereArrayContains("friendIds", uid)
            .get().await()
        for (doc in usersWithFriend.documents) {
            doc.reference.update("friendIds", FieldValue.arrayRemove(uid)).await()
        }

        // 2. Delete all friend requests involving this user
        val sentRequests = firestore.collection("friendRequests")
            .whereEqualTo("fromUid", uid).get().await()
        val receivedRequests = firestore.collection("friendRequests")
            .whereEqualTo("toUid", uid).get().await()
        for (doc in sentRequests.documents + receivedRequests.documents) {
            doc.reference.delete().await()
        }

        // 3. Handle group memberships — owner deletes group+subcollections, member just leaves
        val groups = firestore.collection("groups")
            .whereArrayContains("memberUids", uid).get().await()
        for (groupDoc in groups.documents) {
            if (groupDoc.getString("ownerUid") == uid) {
                val sessions = groupDoc.reference.collection("sessions").get().await()
                for (sessionDoc in sessions.documents) {
                    val locations = sessionDoc.reference.collection("locations").get().await()
                    for (locDoc in locations.documents) locDoc.reference.delete().await()
                    sessionDoc.reference.delete().await()
                }
                groupDoc.reference.delete().await()
            } else {
                groupDoc.reference.update("memberUids", FieldValue.arrayRemove(uid)).await()
            }
        }

        // 4. Delete trip summaries from Firestore
        val trips = firestore.collection("users").document(uid)
            .collection("trips").get().await()
        for (tripDoc in trips.documents) tripDoc.reference.delete().await()

        // 5. Delete user profile document
        firestore.collection("users").document(uid).delete().await()
    }
}
