package com.example.parkease

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.parkease.ui.theme.ParkeaseTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import androidx.navigation.compose.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import com.google.firebase.database.*
import android.util.Log
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.ArrowBack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.TimePickerDialog




class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        initializeSlots()
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            ParkeaseTheme {
                val navController = rememberNavController()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val auth = FirebaseAuth.getInstance()
                    startDestination = if (auth.currentUser != null) {
                        Screen.Home.route
                    } else {
                        Screen.Greeting.route
                    }
                }

                if (startDestination != null) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination!!
                    ) {
                        composable(Screen.Greeting.route) {
                            GreetingScreen(
                                onSignInClick = { navController.navigate(Screen.SignIn.route) },
                                onSignUpClick = { navController.navigate(Screen.SignUp.route) }
                            )
                        }
                        composable(Screen.SignIn.route) {
                            SignInScreen(
                                onSignInSuccess = { navController.navigate(Screen.Home.route) },
                                onSignUpRedirect = { navController.navigate(Screen.SignUp.route) }
                            )
                        }
                        composable(Screen.SignUp.route) {
                            SignUpScreen(
                                onSignUpSuccess = { navController.navigate(Screen.Home.route) },
                                onSignInRedirect = { navController.navigate(Screen.SignIn.route) }
                            )
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(navController)
                        }
                        composable(
                            route = Screen.Parking.route,
                            arguments = listOf(navArgument("floor") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val floor = backStackEntry.arguments?.getString("floor") ?: "floor1"
                            ParkingScreen(floor = floor, navController = navController)
                        }
                        composable(
                            route = Screen.ParkingWithDate.route,
                            arguments = listOf(
                                navArgument("floor") { type = NavType.StringType },
                                navArgument("date") { type = NavType.StringType },
                                navArgument("start") { type = NavType.StringType },
                                navArgument("end") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val floor = backStackEntry.arguments?.getString("floor") ?: "floor1"
                            val date = backStackEntry.arguments?.getString("date")
                            val start = backStackEntry.arguments?.getString("start")
                            val end = backStackEntry.arguments?.getString("end")
                            ParkingScreen(floor = floor, navController = navController, selectedDate = date, startTime = start, endTime = end)
                        }
                        composable(Screen.Calendar.route) {
                            CalendarScreen(navController = navController)
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(navController = navController)
                        }
                        composable(
                            route = "selectFloor/{date}/{start}/{end}",
                            arguments = listOf(
                                navArgument("date") { type = NavType.StringType },
                                navArgument("start") { type = NavType.StringType },
                                navArgument("end") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val date = backStackEntry.arguments?.getString("date") ?: ""
                            val start = backStackEntry.arguments?.getString("start") ?: ""
                            val end = backStackEntry.arguments?.getString("end") ?: ""
                            FloorSelectionScreen(navController, date, start, end)
                        }
                        composable(
                            route = "parkingFixed/{floor}/{date}/{start}/{end}",
                            arguments = listOf(
                                navArgument("floor") { type = NavType.StringType },
                                navArgument("date") { type = NavType.StringType },
                                navArgument("start") { type = NavType.StringType },
                                navArgument("end") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val floor = backStackEntry.arguments?.getString("floor") ?: "floor1"
                            val date = backStackEntry.arguments?.getString("date") ?: ""
                            val start = backStackEntry.arguments?.getString("start") ?: ""
                            val end = backStackEntry.arguments?.getString("end") ?: ""
                            ParkingScreenWithDatePreSelected(floor, date, start, end, navController)
                        }



                    }
                } else {
                    // Optional loading UI while checking auth
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

    }
}

@Composable
fun SignUpScreen(onSignUpSuccess: () -> Unit, onSignInRedirect: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSignInRedirect) {
            Text("Already have an account? Sign In")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                            onSignUpSuccess()
                        } else {
                            Toast.makeText(
                                context,
                                "Sign up failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
    }
}

@Composable
fun SignInScreen(onSignInSuccess: () -> Unit, onSignUpRedirect: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSignUpRedirect) {
            Text("Don’t have an account? Sign Up")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                            onSignInSuccess()
                        } else {
                            Toast.makeText(
                                context,
                                "Login failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign In")
        }
    }
}

@Composable
fun GreetingScreen(onSignInClick: () -> Unit, onSignUpClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to ParkEase!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSignInClick, modifier = Modifier.fillMaxWidth()) {
            Text("Sign In")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onSignUpClick, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Up")
        }
    }
}
fun initializeSlots() {
    val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        .getReference("slots")

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val floors = listOf("floor1", "floor2", "floor3", "floor4")
    val slotCount = 10

    for (floor in floors) {
        for (i in 1..slotCount) {
            val slotId = "slot$i"
            val slotPath = db.child(floor).child(slotId).child(today).child("status")
            slotPath.setValue("available")
                .addOnSuccessListener {
                    Log.d("INIT_SLOTS", "✅ $floor/$slotId set as available for $today")
                }
                .addOnFailureListener {
                    Log.e("INIT_SLOTS", "❌ Failed: ${it.message}")
                }
        }
    }
}

