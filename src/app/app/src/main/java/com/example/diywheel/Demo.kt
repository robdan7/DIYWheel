package com.example.diywheel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.demo.*
import java.io.IOException
import java.util.*

class Demo:  AppCompatActivity() {
    private lateinit var uuid : UUID
    private lateinit var connection : ConnectThread
    private lateinit var bluetoothAdapter : BluetoothAdapter
    private inner class ConnectThread(device : BluetoothDevice) : Thread() {
        private lateinit var device : BluetoothDevice
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {

            device.createRfcommSocketToServiceRecord(uuid)
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()
                textView2.setText("Connected to: " + device.name)

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(socket)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            textView2.setText("Closing bluetooth connection")
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                //Log.e( "Could not close the client socket", e)
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val dev:BluetoothDevice = intent.getParcelableExtra("bluetooth_device") as BluetoothDevice
        textView2.setText("Trying to connect to " + dev.name)
        connection = ConnectThread(dev)
        //var string: String ?= intent.getStringExtra("Device_name")
        //textView2.text = "Connected to: " + string

        Another_button.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

    }
}