package com.example.myapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private var capturedImage: Bitmap? = null
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private var storedImageBase64: String? = null
    private var userId: Int = -1 // Store the user ID after login validation
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        profileImage = findViewById(R.id.profile_image)
        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase

        // Initialize the ActivityResultLauncher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    // Resize captured image
                    capturedImage = resizeBitmap(imageBitmap, 500, 500)
                    profileImage.setImageBitmap(capturedImage)
                } else {
                    Log.e("LoginActivity", "Failed to capture image")
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Capture image on profile image click
        profileImage.setOnClickListener {
            openCamera()
        }

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            val usernameOrEmail = findViewById<EditText>(R.id.username).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()

            // Validate username/email and password
            if (validateLogin(usernameOrEmail, password)) {
                if (storedImageBase64 != null && capturedImage != null) {
                    val storedBitmap = base64ToBitmap(storedImageBase64!!) // Convert Base64 to Bitmap
                    checkImageMatching(storedBitmap) // Pass the converted stored bitmap for matching
                } else {
                    Toast.makeText(this, "No image found for matching", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Invalid login credentials", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.forgot_password).setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    fun goToSignUp(view: View) {
        // Intent to navigate to the Sign Up activity
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    private fun validateLogin(usernameOrEmail: String, password: String): Boolean {
        val query = "SELECT * FROM ${DatabaseHelper.TABLE_USER} WHERE (username = ? OR email = ?) AND password = ?"
        val cursor = db.rawQuery(query, arrayOf(usernameOrEmail, usernameOrEmail, password))

        return if (cursor.moveToFirst()) {
            // Store the username in SharedPreferences
            val username = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USERNAME))
            storeUsernameInPreferences(username)
            storedImageBase64 = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_IMAGE)) // Fetch Base64 encoded image string
            userId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)) // Fetch user_id
            true
        } else {
            Log.e("LoginActivity", "Invalid login credentials")
            false
        }
    }

    private fun storeUsernameInPreferences(username: String) {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("currentUsername", username)
        editor.apply()
    }

    private fun getCurrentUsername(): String {
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("currentUsername", "") ?: ""
    }

    private fun checkImageMatching(storedBitmap: Bitmap?) {
        if (storedBitmap == null) {
            Toast.makeText(this, "No stored image found for matching", Toast.LENGTH_SHORT).show()
            return
        }

        capturedImage?.let { capturedImg ->
            // Resize storedBitmap to match the size of capturedImage
            val resizedStoredBitmap = resizeBitmap(storedBitmap, capturedImg.width, capturedImg.height)

            // Get pixel data from both images
            val capturedPixels = IntArray(capturedImg.width * capturedImg.height)
            val storedPixels = IntArray(resizedStoredBitmap.width * resizedStoredBitmap.height)

            capturedImg.getPixels(capturedPixels, 0, capturedImg.width, 0, 0, capturedImg.width, capturedImg.height)
            resizedStoredBitmap.getPixels(storedPixels, 0, resizedStoredBitmap.width, 0, 0, resizedStoredBitmap.width, resizedStoredBitmap.height)

            // Compare the pixel data
            val isMatch = areImagesSimilar(capturedPixels, storedPixels)
            if (isMatch) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                // Redirect to NotepadActivity with user_id
                val intent = Intent(this, NotepadActivity::class.java)
                intent.putExtra("current_user_id", userId) // Pass the user_id
                startActivity(intent)
                finish()
            } else {
                // If images do not match, prompt for backup token
                promptForBackupToken()
            }
        } ?: run {
            Toast.makeText(this, "No captured image found for matching", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptForBackupToken() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_backup_token, null)
        val backupTokenInput = dialogView.findViewById<EditText>(R.id.backup_token_input)

        AlertDialog.Builder(this)
            .setTitle("Image Mismatch")
            .setMessage("The captured image does not match. Please enter your backup token.")
            .setView(dialogView)
            .setPositiveButton("Enter") { dialog, _ ->
                val backupToken = backupTokenInput.text.toString()
                // Validate backup token
                if (validateBackupTokenWithUser(backupToken)) {
                    Toast.makeText(this, "Login successful with backup token", Toast.LENGTH_SHORT).show()
                    // Redirect to NotepadActivity with user_id
                    val intent = Intent(this, NotepadActivity::class.java)
                    intent.putExtra("current_user_id", userId) // Pass the user_id
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid backup token", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun validateBackupTokenWithUser(backupToken: String): Boolean {
        // Validate backup token in the database (assuming a method to get current user's username)
        val username = getCurrentUsername()
        val query = "SELECT * FROM ${DatabaseHelper.TABLE_USER} WHERE username = ? AND backup_token = ?"
        val cursor = db.rawQuery(query, arrayOf(username, backupToken))
        return cursor.moveToFirst()
    }

    private fun areImagesSimilar(capturedPixels: IntArray, storedPixels: IntArray): Boolean {
        if (capturedPixels.size != storedPixels.size) {
            return false // Images are not the same size
        }

        val tolerance = 50 // Define a color difference tolerance
        var mismatchCount = 0
        val maxMismatches = capturedPixels.size * 0.50

        for (i in capturedPixels.indices) {
            if (!pixelsAreSimilar(capturedPixels[i], storedPixels[i], tolerance)) {
                mismatchCount++
                if (mismatchCount > maxMismatches) return false
            }
        }
        return true
    }

    private fun pixelsAreSimilar(pixel1: Int, pixel2: Int, tolerance: Int): Boolean {
        val r1 = (pixel1 shr 16) and 0xff
        val g1 = (pixel1 shr 8) and 0xff
        val b1 = pixel1 and 0xff
        val r2 = (pixel2 shr 16) and 0xff
        val g2 = (pixel2 shr 8) and 0xff
        val b2 = pixel2 and 0xff

        val redDiff = Math.abs(r1 - r2)
        val greenDiff = Math.abs(g1 - g2)
        val blueDiff = Math.abs(b1 - b2)

        return redDiff <= tolerance && greenDiff <= tolerance && blueDiff <= tolerance
    }

    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedString = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: IllegalArgumentException) {
            Log.e("LoginActivity", "Failed to decode Base64 string to Bitmap", e)
            null
        }
    }
    private fun showForgotPasswordDialog() {
        // Inflate the dialog view from XML layout file
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailInput = dialogView.findViewById<EditText>(R.id.username_input)

        // Create an AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Forgot Password")
            .setMessage("Enter your email address to reset your password.")
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val email = emailInput.text.toString()
                if (email.isNotEmpty()) {
                    // Call a method to validate and process the email for password reset
                    processForgotPassword(email)
                } else {
                    Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Close the dialog
            }
            .show()
    }

    private fun processForgotPassword(email: String) {
        // Perform email validation and check if the email exists in the database
        val query = "SELECT * FROM ${DatabaseHelper.TABLE_USER} WHERE email = ?"
        val cursor = db.rawQuery(query, arrayOf(email))

        if (cursor.moveToFirst()) {
            // If email exists, send a password reset token or perform necessary actions
            Toast.makeText(this, "Password reset instructions sent to $email", Toast.LENGTH_SHORT).show()
            // You could add further logic to send the token or instructions to the user's email
        } else {
            Toast.makeText(this, "Email not found in database", Toast.LENGTH_SHORT).show()
        }
        cursor.close()
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }
}
