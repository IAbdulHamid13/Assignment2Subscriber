package com.example.assignment2subscriber

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.nio.charset.Charset
import java.util.UUID

class MainActivity : AppCompatActivity(), OnMapReadyCallback, viewStudentDetails {
    private var client: Mqtt5AsyncClient? = null
    private lateinit var mMap: GoogleMap
    val studentsLocations = mutableMapOf<Number, MutableList<CustomMarkerPoints>>()
    private lateinit var deviceListAdapter: DeviceListAdapter
    private lateinit var devicesList: RecyclerView
    private var viewButtonPressed: Boolean = false
    lateinit var maxSpeed: TextView
    lateinit var minSpeed: TextView
    lateinit var avgSpeed: TextView
    lateinit var viewallbutton: Button
    lateinit var speedstats: ConstraintLayout
    lateinit var dbhelper: DBHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        maxSpeed = findViewById<TextView>(R.id.tv_max_speed)
        minSpeed = findViewById<TextView>(R.id.tv_min_speed)
        avgSpeed = findViewById<TextView>(R.id.tv_avg_speed)
        viewallbutton = findViewById(R.id.viewallbutton)
        devicesList = findViewById(R.id.devicesList)
        speedstats = findViewById(R.id.speedstats)
        speedstats.visibility = View.GONE
        viewallbutton.visibility = View.GONE
        dbhelper = DBHelper(this, null)
        client = Mqtt5Client.builder().identifier(UUID.randomUUID().toString())
            .serverHost("broker-816037392.sundaebytestt.com").serverPort(1883).build().toAsync()
        viewallbutton.setOnClickListener {
            viewall()
        }
        deviceListAdapter = DeviceListAdapter(emptyList(), this, this)
        devicesList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        devicesList.adapter = deviceListAdapter
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        startReceivingMessages()
    }

    fun populateMapFromDatabase() {
        studentsLocations.clear()
        mMap.clear()
        val db = dbhelper.readableDatabase
        val cursor = db.query(
            "studentlocationdata", null, null, null, null, null, "timestamp ASC"
        )
        if (cursor.count == 0) {
            cursor.close()
            db.close()
            return
        }

        while (cursor.moveToNext()) {
            val studentId = cursor.getInt(cursor.getColumnIndexOrThrow("studentID"))
            val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
            val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
            val speed = cursor.getFloat(cursor.getColumnIndexOrThrow("speed"))
            val timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"))
            val latLng = LatLng(latitude, longitude)
            val studentPoints = studentsLocations.getOrPut(studentId) { mutableListOf() }
            val newCustomPoint = CustomMarkerPoints(studentPoints.size + 1, latLng)
            studentPoints.add(newCustomPoint)
            mMap.addMarker(
                MarkerOptions().position(latLng).title("Student $studentId")
            )
        }
        cursor.close()
        db.close()
        if (studentsLocations.isNotEmpty()) {
            drawPolyline()
            val bounds = LatLngBounds.builder()
            studentsLocations.values.forEach { studentPoints ->
                studentPoints.forEach { bounds.include(it.point) }
            }
            try {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        bounds.build(), 100
                    )
                )
            } catch (e: IllegalStateException) {
                Log.e("MapPopulation", "Could not adjust camera", e)
            }
            updateDevicesListFromDatabase()
        }
    }

    private fun updateDevicesListFromDatabase() {
        val studentSpeedMap = mutableMapOf<Number, MutableList<Double>>()
        val db = dbhelper.readableDatabase
        val cursor = db.query(
            "studentlocationdata", arrayOf("studentID", "speed"), null, null, null, null, null
        )
        while (cursor.moveToNext()) {
            val studentId = cursor.getInt(cursor.getColumnIndexOrThrow("studentID"))
            val speed = cursor.getFloat(cursor.getColumnIndexOrThrow("speed")).toDouble()

            studentSpeedMap.getOrPut(studentId) { mutableListOf() }.add(speed)
        }
        cursor.close()
        db.close()
        val deviceDataList = studentSpeedMap.map { (studentId, speeds) ->
            DeviceData(
                studentId = studentId,
                minSpeed = speeds.minOrNull() ?: 0.0,
                maxSpeed = speeds.maxOrNull() ?: 0.0
            )
        }
        deviceListAdapter.updateDevices(deviceDataList)
    }

    override fun viewStudentDetails(studentId: DeviceData) {
        val studentPoints: MutableList<CustomMarkerPoints>? = studentsLocations[studentId.studentId]
        Log.d("Student points", studentPoints.toString())
        for (point in studentPoints!!) {
            Log.e("POINT.POINT", point.point.toString())
            addMarkerAtLocation1(point.point, studentId.studentId)
        }
        val bound = drawPolyline(studentId.studentId)
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bound, 100
            )
        )
        viewButtonPressed = true
        val headertext: TextView = findViewById(R.id.headerText)
        headertext.text = "Student ID: ${studentId.studentId}"
        val deviceslist: RecyclerView = findViewById(R.id.devicesList)
        devicesList.visibility = View.GONE
        speedstats.visibility = View.VISIBLE
        viewallbutton.visibility = View.VISIBLE
        maxSpeed.text = "Max Speed: ${studentId.maxSpeed}"
        minSpeed.text = "Min Speed: ${studentId.minSpeed}"
        val numberOfPoints = studentPoints.size
        val averagespeed = if (numberOfPoints > 0) {
            (studentId.maxSpeed.toInt() + studentId.minSpeed.toInt()) / numberOfPoints
        } else {
            0.0
        }
        avgSpeed.text = "Avg Speed: ${studentId.maxSpeed}"
    }

    fun viewall() {
        viewButtonPressed = false
        devicesList.visibility = View.VISIBLE
        val headertext: TextView = findViewById(R.id.headerText)
        headertext.text = "Assignment Two - Subscriber"
        speedstats.visibility = View.GONE
        viewallbutton.visibility = View.GONE
    }

    private fun addMarkerAtLocation1(point: LatLng, studentId: Number) {
        mMap.addMarker(MarkerOptions().position(point).title("Marker"))
    }

    override fun drawPolyline(studentId: Number): LatLngBounds {
        val latLngPoints: List<LatLng>?
        val bounds: LatLngBounds.Builder = LatLngBounds.builder()
        val studentPoints = studentsLocations[studentId]
        latLngPoints = studentPoints?.map { it.point }
        val colour = when (studentId.toInt() % 4) {
            0 -> Color.BLUE
            1 -> Color.RED
            2 -> Color.GREEN
            else -> Color.YELLOW
        }
        val polylineOptions =
            PolylineOptions().addAll(latLngPoints).width(5f).geodesic(true).color(colour)
        mMap.addPolyline(polylineOptions)
        latLngPoints?.forEach { bounds.include(it) }

        return bounds.build()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        populateMapFromDatabase()
    }

    private fun addMarkerAtLocation(latLng: LatLng, studentId: Number) {
        val studentPoints = studentsLocations.getOrPut(studentId) { mutableListOf() }
        val newCustomPoint = CustomMarkerPoints(studentPoints.size + 1, latLng)
        studentPoints.add(newCustomPoint)
        mMap.addMarker(
            MarkerOptions().position(latLng).title("Marker ${newCustomPoint.id}")
        )
    }

    private fun updateDevicesList(receivedLoc: LocationData) {
        val deviceDataList: List<DeviceData> = studentsLocations.keys.mapIndexed { _, studentId ->
            dbhelper.insertIntoDB(
                studentID = receivedLoc.getStudentID(),
                latitude = receivedLoc.getlatitude(),
                longitude = receivedLoc.getlongitude(),
                speed = receivedLoc.getSpeed().toFloat(),
                timestamp = receivedLoc.getTimestamp()
            )
            val currentSpeed: Double =
                if (studentId == receivedLoc.getStudentID()) receivedLoc.getSpeed()
                    .toDouble() else 0.0
            val speeds: List<Double> = studentsLocations[studentId]?.map {
                when (it.point) {
                    LatLng(
                        receivedLoc.getlatitude(), receivedLoc.getlongitude()
                    ) -> receivedLoc.getSpeed().toDouble()

                    else -> 0.0
                }
            }?.toList() ?: listOf(currentSpeed)
            DeviceData(
                studentId = studentId, minSpeed = speeds.min(), maxSpeed = speeds.max()
            )
        }
        deviceListAdapter.updateDevices(deviceDataList)
    }

    private fun drawPolyline(): LatLngBounds? {
        if (!viewButtonPressed) {
            var latLngPoints: List<LatLng>?
            val bounds: LatLngBounds.Builder = LatLngBounds.builder()
            for (student in studentsLocations.keys) {
                val studentPoints = studentsLocations[student]
                latLngPoints = studentPoints?.map { it.point }
                val colour = when (student.toInt() % 4) {
                    0 -> Color.BLUE
                    1 -> Color.RED
                    2 -> Color.GREEN
                    else -> Color.YELLOW
                }
                val polylineOptions =
                    PolylineOptions().addAll(latLngPoints).width(5f).geodesic(true).color(colour)
                mMap.addPolyline(polylineOptions)
                latLngPoints?.forEach { bounds.include(it) }
            }
            return bounds.build()
        }
        return null
    }

    private fun connectToBroker() {
        try {
            client?.connect()?.whenComplete { _, throwable ->
                if (throwable != null) Log.e("MQTT", "Connection failed", throwable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopic() {
        try {
            client?.subscribeWith()?.topicFilter("monke")?.callback { publish ->
                runOnUiThread {
                    val received = String(publish.payloadAsBytes, Charset.defaultCharset())
                    val receivedLoc: LocationData = Gson().fromJson(
                        received, LocationData::class.java
                    ) //Sometimes this just doesn't want to work properly and I don't know why
                    val latLng = LatLng(receivedLoc.getlatitude(), receivedLoc.getlongitude())
                    addMarkerAtLocation(latLng, receivedLoc.getStudentID())
                    val bound = drawPolyline()
                    if (bound != null) {
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                bound, 100
                            )
                        )
                        Log.d(
                            "MQTT",
                            "Received message from broker on topic assignment/location: $receivedLoc"
                        )
                        updateDevicesList(receivedLoc)
                    } else Log.d("BOUNDLESS", "Bound is null")
                }
            }?.send()?.whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MQTT", "Subscription failed", throwable)
                } else Log.d("MQTT", "Subscribed to topic assignment/location")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startReceivingMessages() {
        connectToBroker()
        subscribeToTopic()
    }
}