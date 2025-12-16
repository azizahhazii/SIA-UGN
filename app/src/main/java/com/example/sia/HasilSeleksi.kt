package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HasilSeleksi : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // UI Components
    private lateinit var layoutStatusBanner: LinearLayout
    private lateinit var tvStatusBanner: TextView
    private lateinit var tvMessageTitle: TextView
    private lateinit var tvMessageExtra: TextView
    private lateinit var tvInitials: TextView
    private lateinit var tvNama: TextView
    private lateinit var tvNomorPendaftar: TextView
    private lateinit var tvProgram: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnKembaliHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hasil_seleksi)

        // Inisialisasi Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inisialisasi Views
        initializeViews()

        // Back button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Load hasil seleksi
        loadHasilSeleksi()

        // Button kembali ke home
        btnKembaliHome.setOnClickListener {
            startActivity(Intent(this, DashboardMahasiswa::class.java))
            finishAffinity()
        }
    }

    private fun initializeViews() {
        layoutStatusBanner = findViewById(R.id.layout_status_banner)
        tvStatusBanner = findViewById(R.id.tv_status_banner)
        tvMessageTitle = findViewById(R.id.tv_message_title)
        tvMessageExtra = findViewById(R.id.tv_message_extra)
        tvInitials = findViewById(R.id.tvInitials)
        tvNama = findViewById(R.id.tv_nama)
        tvNomorPendaftar = findViewById(R.id.tv_nomor_pendaftar)
        tvProgram = findViewById(R.id.tv_program)
        tvStatus = findViewById(R.id.tv_status)
        btnKembaliHome = findViewById(R.id.btn_kembali_home)
    }

    private fun loadHasilSeleksi() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid

        android.util.Log.d("HasilSeleksi", "========================================")
        android.util.Log.d("HasilSeleksi", "Loading hasil untuk userId: $userId")
        android.util.Log.d("HasilSeleksi", "========================================")

        firestore.collection("pendaftar")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    android.util.Log.d("HasilSeleksi", "❌ Tidak ada data pendaftaran")
                    Toast.makeText(this, "Anda belum melakukan pendaftaran", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val document = documents.documents[0]

                // Ambil data dari Firestore sesuai model pendaftar
                val status = document.getString("status") ?: "Pending"
                val name = document.getString("name") ?: ""
                val pendaftarId = document.getString("pendaftarId") ?: ""
                val prodiPilihan1 = document.getString("prodiPilihan1") ?: "Belum memilih program studi"

                android.util.Log.d("HasilSeleksi", "✓ Data loaded:")
                android.util.Log.d("HasilSeleksi", "  Name: $name")
                android.util.Log.d("HasilSeleksi", "  Pendaftar ID: $pendaftarId")
                android.util.Log.d("HasilSeleksi", "  Program Studi: $prodiPilihan1")
                android.util.Log.d("HasilSeleksi", "  Status: $status")

                // Set initials (ambil 2 huruf pertama dari nama)
                val initials = if (name.isNotEmpty()) {
                    val words = name.trim().split(" ")
                    if (words.size >= 2) {
                        "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
                    } else {
                        name.take(2).uppercase()
                    }
                } else {
                    "?"
                }
                tvInitials.text = initials

                // Set data ke UI
                tvNama.text = name
                tvNomorPendaftar.text = "Nomor Pendaftar: $pendaftarId"
                tvProgram.text = prodiPilihan1

                // Tampilkan hasil berdasarkan status
                when (status) {
                    "Approved" -> {
                        android.util.Log.d("HasilSeleksi", "✓ Status: LULUS")
                        showStatusLulus()
                    }
                    "Rejected" -> {
                        android.util.Log.d("HasilSeleksi", "✓ Status: TIDAK LULUS")
                        showStatusTidakLulus()
                    }
                    else -> {
                        android.util.Log.d("HasilSeleksi", "⚠ Status: PENDING")
                        Toast.makeText(
                            this,
                            "Status seleksi Anda masih dalam proses.\nSilakan cek kembali nanti.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("HasilSeleksi", "========================================")
                android.util.Log.e("HasilSeleksi", "❌ Error loading data", exception)
                android.util.Log.e("HasilSeleksi", "========================================")

                Toast.makeText(
                    this,
                    "Gagal memuat data: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
    }

    private fun showStatusLulus() {
        // Set banner warna hijau
        layoutStatusBanner.setBackgroundColor(
            ContextCompat.getColor(this, R.color.green)
        )
        tvStatusBanner.text = "Selamat! 🎉"

        // Set pesan
        tvMessageTitle.text = "Anda dinyatakan LULUS seleksi\npendaftaran mahasiswa baru"
        tvMessageExtra.visibility = View.GONE

        // Set status
        tvStatus.text = "Diterima"
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green))
    }

    private fun showStatusTidakLulus() {
        // Set banner warna merah
        layoutStatusBanner.setBackgroundColor(
            ContextCompat.getColor(this, R.color.red)
        )
        tvStatusBanner.text = "Mohon Maaf!"

        // Set pesan
        tvMessageTitle.text = "Anda dinyatakan TIDAK LULUS\nseleksi pendaftaran mahasiswa baru"
        tvMessageExtra.visibility = View.VISIBLE
        tvMessageExtra.text = "Jangan putus asa dan tetap semangat untuk kesempatan berikutnya!"

        // Set status
        tvStatus.text = "Tidak Diterima"
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red))
    }
}