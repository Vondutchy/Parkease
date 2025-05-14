package com.example.parkease

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.parkease.navigation.Screen
import com.example.parkease.ui.components.*
import com.example.parkease.ui.screens.*
import com.example.parkease.ui.theme.ParkEaseTheme
import com.example.parkease.ui.viewmodels.AuthViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        enableEdgeToEdge()
        setContent {
            ParkEaseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParkEaseApp()
                }
            }
        }
    }
}

@Composable
fun ParkEaseApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(authState) {
        if (!authState.isAuthenticated) {
            navController.navigate(Screen.Greeting.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (authState.isAuthenticated && 
                currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Calendar.route,
                    Screen.Profile.route
                )
            ) {
                BottomNavigationBar(navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Greeting.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Greeting.route) {
                GreetingScreen(
                    onSignInClick = { navController.navigate(Screen.SignIn.route) },
                    onSignUpClick = { navController.navigate(Screen.SignUp.route) }
                )
            }

            composable(Screen.SignIn.route) {
                SignInScreen(
                    onSignInSuccess = { 
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Greeting.route) {
                                inclusive = true
                            }
                        }
                    },
                    onSignUpClick = { navController.navigate(Screen.SignUp.route) }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Greeting.route) {
                                inclusive = true
                            }
                        }
                    },
                    onSignInClick = { navController.navigate(Screen.SignIn.route) }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(navController)
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(navController)
            }

            composable(
                route = "selectFloor/{date}/{startTime}/{endTime}",
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType },
                    navArgument("startTime") { type = NavType.StringType },
                    navArgument("endTime") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val startTime = backStackEntry.arguments?.getString("startTime") ?: ""
                val endTime = backStackEntry.arguments?.getString("endTime") ?: ""
                FloorSelectionScreen(navController, date, startTime, endTime)
            }

            composable(
                route = Screen.Parking.route,
                arguments = listOf(
                    navArgument("floor") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                ParkingScreen(
                    navController = navController,
                    floor = backStackEntry.arguments?.getString("floor") ?: "1"
                )
            }

            composable(
                route = "parkingFixed/{floor}/{date}/{startTime}/{endTime}",
                arguments = listOf(
                    navArgument("floor") { type = NavType.StringType },
                    navArgument("date") { type = NavType.StringType },
                    navArgument("startTime") { type = NavType.StringType },
                    navArgument("endTime") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val floor = backStackEntry.arguments?.getString("floor") ?: "1"
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val startTime = backStackEntry.arguments?.getString("startTime") ?: ""
                val endTime = backStackEntry.arguments?.getString("endTime") ?: ""
                ParkingScreenWithDatePreSelected(
                    floor = floor,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    navController = navController
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(navController)
            }
        }
    }
}

