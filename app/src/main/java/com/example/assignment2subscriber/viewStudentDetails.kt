package com.example.assignment2subscriber

import com.google.android.gms.maps.model.LatLngBounds

interface viewStudentDetails {
    fun viewStudentDetails(studentId: DeviceData);
    fun drawPolyline(studentId: Number): LatLngBounds;
}