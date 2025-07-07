package com.example.konvo.data

import com.google.firebase.auth.*
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    /* ---------- E‑mail / password ---------- */
    suspend fun loginWithEmail(email: String, pass: String): FirebaseUser =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { cont.resume(it.user!!) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /* ---------- Google ---------- */
    suspend fun loginWithGoogle(cred: AuthCredential): FirebaseUser =
        suspendCancellableCoroutine { cont ->
            auth.signInWithCredential(cred)
                .addOnSuccessListener { cont.resume(it.user!!) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    /* ---------- Phone – final step ---------- */
    suspend fun verifyOtp(otpId: String, code: String): FirebaseUser =
        loginWithGoogle(PhoneAuthProvider.getCredential(otpId, code))

    /* Expose auth instance when you need UID etc. */
    fun currentUser(): FirebaseUser? = auth.currentUser
}
