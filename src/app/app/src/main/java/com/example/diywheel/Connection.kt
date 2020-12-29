package com.example.diywheel

import android.content.*
import android.icu.text.IDNA
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import kotlinx.android.synthetic.main.activity_connection.*
import java.lang.Exception
import java.nio.ByteBuffer


class Connection : AppCompatActivity() {
    private var blePackage: BLEPackage = BLEPackage(4)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        Intent(this, BluetoothLeService::class.java).also { intent ->
            bindService(intent, bluetoothService.connection, Context.BIND_AUTO_CREATE)
        }

        Messagebutton.setOnClickListener {
            blePackage.startNewTransmission()
            bluetoothService.mService.writeCharacteristic(messageInput.text.toString(), UART_SERVICE, RX)
        }
        /*if (bluetoothService.mService.enableCharacteristicNotification(UART_SERVICE, TX)) {
            Connectiontext.setText("GATT Services Discovered!")
        } else {
            Connectiontext.setText("Could not set read notification!")
        }
        readServices()*/
        //actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun init() {
        try {
            if (bluetoothService.mService.enableCharacteristicNotification(UART_SERVICE, TX)) {
                Connectiontext.setText("GATT Services Discovered!")
            } else {
                Connectiontext.setText("Could not set read notification!")
            }
            readServices()
        } catch(e: Exception) {
            Connectiontext.setText("Could not initialize GATT stuff")
        }
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
        filter.addAction(ACTION_DEBUG)
        filter.addAction(ACTION_DATA_SEND_SUCCESS)
        filter.addAction(ACTION_DATA_SEND_SUCCESS)
        this.registerReceiver(gattUpdateReceiver, filter)

        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()

        bluetoothService.mService.disableCharacteristicNotification(UART_SERVICE,TX)
        unbindService(bluetoothService.connection)
        //this.unregisterReceiver(gattUpdateReceiver)
        bluetoothService.mService.disconnect()
    }

    data class DataPackage(
        var tempFET:Float,
        var tempMotor: Float,
        var avgMotorCurrent:Float,
        var avgInputCurrent:Float,
        var avgIqCurent:Float,
        var avgIdCurent:Float,
        var dutyCycleNow:Float,
        var rpm:Long,
        var inpVoltage:Float,
        var ampHours:Float,
        var ampHoursCharged:Float,
        var watt_hours:Float,
        var watt_hours_charged:Float,
        var tachometer:Long,
        var tachometerAbs:Long,
        var fault:Char,
        var throttle:Float
    )

    private fun decodeMessage(data : ByteArray) {
        var buffer: ByteBuffer
        try {
            buffer =ByteBuffer.wrap(data)
        } catch (e: Exception) {
            Infotext.setText("Could not create buffer from data")
            return
        }
        try {
            /*
            var dataPackage: DataPackage = DataPackage(
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getLong(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getFloat(),
                buffer.getLong(),
                buffer.getLong(),
                buffer.getChar(),
                buffer.getFloat())
            val text: String =
                "tempFET: " + dataPackage.tempFET +
                        "\ntempMotor: " + dataPackage.tempMotor +
                        "\navgMotorCurrent: " + dataPackage.avgMotorCurrent +
                        "\navgInputCurrent: " + dataPackage.avgInputCurrent+
                        "\navgIqCurent: "+dataPackage.avgIqCurent+
                        "\navgIdCurent: "+dataPackage.avgIdCurent+
                        "\ndutyCycleNow: "+dataPackage.dutyCycleNow+
                        "\nrpm: "+dataPackage.rpm+
                        "\ninpVoltage: "+dataPackage.inpVoltage+
                        "\nampHours: "+dataPackage.ampHours+
                        "\nampHoursCharged: "+dataPackage.ampHoursCharged+
                        "\nwatt_hours: "+dataPackage.watt_hours+
                        "\nwatt_hours_charged: "+dataPackage.watt_hours_charged+
                        "\ntachometer: "+dataPackage.tachometer+
                        "\ntachometerAbs: "+dataPackage.tachometerAbs+
                        "\nfault: "+dataPackage.fault+
                        "\nthrottle: "+dataPackage.throttle

             */
            Infotext.setText("First float: " + String(data))

            //val str = String(data)
            //Infotext.setText(Infotext.text.toString() + "\nmessage data: " + str)
        } catch(e: Exception) {
            Infotext.setText("Could not get message")
        }

        //Infotext.setText("Message: " + data[0].toInt() + " " + data[1].toInt() + " " + data[2].toInt() + " " + data[3].toInt() + " " + test)
    }

    private val gattUpdateReceiver = object : BroadcastReceiver() {
        private var connected = false
        private val number = (0..10).random()

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
                    if (bluetoothService.mService.enableCharacteristicNotification(UART_SERVICE, TX)) {
                        Connectiontext.setText("GATT Services Discovered!")
                    } else {
                        Connectiontext.setText("Could not set read notification!")
                    }
                    readServices()
                }
                ACTION_DATA_AVAILABLE -> {

                    val message = intent.getByteArrayExtra(EXTRA_DATA)
                    //ErrorCallback.setText(String(message))
                    val packet = blePackage.receive(message)
                    //Infotext.setText(Infotext.text.toString() + "\n Received packet " + packet)
                    if (blePackage.hasDroppedPackages()) {
                        //ErrorCallback.setText("Droped packet!!! " + packet)
                        Infotext.setText(Infotext.text.toString() + "\n Dropped packet " + packet)
                    }
                    if (blePackage.transmissioncomplete()) {
                        decodeMessage(blePackage.getData())
                        Infotext.setText(Infotext.text.toString() + "\n Received all packages!")
                    }
                    //unregisterReceiver(this)
                    //Infotext.setText(message)
                }
                ACTION_DATA_SEND_SUCCESS -> {
                    ErrorCallback.setText(ErrorCallback.text.toString() + " Pressed ")
                    //ErrorCallback.setText("Data was successfully written!")
                }
                ACTION_DEBUG -> {
                    //ErrorCallback.setText("GATT Data available!")
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
                init()
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                mBound = false
            }
        }
    }

}