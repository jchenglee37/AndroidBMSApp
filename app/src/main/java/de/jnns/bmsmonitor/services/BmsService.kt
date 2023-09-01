package de.jnns.bmsmonitor.services

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import de.jnns.bmsmonitor.bluetooth.BleManager
import de.jnns.bmsmonitor.bluetooth.BmsGattClientCallback
import de.jnns.bmsmonitor.bms.BmsCellInfoResponse
import de.jnns.bmsmonitor.bms.BmsGeneralInfoResponse
import de.jnns.bmsmonitor.data.BatteryData
import io.realm.Realm


@ExperimentalUnsignedTypes
class BmsService : Service() {
    // BMS commands, they won't change
//    private val cmdGeneralInfo: ByteArray = ubyteArrayOf(0x10U, 0x00U, 0x00U, 0x40U).toByteArray()
    private val cmdGeneralInfo: ByteArray = ubyteArrayOf(0xA5U, 0x01U, 0x61U, 0x62U).toByteArray()
    private val cmdCellInfo: ByteArray = ubyteArrayOf(0xDDU, 0xA5U, 0x04U, 0x00U, 0xFFU, 0xFCU, 0x77U).toByteArray()
    private val cmdBmsVersion: ByteArray = ubyteArrayOf(0xDDU, 0xA5U, 0x05U, 0x00U, 0xFFU, 0xFBU, 0x77U).toByteArray()

    // bluetooth stuff
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var gattClientCallback: BmsGattClientCallback
    private lateinit var currentBleDevice: BluetoothDevice

    // Handler that is going to poll data from the bms
    // it is going to toggle "dataModeSwitch" and
    // request General or Cell data
    private val dataHandler: Handler = Handler()
//    private var dataModeSwitch = false
    private var dataPollDelay: Long = 0

    // we need both datasets to update the view
    private var cellInfoReceived = false
    private var generalInfoReceived = false

    // bluetooth device mac to use
    private lateinit var bleMac: String
    private lateinit var blePin: String
    private lateinit var bleName: String

    // no need to refresh data in the background
    private var isInForeground = false

    // is connected
    private var isConnected = false
    private var isConnecting = false

    // main dataset
    private var batteryData = BatteryData()

    private lateinit var listener: OnSharedPreferenceChangeListener

