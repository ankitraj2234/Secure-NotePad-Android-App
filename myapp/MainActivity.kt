package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)  // Use the splash layout

        // Use Handler with Looper for better practice
        Handler(Looper.getMainLooper()).postDelayed({
            // Navigate to LoginActivity after 3 seconds
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 3000)
    }
}
