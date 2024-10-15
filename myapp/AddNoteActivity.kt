package com.example.myapp

import android.app.Activity
import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddNoteActivity : AppCompatActivity() {

    private lateinit var headingInput: EditText
    private lateinit var contentInput: EditText
    private lateinit var saveButton: Button
    private lateinit var dbHelper: DatabaseHelper
    private var currentUserId: Int = -1 // Now using Int instead of String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        headingInput = findViewById(R.id.heading_input)
        contentInput = findViewById(R.id.content_input)
        saveButton = findViewById(R.id.save_button)
        dbHelper = DatabaseHelper(this)

        // Get the logged-in user's ID from the intent
        currentUserId = intent.getIntExtra("current_user_id", -1)

        // Check if the activity was started for editing a note
        val noteHeading = intent.getStringExtra("note_heading")
        if (noteHeading != null) {
            loadNoteDetails(noteHeading)
        }

        saveButton.setOnClickListener { saveNote() }
    }

    private fun loadNoteDetails(heading: String) {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_NOTES} WHERE ${DatabaseHelper.COLUMN_HEADING}=? AND ${DatabaseHelper.COLUMN_USER_ID}=?",
            arrayOf(heading, currentUserId.toString())
        )

        if (cursor.moveToFirst()) {
            headingInput.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_HEADING)))
            contentInput.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT)))
        }
        cursor.close()
    }

    private fun saveNote() {
        val heading = headingInput.text.toString()
        val content = contentInput.text.toString()

        if (heading.isNotEmpty() && content.isNotEmpty()) {
            val db = dbHelper.writableDatabase
            val values = ContentValues()
            values.put(DatabaseHelper.COLUMN_HEADING, heading)
            values.put(DatabaseHelper.COLUMN_CONTENT, content)
            values.put(DatabaseHelper.COLUMN_USER_ID, currentUserId) // Save the current user's ID

            // If editing, update the note; otherwise, insert a new note
            if (intent.hasExtra("note_heading")) {
                db.update(
                    DatabaseHelper.TABLE_NOTES,
                    values,
                    "${DatabaseHelper.COLUMN_HEADING}=? AND ${DatabaseHelper.COLUMN_USER_ID}=?",
                    arrayOf(intent.getStringExtra("note_heading"), currentUserId.toString())
                )
                Toast.makeText(this, "Note updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                db.insert(DatabaseHelper.TABLE_NOTES, null, values)
                Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show()
            }

            setResult(Activity.RESULT_OK)
            finish() // Close the activity and return to the NotepadActivity
        } else {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }
}
