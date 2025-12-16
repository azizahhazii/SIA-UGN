package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sia.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sembunyikan Action Bar
        supportActionBar?.hide()

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Button Register
        binding.registerButton.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            // Validasi input
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Password tidak cocok", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            registerUser(fullName, email, password)
        }

        // Link ke Login
        binding.tvLoginLink.setOnClickListener {
            navigateToLogin(false)
        }
    }

    private fun registerUser(fullName: String, email: String, password: String) {
        android.util.Log.d("RegisterActivity", "========================================")
        android.util.Log.d("RegisterActivity", "Starting registration process")
        android.util.Log.d("RegisterActivity", "Email: $email")
        android.util.Log.d("RegisterActivity", "========================================")

        // Disable tombol saat proses register
        binding.registerButton.isEnabled = false
        binding.registerButton.text = "Loading..."

        // Register dengan Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("RegisterActivity", "✓ Firebase Auth registration success")

                    // Register berhasil
                    val userId = auth.currentUser?.uid

                    if (userId == null) {
                        android.util.Log.e("RegisterActivity", "❌ User ID is null!")
                        binding.registerButton.isEnabled = true
                        binding.registerButton.text = "Register"
                        Toast.makeText(this, "Error: User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    android.util.Log.d("RegisterActivity", "✓ User ID: $userId")

                    // Data user yang akan disimpan
                    val userData = hashMapOf(
                        "fullName" to fullName,
                        "email" to email,
                        "role" to "mahasiswa",  // Default role untuk user biasa
                        "createdAt" to System.currentTimeMillis()
                    )

                    android.util.Log.d("RegisterActivity", "Saving user data to Firestore...")
                    android.util.Log.d("RegisterActivity", "Data: $userData")

                    // ✅ TUNGGU SAMPAI FIRESTORE SELESAI MENYIMPAN!
                    firestore.collection("users").document(userId).set(userData)
                        .addOnSuccessListener {
                            android.util.Log.d("RegisterActivity", "========================================")
                            android.util.Log.d("RegisterActivity", "✓✓✓ SUCCESS: User data saved to Firestore")
                            android.util.Log.d("RegisterActivity", "Document ID: $userId")
                            android.util.Log.d("RegisterActivity", "========================================")

                            // Logout user agar harus login manual
                            auth.signOut()
                            android.util.Log.d("RegisterActivity", "✓ User logged out")

                            // Enable button kembali
                            binding.registerButton.isEnabled = true
                            binding.registerButton.text = "Register"

                            // Tampilkan pesan sukses
                            Toast.makeText(
                                this,
                                "Registrasi berhasil! Silakan login dengan akun Anda",
                                Toast.LENGTH_LONG
                            ).show()

                            // Navigate ke Login
                            navigateToLogin(true)
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("RegisterActivity", "========================================")
                            android.util.Log.e("RegisterActivity", "❌❌❌ FAILED: Could not save user data to Firestore")
                            android.util.Log.e("RegisterActivity", "Error: ${e.message}", e)
                            android.util.Log.e("RegisterActivity", "========================================")

                            // Gagal simpan ke Firestore
                            binding.registerButton.isEnabled = true
                            binding.registerButton.text = "Register"

                            Toast.makeText(
                                this,
                                "Gagal menyimpan data user: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            // Opsional: Hapus user dari Authentication jika gagal simpan ke Firestore
                            auth.currentUser?.delete()?.addOnCompleteListener {
                                android.util.Log.d("RegisterActivity", "User deleted from Authentication due to Firestore failure")
                            }
                        }

                } else {
                    android.util.Log.e("RegisterActivity", "========================================")
                    android.util.Log.e("RegisterActivity", "❌ Firebase Auth registration failed")
                    android.util.Log.e("RegisterActivity", "Error: ${task.exception?.message}", task.exception)
                    android.util.Log.e("RegisterActivity", "========================================")

                    // Register gagal
                    binding.registerButton.isEnabled = true
                    binding.registerButton.text = "Register"

                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "Email sudah terdaftar"
                        task.exception?.message?.contains("network") == true ->
                            "Tidak ada koneksi internet"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "Format email tidak valid"
                        else -> "Registrasi gagal: ${task.exception?.message}"
                    }

                    Toast.makeText(
                        this,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToLogin(isFromRegister: Boolean) {
        android.util.Log.d("RegisterActivity", "Navigating to LoginActivity")

        val intent = Intent(this, LoginActivity::class.java)

        // Kirim flag bahwa user baru saja register
        if (isFromRegister) {
            intent.putExtra("REGISTRATION_SUCCESS", true)
        }

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    // Prevent back button saat proses registrasi
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.registerButton.isEnabled) {
            super.onBackPressed()
        } else {
            Toast.makeText(this, "Mohon tunggu proses registrasi selesai", Toast.LENGTH_SHORT).show()
        }
    }
}