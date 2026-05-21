package com.example.sparkfix

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn: Button = findViewById(R.id.btn_start)
        btn.setOnClickListener {
            Toast.makeText(this, "Welcome to SparkFix!", Toast.LENGTH_SHORT).show()
        }
    }
}