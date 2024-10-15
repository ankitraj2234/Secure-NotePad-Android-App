package com.example.myapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2 // Increased version due to schema change
        private const val DATABASE_NAME = "userDatabase.db"
        const val TABLE_USER = "User"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_BACKUP_TOKEN = "backup_token"
        const val COLUMN_IMAGE = "image"

        // Notes table constants
        const val TABLE_NOTES = "Notes"
        const val COLUMN_NOTE_ID = "note_id"
        const val COLUMN_USER_ID = "user_id" // Adding user_id for better association
        const val COLUMN_HEADING = "heading"
        const val COLUMN_CONTENT = "content"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Enable foreign key constraints
        db?.execSQL("PRAGMA foreign_keys=ON;")

        // Create user table
        val createUserTable = ("CREATE TABLE $TABLE_USER (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NAME TEXT, " +
                "$COLUMN_USERNAME TEXT, " +
                "$COLUMN_EMAIL TEXT, " +
                "$COLUMN_PASSWORD TEXT, " +
                "$COLUMN_BACKUP_TOKEN TEXT, " +
                "$COLUMN_IMAGE BLOB)")
        db?.execSQL(createUserTable)

        // Create notes table with user_id column
        val createNotesTable = ("CREATE TABLE $TABLE_NOTES (" +
                "$COLUMN_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_USER_ID INTEGER, " + // Changed from username to user_id
                "$COLUMN_HEADING TEXT, " +
                "$COLUMN_CONTENT TEXT, " +
                "FOREIGN KEY($COLUMN_USER_ID) REFERENCES $TABLE_USER($COLUMN_ID) ON DELETE CASCADE)")
        db?.execSQL(createNotesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // If upgrading from version 1 to 2, add user_id to Notes without dropping tables
            try {
                db?.execSQL("PRAGMA foreign_keys=OFF;") // Temporarily disable foreign keys
                db?.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $COLUMN_USER_ID INTEGER;") // Add the user_id column
                db?.execSQL("PRAGMA foreign_keys=ON;")  // Re-enable foreign keys
            } catch (e: Exception) {
                Log.e("DatabaseUpgrade", "Error upgrading database: ${e.message}")
            }
        }
    }

    // Method to add a note for a specific user
    fun addNote(userId: Int, heading: String, content: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USER_ID, userId) // Changed to use user_id
            put(COLUMN_HEADING, heading)
            put(COLUMN_CONTENT, content)
        }
        val success = db.insert(TABLE_NOTES, null, contentValues)
        db.close()
        return success
    }

    // Method to retrieve all notes for a specific user
    fun getNotesByUser(userId: Int): List<Pair<String, String>> {
        val notesList = mutableListOf<Pair<String, String>>()
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_HEADING, $COLUMN_CONTENT FROM $TABLE_NOTES WHERE $COLUMN_USER_ID=?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        if (cursor.moveToFirst()) {
            do {
                val heading = cursor.getString(0)
                val content = cursor.getString(1)
                notesList.add(Pair(heading, content))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return notesList
    }

    // Other existing methods for user management...
}
