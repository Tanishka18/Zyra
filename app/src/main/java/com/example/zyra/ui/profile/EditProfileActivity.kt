package com.example.zyra.ui.profile

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.zyra.R

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        btnSave = findViewById(R.id.btnSave)

        // Load existing user data
        val prefs = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        etName.setText(prefs.getString("name", "User"))
        etEmail.setText(prefs.getString("email", "user@example.com"))

        btnSave.setOnClickListener {
            val newName = etName.text.toString()
            val newEmail = etEmail.text.toString()

            // Save back to SharedPreferences
            prefs.edit()
                .putString("name", newName)
                .putString("email", newEmail)
                .apply()

            // Finish and go back
            finish()
        }
    }
}
