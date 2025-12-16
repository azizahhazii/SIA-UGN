package com.example.sia

import java.io.Serializable

data class Pendaftar(
    val id: Int,
    val nama: String,
    val email: String,
    val prodi: String,
    val tanggalDaftar: String,
    val status: String,
    // Tambahkan properti untuk dokumen yang akan diverifikasi
    val dokumen: List<DokumenStatus> = emptyList()
) : Serializable

data class DokumenStatus(
    val namaDokumen: String,
    val namaFile: String,
    var status: String, // Bisa 'Approved', 'Rejected', 'Pending'
    val fileUrl: String? = null // URL atau path file
) : Serializable

