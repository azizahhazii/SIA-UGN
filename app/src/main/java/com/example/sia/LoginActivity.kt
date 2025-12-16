package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sia.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sembunyikan Action Bar
        supportActionBar?.hide()

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Cek apakah user sudah login sebelumnya
        checkCurrentUser()

        // Cek apakah user baru saja berhasil registrasi
        if (intent.getBooleanExtra("REGISTRATION_SUCCESS", false)) {
            Toast.makeText(
                this,
                "Registrasi berhasil! Silakan login dengan akun Anda",
                Toast.LENGTH_LONG
            ).show()
        }

        // Button Login
        binding.loginButton.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validasi input
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        // Link ke Register
        binding.tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

    }

    private fun checkCurrentUser() {
        // Cek apakah user sudah login
        val currentUser = auth.currentUser
        if (currentUser != null) {
            android.util.Log.d("LoginActivity", "User already logged in: ${currentUser.uid}")
            // User sudah login, cek role dan navigasi
            getUserDataAndNavigate(currentUser.uid, isAutoLogin = true)
        }
    }

    private fun loginUser(email: String, password: String) {
        android.util.Log.d("LoginActivity", "========================================")
        android.util.Log.d("LoginActivity", "Login attempt with email: $email")
        android.util.Log.d("LoginActivity", "========================================")

        // Disable tombol saat proses login
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Loading..."

        // Login dengan Firebase Auth
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("LoginActivity", "✓ Firebase Auth login success")

                    // Login berhasil
                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        android.util.Log.d("LoginActivity", "✓ User ID: $userId")
                        // Ambil data user dari Firestore untuk verifikasi role
                        getUserDataAndNavigate(userId, isAutoLogin = false)
                    } else {
                        android.util.Log.e("LoginActivity", "❌ User ID is null!")
                        binding.loginButton.isEnabled = true
                        binding.loginButton.text = "Login"
                        Toast.makeText(this, "Error: User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.util.Log.e("LoginActivity", "❌ Firebase Auth login failed")
                    android.util.Log.e("LoginActivity", "Error: ${task.exception?.message}")

                    // Login gagal
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Login"

                    val errorMessage = when {
                        task.exception?.message?.contains("no user record") == true ||
                                task.exception?.message?.contains("invalid-credential") == true ||
                                task.exception?.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ->
                            "Email atau password salah"
                        task.exception?.message?.contains("network") == true ->
                            "Tidak ada koneksi internet"
                        else -> "Login gagal: ${task.exception?.message}"
                    }

                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun getUserDataAndNavigate(userId: String, isAutoLogin: Boolean = false) {
        android.util.Log.d("LoginActivity", "========================================")
        android.util.Log.d("LoginActivity", "Fetching user data from Firestore")
        android.util.Log.d("LoginActivity", "User ID: $userId")
        android.util.Log.d("LoginActivity", "========================================")

        // Ambil data user dari Firestore
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                binding.loginButton.isEnabled = true
                binding.loginButton.text = "Login"

                if (document.exists()) {
                    val role = document.getString("role") ?: "mahasiswa"
                    val fullName = document.getString("fullName") ?: "User"

                    android.util.Log.d("LoginActivity", "✓ User data found")
                    android.util.Log.d("LoginActivity", "  - Full Name: $fullName")
                    android.util.Log.d("LoginActivity", "  - Role: $role")
                    android.util.Log.d("LoginActivity", "  - Role (lowercase): ${role.lowercase()}")
                    android.util.Log.d("LoginActivity", "  - Role equals 'admin': ${role.lowercase() == "admin"}")

                    // DEBUG: Tampilkan semua field di document
                    android.util.Log.d("LoginActivity", "  - All document data: ${document.data}")

                    // Tampilkan pesan selamat datang (kecuali auto-login)
                    if (!isAutoLogin) {
                        Toast.makeText(
                            this,
                            "Selamat datang, $fullName! (Role: $role)",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Navigasi berdasarkan role
                    navigateBasedOnRole(role, fullName)
                } else {
                    android.util.Log.w("LoginActivity", "⚠ User document not found in Firestore")
                    android.util.Log.w("LoginActivity", "  Defaulting to mahasiswa dashboard")

                    // Document tidak ditemukan, default ke mahasiswa
                    if (!isAutoLogin) {
                        Toast.makeText(
                            this,
                            "Login berhasil!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    navigateBasedOnRole("mahasiswa", "User")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("LoginActivity", "❌ Failed to fetch user data from Firestore")
                android.util.Log.e("LoginActivity", "Error: ${e.message}", e)

                // Jika gagal ambil data, default ke mahasiswa
                binding.loginButton.isEnabled = true
                binding.loginButton.text = "Login"

                if (!isAutoLogin) {
                    Toast.makeText(
                        this,
                        "Login berhasil!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                navigateBasedOnRole("mahasiswa", "User")
            }
    }

    private fun navigateBasedOnRole(role: String, userName: String) {
        android.util.Log.d("LoginActivity", "========================================")
        android.util.Log.d("LoginActivity", "navigateBasedOnRole() CALLED")
        android.util.Log.d("LoginActivity", "  - Original role: '$role'")
        android.util.Log.d("LoginActivity", "  - Role lowercase: '${role.lowercase()}'")
        android.util.Log.d("LoginActivity", "  - Role trim: '${role.trim()}'")
        android.util.Log.d("LoginActivity", "========================================")

        val roleLowercase = role.trim().lowercase()

        val targetActivity = when (roleLowercase) {
            "admin" -> {
                android.util.Log.d("LoginActivity", "✓✓✓ MATCHED 'admin' - Going to DashboardAdmin")
                "DashboardAdmin"
            }
            "mahasiswa", "user" -> {
                android.util.Log.d("LoginActivity", "✓ Matched 'mahasiswa' or 'user' - Going to DashboardMahasiswa")
                "DashboardMahasiswa"
            }
            else -> {
                android.util.Log.w("LoginActivity", "⚠ No match for role '$roleLowercase' - Defaulting to DashboardMahasiswa")
                "DashboardMahasiswa"
            }
        }

        val intent = when (targetActivity) {
            "DashboardAdmin" -> {
                android.util.Log.d("LoginActivity", "→→→ Creating Intent for DashboardAdmin")
                Intent(this, DashboardAdmin::class.java).apply {
                    putExtra("USER_NAME", userName)
                    putExtra("USER_ROLE", role)
                }
            }
            else -> {
                android.util.Log.d("LoginActivity", "→ Creating Intent for DashboardMahasiswa")
                Intent(this, DashboardMahasiswa::class.java).apply {
                    putExtra("USER_NAME", userName)
                    putExtra("USER_ROLE", role)
                }
            }
        }

        android.util.Log.d("LoginActivity", "========================================")
        android.util.Log.d("LoginActivity", "Starting Activity: $targetActivity")
        android.util.Log.d("LoginActivity", "========================================")

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showForgotPasswordDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Masukkan email Anda"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        builder.setTitle("Reset Password")
        builder.setMessage("Masukkan email untuk reset password")
        builder.setView(input)

        builder.setPositiveButton("Kirim") { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Email reset password telah dikirim ke $email",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal mengirim email: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}