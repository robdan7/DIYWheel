package com.example.diywheel

import android.app.Service
import android.bluetooth.*
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import java.lang.Exception
import java.util.*

private const val STATE_DISCONNECTED = 0
private const val STATE_CONNECTING = 1
private const val STATE_CONNECTED = 2
private const val STATE_CLOSED = 3

val UART_SERVICE : UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
val RX: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
val TX: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
//private val TAG = BluetoothLeService::class.java.simpleName
const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
const val ACTION_GATT_SERVICES_DISCOVERED =
    "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
const val ACTION_DATA_SEND_SUCCESS = "com.example.bluetooth.le.ACTION_GAT_DATA_SENT"
const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"
const val ACTION_DEBUG = "com.example.bluetooth.le.ACTION_DEBUG"
// A service that interacts with the BLE device via the Android BLE API.

class BluetoothLeService() : Service() {

    private var connectionState = STATE_DISCONNECTED
    private lateinit var bluetoothGatt : BluetoothGatt
    private val binder = LocalBinder()
    private var writeInProgress = false

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            //test.invoke("Callback function")
            val intentAction: String
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    intentAction = ACTION_GATT_CONNECTED
                    connectionState = STATE_CONNECTED
                    broadcastUpdate(intentAction)
                    bluetoothGatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    intentAction = ACTION_GATT_DISCONNECTED
                    connectionState = STATE_DISCONNECTED
                    broadcastUpdate(intentAction)
                }
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    bluetoothGatt.readCharacteristic(bluetoothGatt.getService(UART_SERVICE).getCharacteristic(RX))
                    broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                }
                //else -> Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        fun sendData(data: String) {

        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {

            super.onCharacteristicChanged(gatt, characteristic)

            if (characteristic != null) {
                broadcastUpdate(ACTION_DATA_AVAILABLE,characteristic)
            }
        }

        /**
         * This function will be called whenever a write process takes place with response enabled.
         */
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Write successful
            }
            writeInProgress = false
            broadcastUpdate(ACTION_DATA_SEND_SUCCESS)
        }

        // Result of a characteristic read operation
        /*override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val rx = bluetoothGatt.getService(UART_SERVICE).getCharacteristic(TX)
            val value = rx.getStringValue(0)
            //super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }*/
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }


    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService = this@BluetoothLeService
    }

    override fun onBind(intent: Intent?): IBinder? {
        // TODO Auto-generated method stub
        return binder
    }

    override fun bindService(service: Intent?, conn: ServiceConnection, flags: Int): Boolean {
        return super.bindService(service, conn, flags)
    }

    fun connect(device: BluetoothDevice) {
        this.bluetoothGatt = device.connectGatt(this,false, this.gattCallback)
        device.createBond()
    }

    fun disconnect() {
        if(connectionState != STATE_DISCONNECTED) {
            bluetoothGatt?.disconnect()
            connectionState = STATE_DISCONNECTED
        }
    }

    fun close() {
        if(connectionState == STATE_CLOSED) {
            return
        }
        if (connectionState != STATE_DISCONNECTED) {
            bluetoothGatt?.disconnect()
        }
        connectionState == STATE_CLOSED
        try {
            bluetoothGatt?.close()
        } catch (e: Exception) {

        }
        //bluetoothGatt?.close()
    }

    fun getGattService(uuid: UUID): BluetoothGattService? {
        return bluetoothGatt.getService(uuid)
    }

    /**
     * Write to a service characteristic.
     */
    fun writeCharacteristic(data: String, service: UUID, characteristic: UUID): Boolean {
        if(data == null || data.isBlank()) {
            return false
        }
        return writeCharacteristic(data.toByteArray(), service, characteristic)
    }

    fun writeCharacteristic(data: ByteArray, service: UUID, characteristic: UUID): Boolean {
        if(data == null || data.isEmpty()) {
            return false
        }
        val connection = bluetoothGatt.getService(service).getCharacteristic(characteristic)
        if (connection == null) {
            return false
        }
        // Disable response
        if ((connection.getProperties() and BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) != 0) {
            connection.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        connection.setValue(data)
        bluetoothGatt.writeCharacteristic(connection)
        while(writeInProgress){}
        return true
    }

    fun disableCharacteristicNotification(service: UUID, charUID: UUID): Boolean {

        val characteristic  = bluetoothGatt.getService(service).getCharacteristic(charUID)
        if (characteristic == null) {
            return false
        }

        if (!bluetoothGatt.setCharacteristicNotification(characteristic, false)) {
            return false
        }
        return true
        val descriptor = characteristic.getDescriptor(CCCD)
        if (descriptor == null) {
            return false
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if(!bluetoothGatt.writeDescriptor(descriptor)) {
            return false
        }

        return true
    }

    fun enableCharacteristicNotification(service: UUID, charUID: UUID): Boolean {

        val characteristic  = bluetoothGatt.getService(service).getCharacteristic(charUID)
        if (characteristic == null) {
            return false
        }

        if (!bluetoothGatt.setCharacteristicNotification(characteristic, true)) {
            return false
        }
        val descriptor = characteristic.getDescriptor(CCCD)
        if (descriptor == null) {
            bluetoothGatt.setCharacteristicNotification(characteristic, false)
            return false
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        if(!bluetoothGatt.writeDescriptor(descriptor)) {
            bluetoothGatt.setCharacteristicNotification(characteristic, false)
            return false
        }
        //bluetoothGatt.setCharacteristicNotification(connection, true)
        //val descriptor = connection.getDescriptor(CCCD)
        //descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        //bluetoothGatt.writeDescriptor(descriptor)
        return true
    }

    fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        // You can also include some extra data.
        //intent.putExtra("message", "This is my new message!")
        this.sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        //test.invoke(action)
        val intent = Intent(action)
        val data: ByteArray ?= characteristic.value
        if (data?.isNotEmpty() == true) {
            /*val hexString: String = data.joinToString(separator = "") {
                String.format("%c", it)
            }*/
            intent.putExtra(EXTRA_DATA, data)
        }
        this.sendBroadcast(intent)
    }
}