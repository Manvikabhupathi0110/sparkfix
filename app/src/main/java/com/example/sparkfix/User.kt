package com.example.sparkfix

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "Student", // "Student" or "Electrician"
    // Student specific fields
    val studentId: String = "",
    val hostel: String = "",
    val room: String = "",
    // Electrician specific fields
    val electricianId: String = "",
    val rating: Float = 5.0f,
    val busy: Boolean = false,
    // Common
    val phone: String = ""
)