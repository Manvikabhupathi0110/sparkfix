package com.example.sparkfix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookingAdapter
    private val bookingsList = mutableListOf<Booking>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userRole: String = "Student"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        
        recyclerView = view.findViewById(R.id.rvHistory)
        recyclerView.layoutManager = LinearLayoutManager(context)
        
        loadUserAndData()

        return view
    }

    private fun loadUserAndData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            userRole = doc.getString("role") ?: "Student"
            
            adapter = BookingAdapter(bookingsList, userRole) { booking, action ->
                if (action == "COMPLETE") {
                    updateBookingStatus(booking, "Completed")
                } else if (action == "RATE") {
                    showRatingDialog(booking)
                }
            }
            recyclerView.adapter = adapter
            fetchBookings()
        }
    }

    private fun updateBookingStatus(booking: Booking, newStatus: String) {
        db.collection("bookings").document(booking.id)
            .update("status", newStatus, "completionTime", com.google.firebase.Timestamp.now())
            .addOnSuccessListener {
                if (newStatus == "Completed" && booking.assignedTo.isNotEmpty()) {
                    // Mark electrician as free again
                    db.collection("users").document(booking.assignedTo).update("busy", false)
                        .addOnSuccessListener {
                            // After becoming free, check if there are other pending bookings
                            checkForNewPendingBookings()
                        }
                }
                Toast.makeText(context, "Status Updated!", Toast.LENGTH_SHORT).show()
                fetchBookings()
            }
    }

    private fun checkForNewPendingBookings() {
        // Try to assign the oldest pending booking to a now-free electrician
        db.collection("bookings")
            .whereEqualTo("status", "Pending")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val nextBookingId = docs.documents[0].id
                    // Using a dummy call to the same logic used in BookServiceFragment
                    // Ideally this logic should be in a shared Repository class
                    assignNextAvailableElectrician(nextBookingId)
                }
            }
    }

    private fun assignNextAvailableElectrician(bookingId: String) {
        db.collection("users")
            .whereEqualTo("role", "Electrician")
            .whereEqualTo("busy", false)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val elect = docs.documents[0]
                    val update = hashMapOf(
                        "status" to "Assigned",
                        "assignedTo" to elect.id,
                        "assignedName" to (elect.getString("name") ?: "Electrician")
                    )
                    db.collection("bookings").document(bookingId).update(update as Map<String, Any>)
                    db.collection("users").document(elect.id).update("busy", true)
                }
            }
    }

    private fun showRatingDialog(booking: Booking) {
        val ratingView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rate_electrician, null)
        val ratingBar = ratingView.findViewById<android.widget.RatingBar>(R.id.ratingBar)

        AlertDialog.Builder(requireContext())
            .setTitle("Rate Repair Quality")
            .setView(ratingView)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating
                submitRating(booking, rating)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitRating(booking: Booking, rating: Float) {
        // 1. Mark booking as rated
        db.collection("bookings").document(booking.id).update("isRated", true)
        
        // 2. Update Electrician's total rating (Simple average for demo)
        if (booking.assignedTo.isNotEmpty()) {
            db.collection("users").document(booking.assignedTo).get().addOnSuccessListener { doc ->
                val currentRating = doc.getDouble("rating") ?: 5.0
                val newRating = (currentRating + rating) / 2.0
                db.collection("users").document(booking.assignedTo).update("rating", newRating)
            }
        }
        fetchBookings()
    }

    private fun fetchBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) return

        val query = if (userRole == "Electrician") {
            db.collection("bookings").whereEqualTo("assignedTo", currentUser.uid)
        } else {
            db.collection("bookings").whereEqualTo("userId", currentUser.uid)
        }

        // Use SnapshotListener for real-time updates
        // Remove .orderBy to avoid "Missing Index" errors, sort locally instead
        query.addSnapshotListener { documents, e ->
            if (e != null) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (documents != null) {
                bookingsList.clear()
                for (document in documents) {
                    val booking = document.toObject(Booking::class.java).copy(id = document.id)
                    bookingsList.add(booking)
                }
                
                // Sort locally by timestamp descending
                bookingsList.sortByDescending { it.timestamp }
                
                adapter.notifyDataSetChanged()
            }
        }
    }
}
