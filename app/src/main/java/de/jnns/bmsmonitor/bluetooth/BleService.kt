package de.jnns.bmsmonitor.bluetooth

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import de.jnns.bmsmonitor.R
import java.util.*

@ExperimentalUnsignedTypes
class BleService : Service() {
    private var TAG: String = "BMS"
    // bluetooth stuff
    private var isScanning = false
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var mBluetoothManager: BluetoothManager
    private lateinit var mGattServer: BluetoothGattServer

    val UART_SERVICE: UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
    val UART_CHAR: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")

    val service = BluetoothGattService(UART_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)

    val currentTime = BluetoothGattCharacteristic(UART_CHAR,
        //Read-only characteristic, supports notifications
        BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_WRITE)
    override fun onCreate() {
        super.onCreate()

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//        Log.d("BMS", "writeBytes():" + cmdGeneralInfo.toHexString())
        Log.d("BMS", "mBluetoothManager:" + mBluetoothManager)
        if (mBluetoothManager != null) {
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        }
        val bluetoothAdapter = mBluetoothManager.adapter
        // We can't continue without proper Bluetooth support
        checkBluetoothSupport(bluetoothAdapter)

        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
        startBleScanning()
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System [BluetoothAdapter].
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private fun checkBluetoothSupport(bluetoothAdapter: BluetoothAdapter?): Boolean {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            return false
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported")
            return false
        }

        return true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startBleScanning() {
        if (!isScanning) {
            isScanning = true
            bluetoothLeScanner.startScan(leScanCallback)
        }
    }

    private fun stopBleScanning(delay: Long = 0) {
        if (isScanning) {
            Handler().postDelayed({ bluetoothLeScanner.stopScan(leScanCallback) }, delay)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (result.device != null) {
                if(
                    result.device.name != null &&
                    result.device.name.startsWith("UOOK-BMS") == true
                ) {
                    BleManager.i.addDevice(result.device)
                }
            }
        }
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private val mGattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
//            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                Log.i(TAG, "BluetoothDevice CONNECTED: $device")
//            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
//                //Remove device from any active subscriptions
//                registeredDevices.remove(device)
//            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic
        ) {
//            val now = System.currentTimeMillis()
//            when {
//                TimeProfile.CURRENT_TIME == characteristic.uuid -> {
//                    Log.i(TAG, "Read CurrentTime")
//                    bluetoothGattServer?.sendResponse(device,
//                        requestId,
//                        BluetoothGatt.GATT_SUCCESS,
//                        0,
//                        TimeProfile.getExactTime(now, TimeProfile.ADJUST_NONE))
//                }
//                TimeProfile.LOCAL_TIME_INFO == characteristic.uuid -> {
//                    Log.i(TAG, "Read LocalTimeInfo")
//                    bluetoothGattServer?.sendResponse(device,
//                        requestId,
//                        BluetoothGatt.GATT_SUCCESS,
//                        0,
//                        TimeProfile.getLocalTimeInfo(now))
//                }
//                else -> {
//                    // Invalid characteristic
//                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
//                    bluetoothGattServer?.sendResponse(device,
//                        requestId,
//                        BluetoothGatt.GATT_FAILURE,
//                        0,
//                        null)
//                }
//            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                             descriptor: BluetoothGattDescriptor
        ) {
//            if (TimeProfile.CLIENT_CONFIG == descriptor.uuid) {
//                Log.d(TAG, "Config descriptor read")
//                val returnValue = if (registeredDevices.contains(device)) {
//                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                } else {
//                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
//                }
//                bluetoothGattServer?.sendResponse(device,
//                    requestId,
//                    BluetoothGatt.GATT_SUCCESS,
//                    0,
//                    returnValue)
//            } else {
//                Log.w(TAG, "Unknown descriptor read request")
//                bluetoothGattServer?.sendResponse(device,
//                    requestId,
//                    BluetoothGatt.GATT_FAILURE,
//                    0, null)
//            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                              descriptor: BluetoothGattDescriptor,
                                              preparedWrite: Boolean, responseNeeded: Boolean,
                                              offset: Int, value: ByteArray) {
//            if (TimeProfile.CLIENT_CONFIG == descriptor.uuid) {
//                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
//                    Log.d(TAG, "Subscribe device to notifications: $device")
//                    registeredDevices.add(device)
//                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
//                    Log.d(TAG, "Unsubscribe device from notifications: $device")
//                    registeredDevices.remove(device)
//                }
//
//                if (responseNeeded) {
//                    bluetoothGattServer?.sendResponse(device,
//                        requestId,
//                        BluetoothGatt.GATT_SUCCESS,
//                        0, null)
//                }
//            } else {
//                Log.w(TAG, "Unknown descriptor write request")
//                if (responseNeeded) {
//                    bluetoothGattServer?.sendResponse(device,
//                        requestId,
//                        BluetoothGatt.GATT_FAILURE,
//                        0, null)
//                }
//            }
        }
    }
}