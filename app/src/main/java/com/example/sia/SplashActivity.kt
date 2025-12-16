package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    // Durasi tampilan splash screen (dalam milidetik)
    private val SPLASH_TIME_OUT: Long = 3000 // 3 detik

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Menggunakan Handler untuk menunda transisi
        Handler(Looper.getMainLooper()).postDelayed({
            // Intent untuk memulai LandingPageActivity
            val intent = Intent(this, LandingActivity::class.java)
            startActivity(intent)

            // Menutup SplashActivity agar tidak bisa kembali dengan tombol back
            finish()
        }, SPLASH_TIME_OUT)
    }
}