    override fun onCreate() {
        super.onCreate()

        bleMac = PreferenceManager.getDefaultSharedPreferences(this).getString("macAddress", "")!!
        blePin = PreferenceManager.getDefaultSharedPreferences(this).getString("blePin", "")!!
        dataPollDelay = PreferenceManager.getDefaultSharedPreferences(this).getString("refreshInterval", "5000")!!.toLong()
        dataPollDelay = 5000

        BleManager.i.onUpdateFunctions.add {
            searchForDeviceAndConnect()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        listener = OnSharedPreferenceChangeListener { _, key ->
            if (key == "macAddress") {
                bleMac = PreferenceManager.getDefaultSharedPreferences(this).getString("macAddress", "")!!

                disconnectFromDevice()
                searchForDeviceAndConnect()
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)

        isInForeground = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        disconnectFromDevice()
    }

    private fun onGeneralInfoAvailable(generalInfo: BmsGeneralInfoResponse) {
        batteryData.capacity = generalInfo.capacity
        batteryData.sysTemperature = generalInfo.sysTemperature
        batteryData.packCurrent = generalInfo.packCurrent
        batteryData.packVoltage = generalInfo.packVoltage
        batteryData.maxDischargeCurrent = generalInfo.maxDischargeCurrent
        batteryData.maxChargeCurrent = generalInfo.maxChargeCurrent
        batteryData.bpVersion = generalInfo.bpVersion
        batteryData.bpNumber = generalInfo.bpNumber
        batteryData.packRSOC = generalInfo.packRSOC
        batteryData.packDischargCycle = generalInfo.packDischargCycle
        batteryData.heatsinkTemperature = generalInfo.heatsinkTemperature
        batteryData.cellVoltage1 = generalInfo.cellVoltage1
        batteryData.cellVoltage2 = generalInfo.cellVoltage2
        batteryData.cellVoltage3 = generalInfo.cellVoltage3
        batteryData.cellVoltage4 = generalInfo.cellVoltage4
        batteryData.cellVoltage5 = generalInfo.cellVoltage5
        batteryData.cellVoltage6 = generalInfo.cellVoltage6
        batteryData.cellVoltage7 = generalInfo.cellVoltage7
        batteryData.cellVoltage8 = generalInfo.cellVoltage8
        batteryData.cellVoltage9 = generalInfo.cellVoltage9
        batteryData.cellVoltage10 = generalInfo.cellVoltage10
        batteryData.cellVoltage11 = generalInfo.cellVoltage11
        batteryData.cellVoltage12 = generalInfo.cellVoltage12
        batteryData.cellVoltage13 = generalInfo.cellVoltage13
        batteryData.cellVoltage14 = generalInfo.cellVoltage14
        batteryData.cellVoltage15 = generalInfo.cellVoltage15
        batteryData.cellVoltage16 = generalInfo.cellVoltage16
        generalInfoReceived = true
        sendData()
    }

    private fun onCellInfoAvailable(cellInfo: BmsCellInfoResponse) {
        batteryData.cellCount = cellInfo.cellCount
        batteryData.cellVoltages = cellInfo.cellVoltages

        cellInfoReceived = true
        sendData()
    }

    private fun sendData() {
        if (generalInfoReceived) {
            cellInfoReceived = false
            generalInfoReceived = false

            batteryData.timestamp = System.currentTimeMillis()
            batteryData.bleAddress = currentBleDevice.address

            if (currentBleDevice.name.isNotEmpty()) {
                batteryData.bleName = currentBleDevice.name
            } else {
                batteryData.bleName = "unknown"
            }

            // save battery history
            val realm = Realm.getDefaultInstance()
            realm.beginTransaction()
            realm.copyToRealm(batteryData)
            realm.commitTransaction()

            val intent = Intent("bmsDataIntent")
            intent.putExtra("deviceName", batteryData.bleName)
            intent.putExtra("batteryData", Gson().toJson(batteryData))
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    private fun onConnectionFailed() {
        isConnected = false
        isConnecting = false

        connectToDevice()
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    private fun onConnectionSucceeded() {
        isConnected = true
        isConnecting = false

        dataHandler.postDelayed(object : Runnable {
            override fun run() {
                if (gattClientCallback.isConnected) {
                    if (isInForeground) {
                        Log.d("BMS", "writeBytes()1:" + cmdGeneralInfo.toHexString())
                        writeBytes(cmdGeneralInfo)
                        dataHandler.postDelayed(this, dataPollDelay)
                    }
                } else {
                    connectToDevice()
                }
            }
        }, dataPollDelay)
    }

    private fun searchForDeviceAndConnect() {
        val bleDevice = BleManager.i.bleDevices.firstOrNull { x -> x.address.equals(bleMac, ignoreCase = true) }

        if (bleDevice != null) {
            currentBleDevice = bleDevice
            connectToDevice()
        }
    }

    private fun connectToDevice() {
        if (!isConnected && !isConnecting) {
            isConnecting = true

            // bluetooth uart callbacks
            gattClientCallback = BmsGattClientCallback(
                ::onGeneralInfoAvailable,   // process general info
                ::onCellInfoAvailable,      // process cell info
                ::onConnectionSucceeded,    // on connection succeeded
                ::onConnectionFailed        // on connection fails
            )

//            currentBleDevice.setPin(blePin.toByteArray())
//            currentBleDevice.createBond()
//            currentBleDevice.setPairingConfirmation(false);

            bluetoothGatt = currentBleDevice.connectGatt(this, false, gattClientCallback)
        }
    }

    private fun disconnectFromDevice() {
        if (isConnected) {
            bluetoothGatt.close()

            isConnected = false
        }
    }

    private fun writeBytes(bytes: ByteArray) {
        gattClientCallback.setReceivingLen(128);
        gattClientCallback.writeCharacteristic.value = bytes
        bluetoothGatt.writeCharacteristic(gattClientCallback.writeCharacteristic)
    }
}