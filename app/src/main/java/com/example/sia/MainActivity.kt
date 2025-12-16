package com.example.sia

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set transparent background untuk menghilangkan flash putih
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // JANGAN SET CONTENT VIEW - langsung redirect

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Cek apakah user sudah login
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User sudah login, cek role
            val userId = currentUser.uid

            // Ambil role dari Firestore
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role") ?: "user"
                        val fullName = document.getString("fullName") ?: ""

                        when (role) {
                            "admin" -> {
                                // Redirect ke Dashboard Admin
                                val intent = Intent(this, DashboardAdmin::class.java)
                                intent.putExtra("USER_NAME", fullName)
                                intent.putExtra("USER_ROLE", role)
                                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                                startActivity(intent)
                            }
                            else -> {
                                // Redirect ke Dashboard Mahasiswa
                                val intent = Intent(this, DashboardMahasiswa::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                                startActivity(intent)
                            }
                        }
                    } else {
                        // Document tidak ada, redirect ke Dashboard Mahasiswa
                        val intent = Intent(this, DashboardMahasiswa::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                        startActivity(intent)
                    }
                    finish()
                    overridePendingTransition(0, 0) // No animation
                }
                .addOnFailureListener {
                    // Jika error, redirect ke Dashboard Mahasiswa sebagai default
                    val intent = Intent(this, DashboardMahasiswa::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    startActivity(intent)
                    finish()
                    overridePendingTransition(0, 0)
                }

        } else {
            // User belum login, redirect ke Login
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            startActivity(intent)
            finish()
            overridePendingTransition(0, 0)
        }
    }
}