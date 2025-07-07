package com.example.konvo.feature.auth.vm

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.lifecycle.ViewModel
import com.example.konvo.R
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

fun signOutEverywhere(ctx: Context, onSignedOut: () -> Unit = {}) {
    // Firebase sign-out
    FirebaseAuth.getInstance().signOut()

    // One-tap sign-out
    Identity.getSignInClient(ctx).signOut()

    // Legacy GoogleSignIn sign-out
    GoogleSignIn.getClient(
        ctx,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
    ).signOut().addOnCompleteListener { onSignedOut() }
}


/* -------- one‑shot UI events -------- */
sealed interface AuthEvent {
    data object Success               : AuthEvent
    data object OtpSent              : AuthEvent
    data object NewUser               : AuthEvent
    data class Error(val msg: String) : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /* ---------------- GOOGLE ---------------- */

    fun launchGoogle(
        ctx: Context,
        launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
    ) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(ctx.getString(R.string.web_client_id))
            .requestEmail()
            .build()

        val intent = GoogleSignIn.getClient(ctx, gso).signInIntent
        launcher.launch(intent)
    }

    fun handleGoogleResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) {
            _events.trySend(AuthEvent.Error("Google sign‑in cancelled"))
            return
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount = task.result   // throws ApiException on error
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { _events.trySend(AuthEvent.Success) }
                .addOnFailureListener { _events.trySend(AuthEvent.Error(it.message ?: "Firebase error")) }
        } catch (e: Exception) {
            _events.trySend(AuthEvent.Error(e.localizedMessage ?: "Google sign‑in failed"))
        }
    }

    /* ---------------- EMAIL ---------------- */
    fun loginWithEmail(email: String, pwd: String) {
        auth.signInWithEmailAndPassword(email, pwd)
            .addOnSuccessListener { _events.trySend(AuthEvent.Success) }
            .addOnFailureListener { _events.trySend(AuthEvent.Error(it.message ?: "Auth error")) }
    }

    fun registerWithEmail(email: String, pwd: String) {
        auth.createUserWithEmailAndPassword(email, pwd)
            .addOnSuccessListener { _events.trySend(AuthEvent.NewUser) }
            .addOnFailureListener  { _events.trySend(AuthEvent.Error(it.message ?: "Sign‑up error")) }
    }




    /** Last number we attempted – used for “Resend” */
    var lastPhone: String? = null
        private set

    private var verificationId: String? = null
    private var resendToken   : PhoneAuthProvider.ForceResendingToken? = null

    fun sendOtp(phone: String, act: Activity, forceResend: Boolean = false) {
       // FirebaseAuth.getInstance().firebaseAuthSettings.forceRecaptchaFlowForTesting(false)
        lastPhone = phone

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60, TimeUnit.SECONDS)
            .setActivity(act)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                    auth.signInWithCredential(cred)
                        .addOnSuccessListener { result ->
                            val isNew = result.additionalUserInfo?.isNewUser == true
                            _events.trySend(
                                if (isNew) AuthEvent.NewUser
                                else        AuthEvent.Success
                            )
                        }
                        .addOnFailureListener {
                            _events.trySend(AuthEvent.Error(it.message ?: "Silent sign-in failed"))
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("PHONE‑AUTH", "onVerificationFailed", e)

                    _events.trySend(AuthEvent.Error(e.localizedMessage ?: "Phone error"))
                }
/*
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    resendToken    = token
                    _events.trySend(AuthEvent.OtpSent)
                }

 */

                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id
                    resendToken    = token

                    _events.trySend(AuthEvent.OtpSent)
                }
            })

        if (forceResend && resendToken != null) builder.setForceResendingToken(resendToken!!)
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }


    fun verifyOtp(code: String) {
        val id = verificationId
        if (id == null) {
            _events.trySend(AuthEvent.Error("Missing verification ID"))
            return
        }

        val cred = PhoneAuthProvider.getCredential(id, code)

        auth.signInWithCredential(cred)
            .addOnSuccessListener { result ->
                // 🔍 check if this phone number just created a new account
                val isNew = result.additionalUserInfo?.isNewUser == true
                _events.trySend(
                    if (isNew) AuthEvent.NewUser
                    else        AuthEvent.Success
                )
            }
            .addOnFailureListener {
                _events.trySend(AuthEvent.Error(it.message ?: "OTP error"))
            }
    }


    fun resendLastOtp(activity: Activity) {
        lastPhone?.let { sendOtp(it, activity, forceResend = true) }
    }

    fun trySendOrResend(phone: String, act: Activity) {
        if (phone == lastPhone && resendToken != null) {
            // Same number, session still hot → resend
            sendOtp(phone, act, forceResend = true)
        } else {
            // New number (or we waited >60s) → start fresh
            sendOtp(phone, act, forceResend = false)
        }
    }
}
