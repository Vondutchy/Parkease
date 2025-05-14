package com.example.parkease.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val floorCounts: Map<String, Int> = emptyMap(),
    val reservationData: Map<String, String>? = null
)

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFloorCounts()
        loadCurrentReservation()
    }

    private fun loadFloorCounts() {
        val database = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        val slotsRef = database.getReference("slots")

        slotsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val counts = mutableMapOf<String, Int>()
                for (floor in snapshot.children) {
                    var availableCount = 0
                    for (slot in floor.children) {
                        if (slot.child("status").getValue(String::class.java) == "available") {
                            availableCount++
                        }
                    }
                    counts[floor.key ?: ""] = availableCount
                }
                _uiState.value = _uiState.value.copy(floorCounts = counts)
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        })
    }

    private fun loadCurrentReservation() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        val reservationsRef = database.getReference("reservations").child(userId)

        reservationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val reservation = mutableMapOf<String, String>()
                    reservation["floor"] = snapshot.child("floor").getValue(String::class.java) ?: ""
                    reservation["slotId"] = snapshot.child("slotId").getValue(String::class.java) ?: ""
                    reservation["startTime"] = snapshot.child("startTime").getValue(String::class.java) ?: ""
                    reservation["endTime"] = snapshot.child("endTime").getValue(String::class.java) ?: ""
                    reservation["plate"] = snapshot.child("plate").getValue(String::class.java) ?: ""
                    _uiState.value = _uiState.value.copy(reservationData = reservation)
                } else {
                    _uiState.value = _uiState.value.copy(reservationData = null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(error = error.message)
            }
        })
    }

    fun cancelReservation() {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val database = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
                val reservationRef = database.getReference("reservations").child(userId)
                
                // Get current reservation data
                val snapshot = reservationRef.get().await()
                if (snapshot.exists()) {
                    val floor = snapshot.child("floor").getValue(String::class.java) ?: return@launch
                    val slotId = snapshot.child("slotId").getValue(String::class.java) ?: return@launch
                    val date = snapshot.child("date").getValue(String::class.java) ?: return@launch

                    // Update slot status
                    val slotRef = database.getReference("slots").child(floor).child(slotId)
                    slotRef.child("status").setValue("available").await()

                    // Remove reservation
                    reservationRef.removeValue().await()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
} 