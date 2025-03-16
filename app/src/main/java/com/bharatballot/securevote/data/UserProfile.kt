package com.bharatballot.securevote.data

data class UserProfile(
    val profileId: String = "",           // Unique ID for each profile (UUID or Firestore document ID)
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val aadhaarNumber: String = "",       // Must be a 12-digit validated number
    val phoneNumber: String = "",         // For OTP verification
    val email: String = "",               // For email-based OTP verification
    val isPhoneVerified: Boolean = false, // Status of phone OTP verification
    val isEmailVerified: Boolean = false, // Status of email OTP verification
    val password: String = "",            // Encrypted password
    val biometricRegistered: Boolean = false // Flag to confirm biometric registration
)
