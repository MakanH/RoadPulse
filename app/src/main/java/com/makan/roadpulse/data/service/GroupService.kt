package com.makan.roadpulse.data.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.makan.roadpulse.domain.model.Group
import com.makan.roadpulse.domain.model.LocationPoint
import com.makan.roadpulse.domain.model.MemberLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val notificationService: NotificationService
) {
    private val currentUid get() = auth.currentUser?.uid ?: ""

    suspend fun createGroup(name: String, friendUids: List<String>): Result<String> = runCatching {
        val memberUids = (friendUids + currentUid).distinct()
        val ref = firestore.collection("groups").add(
            hashMapOf(
                "name"            to name,
                "ownerUid"        to currentUid,
                "memberUids"      to memberUids,
                "activeSessionId" to null,
                "createdAt"       to Timestamp.now()
            )
        ).await()
        // patch id field into the doc
        firestore.collection("groups").document(ref.id)
            .update("id", ref.id).await()
        // notify each invited friend
        friendUids.forEach { uid ->
            notificationService.createNotification(
                targetUid  = uid,
                type       = NotificationService.TYPE_GROUP_INVITE,
                title      = "Added to a group",
                body       = "You were added to \"$name\"",
                actionData = mapOf("groupId" to ref.id)
            )
        }
        ref.id
    }

    fun getMyGroups(): Flow<List<Group>> = callbackFlow {
        val listener = firestore.collection("groups")
            .whereArrayContains("memberUids", currentUid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { doc ->
                    doc.toGroup()
                } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = firestore.collection("groups").document(groupId)
            .addSnapshotListener { snap, _ -> trySend(snap?.toGroup()) }
        awaitClose { listener.remove() }
    }

    suspend fun inviteToGroup(groupId: String, friendUid: String): Result<Unit> = runCatching {
        firestore.collection("groups").document(groupId)
            .update("memberUids", FieldValue.arrayUnion(friendUid)).await()
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> = runCatching {
        firestore.collection("groups").document(groupId)
            .update("memberUids", FieldValue.arrayRemove(currentUid)).await()
    }

    suspend fun startGroupSession(
        groupId: String,
        groupName: String,
        otherMemberUids: List<String>
    ): Result<String> = runCatching {
        val sessionRef = firestore.collection("groups").document(groupId)
            .collection("sessions").add(
                hashMapOf(
                    "startedAt"  to Timestamp.now(),
                    "endedAt"    to null,
                    "memberUids" to listOf(currentUid)
                )
            ).await()
        // patch id
        sessionRef.update("id", sessionRef.id).await()
        firestore.collection("groups").document(groupId)
            .update("activeSessionId", sessionRef.id).await()
        // notify other members
        val starterName = auth.currentUser?.let {
            it.displayName?.takeIf { n -> n.isNotBlank() }
                ?: it.email?.substringBefore('@') ?: "Someone"
        } ?: "Someone"
        otherMemberUids.forEach { uid ->
            notificationService.createNotification(
                targetUid  = uid,
                type       = NotificationService.TYPE_GROUP_DRIVE_STARTED,
                title      = "Group drive started",
                body       = "$starterName started a drive in \"$groupName\"",
                actionData = mapOf("groupId" to groupId, "sessionId" to sessionRef.id)
            )
        }
        sessionRef.id
    }

    suspend fun endGroupSession(groupId: String, sessionId: String): Result<Unit> = runCatching {
        val batch = firestore.batch()
        batch.update(
            firestore.collection("groups").document(groupId)
                .collection("sessions").document(sessionId),
            "endedAt", Timestamp.now()
        )
        batch.update(
            firestore.collection("groups").document(groupId),
            "activeSessionId", null
        )
        batch.commit().await()
    }

    suspend fun pushMyLocation(
        groupId: String,
        sessionId: String,
        location: LocationPoint,
        bearing: Float
    ): Result<Unit> = runCatching {
        val user = auth.currentUser!!
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@') ?: "Driver"
        firestore.collection("groups").document(groupId)
            .collection("sessions").document(sessionId)
            .collection("locations").document(currentUid)
            .set(
                hashMapOf(
                    "uid"         to currentUid,
                    "displayName" to displayName,
                    "photoUrl"    to user.photoUrl?.toString(),
                    "latitude"    to location.latitude,
                    "longitude"   to location.longitude,
                    "speed"       to location.speed,
                    "bearing"     to bearing.toDouble(),
                    "timestamp"   to Timestamp.now(),
                    "isActive"    to true
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
    }

    fun getMemberLocations(groupId: String, sessionId: String): Flow<List<MemberLocation>> =
        callbackFlow {
            val listener = firestore.collection("groups").document(groupId)
                .collection("sessions").document(sessionId)
                .collection("locations")
                .addSnapshotListener { snap, _ ->
                    trySend(snap?.documents?.mapNotNull { doc ->
                        val d = doc.data ?: return@mapNotNull null
                        MemberLocation(
                            uid         = d["uid"] as? String ?: "",
                            displayName = d["displayName"] as? String ?: "",
                            photoUrl    = d["photoUrl"] as? String,
                            latitude    = (d["latitude"] as? Number)?.toDouble() ?: 0.0,
                            longitude   = (d["longitude"] as? Number)?.toDouble() ?: 0.0,
                            speed       = (d["speed"] as? Number)?.toDouble() ?: 0.0,
                            bearing     = (d["bearing"] as? Number)?.toFloat() ?: 0f,
                            timestamp   = (d["timestamp"] as? Timestamp)?.toDate()?.time ?: 0L,
                            isActive    = d["isActive"] as? Boolean ?: false
                        )
                    } ?: emptyList())
                }
            awaitClose { listener.remove() }
        }

    suspend fun setMemberInactive(groupId: String, sessionId: String): Result<Unit> = runCatching {
        firestore.collection("groups").document(groupId)
            .collection("sessions").document(sessionId)
            .collection("locations").document(currentUid)
            .update("isActive", false).await()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toGroup(): Group? {
        val d = data ?: return null
        @Suppress("UNCHECKED_CAST")
        return Group(
            id              = id,
            name            = d["name"] as? String ?: "",
            ownerUid        = d["ownerUid"] as? String ?: "",
            memberUids      = d["memberUids"] as? List<String> ?: emptyList(),
            activeSessionId = d["activeSessionId"] as? String
        )
    }
}
