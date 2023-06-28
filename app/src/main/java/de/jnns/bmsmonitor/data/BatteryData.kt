package de.jnns.bmsmonitor.data

import io.realm.RealmList
import io.realm.RealmObject

open class BatteryData(
    var timestamp: Long = 0L,
    var bleName: String = "",
    var bleAddress: String = "",
    var current: Float = 0.0f,
    var totalCapacity: Float = 0.0f,
    var currentCapacity: Float = 0.0f,
    var temperatureCount: Int = 0,
    var cellCount: Int = 0,
    var temperatures: RealmList<Float> = RealmList<Float>(),
    var cellVoltages: RealmList<Float> = RealmList<Float>(),

//    var cycles: Short = 0,
//    var capacity: Float = 0.0f,
//    var temperature: Float = 0.0f,
//    var dischargeCurrent: Float = 0.0f,
//    var packVoltage: Float = 0.0f,
//    var chargeCurrent: Float = 0.0f,
//    var maxChargeCurrent: Float = 0.0f


) : RealmObject() {
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

    val voltage: Float
        get() {
            return cellVoltages.sum()
        }

    val avgTemperature: Float
        get() {
            return temperatures.average().toFloat()
        }

    val maxTemperature: Float
        get() {
            return temperatures.maxOrNull()!!
        }

    val percentage: Float
        get() {
            return currentCapacity / totalCapacity
        }

    val watthours: Float
        get() {
            return currentCapacity * voltage
        }

    val totalWatthours: Float
        get() {
            return totalCapacity * voltage
        }

    val power: Float
        get() {
            return current * voltage
        }

}
