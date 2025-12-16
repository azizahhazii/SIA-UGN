// PendaftarAdapter.kt
package com.example.sia

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class PendaftarAdapter(private var pendaftarList: List<Pendaftar>) :
    RecyclerView.Adapter<PendaftarAdapter.PendaftarViewHolder>() {

    // Menyimpan daftar asli
    private var originalList: List<Pendaftar> = pendaftarList

    // Properti ini menyimpan daftar yang sedang ditampilkan setelah filter status
    private var currentFilteredList: List<Pendaftar> = originalList

    // 1. ViewHolder
    inner class PendaftarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ID Card untuk Klik
        val cardPendaftar: CardView = itemView.findViewById(R.id.cardPendaftarItem)

        // ID Elemen UI
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvEmail: TextView = itemView.findViewById(R.id.tv_email)
        val tvProdiDate: TextView = itemView.findViewById(R.id.tv_prodi_date)
        val tvStatusTag: TextView = itemView.findViewById(R.id.tv_status_tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendaftarViewHolder {
        // Mengubah placeholder ke item_pendaftar.xml yang sudah diperbaiki
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pendaftar_placeholder, parent, false)
        return PendaftarViewHolder(view)
    }

    // 2. Binding Data dan Logika Klik
    override fun onBindViewHolder(holder: PendaftarViewHolder, position: Int) {
        val pendaftar = pendaftarList[position]
        val context = holder.itemView.context

        holder.tvName.text = pendaftar.nama
        holder.tvEmail.text = pendaftar.email
        holder.tvProdiDate.text = "${pendaftar.prodi} - ${pendaftar.tanggalDaftar}"

        // --- Atur Status Dinamis ---
        when (pendaftar.status) {
            "Approved" -> {
                holder.tvStatusTag.text = "Lulus"
                // Pastikan drawable ini ada: res/drawable/bg_status_approved.xml
                holder.tvStatusTag.setBackgroundResource(R.drawable.bg_status_approved)
                holder.tvStatusTag.setTextColor(ContextCompat.getColor(context, R.color.light_bg_gold))
            }
            "Rejected" -> {
                holder.tvStatusTag.text = "Tidak Lulus"
                // Pastikan drawable ini ada: res/drawable/bg_status_rejected.xml
                holder.tvStatusTag.setBackgroundResource(R.drawable.bg_status_rejected)
                holder.tvStatusTag.setTextColor(ContextCompat.getColor(context, R.color.light_bg_gold))
            }
            else -> { // Pending
                holder.tvStatusTag.text = "Pending"
                // Pastikan drawable ini ada: res/drawable/bg_status_pending.xml
                holder.tvStatusTag.setBackgroundResource(R.drawable.bg_status_pending)
                holder.tvStatusTag.setTextColor(ContextCompat.getColor(context, R.color.light_bg_gold))
            }
        }

        // --- LOGIKA KLIK CARD ---
        holder.cardPendaftar.setOnClickListener {
            // Pindah ke VerifikasiDokumenActivity dan membawa data pendaftar
            val intent = Intent(context, VerificationDokumen::class.java).apply {
                putExtra("KEY_ID_PENDAFTAR", pendaftar.id)
                putExtra("KEY_NAMA_PENDAFTAR", pendaftar.nama)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = pendaftarList.size

    // --- FUNGSIONALITAS FILTER UTAMA ---

    fun filterByStatus(status: String) {
        currentFilteredList = if (status == "All") {
            originalList
        } else {
            originalList.filter { it.status == status }
        }

        pendaftarList = currentFilteredList
        notifyDataSetChanged()
    }

    fun filterByQuery(query: String) {
        val searchLower = query.toLowerCase(Locale.getDefault())

        pendaftarList = if (searchLower.isBlank()) {
            currentFilteredList
        } else {
            currentFilteredList.filter {
                it.nama.toLowerCase(Locale.getDefault()).contains(searchLower) ||
                        it.email.toLowerCase(Locale.getDefault()).contains(searchLower) ||
                        it.prodi.toLowerCase(Locale.getDefault()).contains(searchLower)
            }
        }
        notifyDataSetChanged()
    }
}