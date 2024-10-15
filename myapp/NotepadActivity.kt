package com.example.myapp

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NotepadActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var notesListView: ListView
    private lateinit var notesList: MutableList<String>
    private lateinit var notesAdapter: ArrayAdapter<String>

    private var currentUserId: Int = -1 // Placeholder for the current user's ID

    companion object {
        const val REQUEST_CODE_ADD_NOTE = 1 // Unique request code for identifying result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notepad)

        dbHelper = DatabaseHelper(this)
        db = dbHelper.readableDatabase
        notesListView = findViewById(R.id.notes_list_view)
        notesList = mutableListOf()

        // Retrieve the current user's ID passed via intent from the login activity
        currentUserId = intent.getIntExtra("current_user_id", -1)

        loadNotes()

        // Set an item click listener to edit note
        notesListView.setOnItemClickListener { _, _, position, _ ->
            val noteHeading = notesList[position]
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("note_heading", noteHeading) // Pass the heading to edit
            intent.putExtra("current_user_id", currentUserId) // Pass the current user ID to the AddNoteActivity
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE) // Start for result
        }

        // Long press listener to delete note
        notesListView.setOnItemLongClickListener { _, _, position, _ ->
            val noteHeading = notesList[position]
            deleteNoteConfirmation(noteHeading) // Show confirmation dialog
            true
        }

        // Setup the FloatingActionButton
        val addNoteButton: FloatingActionButton = findViewById(R.id.add_note)
        addNoteButton.setOnClickListener {
            // Start AddNoteActivity when the FAB is clicked
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("current_user_id", currentUserId) // Pass the current user ID when adding a note
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE) // Start for result
        }
    }

    private fun deleteNoteConfirmation(heading: String) {
        // Show a confirmation dialog before deleting
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                deleteNoteFromDatabase(heading) // Delete note from database
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteNoteFromDatabase(heading: String) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_NOTES, "${DatabaseHelper.COLUMN_HEADING}=? AND ${DatabaseHelper.COLUMN_USER_ID}=?", arrayOf(heading, currentUserId.toString()))
        loadNotes() // Refresh the notes list
        Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show()
    }

    private fun loadNotes() {
        notesList.clear()
        val cursor: Cursor = db.rawQuery("SELECT ${DatabaseHelper.COLUMN_HEADING} FROM ${DatabaseHelper.TABLE_NOTES} WHERE ${DatabaseHelper.COLUMN_USER_ID}=?", arrayOf(currentUserId.toString()))
        if (cursor.moveToFirst()) {
            do {
                notesList.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        notesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, notesList)
        notesListView.adapter = notesAdapter
    }

    // Handle the result from AddNoteActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check if result comes from AddNoteActivity
        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == Activity.RESULT_OK) {
            loadNotes() // Refresh notes list after adding or editing a note
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.notepad_menu, menu) // Inflate the menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Override the back button to go to the LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Finish the current activity
    }
}
