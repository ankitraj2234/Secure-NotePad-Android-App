package com.example.myapp

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class SignUpActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private var capturedImage: Bitmap? = null
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        profileImage = findViewById(R.id.profile_image)
        dbHelper = DatabaseHelper(this)

        // Set up the ActivityResultLauncher for the camera
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                capturedImage = resizeBitmap(imageBitmap, 500, 500)
                profileImage.setImageBitmap(capturedImage)
            }
        }

        findViewById<Button>(R.id.btn_sign_up).setOnClickListener {
            val name = findViewById<EditText>(R.id.name).text.toString()
            val username = findViewById<EditText>(R.id.username).text.toString()
            val email = findViewById<EditText>(R.id.email).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()
            val confirmPassword = findViewById<EditText>(R.id.confirm_password).text.toString()
            val backupToken = findViewById<EditText>(R.id.backup_token).text.toString()

            if (validateInput(name, username, email, password, confirmPassword, backupToken)) {
                saveUserToDatabase(name, username, email, password, backupToken)
            }
        }
    }

    private fun validateInput(name: String, username: String, email: String, password: String, confirmPassword: String, backupToken: String): Boolean {
        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || backupToken.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        if (capturedImage == null) {
            Toast.makeText(this, "Please capture or upload a profile image", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveUserToDatabase(name: String, username: String, email: String, password: String, backupToken: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("username", username)
            put("email", email)
            put("password", password)
            put("backup_token", backupToken)
            put("image", bitmapToBase64(capturedImage!!))  // Convert image to string
        }

        val newRowId = db.insert("User", null, values)
        if (newRowId != -1L) {
            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
            finish() // Go back to the login activity
        } else {
            Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    fun openCamera(view: View) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }
}
