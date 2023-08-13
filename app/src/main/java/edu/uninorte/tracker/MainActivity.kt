package edu.uninorte.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.LocationManager.*
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpSender {
    fun sendUdpMessage(message: String, ipAddress: String, port: Int) {
        try {
            val udpSocket = DatagramSocket()
            val dataToSend = message.toByteArray()
            val address = InetAddress.getByName(ipAddress)
            val sendPacket = DatagramPacket(dataToSend, dataToSend.size, address, port)

            Log.d("UDP", "sending data over UDP")
            udpSocket.send(sendPacket)
            Log.d("UDP", "data send over UDP")
            udpSocket.close()
            Log.d("UDP", "socket closed")
        } catch (e: Exception) {
            Log.d("UDP", "Exception")
            e.printStackTrace()
        }
    }
}


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

    private lateinit var latitudeValue: TextView
    private lateinit var longitudeValue: TextView
    private lateinit var timeStampValue: TextView
    private lateinit var providerValue: TextView
    private lateinit var ipAddressValue: EditText
    private lateinit var portNumberValue: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        latitudeValue = findViewById(R.id.latitude_value)
        longitudeValue = findViewById(R.id.longitude_value)
        timeStampValue = findViewById(R.id.timeStamp_value)
        providerValue = findViewById(R.id.provider_value)
        ipAddressValue = findViewById(R.id.ip_address_editText)
        portNumberValue = findViewById(R.id.port_number_editText)

        val getLocationButton = findViewById<Button>(R.id.Get_Location_Button)
        val sendTCPButton = findViewById<Button>(R.id.tcp_send_data_button)
        val sendUDPButton = findViewById<Button>(R.id.udp_send_data_button)

        val udpSender = UdpSender()
        var ipAddress: String
        var portNumber: Int
        var message: String


        getLocationButton.setOnClickListener {
            checkLocationPermissions()
        }
        sendTCPButton.setOnClickListener {
            showPopUp("sending data over TCP protocol (coming soon)")
        }
        sendUDPButton.setOnClickListener {
            showPopUp("sending data over UDP protocol")
            ipAddress = ipAddressValue.text.toString()
            portNumber = stringToInt(portNumberValue.text.toString())
            Log.d("UDP", "")
            message = "${latitudeValue.text},${longitudeValue.text},${timeStampValue.text}"
            showPopUp("ipAddress:$ipAddress,message:$message")
            lifecycleScope.launch(Dispatchers.IO){
                udpSender.sendUdpMessage(message, ipAddress, portNumber)
            }
            showPopUp("data send over UDP protocol")
        }
    }


    private fun stringToInt(string: String): Int{
        val intValue = try {
            string.toInt()
        } catch (e: NumberFormatException) {
            0
        }
        return intValue
    }
    private fun showPopUp(message: String){
        Toast.makeText(
            this@MainActivity,
            message,
            Toast.LENGTH_SHORT
        )
            .show()
    }

    //This function contains the logic for requesting Location permissions
    private fun requestLocationPermissions() {
        //Evaluate if "ACCESS_COARSE_LOCATION" or "ACCESS_FINE_LOCATION" have already been rejected
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            || ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            //location permissions have been rejected
            showPopUp("Location permissions rejected")
        } else {
            //Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                500
            )
        }
    }

    //This function contains the logic to check if location permissions have been accepted
    private fun checkLocationPermissions() {
        //Evaluate if "ACCESS_COARSE_LOCATION" or "ACCESS_FINE_LOCATION" are not accepted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Request location permissions
            requestLocationPermissions()
        } else {
            //Get location permissions
            getUserLocation()
        }
    }


    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        showPopUp("Getting location")
        val location = this.fusedLocationProviderClient.lastLocation
        location.addOnSuccessListener {
            if (it != null) {
                latitudeValue.text = it.latitude.toString()
                longitudeValue.text = it.longitude.toString()
                timeStampValue.text = it.time.toString()
                providerValue.text = it.provider
            }
            showPopUp("Location Got")
        }
        for (element in locationManager.allProviders) Log.d("Location", element)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //This is the default command in the function, DO NOT CHANGE THIS LINE!!
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //Check if the accepted permission has the following "requestCode".
        if (requestCode == 500) {
            //The requested code matches the one we have defined.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //If all seems to be working properly, then get user location
                getUserLocation()
            } else {
                //The permissions have not been accepted
                showPopUp("Location Permissions rejected for the first time")
            }
        }
    }
}