package com.example.diywheel

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import kotlinx.android.synthetic.main.activity_connection.*


class Connection : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        Intent(this, BluetoothLeService::class.java).also { intent ->
            bindService(intent, bluetoothService.connection, Context.BIND_AUTO_CREATE)
        }
        Messagebutton.setOnClickListener {
            bluetoothService.mService.writeCharacteristic(messageInput.text.toString(), UART_SERVICE, TX)
        }
        //actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun readServices() {
        if (bluetoothService.mBound) {
            Errortext.setText("Bound to service")
            val uart = bluetoothService.mService.getGattService(UART_SERVICE)
            var result : String = "Services:\n"
            if (uart != null) {
                result += "    UART service\n"
                val tx = uart.getCharacteristic(TX)
                val rx = uart.getCharacteristic(RX)
                if(tx != null) {
                    result += "    TX service\n"
                }
                if(rx != null) {
                    result += "    RX service\n"
                }
            }
        } else {
            Errortext.setText("Error")
        }
    }

    override fun onPause() {
        // Unregister since the activity is paused.
        this.unregisterReceiver(gattUpdateReceiver)
        super.onPause()
    }
    override fun onResume() {
        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        val filter = IntentFilter(ACTION_GATT_CONNECTED)
        filter.addAction(ACTION_DATA_AVAILABLE)
        filter.addAction(ACTION_GATT_DISCONNECTED)
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
        this.registerReceiver(gattUpdateReceiver, filter)

        super.onResume()
    }
    private val gattUpdateReceiver = object : BroadcastReceiver() {
        private var connected = false

        override fun onReceive(context: Context, intent: Intent) {
            Connectiontext.setText("OnReceive!")
            val action = intent.action
            when (action){
                ACTION_GATT_CONNECTED -> {
                    connected = true
                    Connectiontext.setText("GATT Connected!")
                }
                ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    Connectiontext.setText("GATT Disconnected!")
                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    Connectiontext.setText("GATT Services Discovered!")
                    readServices()
                }
                ACTION_DATA_AVAILABLE -> {
                    Connectiontext.setText("GATT Data available!")
                }
                ACTION_DATA_SEND_SUCCESS -> {
                    Errortext.setText("Data was successfully written!")
                }
            }
        }
    }


    private val bluetoothService =  object {
        lateinit var mService: BluetoothLeService   // Object reference to the bluetooth service
        var mBound: Boolean = false
        val connection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                val binder = service as BluetoothLeService.LocalBinder
                mService = binder.getService()
                mBound = true
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                mBound = false
            }
        }
    }

}