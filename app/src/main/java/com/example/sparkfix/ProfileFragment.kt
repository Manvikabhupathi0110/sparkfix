package com.example.sparkfix

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvId: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvPhone: TextView
    
    private lateinit var labelId: TextView
    private lateinit var labelLocation: TextView
    private lateinit var switchDuty: com.google.android.material.switchmaterial.SwitchMaterial

    private var currentUserRole: String = "Student"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        tvName = view.findViewById(R.id.tvProfileName)
        tvEmail = view.findViewById(R.id.tvProfileEmail)
        tvId = view.findViewById(R.id.tvProfileId)
        tvLocation = view.findViewById(R.id.tvProfileLocation)
        tvPhone = view.findViewById(R.id.tvProfilePhone)
        switchDuty = view.findViewById(R.id.switchDutyStatus)
        
        labelId = view.findViewById<View>(R.id.tvProfileId).parent.run { (this as ViewGroup).getChildAt(0) as TextView }
        labelLocation = view.findViewById<View>(R.id.tvProfileLocation).parent.run { (this as ViewGroup).getChildAt(0) as TextView }

        val btnEdit = view.findViewById<Button>(R.id.btnEditProfile)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        loadUserProfile()

        switchDuty.setOnCheckedChangeListener { _, isChecked ->
            val userId = auth.currentUser?.uid ?: return@setOnCheckedChangeListener
            // isChecked == true means "Available" -> busy = false
            // isChecked == false means "Off Duty" -> busy = true
            db.collection("users").document(userId).update("busy", !isChecked)
                .addOnSuccessListener {
                    val msg = if (isChecked) "You are now ONLINE" else "You are now OFFLINE"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
        }

        btnEdit.setOnClickListener {
            showEditProfileDialog()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
        
        return view
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        tvEmail.text = user.email

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userData = document.toObject(User::class.java)
                    userData?.let {
                        currentUserRole = it.role
                        tvName.text = it.name.ifEmpty { "User" }
                        tvPhone.text = it.phone.ifEmpty { "Not set" }

                        if (it.role == "Electrician") {
                            labelId.text = "Electrician ID"
                            tvId.text = it.electricianId.ifEmpty { "Not set" }
                            labelLocation.text = "Rating"
                            tvLocation.text = "${it.rating} / 5.0"
                            
                            // Show and set Duty Switch
                            switchDuty.visibility = View.VISIBLE
                            // If busy is true, switch should be OFF (not available)
                            // If busy is false, switch should be ON (available)
                            switchDuty.isChecked = !it.busy
                        } else {
                            labelId.text = "Student ID"
                            tvId.text = it.studentId.ifEmpty { "Not set" }
                            labelLocation.text = "Hostel & Room"
                            tvLocation.text = if (it.hostel.isNotEmpty()) "${it.hostel}, Room ${it.room}" else "Not set"
                            
                            // Hide Duty Switch for students
                            switchDuty.visibility = View.GONE
                        }
                    }
                }
            }
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etEditPhone)
        
        val layoutStudent = dialogView.findViewById<View>(R.id.layoutEditStudent)
        val layoutElectrician = dialogView.findViewById<View>(R.id.layoutEditElectrician)

        if (currentUserRole == "Electrician") {
            layoutStudent.visibility = View.GONE
            layoutElectrician.visibility = View.VISIBLE
        } else {
            layoutStudent.visibility = View.VISIBLE
            layoutElectrician.visibility = View.GONE
        }

        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                etName.setText(doc.getString("name"))
                etPhone.setText(doc.getString("phone"))
                if (currentUserRole == "Electrician") {
                    dialogView.findViewById<EditText>(R.id.etEditElectricianId).setText(doc.getString("electricianId"))
                } else {
                    dialogView.findViewById<EditText>(R.id.etEditId).setText(doc.getString("studentId"))
                    dialogView.findViewById<EditText>(R.id.etEditHostel).setText(doc.getString("hostel"))
                    dialogView.findViewById<EditText>(R.id.etEditRoom).setText(doc.getString("room"))
                }
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updateMap = mutableMapOf<String, Any>(
                    "name" to etName.text.toString(),
                    "phone" to etPhone.text.toString()
                )
                if (currentUserRole == "Electrician") {
                    updateMap["electricianId"] = dialogView.findViewById<EditText>(R.id.etEditElectricianId).text.toString()
                } else {
                    updateMap["studentId"] = dialogView.findViewById<EditText>(R.id.etEditId).text.toString()
                    updateMap["hostel"] = dialogView.findViewById<EditText>(R.id.etEditHostel).text.toString()
                    updateMap["room"] = dialogView.findViewById<EditText>(R.id.etEditRoom).text.toString()
                }
                
                db.collection("users").document(user.uid).update(updateMap)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profile Updated!", Toast.LENGTH_SHORT).show()
                        loadUserProfile()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}