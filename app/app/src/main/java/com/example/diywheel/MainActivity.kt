package com.example.diywheel

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_connection.*
import kotlinx.android.synthetic.main.activity_main.*

private const val REQUEST_ENABLE_BT = 1
class MainActivity : AppCompatActivity(), AdapterView.OnItemClickListener {

    // -------------- Variables --------------
    private var mutable_list : MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val name_list : MutableList<String> = mutableListOf()
    private var mScanning = false
    private val SCAN_PERIOD: Long = 10000
    private var connected_device = -1
    private var connectionStatus = false
    private var connectionFailures = 0

    /**
     * Collection of bluetooth connection stuff
     */
    private val bluetoothGovernor =  object {
        val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
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

    /**
     * Starting point of application
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start the bluetooth callback service.
        startService(Intent(this, BluetoothLeService::class.java))
        Intent(this, BluetoothLeService::class.java).also { intent ->
            bindService(intent, bluetoothGovernor.connection, Context.BIND_AUTO_CREATE)
        }

        Search.setOnClickListener {
            //startActivity(Intent(this, Connection::class.java))
            search()
        }
        DisconnectButton.setOnClickListener {
            connectionStatus = false
            bluetoothGovernor.mService.disconnect()
            Debugtext.setText("Disconnected!")
        }
        MyList.setOnItemClickListener(this)
    }

    override fun onPause() {
        // Unregister since the activity is paused.
        this.unregisterReceiver(gattUpdateReceiver)

        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothGovernor.mBound) {
            bluetoothGovernor.mService?.close()
        }
    }

    override fun onResume() {
        //val filter = IntentFilter(ACTION_GATT_CONNECTED)
        //filter.addAction(ACTION_GATT_DISCONNECTED)
        //this.registerReceiver(gattUpdateReceiver, filter)


        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        val filter = IntentFilter(ACTION_GATT_CONNECTED)
        filter.addAction(ACTION_DATA_AVAILABLE)
        filter.addAction(ACTION_GATT_DISCONNECTED)
        filter.addAction(ACTION_GATT_SERVICES_DISCOVERED)
        this.registerReceiver(gattUpdateReceiver, filter)

        /*if (bluetoothGovernor.mBound) {
            bluetoothGovernor.mService.disconnect()
            Debugtext.setText("Disconnected!")
        }*/
        super.onResume()
    }


    /**
     * Search for bluetooth devices
     */
    fun search() {
        // Initializes Bluetooth adapter.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
        )

        if (!mScanning) { // Stops scanning after a pre-defined scan period.
            Handler().postDelayed({
                mScanning = false
                bluetoothGovernor.bluetoothLeScanner.stopScan(leScanCallback)
                Demotext.setText("Stopped searching.")
            }, SCAN_PERIOD)
            mScanning = true
            bluetoothGovernor.bluetoothLeScanner.startScan(leScanCallback)
        } else {
            mScanning = false
            bluetoothGovernor.bluetoothLeScanner.stopScan(leScanCallback)
        }

        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, name_list)
        MyList.adapter = arrayAdapter
        Demotext.setText("Searching...")
    }

    /**
     * This adds all discovered devices to a list
     */
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (!mutable_list.contains(result.device) && result.device.name != null && !result.device.name.isBlank()) {
                mutable_list.add(result.device)

                // Add device info to UI
                name_list.add(if (result.device.name == null) "" else result.device.name + "\n" + result.device.address)
                arrayAdapter.notifyDataSetChanged()
            }
        }
    }

    fun test() {
        startActivity(Intent(this, Connection::class.java))
    }

    /**
     * Start device connection
     */
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        //var intent: Intent = Intent(this,Demo::class.java)
        try {
            connected_device = id.toInt()
            //startActivity(Intent(this, Connection::class.java))
            connectDevice()
        } catch(e: Exception) {
            Demotext.setText("Error when clicking device index " + id.toString())
        }
    }

    private fun connectDevice() {
        try {
            if (mScanning) {
                bluetoothGovernor.bluetoothLeScanner.stopScan(leScanCallback)
            }
            connectionStatus = true
            bluetoothGovernor.mService.connect(mutable_list[connected_device])
            Debugtext.setText("Trying to connect!")
            Handler().postDelayed({
                if (connectionStatus) {
                    bluetoothGovernor.mService.disconnect()
                    Demotext.setText("Could not connect to device at all!")
                    connectionStatus = false
                }
            }, 5000)
        } catch (e: Exception) {
            Demotext.setText("Could not connect bluetooth")
        }
    }

    private val gattUpdateReceiver = object : BroadcastReceiver() {
        private var connected = false

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action){
                ACTION_GATT_CONNECTED -> {
                    Demotext.setText("Connected! Tries: " + connectionFailures)
                    Handler().postDelayed({
                        if (connectionStatus) {
                            Demotext.setText("Could not connect to GATT services")
                        }
                    }, 5000)

                }
                ACTION_GATT_SERVICES_DISCOVERED -> {
                    connectionFailures = 0
                    if (!connectionStatus) {
                        bluetoothGovernor.mService.disconnect()
                        return
                    }
                    Demotext.setText("Found Gatt services")
                    connectionStatus = false
                    test()
                }
                ACTION_GATT_DISCONNECTED -> {
                    Debugtext.setText("Disconnected!")
                    if (!connectionStatus) {
                        connectionFailures = 0
                        return
                    }
                    connectionFailures += 1
                    if (connectionFailures <= 5) {
                        connectDevice()
                    } else {
                        Demotext.setText("Could not connect bluetooth")
                    }
                }
            }
        }
    }
}