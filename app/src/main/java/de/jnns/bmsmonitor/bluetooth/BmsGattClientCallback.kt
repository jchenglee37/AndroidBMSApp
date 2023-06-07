package de.jnns.bmsmonitor.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.util.Log
import de.jnns.bmsmonitor.bms.BmsCellInfoResponse
import de.jnns.bmsmonitor.bms.BmsGeneralInfoResponse
import java.util.*

@ExperimentalUnsignedTypes
class BmsGattClientCallback(
    val onGeneralInfoCallback: (cellInfo: BmsGeneralInfoResponse) -> Unit,
    val onCellInfoCallback: (cellInfo: BmsCellInfoResponse) -> Unit,
    val onConnectionSucceeded: () -> Unit,
    val onConnectionFailed: () -> Unit
) :
    BluetoothGattCallback() {

    var isConnected = false

    lateinit var readCharacteristic: BluetoothGattCharacteristic
    lateinit var writeCharacteristic: BluetoothGattCharacteristic

//    private val uartUuid = UUID.fromString("0000ff00-0000-1000-8000-00805f9b34fb")
//    private val rxUuid = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")
//    private val txUuid = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")
    private val uartUuid = UUID.fromString("0000A002-0000-1000-8000-00805f9b34fb")
    private val rxUuid = UUID.fromString("0000C305-0000-1000-8000-00805f9b34fb")
    private val txUuid = UUID.fromString("0000C302-0000-1000-8000-00805f9b34fb")

    private val bufferSize: Int = 256
    private var uartBuffer = ByteArray(bufferSize)
    private var uartBufferPos: Int = 0
    private var uartBytesLeft: Int = 0

    private var isInFrame = false

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d("BluetoothGatt", "connection failed")
            onConnectionFailed()
            isConnected = false
            return
        }

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices()
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            isConnected = false
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.d("BluetoothGatt", "onServicesDiscovered failed")
            return
        }

        var i: Int = 0

        for(serv in gatt.services) {
            i++
            Log.d("BMS", "Services" + i.toString() + ":" + serv.uuid.toString())
        }

        val uartService = gatt.getService(uartUuid)
        Log.d("BMS", "Got uartService:" + uartService.uuid.toString())

        if (uartService != null) {
            readCharacteristic = uartService.getCharacteristic(rxUuid)
            writeCharacteristic = uartService.getCharacteristic(txUuid)
            Log.d("BMS", "Got readCharacteristic:" + readCharacteristic.uuid.toString())
            Log.d("BMS", "Got writeCharacteristic:" + writeCharacteristic.uuid.toString())

            gatt.setCharacteristicNotification(writeCharacteristic, true)
            gatt.setCharacteristicNotification(readCharacteristic, true)

            onConnectionSucceeded()
            isConnected = true
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)

        // Log.d("BluetoothGatt", "BLE Data (" + characteristic.value.size + "): " + characteristic.value.toHexString())
        Log.d("BMS", "BLE Data (" + characteristic.value.size + "): " + characteristic.value.toHexString())

        for (byte: Byte in characteristic.value) {
//            if (uartBufferPos >= bufferSize) {
//                isInFrame = false
//                uartBufferPos = 0
//                uartBytesLeft = 0
//            }
//
//            uartBuffer[uartBufferPos] = byte
//
//            if (isInFrame) {
//                if (uartBufferPos == 3) {
//                    uartBytesLeft = byte.toInt()
//                }
//
//                if (byte.toUByte() == 0x77.toUByte() && uartBytesLeft < 1) {
//                    isInFrame = false
//                    onFrameComplete(uartBufferPos)
//                    uartBufferPos = 0
//                    uartBytesLeft = 0
//                } else {
//
//                    uartBufferPos++
//                    uartBytesLeft--
//                }
//            } else if (byte.toUByte() == 0xDD.toUByte()) {
//                isInFrame = true
//                uartBufferPos++
//            }
            uartBuffer[uartBufferPos] = byte
            uartBufferPos++
            if (uartBufferPos >= bufferSize) {
                break
            }
        }
        onFrameComplete(uartBufferPos)
    }

    private fun onFrameComplete(size: Int) {
        if (size <= 0) {
            return
        }

        val frameBytes = uartBuffer.slice(IntRange(0, size)).toByteArray()

         Log.d("BMS", "FrameData (" + frameBytes.size + "): " + frameBytes.toHexString())

//        if (frameBytes[1] == 0x3.toByte()) {
//            val generalInfo = BmsGeneralInfoResponse(frameBytes)
//            onGeneralInfoCallback(generalInfo)
//        } else if (frameBytes[1] == 0x4.toByte()) {
//            val cellInfo = BmsCellInfoResponse(frameBytes)
//            onCellInfoCallback(cellInfo)
//        }
//
        val generalInfo = BmsGeneralInfoResponse(frameBytes)
        onGeneralInfoCallback(generalInfo)
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}