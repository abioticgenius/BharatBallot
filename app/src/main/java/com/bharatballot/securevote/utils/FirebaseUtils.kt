package com.bharatballot.securevote.utils

import android.util.Log
import com.bharatballot.securevote.data.Candidate
import com.bharatballot.securevote.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseUtils {

    private val firestore = FirebaseFirestore.getInstance()

    // Save UserProfile to Firestore
    fun saveUserProfile(userProfile: UserProfile, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("user_profiles")
            .document(userProfile.profileId)
            .set(userProfile)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // Fetch UserProfile from Firestore by Aadhaar Number
    fun fetchUserProfileByAadhaar(aadhaarNumber: String, onSuccess: (UserProfile?) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("user_profiles")
            .whereEqualTo("aadhaarNumber", aadhaarNumber)
            .get()
            .addOnSuccessListener { documents ->
                val userProfile = documents.firstOrNull()?.toObject(UserProfile::class.java)
                onSuccess(userProfile)
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // Save Candidate to Firestore
    fun saveCandidate(candidate: Candidate, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("candidates")
            .document(candidate.id)
            .set(candidate)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // Fetch all Candidates
    fun fetchCandidates(onSuccess: (List<Candidate>) -> Unit, onFailure: (Exception) -> Unit) {
        firestore.collection("candidates")
            .get()
            .addOnSuccessListener { documents ->
                val candidates = documents.mapNotNull { it.toObject(Candidate::class.java) }
                onSuccess(candidates)
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }
}