val TODAY_DATE: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

@Composable
fun HomeScreen(navController: NavHostController) {

    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.email ?: "User"
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var reservationData by remember { mutableStateOf<Map<String, String>?>(null) }


    LaunchedEffect(userId) {
        if (userId != null) {
            val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
            val reservationRoot = db.getReference("reservations").child(userId)

            reservationRoot.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = Calendar.getInstance()
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

                    for (dateSnapshot in snapshot.children) {
                        val dateKey = dateSnapshot.key ?: continue
                        val endTime = dateSnapshot.child("endTime").getValue(String::class.java) ?: continue
                        val floor = dateSnapshot.child("floor").getValue(String::class.java) ?: continue
                        val slotId = dateSnapshot.child("slotId").getValue(String::class.java) ?: continue

                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val current = sdf.parse(currentTime)
                        val end = sdf.parse(endTime)

                        if (dateKey == currentDate && current != null && end != null && current.after(end)) {
                            // Reservation expired
                            db.getReference("slots").child(floor).child(slotId).child(dateKey).child("status")
                                .setValue("available")
                            reservationRoot.child(dateKey).removeValue()
                            Log.d("EXPIRE", "Expired reservation on $dateKey for $slotId")
                        }
                        else if (dateKey == currentDate) {
                            // Active reservation today
                            val data = mapOf(
                                "plate" to (dateSnapshot.child("plate").getValue(String::class.java) ?: ""),
                                "floor" to floor,
                                "slotId" to slotId,
                                "startTime" to (dateSnapshot.child("startTime").getValue(String::class.java) ?: ""),
                                "endTime" to endTime,
                                "date" to dateKey
                            )
                            reservationData = data
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load reservation", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }



    var showCancelDialog by remember { mutableStateOf(false) }



    // Floor counts
    var floorCounts by remember { mutableStateOf(mapOf<String, Int>()) }

    // Real-time slot count per floor
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        val floors = listOf("floor1", "floor2", "floor3", "floor4")
        val mutableCounts = mutableMapOf<String, Int>()

        for (floor in floors) {
            db.getReference("slots").child(floor)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var count = 0
                        for (slot in snapshot.children) {
                            val todayNode = slot.child(TODAY_DATE)
                            if (todayNode.hasChild("status")) {
                                val status = todayNode.child("status").getValue(String::class.java)
                                if (status == "available") count++
                            }

                        }
                        mutableCounts[floor] = count
                        floorCounts = mutableCounts.toMap()
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        cancelReservation()
                        showCancelDialog = false
                        Toast.makeText(context, "Reservation cancelled", Toast.LENGTH_SHORT).show()
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

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Home", style = MaterialTheme.typography.headlineMedium)
            Text("Good Day, $userName!", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = { /* Navigate to reservation screen */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Text("RESERVE NOW")
            }

            reservationData?.let { res ->
                val plate = res["plate"] ?: ""
                val floor = res["floor"] ?: ""
                val slotId = res["slotId"] ?: ""
                val start = res["startTime"] ?: ""
                val end = res["endTime"] ?: ""

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCancelDialog = true },
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


            Text("Available Slots", style = MaterialTheme.typography.titleMedium)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                items(4) { index ->
                    val floorKey = "floor${index + 1}"
                    val slotCount = floorCounts[floorKey] ?: 0
                    val floorLabel = "${index + 1}st FLOOR"
                        .replace("1st", "1st")
                        .replace("2st", "2nd")
                        .replace("3st", "3rd")
                        .replace("4st", "4th")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable {
                                navController.navigate(Screen.Parking.createRoute(floorKey))
                            },
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
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorSelectionScreen(navController: NavHostController, date: String, start: String, end: String) {
    val floors = listOf("floor1", "floor2", "floor3", "floor4")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Floor") },
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Choose a floor to reserve on:", style = MaterialTheme.typography.titleMedium)

            floors.forEach { floor ->
                Button(
                    onClick = {
                        navController.navigate("parkingFixed/$floor/$date/$start/$end")

                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = floor.replace("floor", "Floor ").uppercase())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotReservationDialogFixedTime(
    floor: String,
    slotId: String,
    date: String,
    startTime: String,
    endTime: String,
    onDismiss: () -> Unit,
    onReserveConfirmed: (Context, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var vehicleList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedVehicle by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // 🔁 Load vehicles
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
                    if (selectedVehicle.isNotEmpty()) {
                        onReserveConfirmed(context, date, startTime, endTime, selectedVehicle)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Please select a vehicle", Toast.LENGTH_SHORT).show()
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
                Text("Time: $startTime – $endTime")

                // 🔽 Vehicle dropdown
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreenWithDatePreSelected(
    floor: String,
    date: String,
    startTime: String,
    endTime: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val dbRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        .getReference("slots").child(floor)

    var slotMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedSlot by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(floor, date) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                for (slot in snapshot.children) {
                    val status = slot.child(date).child("status").getValue(String::class.java) ?: "available"
                    map[slot.key ?: ""] = status
                }
                slotMap = map
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "DB Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Slot - ${floor.uppercase()}") },
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
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (slotMap.isEmpty()) {
                Text("No slots found.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(slotMap.entries.toList()) { (slotId, status) ->
                        val isAvailable = status == "available"
                        Button(
                            onClick = { selectedSlot = slotId },
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

            selectedSlot?.let { slotId ->
                SlotReservationDialogFixedTime(
                    floor = floor,
                    slotId = slotId,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    onDismiss = { selectedSlot = null },
                    onReserveConfirmed = { context, d, s, e, plate ->
                        reserveSlot(context, floor, slotId, d, s, e, plate)
                    }
                )

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    floor: String,
    navController: NavHostController,
    selectedDate: String? = null,
    startTime: String? = null,
    endTime: String? = null
) {
    val context = LocalContext.current
    val TODAY_DATE: String = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val effectiveDate = selectedDate ?: TODAY_DATE

    val dbRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        .getReference("slots")
        .child(floor)

    var slotMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedSlot by remember { mutableStateOf<String?>(null) }

    // 🔄 Load slot availability for this date
    LaunchedEffect(floor, effectiveDate) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                for (slot in snapshot.children) {
                    val status = slot.child(effectiveDate).child("status").getValue(String::class.java) ?: "available"
                    map[slot.key ?: ""] = status
                }

                slotMap = map
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "DB Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slots - ${floor.uppercase()}") },
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
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (slotMap.isEmpty()) {
                Text("No slots found.", style = MaterialTheme.typography.bodyLarge)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(slotMap.entries.toList()) { (slotId, status) ->
                        val isAvailable = status == "available"
                        Button(
                            onClick = { selectedSlot = slotId },
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

            // ✅ Reservation dialog for selected slot
            selectedSlot?.let { slotId ->
                SlotReservationDialog(
                    floor = floor,
                    slotId = slotId,
                    onDismiss = { selectedSlot = null },
                    onReserveConfirmed = { context, date, start, end, plate ->
                        reserveSlot(context, floor, slotId, date, start, end, plate)
                    }
                )


            }
        }
    }
}

fun reserveSlot(
    context: Context,
    floor: String,
    slotId: String,
    date: String,
    startTime: String,
    endTime: String,
    plate: String
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
    val reservationRef = db.getReference("reservations").child(userId).child(date)



    reservationRef.get().addOnSuccessListener { snapshot ->
        val existingDate = snapshot.child("date").getValue(String::class.java)
        if (snapshot.exists() && existingDate == date) {
            Toast.makeText(context, "You already have a reservation for today!", Toast.LENGTH_SHORT).show()
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

            slotRef.setValue("reserved").addOnSuccessListener {
                Log.d("RESERVE_SLOT", "✅ Slot marked as reserved: $floor/$slotId/$date")
            }.addOnFailureListener {
                Log.e("RESERVE_SLOT", "❌ Failed to reserve slot: ${it.message}")
            }

        }
    }
}


fun cancelReservation() {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
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
            }

            // Delete reservation
            reservationRef.removeValue()
        }
    }
}



fun getAvailableSlotCount(floor: String, onResult: (Int) -> Unit) {
    val dbRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        .getReference("slots")
        .child(floor)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotReservationDialog(
    floor: String,
    slotId: String,
    onDismiss: () -> Unit,
    onReserveConfirmed: (Context, String, String, String, String) -> Unit

) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val date = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }


    val calendar = Calendar.getInstance()
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }


    var vehicleList by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedVehicle by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    // 🔁 Load vehicles
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
                    if (date.isNotEmpty() && startTime.isNotEmpty() && endTime.isNotEmpty() && selectedVehicle.isNotEmpty()) {
                        onReserveConfirmed(context, date, startTime, endTime, selectedVehicle)

                        onDismiss()
                    } else {
                        Toast.makeText(context, "Complete all fields first", Toast.LENGTH_SHORT).show()
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

                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour: Int, minute: Int ->
                                startTime = String.format("%02d:%02d", hour, minute)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                ) {
                    Text(if (startTime.isEmpty()) "Pick Start Time" else "Start: $startTime")
                }

                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour: Int, minute: Int ->
                                endTime = String.format("%02d:%02d", hour, minute)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                ) {
                    Text(if (endTime.isEmpty()) "Pick End Time" else "End: $endTime")
                }


                // 🔽 Vehicle dropdown
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavHostController) {
    val context = LocalContext.current

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = dateFormat.format(Date())
    val calendar = Calendar.getInstance()


    var selectedDate by remember { mutableStateOf("") }


    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CALENDAR") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Schedule your Booking Now!", style = MaterialTheme.typography.titleMedium)
            Text("Select Date")

            Button(
                onClick = {
                    val datePicker = DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val cal = Calendar.getInstance()
                            cal.set(year, month, day)
                            selectedDate = dateFormat.format(cal.time)
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH) + 1
                    )

                    datePicker.datePicker.minDate = calendar.timeInMillis + 24 * 60 * 60 * 1000 // disable today
                    datePicker.show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedDate.isEmpty()) "Select Date" else "Date: $selectedDate")
            }


            Button(
                onClick = {
                    val timePicker = android.app.TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            startTime = String.format("%02d:%02d", hour, minute)
                        },
                        8, 0, true // default 08:00
                    )
                    timePicker.show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (startTime.isEmpty()) "Select Start Time" else "Start: $startTime")
            }


            Button(
                onClick = {
                    val timePicker = android.app.TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            endTime = String.format("%02d:%02d", hour, minute)
                        },
                        10, 0, true // default 10:00
                    )
                    timePicker.show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (endTime.isEmpty()) "Select End Time" else "End: $endTime")
            }


            Button(
                onClick = {
                    if (selectedDate <= today) {
                        Toast.makeText(context, "Pick a date after today", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (selectedDate.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                        // Save these values into a temporary navigation route
                        if (selectedDate.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                            // Navigate to new screen that shows floor selection
                            navController.navigate("selectFloor/$selectedDate/$startTime/$endTime")
                        }

                    } else {
                        Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CONTINUE TO SLOT SELECTION")
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        Text("Not logged in.")
        return
    }
    val userId = currentUser.uid

    val userRef = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        .getReference("users").child(userId).child("vehicles")

    var vehicleList by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Fetch vehicle data
    LaunchedEffect(Unit) {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = mutableListOf<Triple<String, String, String>>()
                for (child in snapshot.children) {
                    val brand = child.child("brand").getValue(String::class.java) ?: ""
                    val model = child.child("model").getValue(String::class.java) ?: ""
                    val plate = child.child("plate").getValue(String::class.java) ?: ""
                    newList.add(Triple("$brand $model", model, plate))
                }
                vehicleList = newList
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
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
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("All Vehicles", style = MaterialTheme.typography.titleMedium)

            vehicleList.forEachIndexed { index, vehicle ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Car ${index + 1}", style = MaterialTheme.typography.titleSmall)
                        Text(vehicle.first) // Brand + Model
                        Text("Plate: ${vehicle.third}")
                    }
                }
            }

            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add Vehicle")
            }
        }

        // Add Vehicle Dialog
        if (showAddDialog) {
            var brand by remember { mutableStateOf("") }
            var model by remember { mutableStateOf("") }
            var plate by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Vehicle") },
                confirmButton = {
                    Button(onClick = {
                        val key = userRef.push().key ?: "vehicle${System.currentTimeMillis()}"
                        userRef.child(key).setValue(
                            mapOf("brand" to brand, "model" to model, "plate" to plate)
                        )
                        showAddDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") })
                        OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") })
                        OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Plate Number") })
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Parking, // for current-day slot view
        Screen.Details,
        Screen.Home,
        Screen.Calendar,
        Screen.Profile
    )

    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    when (screen) {
                        is Screen.Parking -> Icon(Icons.Default.DirectionsCar, contentDescription = "Parking")
                        is Screen.Details -> Icon(Icons.Default.Info, contentDescription = "Details")
                        is Screen.Home -> Icon(Icons.Default.Home, contentDescription = "Home")
                        is Screen.Calendar -> Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
                        is Screen.Profile -> Icon(Icons.Default.Person, contentDescription = "Profile")
                        is Screen.SignIn, is Screen.SignUp, is Screen.Greeting, is Screen.ParkingWithDate -> Spacer(modifier = Modifier.size(0.dp))
                    }

                },
                label = { Text(screen::class.simpleName ?: "") }
            )
        }
    }
}


sealed class Screen(val route: String) {
    object Greeting : Screen("greeting")
    object SignIn : Screen("signin")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Details : Screen("details")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object Parking : Screen("parking/{floor}") {
        fun createRoute(floor: String) = "parking/$floor"
    }
    object ParkingWithDate : Screen("parking/{floor}/{date}/{start}/{end}") {
        fun createRoute(floor: String, date: String, start: String, end: String) =
            "parking/$floor/$date/$start/$end"
    }

}

