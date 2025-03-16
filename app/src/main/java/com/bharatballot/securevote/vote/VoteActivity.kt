package com.bharatballot.securevote.vote

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.bharatballot.securevote.data.Candidate
import com.bharatballot.securevote.databinding.ActivityVoteBinding
import com.bharatballot.securevote.utils.EncryptionUtils
import com.bharatballot.securevote.utils.FirebaseUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.*

class VoteActivity : ComponentActivity() {

    private lateinit var binding: ActivityVoteBinding
    private var selectedCandidate: Candidate? = null
    private val candidates = mutableListOf<Candidate>()
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityVoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCandidates()
        setupVoteSubmission()
    }

    // Load candidates from Firestore
    private fun loadCandidates() {
        FirebaseUtils.fetchCandidates(
            onSuccess = { candidatesList ->
                candidates.clear()
                candidates.addAll(candidatesList)
                displayCandidates()
            },
            onFailure = { exception ->
                Toast.makeText(this, "Failed to load candidates: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Display candidates in the ListView
    private fun displayCandidates() {
        val candidateNames = candidates.map { it.name }
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, candidateNames)
        binding.candidateListView.choiceMode = android.widget.ListView.CHOICE_MODE_SINGLE
        binding.candidateListView.adapter = adapter

        binding.candidateListView.setOnItemClickListener { _, _, position, _ ->
            selectedCandidate = candidates[position]
        }
    }

    // Handle vote submission
    private fun setupVoteSubmission() {
        binding.submitVoteButton.setOnClickListener {
            if (selectedCandidate == null) {
                Toast.makeText(this, "Please select a candidate!", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    castVote(selectedCandidate!!)
                }
            }
        }
    }

    // Cast and encrypt the vote, then store it in Firestore
    private suspend fun castVote(candidate: Candidate) {
        try {
            val voterId = currentUser?.uid ?: UUID.randomUUID().toString()
            val voteId = UUID.randomUUID().toString()

            // Step 1: Sign the vote using ECDSA
            val signedVote = generateDigitalSignature(candidate.id)

            // Step 2: Encrypt the signed vote using AES-256
            val encryptedVote = EncryptionUtils.encrypt(signedVote)

            // Step 3: Create Zero-Knowledge Proof (Simulated)
            val zkpProof = generateZKP(voterId, candidate.id)

            // Step 4: Prepare the vote data
            val voteData = hashMapOf(
                "voterIdHash" to EncryptionUtils.encrypt(voterId),
                "candidateId" to candidate.id,
                "encryptedVote" to encryptedVote,
                "zkpProof" to zkpProof,
                "timestamp" to System.currentTimeMillis()
            )

            // Step 5: Store the encrypted vote in Firestore
            firestore.collection("votes")
                .document(voteId)
                .set(voteData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Vote submitted successfully!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to submit vote: ${e.message}", Toast.LENGTH_LONG).show()
                }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Generate ECDSA digital signature for the vote
    private fun generateDigitalSignature(data: String): String {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(256)
        val keyPair = keyPairGenerator.generateKeyPair()

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data.toByteArray())
        val signedData = signature.sign()

        return Base64.encodeToString(signedData, Base64.NO_WRAP)
    }

    // Simulate Zero-Knowledge Proof (ZKP) generation
    private fun generateZKP(voterId: String, candidateId: String): String {
        val zkpData = "$voterId|$candidateId".toByteArray()
        return Base64.encodeToString(zkpData, Base64.NO_WRAP)
    }
}
