package com.example.assignment2subscriber

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DB_NAME, factory, DB_VERSION) {
    companion object {
        private val DB_NAME = "assignment2.db"
        private val DB_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(
            "CREATE TABLE studentlocationdata (" + "studentID INTEGER," + "latitude DOUBLE," + "longitude DOUBLE," + "speed FLOAT," + "timestamp TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    fun insertIntoDB(
        studentID: Number, latitude: Double, longitude: Double, speed: Float, timestamp: String
    ) {
        val values = ContentValues()
        values.put("studentID", studentID.toInt())
        values.put("latitude", latitude)
        values.put("longitude", longitude)
        values.put("speed", speed)
        values.put("timestamp", timestamp)

        val db = this.writableDatabase
        db.insert("studentlocationdata", null, values)
        db.close()
    }
}