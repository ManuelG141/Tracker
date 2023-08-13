package edu.uninorte.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var latitudeValue: TextView
    private lateinit var longitudeValue: TextView
    private lateinit var timeStampValue: TextView
    private lateinit var providerValue: TextView
    private lateinit var ipAddressValue: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        latitudeValue = findViewById(R.id.latitude_value)
        longitudeValue = findViewById(R.id.longitude_value)
        timeStampValue = findViewById(R.id.timeStamp_value)
        providerValue = findViewById(R.id.provider_value)
        ipAddressValue = findViewById(R.id.ip_address_editText)

        val getLocationButton = findViewById<Button>(R.id.Get_Location_Button)
        val sendTCPButton = findViewById<Button>(R.id.tcp_send_data_button)
        val sendUDPButton = findViewById<Button>(R.id.udp_send_data_button)

        getLocationButton.setOnClickListener {
            checkLocationPermissions()
        }
        sendTCPButton.setOnClickListener {
            Toast.makeText(
                this,
                "sending data using TCP (coming soon)",
                Toast.LENGTH_SHORT
            )
                .show()
        }
        sendUDPButton.setOnClickListener {
            Toast.makeText(
                this,
                "sending data using UDP (coming soon)",
                Toast.LENGTH_SHORT
            )
                .show()
        }
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
            Toast.makeText(this, "Location permissions rejected", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "Getting location", Toast.LENGTH_SHORT).show()
        val location = fusedLocationProviderClient.lastLocation
        location.addOnSuccessListener {
            if (it != null) {
                latitudeValue.text = it.latitude.toString()
                longitudeValue.text = it.longitude.toString()
                timeStampValue.text = it.time.toString()
                providerValue.text = LocationManager.GPS_PROVIDER
                Log.d(
                    "location",
                    "lat: ${it.latitude}" +
                            "lon: ${it.longitude}" +
                            "time: ${it.time} " +
                            "PackageManager.FEATURE_LOCATION_GPS: ${LocationManager.GPS_PROVIDER}"
                )
            }
            Toast.makeText(this, "Location Got", Toast.LENGTH_SHORT).show()
        }
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
                Toast.makeText(
                    this,
                    "Location Permissions rejected for the first time",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }
}