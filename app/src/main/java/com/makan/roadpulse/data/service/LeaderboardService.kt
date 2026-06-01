package com.makan.roadpulse.data.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.makan.roadpulse.domain.model.FriendTripStat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUid get() = auth.currentUser?.uid ?: ""

    fun getFriendsTripStats(): Flow<List<FriendTripStat>> = callbackFlow {
        val tripListeners = mutableMapOf<String, ListenerRegistration>()
        val statsByUid    = mutableMapOf<String, List<FriendTripStat>>()

        fun resubscribe(allUids: List<String>) {
            // Drop listeners for uids no longer present
            val stale = tripListeners.keys.filter { it !in allUids }
            stale.forEach { uid -> tripListeners.remove(uid)?.remove(); statsByUid.remove(uid) }

            // Add listeners for new uids
            allUids.filter { it !in tripListeners }.forEach { uid ->
                val reg = firestore.collection("users").document(uid)
                    .collection("trips")
                    .addSnapshotListener { snap, _ ->
                        statsByUid[uid] = snap?.documents?.mapNotNull { doc ->
                            val d = doc.data ?: return@mapNotNull null
                            FriendTripStat(
                                uid             = d["uid"] as? String ?: uid,
                                displayName     = d["displayName"] as? String ?: "",
                                photoUrl        = d["photoUrl"] as? String,
                                tripId          = d["tripId"] as? String ?: doc.id,
                                topSpeed        = (d["topSpeed"] as? Number)?.toDouble() ?: 0.0,
                                avgSpeed        = (d["avgSpeed"] as? Number)?.toDouble() ?: 0.0,
                                distanceKm      = (d["distanceKm"] as? Number)?.toDouble() ?: 0.0,
                                durationSeconds = (d["durationSeconds"] as? Number)?.toLong() ?: 0L,
                                startName       = d["startName"] as? String,
                                endName         = d["endName"] as? String,
                                startTime       = (d["startTime"] as? Timestamp)?.toDate()?.time ?: 0L
                            )
                        } ?: emptyList()
                        trySend(statsByUid.values.flatten())
                    }
                tripListeners[uid] = reg
            }
        }

        // Watch the current user's document for friendIds changes
        val selfListener = firestore.collection("users").document(currentUid)
            .addSnapshotListener { snap, _ ->
                @Suppress("UNCHECKED_CAST")
                val friendIds = snap?.get("friendIds") as? List<String> ?: emptyList()
                resubscribe((friendIds + currentUid).distinct())
            }

        awaitClose {
            selfListener.remove()
            tripListeners.values.forEach { it.remove() }
        }
    }
}
