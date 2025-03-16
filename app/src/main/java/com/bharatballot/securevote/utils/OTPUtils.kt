package com.bharatballot.securevote.utils

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.concurrent.TimeUnit

object OTPUtils {

    private const val TAG = "OTPUtils"

    // Send OTP to a phone number
    fun sendOTPToPhone(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onVerificationCompleted: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    onVerificationCompleted()
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onFailure(e.message ?: "Verification failed")
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    onCodeSent(verificationId)
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Verify OTP
    fun verifyOTP(verificationId: String, otp: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception?.message ?: "OTP Verification failed")
                }
            }
    }

    // Generate a 6-digit OTP manually (for email verification simulation)
    fun generateOTP(): String {
        return (100000..999999).random().toString()
    }
}
