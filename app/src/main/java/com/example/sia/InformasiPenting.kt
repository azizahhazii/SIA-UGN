package com.example.sia

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout

class InformasiPenting : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnDaftar: Button
    private lateinit var navHome: ImageView
    private lateinit var navDaftar: LinearLayout
    private lateinit var navHistory: ImageView
    private lateinit var navProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_informasi_penting)

        supportActionBar?.hide()

        // Initialize views
        initViews()

        // Set click listeners
        setClickListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnDaftar = findViewById(R.id.btnDaftar)

        // Navigation
        navHome = findViewById(R.id.navHome)
        navDaftar = findViewById(R.id.navDaftar)
        navHistory = findViewById(R.id.navHistory)
    }

    private fun setClickListeners() {
        // Back button - Navigate back to previous Activity
        btnBack.setOnClickListener {
            finish() // Close this activity and return to previous
        }

        // Button Daftar - Navigate to Registration Form (FormPendaftaran)
        btnDaftar.setOnClickListener {
            val intent = Intent(this, FormPendaftaran::class.java) // Perbaikan: Ke FormPendaftaran
            startActivity(intent)
        }

        // Bottom Navigation
        navHome.setOnClickListener {
            // Perbaikan: Arahkan ke DashboardMahasiswa sebagai pusat kendali
            val intent = Intent(this, DashboardMahasiswa::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        // Navigasi Daftar - Navigate to Registration Form (FormPendaftaran)
        navDaftar.setOnClickListener {
            val intent = Intent(this, FormPendaftaran::class.java) // Perbaikan: Ke FormPendaftaran
            startActivity(intent)
        }

        navHistory.setOnClickListener {
            // Navigate to History Activity (create later)
        }

    }
}