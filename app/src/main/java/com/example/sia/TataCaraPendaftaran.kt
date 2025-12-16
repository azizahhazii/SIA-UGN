package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sia.databinding.ActivityTataCaraPendaftaranBinding

class TataCaraPendaftaran : AppCompatActivity() {

    private lateinit var binding: ActivityTataCaraPendaftaranBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTataCaraPendaftaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sembunyikan Action Bar
        supportActionBar?.hide()

        // Tombol Back
        binding.btnBack.setOnClickListener {
            finish() // Kembali ke halaman sebelumnya
        }

        // Button Daftar Sekarang
        binding.btnDaftar.setOnClickListener {
            Toast.makeText(this, "Menuju Form Pendaftaran", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, FormPendaftaran::class.java) // Perbaikan: Ke FormPendaftaran
            startActivity(intent)
        }

        // Setup Bottom Navigation
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Home
        binding.navHome.setOnClickListener {
            // Kembali ke Dashboard
            val intent = Intent(this, DashboardMahasiswa::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Daftar (sudah aktif)
        binding.navDaftarLayout.setOnClickListener {
            // Sudah di halaman tata cara/daftar
            Toast.makeText(this, "Anda sudah di halaman Tata Cara Pendaftaran", Toast.LENGTH_SHORT).show()
        }

        // History/Status
        binding.navHistoryLayout.setOnClickListener {
            Toast.makeText(this, "Status Pendaftaran", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, StatusPengumumanActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}