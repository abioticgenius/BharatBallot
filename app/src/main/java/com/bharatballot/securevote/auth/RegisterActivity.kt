package com.bharatballot.securevote.auth

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bharatballot.securevote.auth.OtpVerificationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class RegisterActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var secretKey: SecretKey

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Generate encryption key
        secretKey = generateSecretKey()

        setContent {
            RegisterScreen()
        }
    }

    @Composable
    fun RegisterScreen() {
        var name by remember { mutableStateOf("") }
        var age by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("") }
        var aadhaar by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Register New Profile", style = MaterialTheme.typography.h6)

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = aadhaar, onValueChange = { aadhaar = it }, label = { Text("Aadhaar Number (12 digits)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Set Password") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (validateInputs(name, age, gender, aadhaar, phone, email, password)) {
                    saveUserProfile(name, age, gender, aadhaar, phone, email, password)
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Register and Verify OTP")
            }
        }
    }

    private fun validateInputs(name: String, age: String, gender: String, aadhaar: String, phone: String, email: String, password: String): Boolean {
        return when {
            name.isEmpty() || age.isEmpty() || gender.isEmpty() || aadhaar.isEmpty() ||
                    phone.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                Toast.makeText(this, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                false
            }

            aadhaar.length != 12 -> {
                Toast.makeText(this, "Aadhaar number must be 12 digits.", Toast.LENGTH_SHORT).show()
                false
            }

            else -> true
        }
    }

    private fun saveUserProfile(name: String, age: String, gender: String, aadhaar: String, phone: String, email: String, password: String) {
        val encryptedAadhaar = encryptData(aadhaar)
        val encryptedPhone = encryptData(phone)
        val encryptedEmail = encryptData(email)
        val hashedPassword = hashPassword(password)

        val userProfile = hashMapOf(
            "name" to name,
            "age" to age,
            "gender" to gender,
            "aadhaar" to encryptedAadhaar,
            "phone" to encryptedPhone,
            "email" to encryptedEmail,
            "password" to hashedPassword
        )

        firestore.collection("users").document(aadhaar).set(userProfile)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved. Proceed to OTP Verification.", Toast.LENGTH_SHORT).show()
                navigateToOtpVerification(phone, email)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToOtpVerification(phone: String, email: String) {
        val intent = Intent(this, OtpVerificationActivity::class.java).apply {
            putExtra("phone", phone)
            putExtra("email", email)
        }
        startActivity(intent)
        finish()
    }

    private fun encryptData(data: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encrypted = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    private fun generateSecretKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return Base64.encodeToString(hash, Base64.DEFAULT)
    }
}