@Composable
fun SignUpScreen(onSignUpSuccess: () -> Unit, onSignInClick: () -> Unit) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = "Username")
            }
        )

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        // Phone field
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = "Phone")
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            )
        )

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password")
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        // Confirm Password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
            },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (username.isBlank()) {
                    Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (phone.isBlank()) {
                    Toast.makeText(context, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.length < 6) {
                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                authViewModel.signUp(email, password)
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    // Save additional user info to Firebase Database
                    val userRef = db.getReference("users").child(userId)
                    val userData = mapOf(
                        "username" to username,
                        "email" to email,
                        "phone" to phone
                    )
                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            onSignUpSuccess()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        TextButton(onClick = onSignInClick) {
            Text("Already have an account? Sign In")
        }
    }
}

@Composable
fun SignInScreen(onSignInSuccess: () -> Unit, onSignUpClick: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = "Email")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = "Password")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSignUpClick) {
            Text("Don't have an account? Sign Up")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isLoading = true
                authViewModel.signIn(email, password)
                onSignInSuccess()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign In")
            }
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
    val userId = user?.uid ?: return
    val context = LocalContext.current
    var userName by remember { mutableStateOf("User") }
    var reservationData by remember { mutableStateOf<Map<String, String>?>(null) }

    // Fetch username
    LaunchedEffect(userId) {
        val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        db.getReference("users").child(userId).child("username")
            .get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getValue(String::class.java)
                if (!name.isNullOrEmpty()) {
                    userName = name
                }
            }
    }

    // Check for expired reservations when the screen loads and every 3 seconds
    LaunchedEffect(Unit) {
        checkAndUpdateExpiredReservations()
        while(true) {
            delay(3000) // Check every 3 seconds
            checkAndUpdateExpiredReservations()
        }
    }

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

                        if (dateKey == currentDate) {
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

    // Initialize and track slot counts
    LaunchedEffect(Unit) {
        val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        val floors = listOf("floor1", "floor2", "floor3", "floor4")
        val mutableCounts = mutableMapOf<String, Int>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        for (floor in floors) {
            db.getReference("slots").child(floor)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var count = 0
                        var needsInitialization = true

                        // First check if any slots exist for today
                        for (slot in snapshot.children) {
                            val todayNode = slot.child(today)
                            if (todayNode.exists()) {
                                needsInitialization = false
                                val status = todayNode.child("status").getValue(String::class.java)
                                if (status == "available") count++
                            }
                        }

                        // Initialize slots for today if they don't exist
                        if (needsInitialization) {
                            Log.d("SLOT_INIT", "Initializing slots for $floor on $today")
                            for (i in 1..10) {
                                val slotId = "slot$i"
                                db.getReference("slots")
                                    .child(floor)
                                    .child(slotId)
                                    .child(today)
                                    .child("status")
                                    .setValue("available")
                            }
                            count = 10 // All slots are available after initialization
                        }

                        mutableCounts[floor] = count
                        floorCounts = mutableCounts.toMap()
                        Log.d("SLOT_COUNT", "Updated $floor count to $count")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FLOOR_COUNTS", "Database error: ${error.message}")
                    }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Home", style = MaterialTheme.typography.headlineMedium)
        Text("Good Day, $userName!", style = MaterialTheme.typography.titleMedium)

        // Reservation Card
        if (reservationData != null) {
            val plate = reservationData!!["plate"] ?: ""
            val floor = reservationData!!["floor"] ?: ""
            val slotId = reservationData!!["slotId"] ?: ""
            val start = reservationData!!["startTime"] ?: ""
            val end = reservationData!!["endTime"] ?: ""

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
        } else {
            // No reservation message
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Calendar.route) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Make a reservation now",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Calendar",
                        modifier = Modifier.padding(top = 8.dp)
                    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorSelectionScreen(navController: NavHostController, date: String, startTime: String, endTime: String) {
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
                        navController.navigate("parkingFixed/$floor/$date/$startTime/$endTime")
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

    // Initialize slots for the selected date if they don't exist
    LaunchedEffect(floor, date) {
        // First, check if slots exist for this date
        dbRef.get().addOnSuccessListener { snapshot ->
            var needsInitialization = true
            
            // Check if any slot has data for this date
            for (slot in snapshot.children) {
                if (slot.child(date).exists()) {
                    needsInitialization = false
                    break
                }
            }

            if (needsInitialization) {
                Log.d("SLOTS_INIT", "Initializing slots for $floor on $date")
                // Initialize all slots as available
                for (i in 1..10) {
                    val slotId = "slot$i"
                    dbRef.child(slotId).child(date).child("status").setValue("available")
                        .addOnSuccessListener {
                            Log.d("SLOTS_INIT", "✅ Initialized $slotId as available")
                        }
                        .addOnFailureListener { error ->
                            Log.e("SLOTS_INIT", "❌ Failed to initialize $slotId: ${error.message}")
                        }
                }
            }

            // Set up real-time listener for slot status
            dbRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val map = mutableMapOf<String, String>()
                    for (slot in snapshot.children) {
                        val status = slot.child(date).child("status").getValue(String::class.java) ?: "available"
                        map[slot.key ?: ""] = status
                        Log.d("SLOT_STATUS", "Slot ${slot.key}: $status")
                    }
                    slotMap = map
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load slots: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
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
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
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
    val slotRef = db.getReference("slots").child(floor).child(slotId).child(date).child("status")

    // First check if the slot is still available
    slotRef.get().addOnSuccessListener { snapshot ->
        val currentStatus = snapshot.getValue(String::class.java)
        if (currentStatus != "available") {
            Toast.makeText(context, "This slot is no longer available", Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }

        // Check for existing reservation
        reservationRef.get().addOnSuccessListener { reservationSnapshot ->
            val existingDate = reservationSnapshot.child("date").getValue(String::class.java)
            if (reservationSnapshot.exists() && existingDate == date) {
                Toast.makeText(context, "You already have a reservation for today!", Toast.LENGTH_SHORT).show()
            } else {
                // Create the reservation first
                val reservationData = mapOf(
                    "floor" to floor,
                    "slotId" to slotId,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "plate" to plate,
                    "date" to date
                )

                reservationRef.setValue(reservationData)
                    .addOnSuccessListener {
                        Log.d("RESERVE_SLOT", "✅ Reservation created successfully")
                        // Then update the slot status
                        slotRef.setValue("reserved")
                            .addOnSuccessListener {
                                Log.d("RESERVE_SLOT", "✅ Slot marked as reserved: $floor/$slotId/$date")
                                Toast.makeText(context, "Reservation successful!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Log.e("RESERVE_SLOT", "❌ Failed to update slot status: ${it.message}")
                                // If slot status update fails, delete the reservation
                                reservationRef.removeValue()
                                Toast.makeText(context, "Failed to complete reservation", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Log.e("RESERVE_SLOT", "❌ Failed to create reservation: ${it.message}")
                        Toast.makeText(context, "Failed to create reservation", Toast.LENGTH_SHORT).show()
                    }
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

            if (!floor.isNullOrEmpty() && !slotId.isNullOrEmpty()) {
                // Update slot status to available
                val slotRef = db.getReference("slots").child(floor).child(slotId).child(todayDate).child("status")
                slotRef.setValue("available")
                    .addOnSuccessListener {
                        Log.d("CANCEL_RESERVATION", "✅ Slot marked as available: $floor/$slotId/$todayDate")
                        // Only remove the reservation after successfully updating the slot status
                        reservationRef.removeValue()
                            .addOnSuccessListener {
                                Log.d("CANCEL_RESERVATION", "✅ Reservation removed successfully")
                            }
                            .addOnFailureListener {
                                Log.e("CANCEL_RESERVATION", "❌ Failed to remove reservation: ${it.message}")
                            }
                    }
                    .addOnFailureListener {
                        Log.e("CANCEL_RESERVATION", "❌ Failed to update slot status: ${it.message}")
                    }
            }
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
        .getReference("users").child(userId)

    var vehicleList by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf(currentUser.email ?: "") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch user data
    LaunchedEffect(Unit) {
        userRef.child("username").get().addOnSuccessListener { snapshot ->
            val name = snapshot.getValue(String::class.java)
            if (!name.isNullOrEmpty()) {
                userName = name
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }

        userRef.child("vehicles").addValueEventListener(object : ValueEventListener {
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

        userRef.child("notifications").get().addOnSuccessListener { snapshot ->
            notificationsEnabled = snapshot.getValue(Boolean::class.java) ?: true
        }
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
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showEditNameDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Name")
                            }
                        }
                    }
                }

                // Notifications Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                notificationsEnabled = enabled
                                userRef.child("notifications").setValue(enabled)
                                Toast.makeText(
                                    context,
                                    if (enabled) "Notifications enabled" else "Notifications disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                // Vehicles Section
                Text("Vehicles", style = MaterialTheme.typography.titleMedium)
                vehicleList.forEachIndexed { index, vehicle ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
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

                // Logout Button
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Log Out")
                }
            }
        }

        // Edit Name Dialog
        if (showEditNameDialog) {
            var newName by remember { mutableStateOf(userName) }
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = { Text("Edit Name") },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotBlank()) {
                            userRef.child("username").setValue(newName)
                            userName = newName
                            Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                        }
                        showEditNameDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Add Vehicle Dialog
        if (showAddDialog) {
            var brand by remember { mutableStateOf("") }
            var model by remember { mutableStateOf("") }
            var plate by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Vehicle") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand") }
                        )
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model") }
                        )
                        OutlinedTextField(
                            value = plate,
                            onValueChange = { plate = it },
                            label = { Text("Plate Number") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val key = userRef.child("vehicles").push().key ?: "vehicle${System.currentTimeMillis()}"
                        userRef.child("vehicles").child(key).setValue(
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
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate(Screen.Greeting.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Log Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
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
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            // Pop up to the start destination
                            popUpTo(Screen.Home.route) {
                                inclusive = false
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    when (screen) {
                        is Screen.Home -> Icon(Icons.Default.Home, contentDescription = "Home")
                        is Screen.Calendar -> Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
                        is Screen.Profile -> Icon(Icons.Default.Person, contentDescription = "Profile")
                        else -> Spacer(modifier = Modifier.size(0.dp))
                    }
                },
                label = { Text(screen::class.simpleName ?: "") }
            )
        }
    }
}

fun checkAndUpdateExpiredReservations() {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
    val reservationRef = db.getReference("reservations").child(userId)
    val now = Calendar.getInstance()
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)

    Log.d("EXPIRE_CHECK", "=== Starting Expiration Check ===")
    Log.d("EXPIRE_CHECK", "Current Time: $currentTime")
    Log.d("EXPIRE_CHECK", "Current Date: $currentDate")

    // Force a fresh read from the database
    reservationRef.get().addOnSuccessListener { snapshot ->
        if (!snapshot.exists()) {
            Log.d("EXPIRE_CHECK", "No reservations found in database")
            return@addOnSuccessListener
        }

        Log.d("EXPIRE_CHECK", "Found ${snapshot.childrenCount} reservations")
        
        for (dateSnapshot in snapshot.children) {
            val dateKey = dateSnapshot.key ?: continue
            val endTime = dateSnapshot.child("endTime").getValue(String::class.java) ?: continue
            val floor = dateSnapshot.child("floor").getValue(String::class.java) ?: continue
            val slotId = dateSnapshot.child("slotId").getValue(String::class.java) ?: continue

            Log.d("EXPIRE_CHECK", "=== Checking Reservation ===")
            Log.d("EXPIRE_CHECK", "Date: $dateKey")
            Log.d("EXPIRE_CHECK", "End Time: $endTime")
            Log.d("EXPIRE_CHECK", "Floor: $floor")
            Log.d("EXPIRE_CHECK", "Slot: $slotId")

            // Compare times directly
            if (dateKey == currentDate) {
                Log.d("EXPIRE_CHECK", "Same day reservation - comparing times")
                Log.d("EXPIRE_CHECK", "Current Time: $currentTime")
                Log.d("EXPIRE_CHECK", "End Time: $endTime")
                
                if (currentTime > endTime) {
                    Log.d("EXPIRE_CHECK", "RESERVATION EXPIRED - Removing now")
                    
                    // First update the slot status
                    db.getReference("slots")
                        .child(floor)
                        .child(slotId)
                        .child(dateKey)
                        .child("status")
                        .setValue("available")
                        .addOnSuccessListener {
                            Log.d("EXPIRE_CHECK", "✅ Slot status updated to available")
                            
                            // Then remove the reservation
                            reservationRef.child(dateKey).removeValue()
                                .addOnSuccessListener {
                                    Log.d("EXPIRE_CHECK", "✅ Reservation removed successfully")
                                    // Force a refresh of the UI
                                    reservationRef.get()
                                }
                                .addOnFailureListener { error ->
                                    Log.e("EXPIRE_CHECK", "❌ Failed to remove reservation: ${error.message}")
                                }
                        }
                        .addOnFailureListener { error ->
                            Log.e("EXPIRE_CHECK", "❌ Failed to update slot status: ${error.message}")
                        }
                } else {
                    Log.d("EXPIRE_CHECK", "Reservation still active - Current time not past end time")
                }
            } else {
                Log.d("EXPIRE_CHECK", "Different day reservation - skipping")
            }
        }
    }.addOnFailureListener { error ->
        Log.e("EXPIRE_CHECK", "Failed to read reservations: ${error.message}")
    }
}

