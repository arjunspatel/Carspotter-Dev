package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "AuthService"

class AuthService(private val context: Context) {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    private fun resolveWebClientId(): String {
        val clientId = context.getString(R.string.default_web_client_id)
        require(clientId.isNotBlank()) { "default_web_client_id string resource is blank" }
        return clientId
    }

    /**
     * Attempts silent automatic sign-in for returning users before prompting interactively.
     */
    suspend fun attemptAutoSignIn(): FirebaseUser? {
        auth.currentUser?.let { return it }
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(resolveWebClientId())
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            signInWithCredentialResult(result.credential).getOrNull()
        } catch (_: NoCredentialException) {
            null
        } catch (_: GetCredentialCancellationException) {
            null
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google Sign-In skipped: ${e.message}")
            null
        }
    }

    /**
     * Interactive Google Sign-In button flow via Credential Manager.
     */
    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(resolveWebClientId())
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            signInWithCredentialResult(result.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled by user: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun signInWithCredentialResult(credential: androidx.credentials.Credential): Result<FirebaseUser> {
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken.idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            Log.d(TAG, "Google Sign-In successful for user: ${user.uid}")
            Result.success(user)
        } else {
            Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
        }
    }

    /**
     * Signs out of Firebase Auth and clears active Credential Manager state so the user
     * can switch Google accounts cleanly on next sign-in.
     */
    suspend fun signOut() {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Failed clearing Credential Manager state: ${e.message}")
            }
            try {
                auth.signOut()
                Log.d(TAG, "User signed out successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out: ${e.message}", e)
            }
        }
    }
}
