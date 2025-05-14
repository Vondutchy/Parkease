package com.example.parkease.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkease.ui.components.ErrorMessage
import com.example.parkease.ui.components.LoadingButton
import com.example.parkease.ui.viewmodels.AuthViewModel

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onSignUpRedirect: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onSignInSuccess()
        }
    }

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
            Text("Don't have an account? Sign Up")
        }

        Spacer(modifier = Modifier.height(12.dp))

        LoadingButton(
            text = "Sign In",
            onClick = { viewModel.signIn(email, password) },
            isLoading = uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
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
fun SignUpScreen(
    onSignUpSuccess: () -> Unit,
    onSignInRedirect: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onSignUpSuccess()
        }
    }

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

        LoadingButton(
            text = "Sign Up",
            onClick = { viewModel.signUp(email, password) },
            isLoading = uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
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
fun GreetingScreen(
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
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