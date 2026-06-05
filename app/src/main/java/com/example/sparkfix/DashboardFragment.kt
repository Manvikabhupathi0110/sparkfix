package com.example.sparkfix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

class DashboardFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    private lateinit var tvUserName: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvPending: TextView
    private lateinit var tvCompleted: TextView
    private lateinit var labelRequests: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvUserName = view.findViewById(R.id.tvUserName)
        tvTotal = view.findViewById(R.id.tvTotalCount)
        tvPending = view.findViewById(R.id.tvPendingCount)
        tvCompleted = view.findViewById(R.id.tvCompletedCount)
        
        // Find the label in the card header
        labelRequests = view.findViewById<View>(R.id.tvTotalCount).parent.parent.parent.run { (this as ViewGroup).getChildAt(0) as TextView }

        view.findViewById<CardView>(R.id.cardBook).setOnClickListener {
            (activity as? MainActivity)?.findViewById<View>(R.id.nav_book)?.performClick()
        }

        view.findViewById<CardView>(R.id.cardHistory).setOnClickListener {
            (activity as? MainActivity)?.findViewById<View>(R.id.nav_history)?.performClick()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        startListening()
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
    }

    private fun startListening() {
        val user = auth.currentUser ?: return
        
        db.collection("users").document(user.uid).addSnapshotListener { doc, _ ->
            val role = doc?.getString("role") ?: "Student"
            tvUserName.text = doc?.getString("name") ?: "User"
            
            if (role == "Electrician") {
                labelRequests.text = "My Assignments"
                setupElectricianStats(user.uid)
            } else {
                labelRequests.text = "My Requests"
                setupStudentStats(user.uid)
            }
        }
    }

    private fun setupStudentStats(uid: String) {
        listener?.remove()
        listener = db.collection("bookings")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { docs, _ -> updateStats(docs) }
    }

    private fun setupElectricianStats(uid: String) {
        listener?.remove()
        listener = db.collection("bookings")
            .whereEqualTo("assignedTo", uid)
            .addSnapshotListener { docs, _ -> updateStats(docs) }
    }

    private fun updateStats(docs: QuerySnapshot?) {
        if (docs == null) return
        val total = docs.size()
        var pending = 0
        var completed = 0
        for (doc in docs) {
            val status = doc.getString("status")
            if (status == "Pending" || status == "Assigned") pending++
            else if (status == "Completed") completed++
        }
        tvTotal.text = total.toString()
        tvPending.text = pending.toString()
        tvCompleted.text = completed.toString()
    }
}