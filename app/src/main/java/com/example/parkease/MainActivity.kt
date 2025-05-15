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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import com.kizitonwose.calendar.compose.*
import com.kizitonwose.calendar.core.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.DayOfWeek
import androidx.compose.foundation.border
import com.google.android.gms.tasks.Task // Added this import
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind


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
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) { // Changed from authState to Unit to run once on app start
        if (currentUser != null) {
            // User is already signed in, update AuthViewModel state
            authViewModel.setAuthenticated(true)
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        } else if (!authState.isAuthenticated) {
            navController.navigate(Screen.Greeting.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    // This LaunchedEffect handles navigation when authState changes (e.g., after login/logout)
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && currentUser != null) {
            if (currentRoute == Screen.Greeting.route || currentRoute == Screen.SignIn.route || currentRoute == Screen.SignUp.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        } else if (!authState.isAuthenticated && currentUser == null) {
            // Only navigate to Greeting if not already there and user is truly logged out
            if (currentRoute != Screen.Greeting.route) {
                navController.navigate(Screen.Greeting.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        }
    }

    Log.d("ParkEaseApp_BottomBar", "isAuthenticated: ${authState.isAuthenticated}, currentRoute: $currentRoute, currentUser: ${currentUser?.uid}")

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
    var usernameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsState()

    LaunchedEffect(authState) {
        if (authState.isAuthenticated) {
            Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
            onSignUpSuccess()
        } else if (authState.error != null) {
            Toast.makeText(context, "Sign up failed: ${authState.error}", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen background image
        Image(
            painter = painterResource(id = R.drawable.signin_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Username field
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Username")
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Email field
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
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
            Spacer(modifier = Modifier.height(12.dp))
            // Phone field
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
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
            Spacer(modifier = Modifier.height(12.dp))
            // Password field
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
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
            Spacer(modifier = Modifier.height(12.dp))
            // Confirm Password field
            OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = { confirmPasswordInput = it },
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
                    if (usernameInput.isBlank()) {
                        Toast.makeText(context, "Please enter a username", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (phoneInput.isBlank()) {
                        Toast.makeText(context, "Please enter a phone number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (phoneInput.length != 11) {
                        Toast.makeText(context, "Phone number must be exactly 11 digits", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (passwordInput != confirmPasswordInput) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (passwordInput.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    authViewModel.signUp(emailInput, passwordInput, usernameInput, phoneInput)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !authState.isLoading
            ) {
                if (authState.isLoading) {
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
}

@Composable
fun SignInScreen(onSignInSuccess: () -> Unit, onSignUpClick: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen background image
        Image(
            painter = painterResource(id = R.drawable.signin_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Main content
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
}

@Composable
fun GreetingScreen(onSignInClick: () -> Unit, onSignUpClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Full-screen background image
        Image(
            painter = painterResource(id = R.drawable.greetings_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = onSignInClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(2.dp, Color.Black)
            ) {
                Text("SIGN IN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSignUpClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(2.dp, Color.Black)
            ) {
                Text("SIGN UP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(180.dp)) // Move buttons up from the bottom
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val user = FirebaseAuth.getInstance().currentUser
    val userId = user?.uid ?: return
    val context = LocalContext.current
    var userName by remember { mutableStateOf("User") }
    var displayedReservations by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    val customButtonColor = Color(0xFF3B82F6)
    val whiteCardColor = Color.White
    val blackTextColor = Color.Black

    LaunchedEffect(userId) {
        Log.d("HomeScreen_User", "Attempting to fetch username for userId: $userId")
        val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
        db.getReference("users").child(userId).child("username")
            .get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getValue(String::class.java)
                if (!name.isNullOrEmpty()) userName = name
            }
            .addOnFailureListener { exception ->
                Log.e("HomeScreen_User", "Failed to fetch username: ${exception.message}")
            }
    }

    LaunchedEffect(Unit) {
        checkAndUpdateExpiredReservations()
        while (true) {
            delay(3000)
            checkAndUpdateExpiredReservations()
        }
    }

    // Fetch and process reservations
    LaunchedEffect(userId) {
        if (userId != null) {
            val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
            val reservationRoot = db.getReference("reservations").child(userId)

            reservationRoot.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = Calendar.getInstance()
                    val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.time)
                    
                    val newReservationsList = mutableListOf<Map<String, String>>()
                    var todayReservation: Map<String, String>? = null
                    var earliestFutureReservation: Map<String, String>? = null

                    for (dateSnapshot in snapshot.children) {
                        val dateKey = dateSnapshot.key ?: continue
                        val reservationDetails = mapOf(
                            "plate" to (dateSnapshot.child("plate").getValue(String::class.java) ?: ""),
                            "floor" to (dateSnapshot.child("floor").getValue(String::class.java) ?: ""),
                            "slotId" to (dateSnapshot.child("slotId").getValue(String::class.java) ?: ""),
                            "startTime" to (dateSnapshot.child("startTime").getValue(String::class.java) ?: ""),
                            "endTime" to (dateSnapshot.child("endTime").getValue(String::class.java) ?: ""),
                            "date" to dateKey
                        )

                        if (dateKey == currentDateStr) {
                            todayReservation = reservationDetails
                        } else if (dateKey > currentDateStr) {
                            if (earliestFutureReservation == null || dateKey < earliestFutureReservation!!["date"]!!) {
                                earliestFutureReservation = reservationDetails
                            }
                        }
                    }

                    todayReservation?.let { newReservationsList.add(it) }
                    earliestFutureReservation?.let { newReservationsList.add(it) } // Simplified addition
                    
                    // Ensure no duplicates by date
                    displayedReservations = newReservationsList.distinctBy { it["date"] } 
                    Log.d("HomeScreen_Res", "Displayed Reservations: $displayedReservations")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load reservations: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("HomeScreen_Res", "Failed to load reservations: ${error.message}")
                }
            })
        }
    }

    var showCancelDialog by remember { mutableStateOf(false) }
    var reservationToCancelDate by remember { mutableStateOf<String?>(null) }

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

    if (showCancelDialog && reservationToCancelDate != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        cancelReservation(reservationToCancelDate!!)
                        showCancelDialog = false
                        reservationToCancelDate = null
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.home_bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Home", style = MaterialTheme.typography.headlineMedium)
            Text("Good Day, $userName!", style = MaterialTheme.typography.titleMedium)

            if (displayedReservations.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { displayedReservations.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                    // consider adding .height(IntrinsicSize.Min) or a fixed height for the pager if cards vary
                ) { pageIndex ->
                    val reservation = displayedReservations[pageIndex]
                    val plate = reservation["plate"] ?: ""
                    val floor = reservation["floor"] ?: ""
                    val slotId = reservation["slotId"] ?: ""
                    val start = reservation["startTime"] ?: ""
                    val end = reservation["endTime"] ?: ""
                    val date = reservation["date"] ?: ""

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp) // Optional: add some spacing between paged items if visible
                            .clickable {
                                reservationToCancelDate = date
                                showCancelDialog = true
                            },
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = whiteCardColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = plate, style = MaterialTheme.typography.titleLarge, color = blackTextColor)
                                Text(text = "${floor.uppercase()} • $slotId", color = blackTextColor.copy(alpha = 0.8f))
                                Text(text = "Date: $date", style = MaterialTheme.typography.bodySmall, color = blackTextColor.copy(alpha = 0.7f))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "$start–$end", style = MaterialTheme.typography.titleMedium, color = blackTextColor)
                                Text(text = "reserved", style = MaterialTheme.typography.bodySmall, color = blackTextColor.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
                // Optional: Add PagerIndicator if more than one page
                if (pagerState.pageCount > 1) {
                    Row(
                        Modifier
                            .height(20.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(pagerState.pageCount) { iteration ->
                            val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .background(color, CircleShape)
                                    .size(8.dp)
                            )
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = whiteCardColor)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Make a reservation now",
                            style = MaterialTheme.typography.titleMedium,
                            color = blackTextColor
                        )
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            modifier = Modifier.padding(top = 8.dp),
                            tint = blackTextColor
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

                    // Determine card color based on index
                    val cardColor = if (index == 1 || index == 2) whiteCardColor else customButtonColor
                    val textColor = if (index == 1 || index == 2) blackTextColor else Color.White

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable {
                                navController.navigate(Screen.Parking.createRoute(floorKey))
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(floorLabel, style = MaterialTheme.typography.bodyLarge, color = textColor)
                            Text("$slotCount", style = MaterialTheme.typography.displayMedium, color = textColor)
                            Text("Slots Left", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
                        }
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

@Composable
fun DashedSlotBox(
    slotId: String,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFD32F2F)
    val backgroundColor = if (isAvailable) Color(0xFF4CAF50) else Color.White
    val textColor = if (isAvailable) Color.White else Color.Black
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .drawBehind {
                val strokeWidth = 3.dp.toPx()
                val dashWidth = 10.dp.toPx()
                val dashGap = 6.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    size = size,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .aspectRatio(1f)
    ) {
        Text(slotId, color = textColor, style = MaterialTheme.typography.titleMedium)
    }
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

    // Effect for initial slot check and initialization (runs once per floor/date combination)
    LaunchedEffect(floor, date) {
        Log.d("ParkingScreenWithDate", "Initial check/init effect for floor: $floor, date: $date")
        dbRef.get().addOnSuccessListener { snapshot ->
            var needsInitialization = true
            for (slot in snapshot.children) {
                if (slot.child(date).exists()) {
                    needsInitialization = false
                    break
                }
            }
            if (needsInitialization) {
                Log.d("SLOTS_INIT", "Initializing slots for $floor on $date")
                for (i in 1..10) {
                    val slotId = "slot$i"
                    dbRef.child(slotId).child(date).child("status").setValue("available")
                        .addOnSuccessListener {
                            Log.d("SLOTS_INIT", "✅ Initialized $slotId for $date as available")
                        }
                        .addOnFailureListener { error ->
                            Log.e("SLOTS_INIT", "❌ Failed to initialize $slotId for $date: ${error.message}")
                        }
                }
            }
        }.addOnFailureListener {
            Log.e("ParkingScreenWithDate", "Failed to get initial slot data for $floor, $date: ${it.message}")
        }
    }

    // Effect for setting up and tearing down the Firebase listener
    DisposableEffect(floor, date) {
        Log.d("ParkingScreenWithDate", "DisposableEffect for listener on floor: $floor, date: $date")
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                for (slot in snapshot.children) {
                    val status = slot.child(date).child("status").getValue(String::class.java) ?: "available"
                    map[slot.key ?: ""] = status
                    Log.d("SLOT_STATUS", "Slot ${slot.key} for date $date: $status")
                }
                slotMap = map
                Log.d("ParkingScreenWithDate", "slotMap updated for date $date with ${map.size} entries")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load slots: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("ParkingScreenWithDate", "Firebase listener cancelled for $floor, $date: ${error.message}")
            }
        }

        dbRef.addValueEventListener(valueEventListener)
        Log.d("ParkingScreenWithDate", "Added listener for $floor, $date in DisposableEffect")

        onDispose {
            dbRef.removeEventListener(valueEventListener)
            Log.d("ParkingScreenWithDate", "Removed listener for $floor, $date in DisposableEffect onDispose")
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable(enabled = isAvailable) { selectedSlot = slotId }
                        ) {
                            DashedSlotBox(slotId = slotId, isAvailable = isAvailable, modifier = Modifier.fillMaxSize())
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable(enabled = isAvailable) { selectedSlot = slotId }
                        ) {
                            DashedSlotBox(slotId = slotId, isAvailable = isAvailable, modifier = Modifier.fillMaxSize())
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

fun cancelReservation(date: String) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseDatabase.getInstance("https://parkease-662e2-default-rtdb.asia-southeast1.firebasedatabase.app")
    val reservationRef = db.getReference("reservations").child(userId).child(date)

    reservationRef.get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            val floor = snapshot.child("floor").getValue(String::class.java)
            val slotId = snapshot.child("slotId").getValue(String::class.java)

            if (!floor.isNullOrEmpty() && !slotId.isNullOrEmpty()) {
                // Update slot status to available
                val slotRef = db.getReference("slots").child(floor).child(slotId).child(date).child("status")
                slotRef.setValue("available")
                    .addOnSuccessListener {
                        Log.d("CANCEL_RESERVATION", "✅ Slot marked as available: $floor/$slotId/$date")
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
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var startTime by remember { mutableStateOf("7:00 AM") }
    var endTime by remember { mutableStateOf("12:00 PM") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val currentMonth = remember { YearMonth.now() }
    val firstMonth = remember { currentMonth.minusMonths(12) }
    val lastMonth = remember { currentMonth.plusMonths(12) }
    val daysOfWeek = remember { daysOfWeek() }

    Box(modifier = Modifier.fillMaxSize()) { // Outer Box for background
        Image(
            painter = painterResource(id = R.drawable.calendar_bg),
            contentDescription = "Calendar Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CALENDAR") },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.Profile.route) }) { // Navigate to profile
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Make TopAppBar transparent
                        titleContentColor = Color.Black, // Adjust if text is not visible
                        actionIconContentColor = Color.Black // Adjust if icon is not visible
                    )
                )
            },
            containerColor = Color.Transparent // Make Scaffold container transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))
                Text("Schedule your Booking Now!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(8.dp))

                // Real Calendar Widget
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp)) // Slightly transparent white
                        .border(2.dp, Color(0xFF3B82F6), shape = RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    CalendarView(
                        firstMonth = firstMonth,
                        lastMonth = lastMonth,
                        currentMonth = currentMonth,
                        daysOfWeek = daysOfWeek,
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Time selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { showStartTimePicker = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White)
                    ) {
                        Text(startTime)
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = "to", tint = Color.Black, modifier = Modifier.padding(horizontal = 8.dp))
                    Button(
                        onClick = { showEndTimePicker = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White)
                    ) {
                        Text(endTime)
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        navController.navigate("selectFloor/${selectedDate.toString()}/$startTime/$endTime")
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6), contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("CONTINUE TO SLOT SELECTION", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Time pickers
    if (showStartTimePicker) {
        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                val ampm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour == 0 || hour == 12) 12 else hour % 12
                startTime = String.format("%d:%02d %s", hour12, minute, ampm)
                showStartTimePicker = false
            },
            7, 0, false
        ).show()
    }
    if (showEndTimePicker) {
        TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                val ampm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour == 0 || hour == 12) 12 else hour % 12
                endTime = String.format("%d:%02d %s", hour12, minute, ampm)
                showEndTimePicker = false
            },
            12, 0, false
        ).show()
    }
}

@Composable
fun CalendarView(
    firstMonth: YearMonth,
    lastMonth: YearMonth,
    currentMonth: YearMonth,
    daysOfWeek: List<DayOfWeek>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val calendarState = rememberCalendarState(
        startMonth = firstMonth,
        endMonth = lastMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )
    val today = remember { LocalDate.now() }
    val minSelectableDate = remember { today.plusDays(1) } // Only allow dates after today

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Month header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3B82F6)),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = calendarState.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase() +
                        " " + calendarState.firstVisibleMonth.yearMonth.year,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Calendar grid
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                val isSelected = day.date == selectedDate
                val isDisabled = day.date.isBefore(minSelectableDate)
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp)
                        .background(
                            when {
                                isSelected -> Color(0xFF3B82F6)
                                else -> Color.Transparent
                            },
                            shape = CircleShape
                        )
                        .then(
                            if (!isDisabled) Modifier.clickable { onDateSelected(day.date) } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        color = when {
                            isDisabled -> Color.LightGray
                            isSelected -> Color.White
                            else -> Color.Black
                        }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        // Potentially navigate to Greeting or SignIn, or show a more prominent message
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Greeting.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
        return // Early return to prevent flicker or errors if currentUser is null
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
    LaunchedEffect(userId) { // Changed key to userId to refetch if it changes (though unlikely in this screen)
        Log.d("ProfileScreen_User", "Attempting to fetch username for userId: $userId")
        userRef.child("username").get().addOnSuccessListener { snapshot ->
            val name = snapshot.getValue(String::class.java)
            Log.d("ProfileScreen_User", "Fetched name from Firebase: '$name'")
            if (!name.isNullOrEmpty()) {
                userName = name
            } else {
                // If username is not set in DB, try to use part of email or a default
                userName = currentUser.email?.split("@")?.get(0) ?: "User"
            }
            isLoading = false
        }.addOnFailureListener { exception ->
            Log.e("ProfileScreen_User", "Failed to fetch username: ${exception.message}")
            userName = currentUser.email?.split("@")?.get(0) ?: "User" // Fallback on error
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

            override fun onCancelled(error: DatabaseError) {
                 Log.e("ProfileScreen_Vehicles", "Failed to fetch vehicles: ${error.message}")
            }
        })

        userRef.child("notifications").get().addOnSuccessListener { snapshot ->
            notificationsEnabled = snapshot.getValue(Boolean::class.java) ?: true
        }.addOnFailureListener {
            Log.e("ProfileScreen_Notifs", "Failed to fetch notification settings: ${it.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) { // Outer Box for background
        Image(
            painter = painterResource(id = R.drawable.calendar_bg), // Using calendar_bg as requested
            contentDescription = "Profile Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Make TopAppBar transparent
                        titleContentColor = Color.Black, // Adjust if text is not visible
                        navigationIconContentColor = Color.Black // Adjust if icon is not visible
                    )
                )
            },
            containerColor = Color.Transparent // Make Scaffold container transparent
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding), // Apply padding here too
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)) // Slightly transparent
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
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(onClick = { showEditNameDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color.Black)
                                }
                            }
                        }
                    }

                    // Notifications Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                         colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)) // Slightly transparent
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
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black
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
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF3B82F6),
                                    checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    // Vehicles Section
                    Text("Vehicles", style = MaterialTheme.typography.titleMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                    vehicleList.forEachIndexed { index, vehicle ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)) // Slightly transparent
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Car ${index + 1}", style = MaterialTheme.typography.titleSmall, color = Color.DarkGray)
                                Text(vehicle.first, color = Color.Black) // Brand + Model
                                Text("Plate: ${vehicle.third}", color = Color.Black)
                            }
                        }
                    }

                    Button( // Changed from OutlinedButton to Button
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6), // Signature blue background
                            contentColor = Color.White // White text for contrast
                        )
                    ) {
                        Text("+ Add Vehicle")
                    }

                    // Logout Button
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F) // A more distinct red for logout
                        )
                    ) {
                        Text("Log Out")
                    }
                }
            }
        }
        // ... (Dialogs: Edit Name, Add Vehicle, Logout Confirmation remain the same) ...
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
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                userRef.child("username").setValue(newName)
                                userName = newName
                                Toast.makeText(context, "Name updated", Toast.LENGTH_SHORT).show()
                            }
                            showEditNameDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)) // Signature blue
                    ) {
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
                    Button(
                        onClick = {
                            val key = userRef.child("vehicles").push().key ?: "vehicle${System.currentTimeMillis()}"
                            userRef.child("vehicles").child(key).setValue(
                                mapOf("brand" to brand, "model" to model, "plate" to plate)
                            )
                            showAddDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)) // Signature blue
                    ) {
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
            val authViewModel: AuthViewModel = viewModel() // Get ViewModel for logout
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    Button(
                        onClick = {
                            authViewModel.signOut() // Use ViewModel to handle signout and state update
                            // Navigation is now handled by LaunchedEffect in ParkEaseApp watching authState
                            showLogoutDialog = false // Close dialog
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
    val customColor = Color(0xFF3B82F6)

    NavigationBar(containerColor = customColor) {
        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    val iconColor = Color.White
                    when (screen) {
                        is Screen.Home -> Icon(Icons.Default.Home, contentDescription = "Home", tint = iconColor)
                        is Screen.Calendar -> Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = iconColor)
                        is Screen.Profile -> Icon(Icons.Default.Person, contentDescription = "Profile", tint = iconColor)
                        else -> Spacer(modifier = Modifier.size(0.dp))
                    }
                },
                label = { Text(screen::class.simpleName ?: "", color = Color.White) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.7f),
                    indicatorColor = Color.White.copy(alpha = 0.2f)
                )
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

