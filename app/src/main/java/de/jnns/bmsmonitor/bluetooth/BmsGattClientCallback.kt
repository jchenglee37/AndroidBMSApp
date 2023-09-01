package de.jnns.bmsmonitor.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.os.Build
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

//    lateinit var readCharacteristic: BluetoothGattCharacteristic
    lateinit var writeCharacteristic: BluetoothGattCharacteristic

//    private val uartUuid = UUID.fromString("0000A002-0000-1000-8000-00805f9b34fb")
//    private val rxUuid = UUID.fromString("0000C305-0000-1000-8000-00805f9b34fb")
//    private val txUuid = UUID.fromString("0000C302-0000-1000-8000-00805f9b34fb")
    private val uartUuid = UUID.fromString("01FF0100-BA5E-F4EE-5CA1-EB1E5E4B1CE0")
    private val txUuid = UUID.fromString("01FF0101-BA5E-F4EE-5CA1-EB1E5E4B1CE0")

    private var isInTrans: Boolean = false
    private var receLen: Int = 0
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gatt.requestMtu(247);
            }
//            gatt.discoverServices()
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

        val uartService = gatt.getService(uartUuid)

        if (uartService != null) {
            Log.d("BMS", "uartService:" + uartService)
//            readCharacteristic = uartService.getCharacteristic(rxUuid)
            writeCharacteristic = uartService.getCharacteristic(txUuid)
            Log.d("BMS", "writeCharacteristic:" + writeCharacteristic)
            gatt.setCharacteristicNotification(writeCharacteristic, true)
//            gatt.setCharacteristicNotification(readCharacteristic, true)

            onConnectionSucceeded()
            isConnected = true
        }
    }

    fun setReceivingLen(receiveLen: Int) {
        receLen = receiveLen
        isInTrans = true
        uartBufferPos = 0
    }

    fun isInTransaction(): Boolean {
        return isInTrans
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)

        Log.d("BMS", "BLE Data (" + characteristic.value.size + "): " + characteristic.value.toHexString())

        for (byte: Byte in characteristic.value) {
            uartBuffer[uartBufferPos] = byte
            uartBufferPos++
            if (uartBufferPos >= bufferSize || uartBufferPos >= receLen) {
                onFrameComplete(uartBufferPos)
                Log.d("BMS", "Transaction Done.")
                isInTrans = false
                uartBufferPos = 0
                break
            }
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        var mtu_size: Int = mtu
        // Handle MTU change request from the central device
//        if (mtu_size > 247) {
//            mtu_size = 247
//        }
//        // Respond to the MTU change request
//        bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        Log.d("BMS", "Negotiated mtu_size=" + mtu_size)
        gatt.discoverServices()
    }

    private fun onFrameComplete(size: Int) {
        if (size <= 0) {
            return
        }

        val frameBytes = uartBuffer.slice(IntRange(0, size - 1)).toByteArray()
        val generalInfo = BmsGeneralInfoResponse(frameBytes)
        onGeneralInfoCallback(generalInfo)
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}