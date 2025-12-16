package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatusPengumumanActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvInitials: TextView
    private lateinit var tvNamaPendaftar: TextView
    private lateinit var tvNomorPendaftar: TextView
    private lateinit var btnLihatHasil: Button

    // Bottom Navigation
    private lateinit var navHome: LinearLayout
    private lateinit var navDaftarLayout: LinearLayout
    private lateinit var navHistoryLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status_pengumuman)

        // Inisialisasi Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inisialisasi Views
        val btnBack = findViewById<ImageView>(R.id.btn_back)
        tvInitials = findViewById(R.id.tvInitials)
        tvNamaPendaftar = findViewById(R.id.tv_nama_pendaftar)
        tvNomorPendaftar = findViewById(R.id.tv_nomor_pendaftar)
        btnLihatHasil = findViewById(R.id.btnLihatHasil)

        // Bottom Navigation
        navHome = findViewById(R.id.navHome)
        navDaftarLayout = findViewById(R.id.navDaftarLayout)
        navHistoryLayout = findViewById(R.id.navHistoryLayout)


        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Load status pendaftaran
        loadStatusPendaftaran()

        // Button lihat hasil
        btnLihatHasil.setOnClickListener {
            checkStatusAndNavigate()
        }

        // Bottom Navigation Listeners
        navHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        navDaftarLayout.setOnClickListener {
            // Intent ke TataCaraPendaftaranActivity
            val intent = Intent(this, TataCaraPendaftaran::class.java)
            startActivity(intent)
        }
        navHistoryLayout.setOnClickListener {
            // Already on this page, do nothing or refresh
            loadStatusPendaftaran()
        }
    }

    private fun loadStatusPendaftaran() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User tidak terautentikasi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = currentUser.uid

        // Disable button sementara
        btnLihatHasil.isEnabled = false

        android.util.Log.d("StatusPengumuman", "Loading status untuk userId: $userId")

        firestore.collection("pendaftar")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    android.util.Log.d("StatusPengumuman", "Tidak ada data pendaftaran")
                    showStatusBelumDaftar()
                    return@addOnSuccessListener
                }

                val document = documents.documents[0]
                val status = document.getString("status") ?: "Pending"
                val name = document.getString("name") ?: ""
                val pendaftarId = document.getString("pendaftarId") ?: ""

                android.util.Log.d("StatusPengumuman", "Status: $status, Name: $name")

                // Set nama dan nomor pendaftar
                tvNamaPendaftar.text = name
                tvNomorPendaftar.text = "No. Pendaftar: $pendaftarId"

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

                // Tampilkan status berdasarkan kondisi
                when (status) {
                    "Approved" -> {
                        btnLihatHasil.isEnabled = true
                        btnLihatHasil.text = "Lihat Hasil Seleksi"
                    }
                    "Rejected" -> {
                        btnLihatHasil.isEnabled = true
                        btnLihatHasil.text = "Lihat Hasil Seleksi"
                    }
                    else -> {
                        // Status Pending - button disabled
                        btnLihatHasil.isEnabled = false
                        btnLihatHasil.text = "Hasil Belum Tersedia"
                        btnLihatHasil.alpha = 0.5f
                        Toast.makeText(
                            this,
                            "Hasil seleksi belum diumumkan. Silakan cek kembali nanti.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("StatusPengumuman", "Error loading data", exception)
                Toast.makeText(
                    this,
                    "Gagal memuat data: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                btnLihatHasil.isEnabled = false
            }
    }

    private fun checkStatusAndNavigate() {
        val currentUser = auth.currentUser ?: return

        firestore.collection("pendaftar")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val status = documents.documents[0].getString("status") ?: "Pending"

                    when (status) {
                        "Approved", "Rejected" -> {
                            // Navigate to hasil seleksi
                            val intent = Intent(this, HasilSeleksi::class.java)
                            startActivity(intent)
                        }
                        else -> {
                            Toast.makeText(
                                this,
                                "Hasil seleksi belum diumumkan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
    }

    private fun showStatusBelumDaftar() {
        tvNamaPendaftar.text = "Belum Mendaftar"
        tvNomorPendaftar.text = "No. Pendaftar: -"
        tvInitials.text = "?"
        btnLihatHasil.isEnabled = false
        btnLihatHasil.text = "Belum Ada Data"
        btnLihatHasil.alpha = 0.5f

        Toast.makeText(
            this,
            "Anda belum melakukan pendaftaran",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data saat kembali ke activity
        loadStatusPendaftaran()
    }
}