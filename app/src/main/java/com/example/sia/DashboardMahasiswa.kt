package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sia.databinding.ActivityDashboardMahasiswaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardMahasiswa : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardMahasiswaBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardMahasiswaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sembunyikan Action Bar
        supportActionBar?.hide()

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Cek apakah user sudah login
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Jika belum login, redirect ke login
            navigateToLogin()
            return
        }

        // Load data user dari Firestore
        loadUserData()

        // Menu Hamburger
        binding.ivMenu.setOnClickListener {
            showMenuDialog()
        }

        // Card Tata Cara Pendaftaran
        binding.cardTataCara.setOnClickListener {
            // Intent ke TataCaraPendaftaranActivity
            val intent = Intent(this, TataCaraPendaftaran::class.java)
            startActivity(intent)
        }

        // Card Informasi Penting
        binding.cardInformasi.setOnClickListener {
            // Intent ke InformasiPentingActivity
            val intent = Intent(this, InformasiPenting::class.java)
            startActivity(intent)
        }

        // Button Daftar Mahasiswa Baru
        binding.btnDaftar.setOnClickListener {
            // Intent ke TataCaraPendaftaranActivity
            val intent = Intent(this, TataCaraPendaftaran::class.java)
            startActivity(intent)
        }

        // Button Lihat Detail Pengumuman
        binding.btnLihatDetail.setOnClickListener {
            // Intent ke StatusPengumumanActivity
            val intent = Intent(this, StatusPengumumanActivity::class.java)
            startActivity(intent)
        }

        // Bottom Navigation dengan visual feedback
        setupBottomNavigation()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        // Ambil data user dari Firestore
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("fullName") ?: "Mahasiswa"
                    val email = document.getString("email") ?: ""
                    val role = document.getString("role") ?: "user"

                    // Set nama lengkap user yang login
                    binding.tvName.text = fullName

                    // Set inisial (2 huruf pertama dari nama)
                    val initials = getInitials(fullName)
                    binding.tvInitials.text = initials

                    // Set status berdasarkan role
                    val status = when (role) {
                        "admin" -> "Administrator"
                        "dosen" -> "Dosen"
                        else -> "Calon Mahasiswa"
                    }
                    binding.tvStatus.text = status

                } else {
                    // Jika data tidak ditemukan
                    binding.tvName.text = "Pengguna"
                    binding.tvInitials.text = "P"
                    binding.tvStatus.text = "Calon Mahasiswa"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error memuat data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                // Set default values jika gagal
                binding.tvName.text = "Pengguna"
                binding.tvInitials.text = "?"
                binding.tvStatus.text = "Calon Mahasiswa"
            }
    }

    private fun getInitials(fullName: String): String {
        val names = fullName.trim().split(" ")
        return when {
            names.size >= 2 -> {
                // Ambil huruf pertama dari nama depan dan belakang
                "${names.first().firstOrNull()?.uppercaseChar() ?: ""}${names.last().firstOrNull()?.uppercaseChar() ?: ""}"
            }
            names.size == 1 -> {
                // Jika hanya satu kata, ambil 2 huruf pertama
                fullName.take(2).uppercase()
            }
            else -> "?"
        }
    }

    private fun setupBottomNavigation() {
        // Set Home sebagai aktif (default)
        setActiveNavItem("home")

        // Home
        binding.navHome.setOnClickListener {
            setActiveNavItem("home")
            // Sudah di home, tidak perlu intent
        }

        // Daftar
        binding.navDaftarLayout.setOnClickListener {
            setActiveNavItem("daftar")

            // Intent ke TataCaraPendaftaranActivity
            val intent = Intent(this, TataCaraPendaftaran::class.java)
            startActivity(intent)
        }

        // History/Status
        binding.navHistoryLayout.setOnClickListener {
            setActiveNavItem("history")

            // Intent ke StatusPengumumanActivity
            val intent = Intent(this, StatusPengumumanActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setActiveNavItem(selected: String) {
        // Reset semua ke state tidak aktif
        binding.navHome.alpha = 0.5f
        binding.navDaftarLayout.alpha = 0.5f
        binding.navHistoryLayout.alpha = 0.5f

        // Set yang dipilih menjadi aktif (lebih terang)
        when (selected) {
            "home" -> binding.navHome.alpha = 1.0f
            "daftar" -> binding.navDaftarLayout.alpha = 1.0f
            "history" -> binding.navHistoryLayout.alpha = 1.0f
        }
    }

    private fun showMenuDialog() {
        val menuItems = arrayOf(
            "Status Pendaftaran",
            "Logout"
        )

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Menu")
        builder.setItems(menuItems) { dialog, which ->
            when (which) {
                0 -> {
                    // Intent ke StatusPengumumanActivity
                    val intent = Intent(this, StatusPengumumanActivity::class.java)
                    startActivity(intent)
                }
                1 -> {
                    showLogoutDialog()
                }
            }
            dialog.dismiss()
        }
        builder.show()
    }

    private fun showLogoutDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Logout")
        builder.setMessage("Apakah Anda yakin ingin keluar?")

        builder.setPositiveButton("Ya") { dialog, _ ->
            // Logout dari Firebase
            auth.signOut()

            Toast.makeText(this, "Berhasil logout", Toast.LENGTH_SHORT).show()

            // Redirect ke login
            navigateToLogin()
            dialog.dismiss()
        }

        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}