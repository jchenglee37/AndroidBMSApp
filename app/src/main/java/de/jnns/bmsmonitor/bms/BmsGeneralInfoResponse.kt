package de.jnns.bmsmonitor.bms

import io.realm.RealmList
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BmsGeneralInfoResponse(bytes: ByteArray) {
//    var command: Int = 0
//    var status: Int = 0
//    var dataLength: Int = 0
//    var totalVoltage: Float = 0.0f
//    var totalCurrent: Float = 0.0f
//    var residualCapacity: Float = 0.0f
//    var nominalCapacity: Float = 0.0f
//    var temperatureProbeCount: Int = 0
//    var temperatureProbeValues: RealmList<Float>

    var cycles: Short = 0
    var capacity: Float = 0.0f
    var temperature: Float = 0.0f
    var dischargeCurrent: Float = 0.0f
    var packVoltage: Float = 0.0f
    var chargeCurrent: Float = 0.0f
    var maxChargeCurrent: Float = 0.0f

    init {
//        command = bytes[1].toInt()
//        status = bytes[2].toInt()
//        dataLength = bytes[3].toInt()
//
//        totalVoltage = bytesToShort(bytes[4], bytes[5]) / 100.0f
//
//        totalCurrent = bytesToShort(bytes[6], bytes[7]) / 100.0f
//
//        residualCapacity = bytesToShort(bytes[8], bytes[9]) / 100.0f
//        nominalCapacity = bytesToShort(bytes[10], bytes[11]) / 100.0f
//
//        cycles = bytesToShort(bytes[12], bytes[13])
//
//        // 14 & 15 = production date
//        // 16 & 17 = balance low
//        // 18 & 19 = balance high
//        // 20 & 21 = protection status
//        // 22 = version
//
//        // Pack Capacity
//        capacity = bytes[23].toInt() / 100.0f
//
//        // 24 = MOS status
//        // 25 = number of cells
//
//        temperatureProbeCount = bytes[26].toInt()
//        temperatureProbeValues = RealmList<Float>()
//
//        for (i in 0 until temperatureProbeCount) {
//            temperatureProbeValues.add((bytesToShort(bytes[27 + (i * 2)], bytes[27 + (i * 2) + 1]) - 2731) / 10.0f)
//        }

        packVoltage = bytesToShort(bytes[8], bytes[9]) / 100.0f
        dischargeCurrent = bytesToShort(bytes[10], bytes[11]) / 10.0f
        cycles = bytesToShort(bytes[6], bytes[7])
        capacity = bytesToShort(bytes[4], bytes[5]) / 10.0f
        chargeCurrent = bytesToShort(bytes[12], bytes[13]) / 10.0f
        maxChargeCurrent = bytesToShort(bytes[14], bytes[15]) / 10.0f


        // System temperature
        temperature = bytesToShort(bytes[24], bytes[25]) / 10.0f
    }

    private fun bytesToShort(h: Byte, l: Byte, order: ByteOrder = ByteOrder.BIG_ENDIAN): Short {
        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)

        byteBuffer.order(order)
        byteBuffer.put(h)
        byteBuffer.put(l)
        byteBuffer.flip()

        return byteBuffer.short
    }
}