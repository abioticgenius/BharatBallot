package com.bharatballot.securevote.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bharatballot.securevote.voter.VoterDashboardActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class OtpVerificationActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            OtpVerificationScreen()
        }
    }

    @Composable
    fun OtpVerificationScreen() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var mobileOtp by remember { mutableStateOf("") }
        var emailOtp by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("+91XXXXXXXXXX") } // Replace with actual phone
        var email by remember { mutableStateOf("user@example.com") } // Replace with actual email

        LaunchedEffect(Unit) {
            sendOtpToMobile(phoneNumber)
            sendOtpToEmail(email)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "OTP Verification", style = MaterialTheme.typography.h6)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = mobileOtp,
                onValueChange = { mobileOtp = it },
                label = { Text("Enter Mobile OTP") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = emailOtp,
                onValueChange = { emailOtp = it },
                label = { Text("Enter Email OTP") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                scope.launch {
                    verifyOtp(mobileOtp, emailOtp)
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Verify OTPs")
            }
        }
    }

    private fun sendOtpToMobile(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(applicationContext, "Mobile OTP Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@OtpVerificationActivity.verificationId = verificationId
                    Toast.makeText(applicationContext, "Mobile OTP Sent", Toast.LENGTH_SHORT).show()
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun sendOtpToEmail(email: String) {
        // Mock email OTP sending - replace with actual email service like SendGrid in production
        Log.d("EmailOTP", "Sending OTP to email: $email")
        Toast.makeText(applicationContext, "Email OTP Sent (Mock)", Toast.LENGTH_SHORT).show()
    }

    private fun verifyOtp(mobileOtp: String, emailOtp: String) {
        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId!!, mobileOtp)
            signInWithPhoneAuthCredential(credential, emailOtp)
        } else {
            Toast.makeText(this, "Mobile OTP verification failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, emailOtp: String? = null) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (validateEmailOtp(emailOtp)) {
                        navigateToDashboard()
                    } else {
                        Toast.makeText(this, "Invalid Email OTP", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Mobile OTP Verification Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateEmailOtp(emailOtp: String?): Boolean {
        // Mock validation (In production, validate from backend service)
        return emailOtp == "123456"
    }

    private fun navigateToDashboard() {
        Toast.makeText(this, "OTP Verification Successful!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, VoterDashboardActivity::class.java))
        finish()
    }
}
