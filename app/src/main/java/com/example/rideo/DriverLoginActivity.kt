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

class DriverLoginActivity : AppCompatActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // UI components
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var loginSubtitle: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_login)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        inputEmail = findViewById(R.id.input_email)
        inputPassword = findViewById(R.id.input_password)
        btnLogin = findViewById(R.id.btn_login_driver)
        loginSubtitle = findViewById(R.id.login_subtitle)
        progressBar = findViewById(R.id.progressBar) // ✅ Must initialize ProgressBar
        val backButton = findViewById<ImageView>(R.id.back_button)

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button → go back to role selection
        backButton.setOnClickListener { finish() }

        // Login button logic
        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                showMessage(
                    "Error: Please enter both email and password.",
                    android.R.color.holo_red_light
                )
                return@setOnClickListener
            }

            // Show loading
            showLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        auth.currentUser?.uid?.let { uid ->
                            verifyUserRole(uid)
                        } ?: run { showLoading(false) }
                    } else {
                        showLoading(false)
                        showMessage(
                            "Error: Invalid email or password.",
                            android.R.color.holo_red_light
                        )
                    }
                }
        }
    }

    // Verify Firestore role
    private fun verifyUserRole(uid: String) {
        db.collection("Driver").document(uid).get()
            .addOnSuccessListener { document ->
                showLoading(false)
                val role = document.getString("role")
                if (role == "driver") {
                    redirectToDashboard()
                } else {
                    showMessage(
                        "Access Denied. Logged in as $role",
                        android.R.color.holo_red_light
                    )
                    auth.signOut()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                showMessage(
                    "Error: User profile not found.",
                    android.R.color.holo_red_light
                )
                auth.signOut()
            }
    }

    // Redirect to dashboard
    private fun redirectToDashboard() {
        startActivity(Intent(this, DriverDashboardActivity::class.java))
        finish()
    }

    // Show message
    private fun showMessage(message: String, colorResId: Int) {
        loginSubtitle.text = message
        loginSubtitle.setTextColor(ContextCompat.getColor(this, colorResId))
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Show/hide loading
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
