package com.makan.drivelog.data.service

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.makan.drivelog.domain.model.Trip
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun syncTripToFirestore(trip: Trip) {
        val user = auth.currentUser ?: return
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@') ?: "Driver"
        val durationSeconds = ((trip.endTime ?: trip.startTime) - trip.startTime) / 1_000L
        runCatching {
            firestore.collection("users").document(user.uid)
                .collection("trips").document(trip.id.toString())
                .set(
                    hashMapOf(
                        "tripId"          to trip.id.toString(),
                        "tripName"        to trip.name,
                        "displayName"     to displayName,
                        "photoUrl"        to user.photoUrl?.toString(),
                        "topSpeed"        to trip.topSpeed,
                        "avgSpeed"        to trip.avgSpeed,
                        "distanceKm"      to trip.distance / 1_000.0,
                        "durationSeconds" to durationSeconds,
                        "startName"       to trip.startName,
                        "endName"         to trip.endName,
                        "startTime"       to Timestamp(Date(trip.startTime))
                    )
                ).await()
        }
    }

    suspend fun updateTripNameInFirestore(tripId: Long, newName: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            firestore.collection("users").document(uid)
                .collection("trips").document(tripId.toString())
                .update("tripName", newName).await()
        }
    }
}
