package com.example.sparkfix

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class BookingAdapter(
    private val bookings: List<Booking>,
    private val userRole: String,
    private val onAction: (Booking, String) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppliance: TextView = view.findViewById(R.id.tvApplianceName)
        val tvDate: TextView = view.findViewById(R.id.tvBookingDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvRoom: TextView = view.findViewById(R.id.tvRoomInfo)
        val btnAction: Button = view.findViewById(R.id.btnBookingAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        holder.tvAppliance.text = booking.appliance
        
        // Detailed Status Info
        val statusText = when(booking.status) {
            "Assigned" -> "Status: Assigned to ${booking.assignedName}\nReach out time: 09:00 AM - 05:00 PM"
            "Completed" -> "Status: Fixed at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(booking.completionTime?.toDate() ?: Date())}"
            "Pending (All staff busy)" -> "Status: Queued (All staff busy)\nVisit window: 09:00 AM - 05:00 PM"
            else -> "Status: Pending Assignment\nVisit window: 09:00 AM - 05:00 PM"
        }
        holder.tvStatus.text = statusText
        
        holder.tvRoom.text = "Location: Room ${booking.room}, ${booking.hostel}"

        // Status color coding
        val color = when(booking.status) {
            "Completed" -> android.graphics.Color.parseColor("#10B981")
            "Assigned" -> android.graphics.Color.parseColor("#3B82F6")
            else -> android.graphics.Color.parseColor("#F59E0B") // Pending
        }
        holder.tvStatus.setTextColor(color)
        
        val date = booking.timestamp?.toDate()
        if (date != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            holder.tvDate.text = sdf.format(date)
        } else {
            holder.tvDate.text = "Date unknown"
        }

        // Action Button Logic
        holder.btnAction.visibility = View.GONE
        if (userRole == "Electrician" && booking.status == "Assigned") {
            holder.btnAction.text = "Mark Completed"
            holder.btnAction.visibility = View.VISIBLE
            holder.btnAction.setOnClickListener { onAction(booking, "COMPLETE") }
        } else if (userRole == "Student" && booking.status == "Completed" && !booking.isRated) {
            holder.btnAction.text = "Rate Electrician"
            holder.btnAction.visibility = View.VISIBLE
            holder.btnAction.setOnClickListener { onAction(booking, "RATE") }
        }
    }

    override fun getItemCount() = bookings.size
}