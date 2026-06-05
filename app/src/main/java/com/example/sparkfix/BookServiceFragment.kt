package com.example.sparkfix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button         // Add these imports!
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import java.util.Calendar

class BookServiceFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_book_service, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etStudentId = view.findViewById<EditText>(R.id.etStudentId)
        val etHostel = view.findViewById<EditText>(R.id.etHostel)
        val etWing = view.findViewById<EditText>(R.id.etWing)
        val etRoom = view.findViewById<EditText>(R.id.etRoom)
        val spinner = view.findViewById<Spinner>(R.id.spinnerAppliance)
        val etDesc = view.findViewById<EditText>(R.id.etDescription)
        val imgPreview = view.findViewById<ImageView>(R.id.imgPreview)
        val btnReset = view.findViewById<Button>(R.id.btnReset)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)

        autoFillDetails(etName, etStudentId, etHostel, etRoom)

        btnSubmit.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = etName.text.toString().trim()
            val studentId = etStudentId.text.toString().trim()
            val hostel = etHostel.text.toString().trim()
            val room = etRoom.text.toString().trim()
            val appliance = spinner.selectedItem?.toString() ?: "Unknown"
            val description = etDesc.text.toString().trim()

            if (name.isEmpty() || studentId.isEmpty() || hostel.isEmpty() || room.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false // Prevent double clicks
            
            val booking = hashMapOf(
                "userId" to user.uid,
                "name" to name,
                "studentId" to studentId,
                "hostel" to hostel,
                "wing" to etWing.text.toString().trim(),
                "room" to room,
                "appliance" to appliance,
                "description" to description,
                "status" to "Pending",
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            db.collection("bookings")
                .add(booking)
                .addOnSuccessListener { docRef ->
                    assignElectricianIfPossible(docRef.id)
                    Toast.makeText(requireContext(), "Complaint Submitted Successfully!", Toast.LENGTH_LONG).show()
                    btnSubmit.isEnabled = true
                    btnReset.performClick()
                }
                .addOnFailureListener { e ->
                    Log.e("SparkFix", "Error adding document", e)
                    Toast.makeText(requireContext(), "Submission Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSubmit.isEnabled = true
                }
        }

        btnReset.setOnClickListener {
            etName.text.clear()
            etStudentId.text.clear()
            etHostel.text.clear()
            etWing.text.clear()
            etRoom.text.clear()
            etDesc.text.clear()
            spinner.setSelection(0)
            imgPreview.setImageDrawable(null)
            imgPreview.visibility = View.GONE
        }
    }

    private fun autoFillDetails(etName: EditText, etId: EditText, etHostel: EditText, etRoom: EditText) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                etName.setText(doc.getString("name"))
                etId.setText(doc.getString("studentId"))
                etHostel.setText(doc.getString("hostel"))
                etRoom.setText(doc.getString("room"))
            }
        }
    }

    private fun assignElectricianIfPossible(bookingId: String) {
        db.collection("users")
            .whereEqualTo("role", "Electrician")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    // Update status so student knows why it's pending
                    db.collection("bookings").document(bookingId).update("status", "Pending (No staff registered)")
                    return@addOnSuccessListener
                }

                // Check for a free electrician. Handle cases where 'busy' field doesn't exist yet.
                // If it doesn't exist, we assume they are free.
                val freeElectrician = docs.documents.find { it.getBoolean("busy") != true }

                if (freeElectrician != null) {
                    val electId = freeElectrician.id
                    val electName = freeElectrician.getString("name") ?: "Electrician"

                    val updateData = mapOf(
                        "status" to "Assigned",
                        "assignedTo" to electId,
                        "assignedName" to electName
                    )

                    db.collection("bookings").document(bookingId).update(updateData)
                        .addOnSuccessListener {
                            // Immediately mark electrician as busy
                            db.collection("users").document(electId).update("busy", true)
                        }
                } else {
                    // Everyone is currently working
                    db.collection("bookings").document(bookingId).update("status", "Pending (Staff busy)")
                }
            }
            .addOnFailureListener {
                db.collection("bookings").document(bookingId).update("status", "Pending (Assignment error)")
            }
    }
}

