package com.example.sparkfix

import com.google.firebase.Timestamp

data class Booking(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val appliance: String = "",
    val description: String = "",
    val hostel: String = "",
    val room: String = "",
    val status: String = "Pending", // "Pending", "Assigned", "Completed"
    val timestamp: Timestamp? = null,
    val assignedTo: String = "", // Electrician UID
    val assignedName: String = "",
    val isRated: Boolean = false,
    val completionTime: Timestamp? = null
)