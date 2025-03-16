package com.bharatballot.securevote.voter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Base64

class VoterDashboardActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoterDashboardScreen()
        }
    }

    @Composable
    fun VoterDashboardScreen() {
        var candidates by remember { mutableStateOf(listOf<String>()) }
        var selectedCandidate by remember { mutableStateOf("") }
        var isVoteConfirmed by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            fetchCandidates { candidates = it }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Voter Dashboard") })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Your Candidate", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(candidates) { candidate ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { selectedCandidate = candidate }
                        ) {
                            Text(
                                text = candidate,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedCandidate.isNotEmpty()) {
                            castVote(selectedCandidate) {
                                isVoteConfirmed = true
                            }
                        } else {
                            Toast.makeText(this@VoterDashboardActivity, "Please select a candidate", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cast Vote")
                }

                if (isVoteConfirmed) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("✅ Your vote has been successfully recorded!", color = MaterialTheme.colors.primary)
                }
            }
        }
    }

    private fun fetchCandidates(callback: (List<String>) -> Unit) {
        firestore.collection("candidates").get()
            .addOnSuccessListener { snapshot ->
                val candidateList = snapshot.documents.mapNotNull { it.getString("name") }
                callback(candidateList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch candidates.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun castVote(candidate: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val voteData = candidate.toByteArray()

        // Generate AES key for encryption
        val aesKey = generateAESKey()
        val encryptedVote = encryptVote(voteData, aesKey)

        // Generate digital signature using ECDSA
        val signature = signVote(voteData)

        val voteMap = hashMapOf(
            "voter_id" to userId,
            "candidate" to Base64.encodeToString(encryptedVote, Base64.DEFAULT),
            "signature" to Base64.encodeToString(signature, Base64.DEFAULT),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("votes").add(voteMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Vote successfully cast!", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to cast vote. Try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    private fun encryptVote(data: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }

    private fun signVote(data: ByteArray): ByteArray {
        val keyPairGen = KeyPairGenerator.getInstance("EC")
        keyPairGen.initialize(256)
        val keyPair = keyPairGen.generateKeyPair()
        val privateKey: PrivateKey = keyPair.private

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
}
