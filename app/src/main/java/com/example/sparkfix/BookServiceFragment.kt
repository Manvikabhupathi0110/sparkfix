package com.example.sparkfix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button         // Add these imports!
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.fragment.app.Fragment

class BookServiceFragment : Fragment() {
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
}