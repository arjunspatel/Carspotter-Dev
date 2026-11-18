package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth

enum class OperationType {
    CREATE, UPDATE, DELETE, LIST, GET, WRITE
}

data class FirestoreErrorInfo(
    val error: String,
    val operationType: OperationType,
    val path: String?,
    val userId: String?,
    val authEmail: String?,
    val signInProvider: String?
)

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?,
    userIdOverride: String? = null
): Nothing {
    val currentUser = try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }
    val errInfo = FirestoreErrorInfo(
        error = error.message ?: error.toString(),
        operationType = operationType,
        path = path,
        userId = userIdOverride ?: currentUser?.uid,
        authEmail = currentUser?.email,
        signInProvider = currentUser?.providerData?.firstOrNull()?.providerId
    )
    Log.e("FirestoreError", "Firestore Operation Failed: $errInfo", error)
    throw error
}
