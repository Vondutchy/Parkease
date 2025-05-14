package com.example.parkease.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parkease.navigation.Screen
import com.example.parkease.ui.components.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var vehicles by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var newVehiclePlate by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (userId != null) {
            val vehicleRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users").child(userId).child("vehicles")

            vehicleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<String>()
                    for (vehicle in snapshot.children) {
                        val plate = vehicle.child("plate").getValue(String::class.java)
                        if (!plate.isNullOrBlank()) list.add(plate)
                    }
                    vehicles = list
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBarWithBack(
                title = "Profile",
                onBackClick = { navController.navigateUp() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Account Information",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Email: ${FirebaseAuth.getInstance().currentUser?.email}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Vehicles",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { showAddVehicleDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
                    }
                }
            }

            items(vehicles) { plate ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(plate)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (userId != null) {
                                        val vehicleRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
                                            .getReference("users").child(userId).child("vehicles")
                                            .orderByChild("plate")
                                            .equalTo(plate)

                                        vehicleRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                snapshot.children.forEach { it.ref.removeValue() }
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Vehicle")
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate(Screen.Greeting.route) {
                            popUpTo(0)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            }
        }
    }

    if (showAddVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("Add Vehicle") },
            text = {
                OutlinedTextField(
                    value = newVehiclePlate,
                    onValueChange = { newVehiclePlate = it },
                    label = { Text("License Plate") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newVehiclePlate.isNotBlank() && userId != null) {
                            val vehicleRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
                                .getReference("users").child(userId).child("vehicles")
                                .push()

                            vehicleRef.setValue(mapOf("plate" to newVehiclePlate))
                            newVehiclePlate = ""
                            showAddVehicleDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
} 