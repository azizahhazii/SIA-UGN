package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardAdmin : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var userName: String? = null
    private var userRole: String? = null

    // Views for statistics
    private lateinit var tvTotalNumber: TextView
    private lateinit var tvStatApprovedNumber: TextView
    private lateinit var tvStatReviewNumber: TextView
    private lateinit var tvRejectNumber: TextView
    private lateinit var tvNotificationBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_admin)

        // Hide action bar
        supportActionBar?.hide()

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Ambil data dari Intent
        userName = intent.getStringExtra("USER_NAME")
        userRole = intent.getStringExtra("USER_ROLE")

        android.util.Log.d("DashboardAdmin", "========================================")
        android.util.Log.d("DashboardAdmin", "Dashboard Admin Started")
        android.util.Log.d("DashboardAdmin", "User Name: $userName")
        android.util.Log.d("DashboardAdmin", "User Role: $userRole")
        android.util.Log.d("DashboardAdmin", "========================================")

        // Inisialisasi Views
        val greetingText: TextView = findViewById(R.id.greeting_text)
        val btnLogout: ImageButton = findViewById(R.id.btn_logout_icon)
        val btnKelolaPendaftaran: androidx.appcompat.widget.AppCompatButton = findViewById(R.id.btn_kelola_pendaftaran)

        // Bottom Navigation
        val navHomeActive: LinearLayout = findViewById(R.id.nav_home_active)
        val navForm: ImageButton = findViewById(R.id.nav_form)

        // Inisialisasi Statistics Views
        tvTotalNumber = findViewById(R.id.total_number)
        tvStatApprovedNumber = findViewById(R.id.stat_approved_number)
        tvStatReviewNumber = findViewById(R.id.stat_review_number)
        tvRejectNumber = findViewById(R.id.reject_number)
        tvNotificationBadge = findViewById(R.id.notification_badge)

        // Set greeting text
        greetingText.text = "Selamat datang, ${userName ?: "Admin"}!"

        // Load statistics dari Firestore
        loadStatistics()

        // Logout button
        btnLogout.setOnClickListener {
            handleLogout()
        }

        // Button Kelola Pendaftaran (dari tengah)
        btnKelolaPendaftaran.setOnClickListener {
            navigateToKelolaPendaftaran()
        }

        // Bottom Navigation - Home (already active)
        navHomeActive.setOnClickListener {
            android.util.Log.d("DashboardAdmin", "Home clicked - Already on Dashboard")
            Toast.makeText(this, "Anda sudah berada di Home", Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation - Kelola Pendaftaran
        navForm.setOnClickListener {
            navigateToKelolaPendaftaran()
        }
    }

    private fun loadStatistics() {
        android.util.Log.d("DashboardAdmin", "========================================")
        android.util.Log.d("DashboardAdmin", "Loading Statistics from Firestore")
        android.util.Log.d("DashboardAdmin", "========================================")

        // Set loading state
        tvTotalNumber.text = "..."
        tvStatApprovedNumber.text = "..."
        tvStatReviewNumber.text = "..."
        tvRejectNumber.text = "..."

        // Ambil data dari collection "pendaftar"
        firestore.collection("pendaftar")
            .get()
            .addOnSuccessListener { documents ->
                var totalPendaftar = 0
                var menungguVerifikasi = 0
                var diterima = 0
                var ditolak = 0

                // Hitung berdasarkan status
                for (document in documents) {
                    totalPendaftar++

                    val status = document.getString("status")?.lowercase() ?: "menunggu"

                    when (status) {
                        "menunggu", "menunggu verifikasi", "pending", "menunggu review" -> menungguVerifikasi++
                        "diterima", "approved", "disetujui" -> diterima++
                        "ditolak", "rejected", "tidak disetujui" -> ditolak++
                        else -> menungguVerifikasi++ // Default ke menunggu jika status tidak dikenali
                    }
                }

                // Update UI
                tvTotalNumber.text = totalPendaftar.toString()
                tvStatApprovedNumber.text = diterima.toString()
                tvStatReviewNumber.text = menungguVerifikasi.toString()
                tvRejectNumber.text = ditolak.toString()

                // Update notification badge (menunggu verifikasi)
                tvNotificationBadge.text = menungguVerifikasi.toString()

                android.util.Log.d("DashboardAdmin", "✓ Statistics loaded successfully")
                android.util.Log.d("DashboardAdmin", "  Total Pendaftar: $totalPendaftar")
                android.util.Log.d("DashboardAdmin", "  Menunggu Verifikasi: $menungguVerifikasi")
                android.util.Log.d("DashboardAdmin", "  Diterima: $diterima")
                android.util.Log.d("DashboardAdmin", "  Ditolak: $ditolak")
                android.util.Log.d("DashboardAdmin", "========================================")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DashboardAdmin", "❌ Failed to load statistics", e)

                // Set nilai 0 jika gagal
                tvTotalNumber.text = "0"
                tvStatApprovedNumber.text = "0"
                tvStatReviewNumber.text = "0"
                tvRejectNumber.text = "0"
                tvNotificationBadge.text = "0"

                Toast.makeText(
                    this,
                    "Gagal memuat statistik: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun navigateToKelolaPendaftaran() {
        android.util.Log.d("DashboardAdmin", "Navigate to Kelola Pendaftaran")

        val intent = Intent(this, KelolaPendaftaranActivity::class.java)
        intent.putExtra("USER_NAME", userName)
        intent.putExtra("USER_ROLE", userRole)
        startActivity(intent)

        // Smooth transition
        overridePendingTransition(0, 0)
    }

    private fun handleLogout() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari akun admin?")
            .setPositiveButton("Ya, Keluar") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performLogout() {
        android.util.Log.d("DashboardAdmin", "========================================")
        android.util.Log.d("DashboardAdmin", "Performing Logout")
        android.util.Log.d("DashboardAdmin", "========================================")

        // Sign out dari Firebase Auth
        auth.signOut()

        android.util.Log.d("DashboardAdmin", "✓ Firebase Auth signed out")

        Toast.makeText(this, "Logout berhasil.", Toast.LENGTH_SHORT).show()

        // Redirect ke LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()

        android.util.Log.d("DashboardAdmin", "→ Redirected to LoginActivity")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Disable back button agar tidak bisa kembali setelah login
        AlertDialog.Builder(this)
            .setTitle("Keluar Aplikasi")
            .setMessage("Apakah Anda ingin keluar dari aplikasi?")
            .setPositiveButton("Ya") { dialog, which ->
                finishAffinity() // Close all activities
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh statistics ketika kembali ke dashboard
        android.util.Log.d("DashboardAdmin", "onResume - Refreshing statistics")
        loadStatistics()
    }
}