package com.example.assignment2subscriber

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceListAdapter(
    private var devices: List<DeviceData>,
    private val context: Context,
    private var viewStudentDetails: viewStudentDetails,
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceId: TextView = view.findViewById(R.id.deviceId)
        val minSpeedValue: TextView = view.findViewById(R.id.minSpeedValue)
        val maxSpeedValue: TextView = view.findViewById(R.id.maxSpeedValue)
        val viewMoreButton: Button = view.findViewById(R.id.viewMoreButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.device_list_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceId.text = "Student ID: ${device.studentId}"
        holder.minSpeedValue.text = "${device.minSpeed} m/s"
        holder.maxSpeedValue.text = "${device.maxSpeed} m/s"
//        val studentPoints = studentsLocations[position]
        holder.viewMoreButton.setOnClickListener {
//            val intent = Intent(context, DeviceDetailsActivity::class.java).apply {
//                putExtra("STUDENT_ID", device.studentId.toInt())
//                putExtra("MIN_SPEED", device.minSpeed)
//                putExtra("MAX_SPEED", device.maxSpeed)
//                putExtra("STUDENTS_LOCATIONS", studentPoints)
//            }
//            context.startActivity(intent)
//            onViewMoreClick(device.studentId)
            viewStudentDetails.viewStudentDetails(device)
        }
    }

    override fun getItemCount() = devices.size

    fun updateDevices(newDevices: List<DeviceData>) {
        devices = newDevices
        notifyDataSetChanged()
    }
}