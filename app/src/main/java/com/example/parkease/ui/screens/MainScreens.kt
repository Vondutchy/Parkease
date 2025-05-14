package com.example.parkease.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.parkease.navigation.Screen
import com.example.parkease.ui.components.*
import com.example.parkease.ui.viewmodels.HomeViewModel
import com.example.parkease.ui.viewmodels.ParkingViewModel
import com.example.parkease.ui.viewmodels.CalendarViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        topBar = {
            TopAppBar(
                title = { Text("ParkEase") },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Welcome to ParkEase", style = MaterialTheme.typography.headlineMedium)
            Text("Good Day, ${FirebaseAuth.getInstance().currentUser?.email ?: "User"}!", 
                style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = { navController.navigate(Screen.Calendar.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New Reservation")
            }

            uiState.reservationData?.let { res ->
                ReservationCard(
                    plate = res["plate"] ?: "",
                    floor = res["floor"] ?: "",
                    slotId = res["slotId"] ?: "",
                    start = res["startTime"] ?: "",
                    end = res["endTime"] ?: "",
                    onClick = { showCancelDialog = true }
                )
            }

            SectionTitle(text = "Available Slots")

            if (uiState.isLoading) {
                LoadingScreen()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(4) { index ->
                        val floorKey = "floor${index + 1}"
                        val slotCount = uiState.floorCounts[floorKey] ?: 0
                        val floorLabel = "${index + 1}st FLOOR"
                            .replace("1st", "1st")
                            .replace("2st", "2nd")
                            .replace("3st", "3rd")
                            .replace("4st", "4th")

                        FloorCard(
                            floorLabel = floorLabel,
                            slotCount = slotCount,
                            onClick = { navController.navigate(Screen.Parking.createRoute(floorKey)) }
                        )
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelReservation()
                        showCancelDialog = false
                    }
                ) {
                    Text("Cancel Reservation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("Cancel Reservation") },
            text = { Text("Are you sure you want to cancel your reservation?") }
        )
    }

    uiState.error?.let { error ->
        ErrorMessage(
            message = error,
            onDismiss = { /* Error will be cleared on next action */ }
        )
    }
}

@Composable
fun ReservationCard(
    plate: String,
    floor: String,
    slotId: String,
    start: String,
    end: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = plate, style = MaterialTheme.typography.titleLarge)
                Text(text = "${floor.uppercase()} • $slotId")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "$start–$end", style = MaterialTheme.typography.titleMedium)
                Text(text = "reserved", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FloorCard(
    floorLabel: String,
    slotCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(floorLabel, style = MaterialTheme.typography.bodyLarge)
            Text("$slotCount", style = MaterialTheme.typography.displayMedium)
            Text("Slots Left", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    navController: NavHostController,
    floor: String,
    viewModel: ParkingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val TODAY_DATE = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(floor) {
        viewModel.loadSlots(floor, TODAY_DATE)
    }

    Scaffold(
        topBar = {
            TopAppBarWithBack(
                title = "Floor $floor",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                LoadingScreen()
            } else if (uiState.slotMap.isEmpty()) {
                Text("No slots found.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.slotMap.entries.toList()) { (slotId, status) ->
                        val isAvailable = status == "available"
                        Button(
                            onClick = { viewModel.selectSlot(slotId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFB71C1C),
                                disabledContainerColor = Color(0xFFB71C1C),
                                disabledContentColor = Color.White
                            ),
                            enabled = isAvailable
                        ) {
                            Text(slotId)
                        }
                    }
                }
            }
        }
    }

    uiState.selectedSlot?.let { slotId ->
        val TODAY_DATE = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }
        SlotReservationDialog(
            floor = floor,
            slotId = slotId,
            date = TODAY_DATE,
            startTime = null,
            endTime = null,
            onDismiss = { viewModel.selectSlot(null) },
            onReserveConfirmed = { date, start, end, plate ->
                viewModel.reserveSlot(
                    floor = floor,
                    slotId = slotId,
                    date = date,
                    startTime = start,
                    endTime = end,
                    plate = plate,
                    onSuccess = { navController.popBackStack() }
                )
            }
        )
    }

    uiState.error?.let { error ->
        ErrorMessage(
            message = error,
            onDismiss = { /* Error will be cleared on next action */ }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Date & Time") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Choose your parking time",
                style = MaterialTheme.typography.titleLarge
            )

            Button(
                onClick = {
                    val date = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    viewModel.updateDate(date)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.selectedDate.isEmpty()) "Select Date" else "Date: ${uiState.selectedDate}")
            }

            Button(
                onClick = {
                    // Show time picker
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.startTime.isEmpty()) "Select Start Time" else "Start: ${uiState.startTime}")
            }

            Button(
                onClick = {
                    // Show time picker
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.endTime.isEmpty()) "Select End Time" else "End: ${uiState.endTime}")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.validateAndProceed {
                        navController.navigate(
                            Screen.ParkingWithDate.createRoute(
                                floor = "1",
                                date = uiState.selectedDate,
                                start = uiState.startTime,
                                end = uiState.endTime
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Slot Selection")
            }
        }
    }

    uiState.error?.let { error ->
        ErrorMessage(
            message = error,
            onDismiss = { /* Error will be cleared on next action */ }
        )
    }
} 