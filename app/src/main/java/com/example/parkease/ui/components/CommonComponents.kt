package com.example.parkease.ui.components

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarWithBack(
    title: String,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun LoadingButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotReservationDialog(
    floor: String,
    slotId: String,
    date: String,
    startTime: String?,
    endTime: String?,
    onDismiss: () -> Unit,
    onReserveConfirmed: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var vehicleList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedVehicle by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var localStartTime by remember { mutableStateOf(startTime ?: "") }
    var localEndTime by remember { mutableStateOf(endTime ?: "") }

    // Load vehicles
    LaunchedEffect(Unit) {
        val vehicleRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("users").child(userId).child("vehicles")

        vehicleRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                for (vehicle in snapshot.children) {
                    val plate = vehicle.child("plate").getValue(String::class.java)
                    if (!plate.isNullOrBlank()) list.add(plate)
                }
                vehicleList = list
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (selectedVehicle.isNotEmpty() && 
                        (startTime != null || localStartTime.isNotEmpty()) && 
                        (endTime != null || localEndTime.isNotEmpty())) {
                        onReserveConfirmed(
                            date,
                            startTime ?: localStartTime,
                            endTime ?: localEndTime,
                            selectedVehicle
                        )
                    } else {
                        Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Reserve Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Reserve Slot $slotId") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Date: $date")
                
                if (startTime == null) {
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    localStartTime = String.format("%02d:%02d", hour, minute)
                                },
                                8, 0, true
                            ).show()
                        }
                    ) {
                        Text(if (localStartTime.isEmpty()) "Pick Start Time" else "Start: $localStartTime")
                    }
                } else {
                    Text("Start Time: $startTime")
                }

                if (endTime == null) {
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    localEndTime = String.format("%02d:%02d", hour, minute)
                                },
                                10, 0, true
                            ).show()
                        }
                    ) {
                        Text(if (localEndTime.isEmpty()) "Pick End Time" else "End: $localEndTime")
                    }
                } else {
                    Text("End Time: $endTime")
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Vehicle") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        vehicleList.forEach { plate ->
                            DropdownMenuItem(
                                text = { Text(plate) },
                                onClick = {
                                    selectedVehicle = plate
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
} 