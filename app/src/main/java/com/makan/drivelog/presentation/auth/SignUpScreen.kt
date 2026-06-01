package com.makan.drivelog.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.presentation.theme.TealPrimary

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = authState is AuthViewModel.AuthUiState.Loading
    val error = (authState as? AuthViewModel.AuthUiState.Error)?.message
    val activity = LocalContext.current as Activity

    var displayName by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }

    val specialChars = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
    val hasMin10     = password.length >= 10
    val hasUpper     = password.any { it.isUpperCase() }
    val hasSpecial   = password.any { it in specialChars }
    val passwordValid = hasMin10 && hasUpper && hasSpecial

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        Icon(Icons.Filled.DirectionsCar, contentDescription = null,
            modifier = Modifier.size(56.dp), tint = TealPrimary)
        Spacer(Modifier.height(8.dp))
        Text("DriveLog", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = TealPrimary)
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = displayName, onValueChange = { displayName = it; viewModel.clearError() },
            label = { Text("Display name") },
            singleLine = true, enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it; viewModel.clearError() },
            label = { Text("Email") },
            singleLine = true, enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Password") },
            singleLine = true, enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(), colors = authFieldColors()
        )
        Spacer(Modifier.height(6.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            PasswordRule("At least 10 characters", hasMin10)
            PasswordRule("At least one uppercase letter", hasUpper)
            PasswordRule("At least one special character (!@#\$…)", hasSpecial)
        }
        Spacer(Modifier.height(8.dp))

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.signUpWithEmail(email, password, displayName) },
            enabled = !isLoading && displayName.isNotBlank() && email.isNotBlank() && passwordValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Create account", color = Color.White, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onNavigateToLogin, enabled = !isLoading) {
            Text("Already have an account? Sign in",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text("  or  ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = { googleSignInLauncher.launch(viewModel.getGoogleSignInIntent(activity)) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            GoogleIcon()
            Spacer(Modifier.width(8.dp))
            Text("Continue with Google", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PasswordRule(text: String, met: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = if (met) "✓" else "·",
            fontSize = 12.sp,
            color = if (met) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (met) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
