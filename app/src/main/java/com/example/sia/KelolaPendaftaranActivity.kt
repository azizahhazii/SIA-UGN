package com.example.sia

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class KelolaPendaftaranActivity : AppCompatActivity() {

    private lateinit var pendaftarAdapter: PendaftarAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvPendaftarCount: TextView
    private lateinit var firestore: FirebaseFirestore

    // List untuk menyimpan data dari Firestore
    private val pendaftarList = mutableListOf<Pendaftar>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_kelola_pendaftaran)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_pendaftaran)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi Firebase Firestore
        firestore = FirebaseFirestore.getInstance()

        // Setup UI
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        tvPendaftarCount = findViewById(R.id.tv_pendaftar_count)
        recyclerView = findViewById(R.id.recycler_pendaftar)
        val searchBar = findViewById<SearchView>(R.id.search_bar)

        // Navigasi Bawah
        val navHome = findViewById<ImageButton>(R.id.nav_home)

        // Filter Buttons
        val filterAll = findViewById<TextView>(R.id.filter_all)
        val filterPending = findViewById<TextView>(R.id.filter_pending)
        val filterApproved = findViewById<TextView>(R.id.filter_approved)
        val filterRejected = findViewById<TextView>(R.id.filter_rejected)

        // Setup RecyclerView dan Adapter
        pendaftarAdapter = PendaftarAdapter(pendaftarList)
        recyclerView.adapter = pendaftarAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Atur filter 'All' sebagai default aktif
        val allFilters = listOf(filterAll, filterPending, filterApproved, filterRejected)
        setActiveFilterStyle(filterAll, allFilters)

        // Ubah warna teks SearchView menjadi hitam
        setupSearchViewStyle(searchBar)

        // Load data dari Firestore
        loadPendaftarFromFirestore()

        // Aksi Back
        btnBack?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Aksi Navigasi Bawah
        navHome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Logika Search (Pencarian)
        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                pendaftarAdapter.filterByQuery(newText.orEmpty())
                tvPendaftarCount.text = pendaftarAdapter.itemCount.toString()
                return true
            }
        })

        // Logika Filter Status
        filterAll.setOnClickListener {
            android.util.Log.d("KelolaPendaftaran", "Filter clicked: All")
            pendaftarAdapter.filterByStatus("All")
            setActiveFilterStyle(it, allFilters)
            tvPendaftarCount.text = pendaftarAdapter.itemCount.toString()
        }

        filterPending.setOnClickListener {
            android.util.Log.d("KelolaPendaftaran", "Filter clicked: Pending")
            pendaftarAdapter.filterByStatus("Pending")
            setActiveFilterStyle(it, allFilters)
            tvPendaftarCount.text = pendaftarAdapter.itemCount.toString()
        }

        filterApproved.setOnClickListener {
            android.util.Log.d("KelolaPendaftaran", "Filter clicked: Approved")
            pendaftarAdapter.filterByStatus("Approved")
            setActiveFilterStyle(it, allFilters)
            tvPendaftarCount.text = pendaftarAdapter.itemCount.toString()
        }

        filterRejected.setOnClickListener {
            android.util.Log.d("KelolaPendaftaran", "Filter clicked: Rejected")
            pendaftarAdapter.filterByStatus("Rejected")
            setActiveFilterStyle(it, allFilters)
            tvPendaftarCount.text = pendaftarAdapter.itemCount.toString()
        }
    }

    private fun setupSearchViewStyle(searchView: SearchView) {
        // Mengubah warna teks input menjadi hitam
        try {
            val searchTextId = searchView.context.resources.getIdentifier("android:id/search_src_text", null, null)
            val searchTextView = searchView.findViewById<TextView>(searchTextId)
            searchTextView?.setTextColor(ContextCompat.getColor(this, R.color.black))
            searchTextView?.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } catch (e: Exception) {
            android.util.Log.e("KelolaPendaftaran", "Error setting SearchView text color", e)
        }
    }

    private fun loadPendaftarFromFirestore() {
        android.util.Log.d("KelolaPendaftaran", "========================================")
        android.util.Log.d("KelolaPendaftaran", "Loading Pendaftar from Firestore")
        android.util.Log.d("KelolaPendaftaran", "========================================")

        firestore.collection("pendaftar")
            .orderBy("submissionDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val newList = mutableListOf<Pendaftar>()

                android.util.Log.d("KelolaPendaftaran", "✓ Documents retrieved: ${documents.size()}")

                for (document in documents) {
                    try {
                        // Ambil data langsung dari document
                        val userId = document.getString("userId") ?: ""
                        val pendaftarId = document.getString("pendaftarId") ?: ""
                        val name = document.getString("name") ?: ""
                        val email = document.getString("email") ?: ""
                        val prodiPilihan1 = document.getString("prodiPilihan1") ?: ""
                        val status = document.getString("status") ?: "Pending"
                        val submissionDate = document.getTimestamp("submissionDate")

                        // Format tanggal
                        val dateString = if (submissionDate != null) {
                            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
                            sdf.format(submissionDate.toDate())
                        } else {
                            "N/A"
                        }

                        // Convert ke Pendaftar untuk RecyclerView
                        val pendaftar = Pendaftar(
                            if (pendaftarId.isNotEmpty()) pendaftarId.hashCode() else document.id.hashCode(),
                            name,
                            email,
                            prodiPilihan1,
                            dateString,
                            status
                        )

                        newList.add(pendaftar)

                        android.util.Log.d("KelolaPendaftaran",
                            "  → [${newList.size}] Name: '$name' | Status: '$status' | Email: '$email' | Prodi: '$prodiPilihan1'")

                    } catch (e: Exception) {
                        android.util.Log.e("KelolaPendaftaran", "❌ Error parsing document: ${document.id}", e)
                        android.util.Log.e("KelolaPendaftaran", "   Document data: ${document.data}")
                    }
                }

                // Update data list dan refresh adapter
                pendaftarList.clear()
                pendaftarList.addAll(newList)
                pendaftarAdapter.notifyDataSetChanged()
                tvPendaftarCount.text = pendaftarList.size.toString()

                android.util.Log.d("KelolaPendaftaran", "========================================")
                android.util.Log.d("KelolaPendaftaran", "✓ Total pendaftar loaded: ${newList.size}")
                android.util.Log.d("KelolaPendaftaran", "✓ Displayed after filter: ${pendaftarAdapter.itemCount}")

                // Log per status untuk debugging
                val pendingCount = newList.count { it.status.equals("Pending", ignoreCase = true) }
                val approvedCount = newList.count { it.status.equals("Approved", ignoreCase = true) }
                val rejectedCount = newList.count { it.status.equals("Rejected", ignoreCase = true) }

                android.util.Log.d("KelolaPendaftaran", "✓ Status breakdown:")
                android.util.Log.d("KelolaPendaftaran", "   - Pending: $pendingCount")
                android.util.Log.d("KelolaPendaftaran", "   - Approved: $approvedCount")
                android.util.Log.d("KelolaPendaftaran", "   - Rejected: $rejectedCount")
                android.util.Log.d("KelolaPendaftaran", "========================================")

                if (newList.isEmpty()) {
                    Toast.makeText(this, "Belum ada pendaftar", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("KelolaPendaftaran", "========================================")
                android.util.Log.e("KelolaPendaftaran", "❌ Failed to load pendaftar", exception)
                android.util.Log.e("KelolaPendaftaran", "========================================")

                Toast.makeText(
                    this,
                    "Gagal memuat data: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun setActiveFilterStyle(activeView: View, allFilters: List<TextView>? = null) {
        val context = activeView.context
        val activeBg = ContextCompat.getDrawable(context, R.drawable.bg_filter_active)
        val defaultBg = ContextCompat.getDrawable(context, R.drawable.bg_filter_default)

        // Reset semua ke style default
        allFilters?.forEach { view ->
            view.setBackground(defaultBg)
            view.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        // Set view yang diklik menjadi style aktif
        if (activeView is TextView) {
            activeView.setBackground(activeBg)
            activeView.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }
}