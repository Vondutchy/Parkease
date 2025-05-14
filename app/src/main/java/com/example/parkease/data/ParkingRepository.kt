package com.example.parkease.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ParkingRepository {
    private val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
    
    fun reserveSlot(
        context: Context,
        floor: String,
        slotId: String,
        date: String,
        startTime: String,
        endTime: String,
        plate: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val reservationRef = db.getReference("reservations").child(userId).child(date)

        reservationRef.get().addOnSuccessListener { snapshot ->
            val existingDate = snapshot.child("date").getValue(String::class.java)
            if (snapshot.exists() && existingDate == date) {
                onError("You already have a reservation for today!")
            } else {
                val reservationData = mapOf(
                    "floor" to floor,
                    "slotId" to slotId,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "plate" to plate
                )
                reservationRef.setValue(reservationData)

                val slotRef = db.getReference("slots").child(floor).child(slotId).child(date).child("status")
                Log.d("RESERVE_SLOT", "Writing status to /slots/$floor/$slotId/$date/status")

                slotRef.setValue("reserved")
                    .addOnSuccessListener {
                        Log.d("RESERVE_SLOT", "✅ Slot marked as reserved: $floor/$slotId/$date")
                        onSuccess()
                    }
                    .addOnFailureListener {
                        Log.e("RESERVE_SLOT", "❌ Failed to reserve slot: ${it.message}")
                        onError(it.message ?: "Failed to reserve slot")
                    }
            }
        }
    }

    fun cancelReservation(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val reservationRef = db.getReference("reservations").child(userId).child(todayDate)

        reservationRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val floor = snapshot.child("floor").getValue(String::class.java)
                val slotId = snapshot.child("slotId").getValue(String::class.java)
                val date = snapshot.child("date").getValue(String::class.java)

                if (!floor.isNullOrEmpty() && !slotId.isNullOrEmpty() && !date.isNullOrEmpty()) {
                    val slotStatusRef = db.getReference("slots").child(floor).child(slotId).child(date).child("status")
                    slotStatusRef.setValue("available")
                        .addOnSuccessListener {
                            reservationRef.removeValue()
                                .addOnSuccessListener { onSuccess() }
                                .addOnFailureListener { onError(it.message ?: "Failed to cancel reservation") }
                        }
                        .addOnFailureListener { onError(it.message ?: "Failed to update slot status") }
                } else {
                    onError("Invalid reservation data")
                }
            } else {
                onError("No active reservation found")
            }
        }
    }

    fun getAvailableSlotCount(floor: String, onResult: (Int) -> Unit) {
        val dbRef = db.getReference("slots").child(floor)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for (slotSnapshot in snapshot.children) {
                    val status = slotSnapshot.child("status").getValue(String::class.java)
                    Log.d("FIREBASE_SLOT", "Floor: $floor, Slot: ${slotSnapshot.key}, Status: $status")
                    if (status == "available") count++
                }
                Log.d("FIREBASE_SLOT", "Total available for $floor = $count")
                onResult(count)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE_SLOT", "Database error: ${error.message}")
                onResult(0)
            }
        })
    }
} 