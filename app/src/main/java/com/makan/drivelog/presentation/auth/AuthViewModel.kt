package com.makan.drivelog.presentation.auth

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.data.service.AuthService
import com.makan.drivelog.data.service.UserProfileService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: AuthService,
    private val userProfileService: UserProfileService
) : ViewModel() {

    sealed class AuthUiState {
        object Idle    : AuthUiState()
        object Loading : AuthUiState()
        object Success : AuthUiState()
        data class Error(val message: String) : AuthUiState()
    }

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbar = _snackbar.asSharedFlow()

    fun signInWithEmail(email: String, password: String, tosAccepted: Boolean = false) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            authService.signInWithEmail(email, password).fold(
                onSuccess = { user ->
                    runCatching { userProfileService.createOrUpdateProfile(user, tosAccepted = tosAccepted) }
                    _state.value = AuthUiState.Success
                },
                onFailure = { _state.value = AuthUiState.Error(friendlyMessage(it)) }
            )
        }
    }

    fun signUpWithEmail(email: String, password: String, displayName: String, tosAccepted: Boolean = false) {
        viewModelScope.launch {
            val special = "!@#\$%^&*()_+-=[]{}|;':\",./<>?"
            when {
                password.length < 10 ->
                    { _state.value = AuthUiState.Error("Password must be at least 10 characters."); return@launch }
                !password.any { it.isUpperCase() } ->
                    { _state.value = AuthUiState.Error("Password must contain an uppercase letter."); return@launch }
                !password.any { it in special } ->
                    { _state.value = AuthUiState.Error("Password must contain a special character."); return@launch }
            }
            _state.value = AuthUiState.Loading
            authService.signUpWithEmail(email, password).fold(
                onSuccess = { user ->
                    runCatching { userProfileService.createOrUpdateProfile(user, displayName, tosAccepted = tosAccepted) }
                    _state.value = AuthUiState.Success
                },
                onFailure = { _state.value = AuthUiState.Error(friendlyMessage(it)) }
            )
        }
    }

    fun getGoogleSignInIntent(activity: Activity): Intent =
        authService.getGoogleSignInIntent(activity)

    fun handleGoogleSignInResult(result: ActivityResult, tosAccepted: Boolean = false) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            authService.handleGoogleSignInResult(result.data).fold(
                onSuccess = { user ->
                    runCatching { userProfileService.createOrUpdateProfile(user, tosAccepted = tosAccepted) }
                    _state.value = AuthUiState.Success
                },
                onFailure = { _state.value = AuthUiState.Error(friendlyMessage(it)) }
            )
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            authService.refreshUser().fold(
                onSuccess = { verified ->
                    _state.value = if (verified) AuthUiState.Success
                    else AuthUiState.Error(
                        "Email not verified yet. Please check your inbox and click the verification link."
                    )
                },
                onFailure = { _state.value = AuthUiState.Error(friendlyMessage(it)) }
            )
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch {
            authService.sendVerificationEmail()
            _snackbar.tryEmit("Verification email sent")
            clearError()
        }
    }

    fun signOut() {
        authService.signOut()
        _state.value = AuthUiState.Idle
    }

    fun clearError() { if (_state.value is AuthUiState.Error) _state.value = AuthUiState.Idle }

    private fun friendlyMessage(t: Throwable): String = when {
        t.message?.contains("email address is already in use", ignoreCase = true) == true ->
            "That email is already registered. Try signing in."
        t.message?.contains("no user record", ignoreCase = true) == true ->
            "No account found with that email."
        t.message?.contains("password is invalid", ignoreCase = true) == true ||
        t.message?.contains("wrong-password", ignoreCase = true) == true ->
            "Incorrect password."
        t.message?.contains("badly formatted", ignoreCase = true) == true ->
            "Please enter a valid email address."
        t.message?.contains("canceled", ignoreCase = true) == true -> "Sign-in was cancelled."
        else -> t.message ?: "Authentication failed. Please try again."
    }
}
