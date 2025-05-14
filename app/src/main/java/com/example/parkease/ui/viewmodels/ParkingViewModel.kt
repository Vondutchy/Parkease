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

data class ParkingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val slotMap: Map<String, String> = emptyMap(),
    val selectedSlot: String? = null
)

class ParkingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    fun loadSlots(floor: String, date: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val database = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        val slotsRef = database.getReference("slots").child(floor)

        slotsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val slots = mutableMapOf<String, String>()
                for (slot in snapshot.children) {
                    val slotId = slot.key ?: continue
                    val status = slot.child("status").getValue(String::class.java) ?: "available"
                    slots[slotId] = status
                }
                _uiState.value = _uiState.value.copy(
                    slotMap = slots,
                    isLoading = false
                )
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(
                    error = error.message,
                    isLoading = false
                )
            }
        })
    }

    fun selectSlot(slotId: String?) {
        _uiState.value = _uiState.value.copy(selectedSlot = slotId)
    }

    fun reserveSlot(
        floor: String,
        slotId: String,
        date: String,
        startTime: String,
        endTime: String,
        plate: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val database = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
                
                // Update slot status
                val slotRef = database.getReference("slots").child(floor).child(slotId)
                slotRef.child("status").setValue("occupied").await()

                // Create reservation
                val reservationRef = database.getReference("reservations").child(userId)
                val reservation = mapOf(
                    "floor" to floor,
                    "slotId" to slotId,
                    "date" to date,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "plate" to plate
                )
                reservationRef.setValue(reservation).await()

                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
} 