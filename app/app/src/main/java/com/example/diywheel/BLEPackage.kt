package com.example.diywheel

class BLEPackage(numPackages: Int) {
    private val PAYLOAD_SIZE = 19   // Data bytes per packet
    private val receivedData : ByteArray = ByteArray(numPackages*PAYLOAD_SIZE)
    private var lastPackage : Int = -1
    private var receiveComplete = false
    private var droppedPackage = false

    public fun getData() : ByteArray {
        return this.receivedData
    }
    public fun receive(byteArray: ByteArray): Int {
        if (droppedPackage || receiveComplete) {
            return -1
        }
        val packetInfo = byteArray[0].toInt() // last packet bit plus packet index
        val currentPackage = packetInfo.and(0b01111111)

        // Check if one or more packets have been dropped
        if (currentPackage > lastPackage+1) {
            droppedPackage = true
            return lastPackage
        }

        // Check if last packet bit is set
        if ((packetInfo and 0b10000000) != 0) {
            receiveComplete = true
        }

        // Store data
        var i = 0;
        while(i < PAYLOAD_SIZE) {
            receivedData[currentPackage*PAYLOAD_SIZE+i] = byteArray[i+1]
            i++
        }
        lastPackage = currentPackage
        return currentPackage
    }

    public fun startNewTransmission() {
        this.lastPackage = -1
        this.droppedPackage = false
        this.receiveComplete = false
    }

    public fun transmissioncomplete() : Boolean{
        return this.receiveComplete
    }

    public fun hasDroppedPackages() : Boolean {
        return this.droppedPackage
    }
}