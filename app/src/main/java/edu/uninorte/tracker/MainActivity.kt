package edu.uninorte.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket

// this is an specific class to send data using UDP protocol
class UdpSender {
    // this method just send the data using UDP protocol
    fun sendUdpMessage(message: String, ipAddress: String, port: Int) {
        // first try doing this
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
        } // is some there is some problem, the app will do this
        catch (e: Exception) {
            Log.d("UDP", "Exception")
            e.printStackTrace()
        }
    }
}

class WolSender {
    private var status = "unknown"

    // this method just send the data using TCP protocol
    fun sendMagicPacket(ipAddress: String, port: Int): String {
        // first try doing this
        try {
            // first create the socket to connect to Ip Address over the TCP port
            val socket = Socket(ipAddress, port)
            // create the outputStream that allow to send the data
            val outputStream: OutputStream = socket.getOutputStream()
            // create the inputStream that allow to receive the data
            val inputStream: InputStream = socket.getInputStream()

            Log.d("TCP", "sending data over TCP")
            outputStream.write("hi".toByteArray())
            Log.d("TCP", "data send over TCP")

            // Receive response from the server
            val buffer = ByteArray(1024)
            val bytesRead = inputStream.read(buffer)
            if (bytesRead != -1) {
                status = String(buffer, 0, bytesRead)
            }
            Log.d("TCP", "received status: $status")

            Log.d("TCP", "sending data over TCP")
            outputStream.write("bye".toByteArray())
            Log.d("TCP", "data send over TCP")

            outputStream.close()
            Log.d("TCP", "outputStream closed")
            inputStream.close()
            Log.d("TCP", "inputStream closed")
            socket.close()
            Log.d("TCP", "socket closed")
            return status
        } // is some there is some problem, the app will do this
        catch (e: Exception) {
            Log.d("TCP", "Exception")
            e.printStackTrace()
            return "error"
        }
    }
}

class MainActivity : AppCompatActivity() {

    private val locationService: LocationService = LocationService()

    private var latitude: Double = 0.00
    private var longitude: Double = 0.00
    private var timeStamp: Long = 0

    private lateinit var ipAddress: String
    private var portNumber = 0
    private lateinit var message: String
    private lateinit var url: String

    private lateinit var latitudeValue: TextView
    private lateinit var longitudeValue: TextView
    private lateinit var timeStampValue: TextView
    private lateinit var statusValue: TextView
    private lateinit var ipAddressValue: EditText
    private lateinit var portNumberValue: EditText

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        latitudeValue = findViewById(R.id.latitude_value)
        longitudeValue = findViewById(R.id.longitude_value)
        timeStampValue = findViewById(R.id.timeStamp_value)
        statusValue = findViewById(R.id.status_value)
        ipAddressValue = findViewById(R.id.ip_address_editText)
        portNumberValue = findViewById(R.id.port_number_editText)

        val getLocationButton = findViewById<Button>(R.id.Get_Location_Button)
        val sendUDPButton = findViewById<Button>(R.id.udp_send_data_button)
        val wakeOnServerButton = findViewById<Button>(R.id.wake_on_server_button)

        val udpSender = UdpSender()
        val wolSender = WolSender()

        getLocationButton.setOnClickListener {
            checkLocationPermissions()
            sendDataToServer()
        }
        sendUDPButton.setOnClickListener {
            showPopUp("sending data over UDP protocol")
            setDataToSend()
            showPopUp("ipAddress:$ipAddress,message:$message")
            lifecycleScope.launch(Dispatchers.IO) {
                udpSender.sendUdpMessage(message, ipAddress, portNumber)
            }
            showPopUp("data send over UDP protocol")
        }
        wakeOnServerButton.setOnClickListener {
            showPopUp("sending magic packet!")
            statusValue.text = "Asking for!"
            ipAddress = ipAddressValue.text.toString()
            portNumber = 25565
            var response: String
            lifecycleScope.launch(Dispatchers.IO) {
                response = wolSender.sendMagicPacket(ipAddress, portNumber)
                runOnUiThread {
                    statusValue.text = response
                }
            }
        }
    }

    private fun setDataToSend() {
        ipAddress = ipAddressValue.text.toString()
        portNumber = stringToInt(portNumberValue.text.toString())
        message = "${latitudeValue.text},${longitudeValue.text},${timeStampValue.text}"
    }

    private fun stringToInt(string: String): Int {
        val intValue = try {
            string.toInt()
        } catch (e: NumberFormatException) {
            0
        }
        return intValue
    }

    private fun showPopUp(message: String) {
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

        lifecycleScope.launch(Dispatchers.IO) {
            val result = locationService.getUserLocation(this@MainActivity)
            if (result != null) {
                latitude = result.latitude
                longitude = result.longitude
                timeStamp = result.time
            }
        }

        latitudeValue.text = latitude.toString()
        longitudeValue.text = longitude.toString()
        timeStampValue.text = timeStamp.toString()
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

    private fun sendDataToServer() {
        ipAddress = ipAddressValue.text.toString()
        url = "http://$ipAddress:25563/includes/api.php"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                //Data to send (it can be pars key-value)
                val jsonParams = JSONObject()
                jsonParams.put("latitude", "$latitude")
                jsonParams.put("longitude", "$longitude")
                jsonParams.put("timeStamp", "$timeStamp")

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonParams.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (responseBody != null) {
                    runOnUiThread {
                        showPopUp(responseBody)
                    }

                }

            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }
}