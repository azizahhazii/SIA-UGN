package com.example.sia.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class pendaftar(
    val userId: String = "",
    val pendaftarId: String = "",

    val name: String = "",
    val email: String = "",
    val prodiPilihan1: String = "",
    val prodiPilihan2: String = "",
    val prodiPilihan3: String = "",
    val gender: String = "",
    val ktpUrl: String = "",

    val sekolahAsal: String = "",
    val statusKelulusan: String = "",
    val ijazahTerakhir: String = "",
    val ijazahUrl: String = "",
    val sklUrl: String = "",
    val transkripNilaiUrl: String = "",

    val status: String = "Pending",

    @ServerTimestamp
    val submissionDate: Timestamp? = null
)