package com.example.rideo

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY: Long = 1000
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Set splash layout
        setContentView(R.layout.activity_onboarding)

        // Start animations
        animateLogoAndText()

        // Delay before checking login
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, SPLASH_DELAY)
    }

    /**
     * Check if the user is already logged in
     * If yes, redirect directly to Rider or Driver dashboard
     * If not, go to RoleSelectionActivity
     */
    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // koi user login nhi hai role selection mai push hoga code  ye cache check krta hai
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        } else {
            // User logged in(verify hone ke baad cache memory se) → Check role in Firestore
            db.collection("Rider").document(currentUser.uid).get()
                .addOnSuccessListener { riderDoc ->
                    if (riderDoc.exists() && riderDoc.getString("role") == "RIDER") {
                        // Logged in as Rider
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Maybe a driver? check driver collection
                        db.collection("Driver").document(currentUser.uid).get()
                            .addOnSuccessListener { driverDoc ->
                                if (driverDoc.exists() && driverDoc.getString("role") == "driver") {
                                    // Logged in as Driver
                                    startActivity(Intent(this, DriverDashboardActivity::class.java))
                                } else {
                                    // Role not found or mismatch → reset
                                    auth.signOut()
                                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                                }
                                finish()
                            }
                    }
                }
                .addOnFailureListener {
                    // Network or Firebase error → go to login selection
                    auth.signOut()
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                }
        }
    }

    /**
     * Simple animation for logo + text
     */
    private fun animateLogoAndText() {
        val logoContainer = findViewById<CardView>(R.id.logo_container)
        val logoAnimator = AnimatorInflater.loadAnimator(this, R.animator.logo_pop_animation) as AnimatorSet
        logoAnimator.setTarget(logoContainer)
        logoAnimator.start()

        val appNameText = findViewById<TextView>(R.id.app_name_text)
        val taglineText = findViewById<TextView>(R.id.tagline_text)

        val textAnimator = AnimatorInflater.loadAnimator(this, R.animator.text_slide_up) as AnimatorSet
        textAnimator.startDelay = 200
        textAnimator.setTarget(appNameText)
        textAnimator.start()

        val taglineAnimator = AnimatorInflater.loadAnimator(this, R.animator.text_slide_up) as AnimatorSet
        taglineAnimator.startDelay = 400
        taglineAnimator.setTarget(taglineText)
        taglineAnimator.start()
    }
}
