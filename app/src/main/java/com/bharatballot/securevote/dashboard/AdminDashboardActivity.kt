package com.bharatballot.securevote.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bharatballot.securevote.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AdminDashboardScreen()
        }
    }

    @Composable
    fun AdminDashboardScreen() {
        var voteCounts by remember { mutableStateOf(mapOf<String, Int>()) }
        var candidates by remember { mutableStateOf(listOf<String>()) }

        LaunchedEffect(Unit) {
            fetchCandidates { candidates = it }
            fetchVoteCounts { voteCounts = it }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard") },
                    actions = {
                        Button(onClick = { logout() }) {
                            Text("Logout")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)) {

                Text("Real-Time Vote Counts", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(candidates) { candidate ->
                        val count = voteCounts[candidate] ?: 0
                        Text(text = "$candidate: $count votes", style = MaterialTheme.typography.body1)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { fetchAuditLogs() }, modifier = Modifier.fillMaxWidth()) {
                    Text("View Audit Logs")
                }
            }
        }
    }

    private fun fetchCandidates(callback: (List<String>) -> Unit) {
        firestore.collection("candidates").get()
            .addOnSuccessListener { snapshot ->
                val candidateNames = snapshot.documents.mapNotNull { it.getString("name") }
                callback(candidateNames)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch candidates.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchVoteCounts(callback: (Map<String, Int>) -> Unit) {
        firestore.collection("votes").get()
            .addOnSuccessListener { snapshot ->
                val counts = mutableMapOf<String, Int>()
                for (doc in snapshot.documents) {
                    val candidate = doc.getString("candidate")
                    candidate?.let {
                        counts[it] = counts.getOrDefault(it, 0) + 1
                    }
                }
                callback(counts)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch vote counts.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAuditLogs() {
        firestore.collection("audit_logs").get()
            .addOnSuccessListener { snapshot ->
                val logs = snapshot.documents.mapNotNull { it.getString("log") }
                logs.forEach {
                    println("Audit Log: $it")
                }
                Toast.makeText(this, "Check logcat for audit logs.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch audit logs.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logAudit(action: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val logEntry = hashMapOf(
            "log" to "[${
                sdf.format(Date())
            }] Action: $action",
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("audit_logs").add(logEntry)
    }

    private fun logout() {
        auth.signOut()
        logAudit("Admin Logged Out")
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
