package com.example.sia

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.sia.databinding.ActivityFormPendaftaranBinding
import com.example.sia.model.pendaftar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FormPendaftaran : AppCompatActivity() {

    private lateinit var binding: ActivityFormPendaftaranBinding

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Uri file yang dipilih
    private var ktpUri: Uri? = null
    private var ktpFileName: String = ""

    // Progress dialog
    private lateinit var progressDialog: ProgressDialog

    // Activity Result Launcher untuk memilih file
    private val pickKtpLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            ktpUri = it
            ktpFileName = getFileName(it)
            // Ubah text button menjadi nama file dan hilangkan icon
            binding.btnUploadKTP.text = ktpFileName
            binding.btnUploadKTP.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            Toast.makeText(this, "File KTP dipilih: $ktpFileName", Toast.LENGTH_SHORT).show()
            android.util.Log.d("FormPendaftaran", "File selected: $ktpFileName, URI: $ktpUri")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityFormPendaftaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Cek apakah user sudah login (Anonymous atau dengan Auth)
        if (auth.currentUser == null) {
            android.util.Log.d("FormPendaftaran", "No user logged in, signing in anonymously...")
            auth.signInAnonymously().addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                android.util.Log.d("FormPendaftaran", "✓ Anonymous login success! User ID: $userId")
            }.addOnFailureListener { exception ->
                android.util.Log.e("FormPendaftaran", "❌ Anonymous login failed!", exception)
                Toast.makeText(this, "Gagal memulai sesi: ${exception.message}", Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            android.util.Log.d(
                "FormPendaftaran",
                "✓ User already logged in: ${auth.currentUser?.uid}"
            )
        }

        // Inisialisasi Progress Dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Mohon tunggu...")
        progressDialog.setCancelable(false)

        // Inisialisasi Cloudinary
        initCloudinary()

        setupSpinners()
        setupClickListeners()
    }

    private fun initCloudinary() {
        try {
            // Konfigurasi Cloudinary
            val config = hashMapOf<String, Any>(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )

            android.util.Log.d("Cloudinary", "Initializing Cloudinary...")
            android.util.Log.d("Cloudinary", "Cloud Name: ${BuildConfig.CLOUDINARY_CLOUD_NAME}")

            MediaManager.init(this, config)

            android.util.Log.d("Cloudinary", "✓ Cloudinary initialized successfully!")

        } catch (e: Exception) {
            android.util.Log.e("Cloudinary", "❌ Failed to initialize Cloudinary!", e)
            Toast.makeText(this, "Error inisialisasi Cloudinary: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun setupClickListeners() {
        // Tombol Upload KTP
        binding.btnUploadKTP.setOnClickListener {
            openFilePicker()
        }

        // Tombol Next/Daftar
        binding.btnNext.setOnClickListener {
            android.util.Log.d("FormPendaftaran", "========================================")
            android.util.Log.d("FormPendaftaran", "Button NEXT clicked")
            android.util.Log.d("FormPendaftaran", "========================================")

            if (validateForm()) {
                android.util.Log.d("FormPendaftaran", "✓ Form validation passed")
                uploadToCloudinary()
            } else {
                android.util.Log.e("FormPendaftaran", "❌ Form validation failed")
                Toast.makeText(this, "Mohon lengkapi semua data wajib.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openFilePicker() {
        // Membuka file picker untuk image saja
        pickKtpLauncher.launch("image/*")
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "ktp_${System.currentTimeMillis()}.jpg"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                cursor.moveToFirst()
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun uploadToCloudinary() {
        android.util.Log.d("Cloudinary", "========================================")
        android.util.Log.d("Cloudinary", "Starting Cloudinary Upload Process")
        android.util.Log.d("Cloudinary", "========================================")

        progressDialog.show()
        progressDialog.setMessage("Mengunggah dokumen KTP...")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.util.Log.e("Cloudinary", "❌ Current user is NULL!")
            progressDialog.dismiss()
            Toast.makeText(this, "Sesi tidak valid, mohon coba lagi", Toast.LENGTH_SHORT).show()
            return
        }

        if (ktpUri == null) {
            android.util.Log.e("Cloudinary", "❌ KTP URI is NULL!")
            progressDialog.dismiss()
            Toast.makeText(this, "Mohon upload KTP terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        android.util.Log.d("Cloudinary", "User ID: $userId")
        android.util.Log.d("Cloudinary", "KTP URI: $ktpUri")
        android.util.Log.d("Cloudinary", "Dispatching upload request...")

        // Upload ke Cloudinary
        val requestId = MediaManager.get().upload(ktpUri)
            .option("folder", "sia_ktp") // Folder di Cloudinary
            .option("resource_type", "auto") // Auto detect type
            .option("public_id", "ktp_${userId}_${System.currentTimeMillis()}") // Unique filename
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    android.util.Log.d("Cloudinary", "→ Upload Started")
                    android.util.Log.d("Cloudinary", "  Request ID: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes * 100 / totalBytes).toInt()
                    android.util.Log.d(
                        "Cloudinary",
                        "→ Upload Progress: $progress% ($bytes/$totalBytes bytes)"
                    )

                    runOnUiThread {
                        progressDialog.setMessage("Mengunggah: $progress%")
                    }
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    android.util.Log.d("Cloudinary", "========================================")
                    android.util.Log.d("Cloudinary", "✓✓✓ UPLOAD SUCCESS ✓✓✓")
                    android.util.Log.d("Cloudinary", "========================================")
                    android.util.Log.d("Cloudinary", "Request ID: $requestId")

                    // Ambil URL gambar yang sudah di-upload
                    val imageUrl =
                        resultData["secure_url"] as? String ?: resultData["url"] as? String

                    if (imageUrl != null) {
                        android.util.Log.d("Cloudinary", "✓ Image URL: $imageUrl")

                        runOnUiThread {
                            progressDialog.setMessage("Menyimpan data...")
                            saveToFirestore(userId, imageUrl)
                        }
                    } else {
                        android.util.Log.e("Cloudinary", "❌ URL tidak ditemukan dalam result!")
                        android.util.Log.e("Cloudinary", "Full result: $resultData")

                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@FormPendaftaran,
                                "Upload gagal: URL tidak valid",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    android.util.Log.e("Cloudinary", "========================================")
                    android.util.Log.e("Cloudinary", "❌❌❌ UPLOAD ERROR ❌❌❌")
                    android.util.Log.e("Cloudinary", "========================================")
                    android.util.Log.e("Cloudinary", "Request ID: $requestId")
                    android.util.Log.e("Cloudinary", "Error Code: ${error.code}")
                    android.util.Log.e("Cloudinary", "Error Description: ${error.description}")

                    runOnUiThread {
                        progressDialog.dismiss()

                        val errorMessage = when (error.code) {
                            ErrorInfo.NETWORK_ERROR -> "Gagal upload: Tidak ada koneksi internet"
                            else -> when {
                                error.description?.contains(
                                    "authentication",
                                    ignoreCase = true
                                ) == true ->
                                    "Gagal upload: API key tidak valid"

                                error.description?.contains("invalid", ignoreCase = true) == true ->
                                    "Gagal upload: File tidak valid"

                                error.description?.contains(
                                    "unauthorized",
                                    ignoreCase = true
                                ) == true ->
                                    "Gagal upload: API key tidak valid"

                                else -> "Upload gagal: ${error.description ?: "Unknown error"}"
                            }
                        }

                        Toast.makeText(this@FormPendaftaran, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    android.util.Log.d("Cloudinary", "→ Upload rescheduled")
                    android.util.Log.d("Cloudinary", "  Request ID: $requestId")
                    android.util.Log.d("Cloudinary", "  Reason: ${error.description}")
                }
            })
            .dispatch()

        android.util.Log.d("Cloudinary", "✓ Upload dispatched with request ID: $requestId")
    }

    private fun saveToFirestore(userId: String, ktpUrl: String) {
        android.util.Log.d("Firestore", "========================================")
        android.util.Log.d("Firestore", "Starting Firestore Save Process")
        android.util.Log.d("Firestore", "========================================")

        // Generate ID untuk dokumen pendaftar
        val pendaftarRef = firestore.collection("pendaftar").document()
        val pendaftarId = pendaftarRef.id

        android.util.Log.d("Firestore", "Generated Pendaftar ID: $pendaftarId")
        android.util.Log.d("Firestore", "User ID: $userId")
        android.util.Log.d("Firestore", "KTP URL: $ktpUrl")

        val pendaftarData = pendaftar(
            userId = userId,
            pendaftarId = pendaftarId,
            name = binding.etNamaLengkap.text.toString().trim(),
            email = binding.etEmail.text.toString().trim(),
            prodiPilihan1 = binding.spinnerProdi1.selectedItem.toString(),
            prodiPilihan2 = binding.spinnerProdi2.selectedItem.toString(),
            prodiPilihan3 = binding.spinnerProdi3.selectedItem.toString(),
            gender = binding.spinnerJenisKelamin.selectedItem.toString(),
            ktpUrl = ktpUrl,
            status = "Pending"
            // submissionDate akan otomatis diisi oleh @ServerTimestamp
        )

        android.util.Log.d("Firestore", "Data to save:")
        android.util.Log.d("Firestore", "  - Name: ${pendaftarData.name}")
        android.util.Log.d("Firestore", "  - Email: ${pendaftarData.email}")
        android.util.Log.d("Firestore", "  - Prodi 1: ${pendaftarData.prodiPilihan1}")
        android.util.Log.d("Firestore", "  - Gender: ${pendaftarData.gender}")
        android.util.Log.d("Firestore", "  - Status: ${pendaftarData.status}")

        firestore.collection("pendaftar")
            .document(pendaftarId)
            .set(pendaftarData)
            .addOnSuccessListener {
                android.util.Log.d("Firestore", "========================================")
                android.util.Log.d("Firestore", "✓✓✓ FIRESTORE SAVE SUCCESS ✓✓✓")
                android.util.Log.d("Firestore", "========================================")

                progressDialog.dismiss()
                Toast.makeText(this, "Pendaftaran Tahap 1 berhasil!", Toast.LENGTH_SHORT).show()

                // Pindah ke FormPendaftaran2 (Tahap 2)
                val intent = Intent(this, FormPendaftaran2::class.java)
                intent.putExtra("PENDAFTAR_ID", pendaftarId)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("Firestore", "========================================")
                android.util.Log.e("Firestore", "❌❌❌ FIRESTORE SAVE FAILED ❌❌❌")
                android.util.Log.e("Firestore", "========================================")
                android.util.Log.e("Firestore", "Error: ${exception.message}", exception)

                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Gagal menyimpan: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setupSpinners() {
        // Data Program Studi
        val prodiOptions = arrayOf(
            "Pilih Program Studi",
            "Sistem Informasi",
            "Teknik Informatika",
            "Akuntansi",
            "Manajemen"
        )
        val prodiAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, prodiOptions)
        prodiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerProdi1.adapter = prodiAdapter
        binding.spinnerProdi2.adapter = prodiAdapter
        binding.spinnerProdi3.adapter = prodiAdapter

        // Data Jenis Kelamin
        val jkOptions = arrayOf("Pilih Jenis Kelamin", "Laki-laki", "Perempuan")
        val jkAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, jkOptions)
        jkAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerJenisKelamin.adapter = jkAdapter
    }

    private fun validateForm(): Boolean {
        android.util.Log.d("Validation", "========================================")
        android.util.Log.d("Validation", "Starting Form Validation")
        android.util.Log.d("Validation", "========================================")

        // Validasi Nama
        if (binding.etNamaLengkap.text.isNullOrBlank()) {
            android.util.Log.e("Validation", "❌ Nama lengkap is blank")
            binding.etNamaLengkap.error = "Nama lengkap wajib diisi"
            binding.etNamaLengkap.requestFocus()
            return false
        }
        android.util.Log.d("Validation", "✓ Nama lengkap: ${binding.etNamaLengkap.text}")

        // Validasi Email
        if (binding.etEmail.text.isNullOrBlank()) {
            android.util.Log.e("Validation", "❌ Email is blank")
            binding.etEmail.error = "Email wajib diisi"
            binding.etEmail.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text.toString())
                .matches()
        ) {
            android.util.Log.e("Validation", "❌ Email format invalid: ${binding.etEmail.text}")
            binding.etEmail.error = "Format email tidak valid"
            binding.etEmail.requestFocus()
            return false
        }
        android.util.Log.d("Validation", "✓ Email: ${binding.etEmail.text}")

        // Validasi Prodi 1 (wajib)
        if (binding.spinnerProdi1.selectedItemPosition == 0) {
            android.util.Log.e("Validation", "❌ Prodi 1 not selected")
            Toast.makeText(this, "Pilih Program Studi 1", Toast.LENGTH_SHORT).show()
            return false
        }
        android.util.Log.d("Validation", "✓ Prodi 1: ${binding.spinnerProdi1.selectedItem}")

        // Validasi Jenis Kelamin
        if (binding.spinnerJenisKelamin.selectedItemPosition == 0) {
            android.util.Log.e("Validation", "❌ Gender not selected")
            Toast.makeText(this, "Pilih Jenis Kelamin", Toast.LENGTH_SHORT).show()
            return false
        }
        android.util.Log.d("Validation", "✓ Gender: ${binding.spinnerJenisKelamin.selectedItem}")

        // Validasi KTP
        if (ktpUri == null) {
            android.util.Log.e("Validation", "❌ KTP URI is null")
            Toast.makeText(this, "Upload KTP terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }
        android.util.Log.d("Validation", "✓ KTP URI: $ktpUri")

        android.util.Log.d("Validation", "========================================")
        android.util.Log.d("Validation", "✓✓✓ ALL VALIDATIONS PASSED ✓✓✓")
        android.util.Log.d("Validation", "========================================")
        return true
    }
}