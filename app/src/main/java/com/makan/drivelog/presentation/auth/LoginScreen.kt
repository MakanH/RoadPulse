package com.makan.drivelog.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.presentation.theme.TealPrimary
import androidx.compose.ui.text.LinkAnnotation
import kotlinx.coroutines.launch

internal const val TERMS_URL   = "https://makan.github.io/drivelog/terms-of-service.html"
internal const val PRIVACY_URL = "https://makan.github.io/drivelog/privacy-policy.html"

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = authState is AuthViewModel.AuthUiState.Loading
    val error = (authState as? AuthViewModel.AuthUiState.Error)?.message
    val activity = LocalContext.current as Activity
    val scope    = rememberCoroutineScope()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            value = email, onValueChange = { email = it; viewModel.clearError() },
            label = { Text("Email") },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = authFieldColors()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it; viewModel.clearError() },
            label = { Text("Password") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = authFieldColors()
        )
        Spacer(Modifier.height(8.dp))

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.signInWithEmail(email, password) },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Sign in", color = Color.White, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onNavigateToSignUp, enabled = !isLoading) {
            Text("Don't have an account? Sign up",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant)
            Text("  or  ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    googleSignInLauncher.launch(viewModel.getGoogleSignInIntent(activity))
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = !isLoading)
        ) {
            GoogleIcon()
            Spacer(Modifier.width(8.dp))
            Text("Continue with Google", fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(24.dp))
        LegalFooterLinks()
        Spacer(Modifier.height(16.dp))
    }
}

// ── Informational footer links (Login only) ───────────────────────────────────

@Composable
private fun LegalFooterLinks() {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = muted, textDecoration = TextDecoration.Underline, fontSize = 11.sp)
    )
    Text(
        text = buildAnnotatedString {
            pushLink(LinkAnnotation.Url(url = PRIVACY_URL, styles = linkStyle))
            append("Privacy Policy")
            pop()
            withStyle(SpanStyle(color = muted, fontSize = 11.sp)) { append("  ·  ") }
            pushLink(LinkAnnotation.Url(url = TERMS_URL, styles = linkStyle))
            append("Terms of Service")
            pop()
        },
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

// ── Shared composables used by SignUpScreen ───────────────────────────────────

@Composable
internal fun TosCheckboxRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = TealPrimary,
            textDecoration = TextDecoration.Underline,
            fontSize = 13.sp
        )
    )
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = secondary, fontSize = 13.sp)) { append("I agree to the ") }
        pushLink(LinkAnnotation.Url(url = TERMS_URL, styles = linkStyle))
        append("Terms of Service")
        pop()
        withStyle(SpanStyle(color = secondary, fontSize = 13.sp)) { append(" and ") }
        pushLink(LinkAnnotation.Url(url = PRIVACY_URL, styles = linkStyle))
        append("Privacy Policy")
        pop()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(24.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = TealPrimary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(Modifier.width(10.dp))
        Text(text = annotated, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun GoogleIcon() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text("G", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
    }
}

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = TealPrimary,
    focusedLabelColor    = TealPrimary,
    cursorColor          = TealPrimary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
)
