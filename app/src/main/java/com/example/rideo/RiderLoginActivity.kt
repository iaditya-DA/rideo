package com.example.rideo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RiderLoginActivity : AppCompatActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // View declarations
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var loginSubtitle: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rider_login)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- View Initialization ---
        inputEmail = findViewById(R.id.input_email)
        inputPassword = findViewById(R.id.input_password)
        btnLogin = findViewById(R.id.btn_login_rider)
        loginSubtitle = findViewById(R.id.login_subtitle)
        progressBar = findViewById(R.id.progressBar) // ✅ Important: Initialize ProgressBar
        val backButton = findViewById<ImageView>(R.id.back_button)

        // --- Edge-to-Edge setup ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Auto Login Check ---
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User already logged in, verify role directly
            verifyUserRole(currentUser.uid)
        }

        // --- Back Navigation ---
        backButton.setOnClickListener {
            finish() // Return to role selection screen
        }

        // --- Login Button Click ---
        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showMessage(
                    "Error: Please enter both email/ID and password.",
                    android.R.color.holo_red_light
                )
                return@setOnClickListener
            }

            showLoading(true)

            // Start Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.uid?.let { uid ->
                            verifyUserRole(uid)
                        } ?: run { showLoading(false) }
                    } else {
                        showMessage(
                            "Error: Invalid email or password.",
                            android.R.color.holo_red_light
                        )
                        showLoading(false)
                    }
                }
        }
    }

    /**
     * Checks Firestore to ensure the user has the 'RIDER' role.
     */
    private fun verifyUserRole(uid: String) {
        showLoading(true)
        db.collection("Rider").document(uid).get()
            .addOnSuccessListener { document ->
                showLoading(false)
                val role = document.getString("role")

                if (role == "RIDER") {
                    showMessage(
                        "Login successful! Redirecting...",
                        android.R.color.holo_green_light
                    )
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    showMessage(
                        "Access Denied. You are logged in as a $role.",
                        android.R.color.holo_red_light
                    )
                    auth.signOut()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                showMessage(
                    "Login error: User profile not found in database.",
                    android.R.color.holo_red_light
                )
                auth.signOut()
            }
    }

    /**
     * Updates the subtitle text and color + shows a Toast.
     */
    private fun showMessage(message: String, colorResId: Int) {
        loginSubtitle.text = message
        loginSubtitle.setTextColor(ContextCompat.getColor(this, colorResId))
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Handles loading UI state: shows/hides ProgressBar & disables/enables login button.
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false
            btnLogin.text = "VERIFYING..."
        } else {
            progressBar.visibility = View.GONE
            btnLogin.isEnabled = true
            btnLogin.text = "LOGIN"
        }
    }
}
