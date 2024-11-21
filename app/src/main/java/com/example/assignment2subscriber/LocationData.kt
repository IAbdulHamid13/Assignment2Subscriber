package com.example.assignment2subscriber

class LocationData(
    studentID: Number, latitude: Double, longitude: Double, speed: Float, timestamp: String
) {
    private var latitude: Double = latitude
    private var longitude: Double = longitude
    private var speed: Float = speed
    private var timestamp: String = timestamp
    private var studentID: Number = studentID

    override fun toString(): String {
        return "Student ID = $studentID, Latitude = $latitude, Longitude = $longitude, Speed = $speed, Timestamp = $timestamp"
    }

    fun getlatitude(): Double {
        return latitude
    }

    fun getlongitude(): Double {
        return longitude
    }

    fun getStudentID(): Number {
        return studentID
    }

    fun getSpeed(): Float {
        return speed
    }
    fun getTimestamp(): String {
        return timestamp
    }
}