package com.example.sia

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class VerificationDokumen : AppCompatActivity() {

    private var idPendaftar: Int = -1
    private var namaPendaftar: String? = null
    private var pendaftarIdString: String = ""
    private lateinit var firestore: FirebaseFirestore

    // Status untuk setiap dokumen
    private var ktpStatus: String = "Pending"
    private var ijazahStatus: String = "Pending"
    private var sklStatus: String = "Pending"
    private var transkripStatus: String = "Pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_verification_dokumen)

        // 1. Edge-to-Edge Setup
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi Firestore
        firestore = FirebaseFirestore.getInstance()

        // 2. AMBIL DATA DARI INTENT (dari Adapter)
        idPendaftar = intent.getIntExtra(IntentKeys.ID_PENDAFTAR, -1)
        namaPendaftar = intent.getStringExtra(IntentKeys.NAMA_PENDAFTAR)

        // 3. TEMUKAN ELEMEN UI
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val btnApproveAll = findViewById<CardView>(R.id.btnApproveAll)
        val btnRejectAll = findViewById<CardView>(R.id.btnRejectAll)

        // Kontainer Dokumen
        val documentKtp = findViewById<View>(R.id.documentKtp)
        val documentAkta = findViewById<View>(R.id.documentAkta)
        val documentKk = findViewById<View>(R.id.documentKk)
        val documentIjazah = findViewById<View>(R.id.documentIjazah)
        val documentSkl = findViewById<View>(R.id.documentSkl)
        val documentTranskrip = findViewById<View>(R.id.documentTranskrip)

        // Sembunyikan semua dokumen dulu
        documentKtp.visibility = View.GONE
        documentAkta.visibility = View.GONE
        documentKk.visibility = View.GONE
        documentIjazah.visibility = View.GONE
        documentSkl.visibility = View.GONE
        documentTranskrip.visibility = View.GONE

        // 4. TAMPILKAN DATA & VALIDASI
        if (idPendaftar != -1 && !namaPendaftar.isNullOrEmpty()) {
            tvUserName.text = "$namaPendaftar - Verifikasi Dokumen"

            // Load data dari Firestore
            loadPendaftarData(documentKtp, documentIjazah, documentSkl, documentTranskrip)

        } else {
            Toast.makeText(this, "Kesalahan: Data pendaftar tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
        }

        // 5. PENANGANAN KLIK

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnApproveAll.setOnClickListener {
            showConfirmationDialog(
                title = "Konfirmasi Persetujuan",
                message = "Apakah Anda yakin ingin meluluskan pendaftar ini?\n\nSemua dokumen akan disetujui dan status akan berubah menjadi Lulus.",
                confirmText = "Ya, Luluskan",
                cancelText = "Batal",
                isApprove = true,
                onConfirm = {
                    performVerificationAll("Approved")
                }
            )
        }

        btnRejectAll.setOnClickListener {
            showConfirmationDialog(
                title = "Konfirmasi Penolakan",
                message = "Apakah Anda yakin ingin menolak pendaftar ini?\n\nSemua dokumen akan ditolak dan status akan berubah menjadi Tidak Lulus.",
                confirmText = "Ya, Tolak",
                cancelText = "Batal",
                isApprove = false,
                onConfirm = {
                    performVerificationAll("Rejected")
                }
            )
        }
    }

    /**
     * Load data pendaftar dari Firestore
     */
    private fun loadPendaftarData(
        documentKtp: View,
        documentIjazah: View,
        documentSkl: View,
        documentTranskrip: View
    ) {
        android.util.Log.d("VerificationDokumen", "========================================")
        android.util.Log.d("VerificationDokumen", "Loading Pendaftar Data")
        android.util.Log.d("VerificationDokumen", "Name: $namaPendaftar")
        android.util.Log.d("VerificationDokumen", "========================================")

        // Query berdasarkan nama
        firestore.collection("pendaftar")
            .whereEqualTo("name", namaPendaftar)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val pendaftarData = document.toObject(com.example.sia.model.pendaftar::class.java)

                    if (pendaftarData != null) {
                        pendaftarIdString = pendaftarData.pendaftarId

                        android.util.Log.d("VerificationDokumen", "✓ Pendaftar found: ${pendaftarData.pendaftarId}")

                        // Setup dokumen dengan URL dari database
                        // Hanya tampilkan jika URL tidak kosong
                        if (pendaftarData.ktpUrl.isNotEmpty()) {
                            initializeDocumentView(
                                documentKtp,
                                "KTP/Identitas",
                                pendaftarData.ktpUrl,
                                "ktp"
                            )
                        } else {
                            documentKtp.visibility = View.GONE
                        }

                        if (pendaftarData.ijazahUrl.isNotEmpty()) {
                            initializeDocumentView(
                                documentIjazah,
                                "Ijazah Terakhir",
                                pendaftarData.ijazahUrl,
                                "ijazah"
                            )
                        } else {
                            documentIjazah.visibility = View.GONE
                        }

                        if (pendaftarData.sklUrl.isNotEmpty()) {
                            initializeDocumentView(
                                documentSkl,
                                "SKL (Surat Keterangan Lulus)",
                                pendaftarData.sklUrl,
                                "skl"
                            )
                        } else {
                            documentSkl.visibility = View.GONE
                        }

                        if (pendaftarData.transkripNilaiUrl.isNotEmpty()) {
                            initializeDocumentView(
                                documentTranskrip,
                                "Transkrip Nilai/Rapor",
                                pendaftarData.transkripNilaiUrl,
                                "transkrip"
                            )
                        } else {
                            documentTranskrip.visibility = View.GONE
                        }

                    } else {
                        Toast.makeText(this, "Data pendaftar tidak ditemukan", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    android.util.Log.e("VerificationDokumen", "❌ No document found")
                    Toast.makeText(this, "Data pendaftar tidak ditemukan di database", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("VerificationDokumen", "❌ Error loading data", exception)
                Toast.makeText(this, "Gagal memuat data: ${exception.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    /**
     * Fungsi untuk menginisialisasi tampilan item dokumen.
     */
    private fun initializeDocumentView(docView: View, title: String, fileUrl: String, docType: String) {
        // Sesuaikan dengan ID di item_document.xml yang ada
        val tvDocumentLabel = docView.findViewById<TextView>(R.id.tvDocumentLabel)
        val tvFileName = docView.findViewById<TextView>(R.id.tvFileName)
        val btnViewDoc = docView.findViewById<ImageView>(R.id.btnView)
        val btnApprove = docView.findViewById<CardView>(R.id.btnApprove)
        val btnReject = docView.findViewById<CardView>(R.id.btnReject)
        val cardStatusBadge = docView.findViewById<CardView>(R.id.cardStatusBadge)
        val tvStatusBadge = docView.findViewById<TextView>(R.id.tvStatusBadge)

        // Set title
        tvDocumentLabel.text = title

        // Set status awal ke Pending
        updateStatusUI(cardStatusBadge, tvStatusBadge, "Pending")

        android.util.Log.d("VerificationDokumen", "✓ $title URL: $fileUrl")

        // Ekstrak nama file dari URL
        val fileName = extractFileName(fileUrl)
        tvFileName.text = fileName

        // Tampilkan gambar dengan Glide ketika diklik view button
        btnViewDoc.setOnClickListener {
            showImageDialog(fileUrl, title)
        }

        // Button Approve individual
        btnApprove.setOnClickListener {
            updateDocumentStatus(docType, "Approved")
            updateStatusUI(cardStatusBadge, tvStatusBadge, "Approved")
        }

        // Button Reject individual
        btnReject.setOnClickListener {
            updateDocumentStatus(docType, "Rejected")
            updateStatusUI(cardStatusBadge, tvStatusBadge, "Rejected")
        }

        // Tampilkan view
        docView.visibility = View.VISIBLE
    }

    /**
     * Ekstrak nama file dari URL Cloudinary
     */
    private fun extractFileName(url: String): String {
        return try {
            val fileName = url.substringAfterLast("/").substringBefore("?")
            if (fileName.length > 30) {
                fileName.take(27) + "..."
            } else {
                fileName
            }
        } catch (e: Exception) {
            "document.jpg"
        }
    }

    /**
     * Update status UI dengan CardView dan TextView
     */
    private fun updateStatusUI(cardStatusBadge: CardView, tvStatusBadge: TextView, status: String) {
        tvStatusBadge.text = status

        when (status) {
            "Approved" -> {
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#4CAF50")) // Hijau
                tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"))
                cardStatusBadge.visibility = View.VISIBLE
            }
            "Rejected" -> {
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#E57373")) // Merah
                tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"))
                cardStatusBadge.visibility = View.VISIBLE
            }
            "Pending" -> {
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#FFC107")) // Kuning/Orange
                tvStatusBadge.setTextColor(Color.parseColor("#000000")) // Text hitam untuk Pending
                cardStatusBadge.visibility = View.VISIBLE
            }
            "Tidak Ada" -> {
                cardStatusBadge.setCardBackgroundColor(Color.parseColor("#9E9E9E")) // Abu-abu
                tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"))
                cardStatusBadge.alpha = 0.5f
                cardStatusBadge.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Tampilkan gambar dalam dialog fullscreen
     */
    private fun showImageDialog(imageUrl: String, title: String) {
        android.util.Log.d("VerificationDokumen", "Opening image: $imageUrl")

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_viewer)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        val imageView = dialog.findViewById<ImageView>(R.id.fullscreenImage)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvImageTitle)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseDialog)

        tvTitle.text = title

        // Load gambar dengan Glide
        Glide.with(this)
            .load(imageUrl)
            .placeholder(android.R.drawable.progress_indeterminate_horizontal)
            .error(android.R.drawable.stat_notify_error)
            .into(imageView)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Tampilkan dialog konfirmasi custom yang keren
     */
    private fun showConfirmationDialog(
        title: String,
        message: String,
        confirmText: String,
        cancelText: String,
        isApprove: Boolean,
        onConfirm: () -> Unit
    ) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialog.findViewById<TextView>(R.id.tvDialogMessage)
        val btnConfirm = dialog.findViewById<CardView>(R.id.btnDialogConfirm)
        val btnCancel = dialog.findViewById<CardView>(R.id.btnDialogCancel)
        val tvConfirm = dialog.findViewById<TextView>(R.id.tvConfirmText)
        val tvCancel = dialog.findViewById<TextView>(R.id.tvCancelText)
        val iconDialog = dialog.findViewById<ImageView>(R.id.iconDialog)

        tvTitle.text = title
        tvMessage.text = message
        tvConfirm.text = confirmText
        tvCancel.text = cancelText

        // Set icon dan warna sesuai action
        if (isApprove) {
            iconDialog.setImageResource(android.R.drawable.ic_dialog_info)
            iconDialog.setColorFilter(Color.parseColor("#4CAF50"))
            btnConfirm.setCardBackgroundColor(Color.parseColor("#4CAF50"))
        } else {
            iconDialog.setImageResource(android.R.drawable.ic_dialog_alert)
            iconDialog.setColorFilter(Color.parseColor("#E57373"))
            btnConfirm.setCardBackgroundColor(Color.parseColor("#E57373"))
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Update status dokumen individual
     */
    private fun updateDocumentStatus(docType: String, status: String) {
        when (docType) {
            "ktp" -> ktpStatus = status
            "ijazah" -> ijazahStatus = status
            "skl" -> sklStatus = status
            "transkrip" -> transkripStatus = status
        }

        android.util.Log.d("VerificationDokumen", "✓ $docType status updated to: $status")
        Toast.makeText(this, "Status dokumen diperbarui: $status", Toast.LENGTH_SHORT).show()
    }

    /**
     * Fungsi untuk Verifikasi Semua Dokumen (Approve/Reject All)
     */
    private fun performVerificationAll(status: String) {
        if (pendaftarIdString.isEmpty()) {
            Toast.makeText(this, "Error: Pendaftar ID tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("VerificationDokumen", "========================================")
        android.util.Log.d("VerificationDokumen", "Updating All Status to: $status")
        android.util.Log.d("VerificationDokumen", "Pendaftar ID: $pendaftarIdString")
        android.util.Log.d("VerificationDokumen", "========================================")

        // Update status di Firestore
        firestore.collection("pendaftar")
            .document(pendaftarIdString)
            .update("status", status)
            .addOnSuccessListener {
                android.util.Log.d("VerificationDokumen", "✓ Status updated successfully to: $status")

                val message = when (status) {
                    "Approved" -> "Semua dokumen berhasil disetujui!"
                    "Rejected" -> "Semua dokumen ditolak."
                    else -> "Status diperbarui."
                }

                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                // Kembali ke halaman sebelumnya
                finish()
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("VerificationDokumen", "❌ Failed to update status", exception)
                Toast.makeText(
                    this,
                    "Gagal memperbarui status: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}