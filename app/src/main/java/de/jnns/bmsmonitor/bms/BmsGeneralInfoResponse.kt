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

    var capacity: Float = 0.0f
    var sysTemperature: Float = 0.0f
    var packCurrent: Float = 0.0f
    var packVoltage: Float = 0.0f
    var maxDischargeCurrent: Float = 0.0f
    var maxChargeCurrent: Float = 0.0f
    var bpVersion: Byte = 0
    var bpNumber: Byte = 0
    var packRSOC: Byte = 0
    var packDischargCycle: Short = 0
    var heatsinkTemperature: Float = 0.0f
    var cellVoltage1: Float = 0.0f
    var cellVoltage2: Float = 0.0f
    var cellVoltage3: Float = 0.0f
    var cellVoltage4: Float = 0.0f
    var cellVoltage5: Float = 0.0f
    var cellVoltage6: Float = 0.0f
    var cellVoltage7: Float = 0.0f
    var cellVoltage8: Float = 0.0f
    var cellVoltage9: Float = 0.0f
    var cellVoltage10: Float = 0.0f
    var cellVoltage11: Float = 0.0f
    var cellVoltage12: Float = 0.0f
    var cellVoltage13: Float = 0.0f
    var cellVoltage14: Float = 0.0f
    var cellVoltage15: Float = 0.0f
    var cellVoltage16: Float = 0.0f


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

        bpVersion = bytes[0]
        bpNumber = bytes[1]
        packRSOC = bytes[2]
        capacity = bytesToShort(bytes[4], bytes[5]) / 10.0f
        packVoltage = bytesToShort(bytes[8], bytes[9]) / 100.0f
        packCurrent = bytesToShort(bytes[10], bytes[11]) / 100.0f
        maxDischargeCurrent = bytesToShort(bytes[12], bytes[13]) / 100.0f
        maxChargeCurrent = bytesToShort(bytes[14], bytes[15]) / 100.0f

        packDischargCycle = bytesToShort(bytes[74], bytes[75])

        // System temperature
        sysTemperature = bytesToShort(bytes[52], bytes[53]) / 100.0f
        heatsinkTemperature = bytesToShort(bytes[54], bytes[55]) / 100.0f

        cellVoltage1 = bytesToShort(bytes[96], bytes[97]) / 1000.0f
        cellVoltage2 = bytesToShort(bytes[98], bytes[99]) / 1000.0f
        cellVoltage3 = bytesToShort(bytes[100], bytes[101]) / 1000.0f
        cellVoltage4 = bytesToShort(bytes[102], bytes[103]) / 1000.0f
        cellVoltage5 = bytesToShort(bytes[104], bytes[105]) / 1000.0f
        cellVoltage6 = bytesToShort(bytes[106], bytes[107]) / 1000.0f
        cellVoltage7 = bytesToShort(bytes[108], bytes[109]) / 1000.0f
        cellVoltage8 = bytesToShort(bytes[110], bytes[111]) / 1000.0f
        cellVoltage9 = bytesToShort(bytes[112], bytes[113]) / 1000.0f
        cellVoltage10 = bytesToShort(bytes[114], bytes[115]) / 1000.0f
        cellVoltage11 = bytesToShort(bytes[116], bytes[117]) / 1000.0f
        cellVoltage12 = bytesToShort(bytes[118], bytes[119]) / 1000.0f
        cellVoltage13 = bytesToShort(bytes[120], bytes[121]) / 1000.0f
        cellVoltage14 = bytesToShort(bytes[122], bytes[123]) / 1000.0f
        cellVoltage15 = bytesToShort(bytes[124], bytes[125]) / 1000.0f
        cellVoltage16 = bytesToShort(bytes[126], bytes[127]) / 1000.0f
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