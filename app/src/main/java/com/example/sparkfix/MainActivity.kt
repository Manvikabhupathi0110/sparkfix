package com.example.sparkfix

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Hide "Book" tab for Electricians
        val uid = auth.currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists() && doc.getString("role") == "Electrician") {
                        bottomNav.menu.findItem(R.id.nav_book)?.isVisible = false
                    }
                }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, DashboardFragment())
            .commit()

        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_history   -> HistoryFragment()
                R.id.nav_book      -> BookServiceFragment()
                R.id.nav_profile   -> ProfileFragment()
                R.id.nav_more      -> AboutFragment() // We'll show both About & Help here, for now About
                else               -> DashboardFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
            true
        }
    }
}