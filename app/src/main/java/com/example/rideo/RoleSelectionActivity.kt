package com.example.rideo

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RoleSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_role_selection)

        // --- Edge-to-Edge setup ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Find Cards ---
        val riderCard = findViewById<CardView>(R.id.rider_card)
        val driverCard = findViewById<CardView>(R.id.driver_card)

        // --- Load Popup Animation ---
        val popAnim = AnimationUtils.loadAnimation(this, R.anim.card_pop)

        // --- Common Card Click Listener ---
        val cardClickListener = View.OnClickListener { view ->
            view.startAnimation(popAnim)
            view.postDelayed({
                when (view.id) {
                    R.id.rider_card -> startActivity(Intent(this, RiderLoginActivity::class.java))
                    R.id.driver_card -> startActivity(Intent(this, DriverLoginActivity::class.java))
                }
            }, 150)
        }

        riderCard.setOnClickListener(cardClickListener)
        driverCard.setOnClickListener(cardClickListener)

        // --- Add Touch Animation Effect (with performClick for accessibility) ---
        val touchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start()
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    v.performClick() // 👈 Proper accessibility click trigger
                }
                MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            true // consume the touch event
        }

        riderCard.setOnTouchListener(touchListener)
        driverCard.setOnTouchListener(touchListener)
    }
}
