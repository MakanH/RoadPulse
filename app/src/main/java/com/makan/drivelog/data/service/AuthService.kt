package com.makan.drivelog.data.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.makan.drivelog.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    // Tracks email verification confirmed via reload() — reset on sign-out
    private val _emailJustVerified = MutableStateFlow(false)
    val emailJustVerified: StateFlow<Boolean> = _emailJustVerified.asStateFlow()

    init {
        auth.addAuthStateListener { if (it.currentUser == null) _emailJustVerified.value = false }
    }

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    fun isGoogleUser(): Boolean =
        auth.currentUser?.providerData?.any { it.providerId == "google.com" } == true

    suspend fun signUpWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user!!
        runCatching { user.sendEmailVerification().await() }
        user
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user!!
    }

    suspend fun sendVerificationEmail(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await() ?: Unit
    }

    suspend fun refreshUser(): Result<Boolean> = runCatching {
        auth.currentUser?.reload()?.await()
        val verified = auth.currentUser?.isEmailVerified == true
        if (verified) _emailJustVerified.value = true
        verified
    }

    fun getGoogleSignInIntent(activity: Activity): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso).signInIntent
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
        val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
        auth.signInWithCredential(credential).await().user!!
    }

    fun signOut() = auth.signOut()

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        auth.currentUser?.delete()?.await() ?: Unit
    }
}
