package com.example.sia

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.sia.databinding.ActivityFormPendaftaran2Binding
import com.google.firebase.firestore.FirebaseFirestore

class FormPendaftaran2 : AppCompatActivity() {

    private lateinit var binding: ActivityFormPendaftaran2Binding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    // Data dari Intent (dari FormPendaftaran)
    private var pendaftarId: String = ""
    private var userId: String = ""

    // URI file yang dipilih
    private var ijazahUri: Uri? = null
    private var sklUri: Uri? = null
    private var transkripUri: Uri? = null

    // URL hasil upload Cloudinary
    private var ijazahUrl: String = ""
    private var sklUrl: String = ""
    private var transkripUrl: String = ""

    // Status upload
    private var isIjazahUploaded: Boolean = false
    private var isSKLUploaded: Boolean = false
    private var isTranskripUploaded: Boolean = false

    // Activity Result Launchers
    private val pickIjazahLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            ijazahUri = it
            val fileName = getFileName(it)
            // Ubah text button dan hilangkan icon
            binding.btnUploadIjazah.text = fileName
            binding.btnUploadIjazah.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            Toast.makeText(this, "File Ijazah dipilih: $fileName", Toast.LENGTH_SHORT).show()
            android.util.Log.d("FormPendaftaran2", "Ijazah selected: $fileName")
        }
    }

    private val pickSKLLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            sklUri = it
            val fileName = getFileName(it)
            // Ubah text button dan hilangkan icon
            binding.btnUploadSKL.text = fileName
            binding.btnUploadSKL.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            Toast.makeText(this, "File SKL dipilih: $fileName", Toast.LENGTH_SHORT).show()
            android.util.Log.d("FormPendaftaran2", "SKL selected: $fileName")
        }
    }

    private val pickTranskripLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            transkripUri = it
            val fileName = getFileName(it)
            // Ubah text button dan hilangkan icon
            binding.btnUploadTranskrip.text = fileName
            binding.btnUploadTranskrip.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            Toast.makeText(this, "File Transkrip dipilih: $fileName", Toast.LENGTH_SHORT).show()
            android.util.Log.d("FormPendaftaran2", "Transkrip selected: $fileName")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityFormPendaftaran2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi Firebase Firestore
        firestore = FirebaseFirestore.getInstance()

        // Ambil data dari Intent
        pendaftarId = intent.getStringExtra("PENDAFTAR_ID") ?: ""
        userId = intent.getStringExtra("USER_ID") ?: ""

        android.util.Log.d("FormPendaftaran2", "========================================")
        android.util.Log.d("FormPendaftaran2", "Form Pendaftaran 2 Started")
        android.util.Log.d("FormPendaftaran2", "Pendaftar ID: $pendaftarId")
        android.util.Log.d("FormPendaftaran2", "User ID: $userId")
        android.util.Log.d("FormPendaftaran2", "========================================")

        if (pendaftarId.isEmpty() || userId.isEmpty()) {
            android.util.Log.e("FormPendaftaran2", "❌ Missing Pendaftar ID or User ID!")
            Toast.makeText(this, "Error: Data tidak lengkap", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Inisialisasi Progress Dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Mohon tunggu...")
        progressDialog.setCancelable(false)

        // Inisialisasi Cloudinary (sudah di-init di FormPendaftaran, tapi aman untuk double-init)
        initCloudinary()

        setupSpinners()
        updateInitialStatus()
        initListeners()
    }

    private fun initCloudinary() {
        try {
            // MediaManager.init() aman dipanggil multiple times
            // Jika sudah di-init, akan di-skip otomatis
            val config = hashMapOf<String, Any>(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            MediaManager.init(this, config)
            android.util.Log.d("Cloudinary", "✓ Cloudinary config set in FormPendaftaran2")
        } catch (e: IllegalStateException) {
            // Cloudinary sudah di-init sebelumnya (dari FormPendaftaran)
            android.util.Log.d("Cloudinary", "✓ Cloudinary already initialized")
        } catch (e: Exception) {
            android.util.Log.e("Cloudinary", "❌ Failed to initialize Cloudinary", e)
        }
    }

    private fun setupSpinners() {
        // Status Kelulusan
        val kelulusanOptions = arrayOf("Pilih Status", "Sudah", "Belum")
        val kelulusanAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kelulusanOptions)
        kelulusanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatusKelulusan.adapter = kelulusanAdapter

        // Ijazah Terakhir
        val ijazahOptions = arrayOf("Pilih Jenis Ijazah", "SMA", "SMK", "MA")
        val ijazahAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ijazahOptions)
        ijazahAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerIjazahTerakhir.adapter = ijazahAdapter
    }

    private fun updateInitialStatus() {
        binding.tvIjazahFileName.visibility = View.GONE
        binding.tvSKLFileName.visibility = View.GONE
        binding.tvTranskripFileName.visibility = View.GONE
    }

    private fun initListeners() {
        // Upload Ijazah Terakhir
        binding.btnUploadIjazah.setOnClickListener {
            pickIjazahLauncher.launch("image/*")
        }

        // Upload SKL
        binding.btnUploadSKL.setOnClickListener {
            pickSKLLauncher.launch("image/*")
        }

        // Upload Transkrip Nilai
        binding.btnUploadTranskrip.setOnClickListener {
            pickTranskripLauncher.launch("image/*")
        }

        // Tombol Next/Submit
        binding.btnNext.setOnClickListener {
            android.util.Log.d("FormPendaftaran2", "========================================")
            android.util.Log.d("FormPendaftaran2", "Button NEXT clicked")
            android.util.Log.d("FormPendaftaran2", "========================================")

            if (validateForm()) {
                android.util.Log.d("FormPendaftaran2", "✓ Form validation passed")
                startUploadProcess()
            } else {
                android.util.Log.e("FormPendaftaran2", "❌ Form validation failed")
                Toast.makeText(this, "Mohon lengkapi semua data dan upload dokumen wajib.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "doc_${System.currentTimeMillis()}.jpg"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                cursor.moveToFirst()
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun startUploadProcess() {
        android.util.Log.d("FormPendaftaran2", "========================================")
        android.util.Log.d("FormPendaftaran2", "Starting Upload Process")
        android.util.Log.d("FormPendaftaran2", "========================================")

        progressDialog.show()
        progressDialog.setMessage("Mengunggah dokumen...")

        // Upload berurutan: Ijazah/SKL -> Transkrip -> Save to Firestore
        uploadNextDocument()
    }

    private fun uploadNextDocument() {
        when {
            // 1. Upload Ijazah (jika ada)
            ijazahUri != null && !isIjazahUploaded -> {
                uploadToCloudinary(
                    uri = ijazahUri!!,
                    folder = "sia_ijazah",
                    docType = "Ijazah",
                    onSuccess = { url ->
                        ijazahUrl = url
                        isIjazahUploaded = true
                        uploadNextDocument() // Lanjut ke dokumen berikutnya
                    }
                )
            }
            // 2. Upload SKL (jika ada dan Ijazah tidak ada)
            sklUri != null && !isSKLUploaded && !isIjazahUploaded -> {
                uploadToCloudinary(
                    uri = sklUri!!,
                    folder = "sia_skl",
                    docType = "SKL",
                    onSuccess = { url ->
                        sklUrl = url
                        isSKLUploaded = true
                        uploadNextDocument()
                    }
                )
            }
            // 3. Upload Transkrip (wajib)
            transkripUri != null && !isTranskripUploaded -> {
                uploadToCloudinary(
                    uri = transkripUri!!,
                    folder = "sia_transkrip",
                    docType = "Transkrip",
                    onSuccess = { url ->
                        transkripUrl = url
                        isTranskripUploaded = true
                        uploadNextDocument()
                    }
                )
            }
            // 4. Semua upload selesai -> Save to Firestore
            else -> {
                android.util.Log.d("FormPendaftaran2", "✓ All uploads completed")
                saveToFirestore()
            }
        }
    }

    private fun uploadToCloudinary(
        uri: Uri,
        folder: String,
        docType: String,
        onSuccess: (String) -> Unit
    ) {
        android.util.Log.d("Cloudinary", "========================================")
        android.util.Log.d("Cloudinary", "Uploading $docType")
        android.util.Log.d("Cloudinary", "========================================")

        progressDialog.setMessage("Mengunggah $docType...")

        val requestId = MediaManager.get().upload(uri)
            .option("folder", folder)
            .option("resource_type", "auto")
            .option("public_id", "${folder}_${userId}_${System.currentTimeMillis()}")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    android.util.Log.d("Cloudinary", "→ Upload $docType started")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes * 100 / totalBytes).toInt()
                    android.util.Log.d("Cloudinary", "→ Upload $docType: $progress%")
                    runOnUiThread {
                        progressDialog.setMessage("Mengunggah $docType: $progress%")
                    }
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    android.util.Log.d("Cloudinary", "✓ Upload $docType SUCCESS")

                    val imageUrl = resultData["secure_url"] as? String ?: resultData["url"] as? String

                    if (imageUrl != null) {
                        android.util.Log.d("Cloudinary", "✓ $docType URL: $imageUrl")
                        runOnUiThread {
                            onSuccess(imageUrl)
                        }
                    } else {
                        android.util.Log.e("Cloudinary", "❌ URL not found for $docType")
                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@FormPendaftaran2,
                                "Upload $docType gagal: URL tidak valid",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    android.util.Log.e("Cloudinary", "❌ Upload $docType ERROR: ${error.description}")

                    runOnUiThread {
                        progressDialog.dismiss()

                        val errorMessage = when (error.code) {
                            ErrorInfo.NETWORK_ERROR -> "Upload $docType gagal: Tidak ada koneksi internet"
                            else -> "Upload $docType gagal: ${error.description}"
                        }

                        Toast.makeText(this@FormPendaftaran2, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    android.util.Log.d("Cloudinary", "→ Upload $docType rescheduled")
                }
            })
            .dispatch()

        android.util.Log.d("Cloudinary", "✓ Upload $docType dispatched: $requestId")
    }

    private fun saveToFirestore() {
        android.util.Log.d("Firestore", "========================================")
        android.util.Log.d("Firestore", "Updating Firestore Document")
        android.util.Log.d("Firestore", "========================================")

        progressDialog.setMessage("Menyimpan data...")

        val updateData = hashMapOf<String, Any>(
            "sekolahAsal" to binding.etSekolahAsal.text.toString().trim(),
            "statusKelulusan" to binding.spinnerStatusKelulusan.selectedItem.toString(),
            "ijazahTerakhir" to binding.spinnerIjazahTerakhir.selectedItem.toString()
        )

        // Tambahkan URL dokumen yang sudah diupload
        if (ijazahUrl.isNotEmpty()) {
            updateData["ijazahUrl"] = ijazahUrl
            android.util.Log.d("Firestore", "  - Ijazah URL: $ijazahUrl")
        }
        if (sklUrl.isNotEmpty()) {
            updateData["sklUrl"] = sklUrl
            android.util.Log.d("Firestore", "  - SKL URL: $sklUrl")
        }
        if (transkripUrl.isNotEmpty()) {
            updateData["transkripNilaiUrl"] = transkripUrl
            android.util.Log.d("Firestore", "  - Transkrip URL: $transkripUrl")
        }

        android.util.Log.d("Firestore", "Updating document: $pendaftarId")
        android.util.Log.d("Firestore", "Data: $updateData")

        firestore.collection("pendaftar")
            .document(pendaftarId)
            .update(updateData)
            .addOnSuccessListener {
                android.util.Log.d("Firestore", "========================================")
                android.util.Log.d("Firestore", "✓✓✓ FIRESTORE UPDATE SUCCESS ✓✓✓")
                android.util.Log.d("Firestore", "========================================")

                progressDialog.dismiss()
                Toast.makeText(
                    this,
                    "Pendaftaran selesai! Data Anda akan diverifikasi.",
                    Toast.LENGTH_LONG
                ).show()

                // Navigasi ke Dashboard
                val intent = Intent(this, DashboardMahasiswa::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("Firestore", "========================================")
                android.util.Log.e("Firestore", "❌❌❌ FIRESTORE UPDATE FAILED ❌❌❌")
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

    private fun validateForm(): Boolean {
        android.util.Log.d("Validation", "========================================")
        android.util.Log.d("Validation", "Starting Form Validation")
        android.util.Log.d("Validation", "========================================")

        // 1. Validasi Sekolah Asal
        if (binding.etSekolahAsal.text.isNullOrBlank()) {
            android.util.Log.e("Validation", "❌ Sekolah asal is blank")
            binding.etSekolahAsal.error = "Nama sekolah asal wajib diisi"
            binding.etSekolahAsal.requestFocus()
            return false
        }
        android.util.Log.d("Validation", "✓ Sekolah asal: ${binding.etSekolahAsal.text}")

        // 2. Validasi Status Kelulusan
        if (binding.spinnerStatusKelulusan.selectedItemPosition == 0) {
            android.util.Log.e("Validation", "❌ Status kelulusan not selected")
            Toast.makeText(this, "Pilih status kelulusan", Toast.LENGTH_SHORT).show()
            return false
        }
        android.util.Log.d("Validation", "✓ Status kelulusan: ${binding.spinnerStatusKelulusan.selectedItem}")

        // 3. Validasi Ijazah Terakhir
        if (binding.spinnerIjazahTerakhir.selectedItemPosition == 0) {
            android.util.Log.e("Validation", "❌ Ijazah terakhir not selected")
            Toast.makeText(this, "Pilih jenis ijazah terakhir", Toast.LENGTH_SHORT).show()
            return false
        }
        android.util.Log.d("Validation", "✓ Ijazah terakhir: ${binding.spinnerIjazahTerakhir.selectedItem}")

        // 4. Validasi Dokumen: Wajib Ijazah ATAU SKL
        val hasIjazahOrSKL = ijazahUri != null || sklUri != null
        if (!hasIjazahOrSKL) {
            android.util.Log.e("Validation", "❌ No Ijazah or SKL uploaded")
            Toast.makeText(this, "Wajib upload Ijazah Terakhir atau SKL", Toast.LENGTH_SHORT).show()
            return false
        }
        android.util.Log.d("Validation", "✓ Ijazah/SKL: ${if (ijazahUri != null) "Ijazah" else "SKL"}")

        // 5. Validasi Transkrip (Wajib)
        if (transkripUri == null) {
            android.util.Log.e("Validation", "❌ No Transkrip uploaded")
            Toast.makeText(this, "Wajib upload Transkrip Nilai/Rapor", Toast.LENGTH_SHORT).show()
            return false
        }
        android.util.Log.d("Validation", "✓ Transkrip: $transkripUri")

        android.util.Log.d("Validation", "========================================")
        android.util.Log.d("Validation", "✓✓✓ ALL VALIDATIONS PASSED ✓✓✓")
        android.util.Log.d("Validation", "========================================")
        return true
    }
}