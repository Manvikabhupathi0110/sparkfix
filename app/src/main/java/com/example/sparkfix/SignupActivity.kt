package com.example.sparkfix

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val rgRole = findViewById<RadioGroup>(R.id.rgRole)
        val layoutStudent = findViewById<View>(R.id.layoutStudentFields)
        val layoutElectrician = findViewById<View>(R.id.layoutElectricianFields)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val tvLogin = findViewById<TextView>(R.id.tvGoToLogin)

        rgRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbStudent) {
                layoutStudent.visibility = View.VISIBLE
                layoutElectrician.visibility = View.GONE
            } else {
                layoutStudent.visibility = View.GONE
                layoutElectrician.visibility = View.VISIBLE
            }
        }

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role = if (findViewById<RadioButton>(R.id.rbStudent).isChecked) "Student" else "Electrician"

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill basic details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignup.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = User(
                        uid = result.user?.uid ?: "",
                        name = name,
                        email = email,
                        role = role,
                        studentId = if (role == "Student") findViewById<EditText>(R.id.etStudentId).text.toString() else "",
                        hostel = if (role == "Student") findViewById<EditText>(R.id.etHostel).text.toString() else "",
                        room = if (role == "Student") findViewById<EditText>(R.id.etRoom).text.toString() else "",
                        electricianId = if (role == "Electrician") findViewById<EditText>(R.id.etElectricianId).text.toString() else "",
                        busy = false
                    )

                    db.collection("users").document(user.uid).set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account Created!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finishAffinity()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            btnSignup.isEnabled = true
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Auth Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    btnSignup.isEnabled = true
                }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}