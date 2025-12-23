package com.example.tripexpensetracker.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import com.google.firebase.FirebaseException

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) {
    val currentUser = auth.currentUser

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun userId(): String? {
        return auth.currentUser?.uid
    }

    fun signOut() {
        auth.signOut()
    }

    fun sendVerificationCode(
        phoneNumber: String,
        activity: android.app.Activity,
        onCodeSent: (String, com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (com.google.firebase.auth.PhoneAuthCredential) -> Unit,
        onVerificationFailed: (Exception) -> Unit
    ) {
        val callbacks = object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                onVerificationCompleted(credential)
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                onVerificationFailed(e)
            }

            override fun onCodeSent(
                verificationId: String,
                token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
            ) {
                onCodeSent(verificationId, token)
            }
        }

        val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun signInWithCredential(credential: com.google.firebase.auth.PhoneAuthCredential): Flow<Result<Boolean>> = callbackFlow {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    trySend(Result.success(true))
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Phone Auth failed")))
                }
                close()
            }
        awaitClose {}
    }

    fun linkPassword(phone: String, password: String, displayName: String? = null, photoUri: android.net.Uri? = null): Flow<Result<Boolean>> = callbackFlow {
        val email = "$phone@tripapp.com"
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
        
        // Link the credential to the current user
        auth.currentUser?.linkWithCredential(credential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                         kotlinx.coroutines.GlobalScope.launch {
                             var photoUrl: String? = null
                             if (photoUri != null) {
                                 photoUrl = userRepository.uploadProfilePicture(uid, photoUri)
                             }
                             userRepository.saveUser(uid, phone, displayName, photoUrl)
                         }
                    }
                    trySend(Result.success(true))
                } else {
                    if (task.exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                         trySend(Result.failure(Exception("Account already exists with this password")))
                    } else {
                        trySend(Result.failure(task.exception ?: Exception("Link password failed")))
                    }
                }
                close()
            }
        awaitClose {}
    }

    fun login(phone: String, password: String): Flow<Result<Boolean>> = callbackFlow {
        val email = "$phone@tripapp.com"
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        kotlinx.coroutines.GlobalScope.launch {
                            userRepository.saveUser(uid, phone)
                        }
                    }
                    trySend(Result.success(true))
                } else {
                    trySend(Result.failure(task.exception ?: Exception("Login failed")))
                }
                close()
            }
        awaitClose {}
    }

    fun reauthenticate(password: String): Flow<Result<Boolean>> = callbackFlow {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        trySend(Result.success(true))
                    } else {
                        trySend(Result.failure(task.exception ?: Exception("Re-authentication failed")))
                    }
                    close()
                }
        } else {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
        }
        awaitClose {}
    }

    fun updatePassword(password: String): Flow<Result<Boolean>> = callbackFlow {
        val user = auth.currentUser
        if (user != null) {
            user.updatePassword(password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        trySend(Result.success(true))
                    } else {
                        trySend(Result.failure(task.exception ?: Exception("Password update failed")))
                    }
                    close()
                }
        } else {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
        }
        awaitClose {}
    }
}

