package edu.uninorte.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager.*
import android.os.Bundle

import android.widget.Button

import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


// this is an specific class to send data using UDP protocol


class MainActivity : AppCompatActivity() {

    private val locationService: LocationService = LocationService()

    private var latitude: Double = 0.00
    private var longitude: Double = 0.00
    private var timeStamp: Long = 0
    private val sendInterval = 10000L // Interval to send in milliseconds(10 seconds)

    private var sendData = false
    private var showUi = true

    private lateinit var latitudeValue: TextView
    private lateinit var longitudeValue: TextView
    private lateinit var timeStampValue: TextView
    private lateinit var statusValue: TextView


    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        latitudeValue = findViewById(R.id.latitude_value)
        longitudeValue = findViewById(R.id.longitude_value)
        timeStampValue = findViewById(R.id.timeStamp_value)
        statusValue = findViewById(R.id.status_value)


        val getLocationButton = findViewById<Button>(R.id.Get_Location_Button)

        getLocationButton.setOnClickListener {
            checkLocationPermissions()
            if (sendData) {
                getLocationButton.text = getString(R.string.stop_sending_data)
                sendDataToServer()
            } else {
                getLocationButton.text = getString(R.string.start_sending_data)
            }
        }
    }

    private fun showPopUp(message: String) {
        if (showUi){
            runOnUiThread {
                Toast.makeText(
                    this@MainActivity,
                    message,
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
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
            sendData = !sendData
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getUserLocation() {
//        showPopUp("Getting location")
//        val location = this.fusedLocationProviderClient.lastLocation
//        location.addOnSuccessListener {
//            if (it != null) {
//                latitudeValue.text = it.latitude.toString()
//                longitudeValue.text = it.longitude.toString()
//                timeStampValue.text = it.time.toString()
//            }
//            showPopUp("Location Got")
//        }
//        for (element in locationManager.allProviders) Log.d("Location", element)

        val result = locationService.getUserLocation(this@MainActivity)
        if (result != null) {
            latitude = result.latitude
            longitude = result.longitude
            timeStamp = result.time

        }
        if (showUi){
            runOnUiThread {
                latitudeValue.text = latitude.toString()
                longitudeValue.text = longitude.toString()
                timeStampValue.text = timeStamp.toString()
            }
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
                sendData = !sendData
            } else {
                //The permissions have not been accepted
                showPopUp("Location Permissions rejected for the first time")
            }
        }
    }

    private fun sendDataToServer() {


        lifecycleScope.launch(Dispatchers.IO) {
            while (sendData) {
                try {
                    getUserLocation()

                    val client = OkHttpClient()

                    // Data to send (it can be pars key-value)
                    val jsonParams = JSONObject()
                    jsonParams.put("latitude", "$latitude")
                    jsonParams.put("longitude", "$longitude")
                    jsonParams.put("timeStamp", "$timeStamp")

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = jsonParams.toString().toRequestBody(mediaType)

                    // List of URLs to send data to
                    val urls = listOf(
                        "http://hostname8913.ddns.net/includes/api.php",
                        "http://hostname8914.ddns.net/includes/api.php",
                        "http://hostname8915.ddns.net/includes/api.php",
                        "http://hostname8916.ddns.net/includes/api.php"
                    )

                    for (url in urls) {
                        val request = Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        val responseBody = response.body?.string()
                        showPopUp(responseBody.toString())



                    }
                    delay(sendInterval)
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }
}