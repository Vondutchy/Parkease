package com.example.parkease.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Vehicle(
    val brand: String,
    val model: String,
    val plate: String
)

data class ProfileUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        val vehicleRef = db.getReference("users").child(userId).child("vehicles")

        _uiState.value = _uiState.value.copy(isLoading = true)

        vehicleRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val vehicles = mutableListOf<Vehicle>()
                for (vehicle in snapshot.children) {
                    val brand = vehicle.child("brand").getValue(String::class.java) ?: ""
                    val model = vehicle.child("model").getValue(String::class.java) ?: ""
                    val plate = vehicle.child("plate").getValue(String::class.java) ?: ""
                    if (brand.isNotEmpty() && model.isNotEmpty() && plate.isNotEmpty()) {
                        vehicles.add(Vehicle(brand, model, plate))
                    }
                }
                _uiState.value = _uiState.value.copy(
                    vehicles = vehicles,
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

    fun addVehicle(brand: String, model: String, plate: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
                val vehicleRef = db.getReference("users").child(userId).child("vehicles")

                val key = vehicleRef.push().key ?: "vehicle${System.currentTimeMillis()}"
                vehicleRef.child(key).setValue(
                    mapOf(
                        "brand" to brand,
                        "model" to model,
                        "plate" to plate
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
} 