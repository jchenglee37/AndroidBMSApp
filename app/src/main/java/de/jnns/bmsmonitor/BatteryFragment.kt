package de.jnns.bmsmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.github.anastr.speedviewlib.components.Section
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.google.gson.Gson
import de.jnns.bmsmonitor.data.BatteryData
import de.jnns.bmsmonitor.databinding.FragmentBatteryBinding
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt


@ExperimentalUnsignedTypes
class BatteryFragment : Fragment() {
    private var _binding: FragmentBatteryBinding? = null
    private val binding get() = _binding!!

    // no need to refresh data in the background
    private var isInForeground = false

    // time calculation smoothing
    // default 5 min
    private var smoothCount = 60 * 5
    private var smoothIndex = 0
    private var wattValues = FloatArray(smoothCount)

    private var minCellVoltage = 0
    private var maxCellVoltage = 0

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val msg: String = intent.getStringExtra("batteryData")!!
                if (msg.isNotEmpty()) {
                    updateUi(Gson().fromJson(msg, BatteryData::class.java))
                }
            } catch (ex: Exception) {
                // ignored
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mMessageReceiver, IntentFilter("bmsDataIntent"))
        _binding = FragmentBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mMessageReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.speedViewSpeed.clearSections()
        binding.speedViewSpeed.addSections(
            Section(0.00000000f, 0.11111111f, ContextCompat.getColor(requireContext(), R.color.batteryChargeHigh), 72.0f),
            Section(0.11111111f, 0.22222222f, ContextCompat.getColor(requireContext(), R.color.batteryChargeMedium), 72.0f),
            Section(0.22222222f, 0.33333333f, ContextCompat.getColor(requireContext(), R.color.batteryChargeLow), 72.0f),
            Section(0.33333333f, 0.44444444f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeLow), 72.0f),
            Section(0.44444444f, 0.66666666f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeMedium), 72.0f),
            Section(0.66666666f, 0.88888888f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeHigh), 72.0f),
            Section(0.88888888f, 1.00000000f, ContextCompat.getColor(requireContext(), R.color.batteryDischargeHighest), 72.0f)
        )

        binding.speedViewSpeed.minSpeed = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("minPower", "-500")!!.toFloat()
        binding.speedViewSpeed.maxSpeed = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("maxPower", "1000")!!.toFloat()

        minCellVoltage = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("minCellVoltage", "2500")!!.toInt()
        maxCellVoltage = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("maxCellVoltage", "4200")!!.toInt()

        binding.barchartCells1.setNoDataTextColor(requireActivity().getColor(R.color.white))
        binding.barchartCells1.setNoDataText("...")

        binding.barchartCells1.setPinchZoom(false)
        binding.barchartCells1.setTouchEnabled(false)
        binding.barchartCells1.isClickable = false
        binding.barchartCells1.isDoubleTapToZoomEnabled = false

        binding.barchartCells1.setDrawBorders(false)
        binding.barchartCells1.setDrawValueAboveBar(true)
        binding.barchartCells1.setDrawBorders(false)
        binding.barchartCells1.setDrawGridBackground(false)

        binding.barchartCells1.description.isEnabled = false
        binding.barchartCells1.legend.isEnabled = false

        binding.barchartCells1.axisLeft.setDrawGridLines(false)
        binding.barchartCells1.axisLeft.setDrawLabels(false)
        binding.barchartCells1.axisLeft.setDrawAxisLine(false)

        binding.barchartCells1.xAxis.setDrawGridLines(false)
        binding.barchartCells1.xAxis.setDrawLabels(false)
        binding.barchartCells1.xAxis.setDrawAxisLine(false)

        binding.barchartCells1.axisRight.isEnabled = false


        binding.barchartCells2.setNoDataTextColor(requireActivity().getColor(R.color.white))
        binding.barchartCells2.setNoDataText("...")

        binding.barchartCells2.setPinchZoom(false)
        binding.barchartCells2.setTouchEnabled(false)
        binding.barchartCells2.isClickable = false
        binding.barchartCells2.isDoubleTapToZoomEnabled = false

        binding.barchartCells2.setDrawBorders(false)
        binding.barchartCells2.setDrawValueAboveBar(true)
        binding.barchartCells2.setDrawBorders(false)
        binding.barchartCells2.setDrawGridBackground(false)

        binding.barchartCells2.description.isEnabled = false
        binding.barchartCells2.legend.isEnabled = false

        binding.barchartCells2.axisLeft.setDrawGridLines(false)
        binding.barchartCells2.axisLeft.setDrawLabels(false)
        binding.barchartCells2.axisLeft.setDrawAxisLine(false)

        binding.barchartCells2.xAxis.setDrawGridLines(false)
        binding.barchartCells2.xAxis.setDrawLabels(false)
        binding.barchartCells2.xAxis.setDrawAxisLine(false)

        binding.barchartCells2.axisRight.isEnabled = false
    }

    override fun onResume() {
        super.onResume()

        isInForeground = true

        binding.speedViewSpeed.speedTo(0.0f, 0)
        binding.labelStatus.text = getString(R.string.waitForBms)
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    private fun updateUi(batteryData: BatteryData) {
        if (!isInForeground) {
            return
        }

        requireActivity().runOnUiThread {
            binding.labelStatus.text = String.format(resources.getString(R.string.connectedToBms), batteryData.bpVersion, batteryData.bpNumber)

            // Power Gauge
            val powerUsage = batteryData.packCurrent * batteryData.packVoltage

            var chargePower:Float = 0.0f

            if(batteryData.packCurrent >= 0.0f)
            {
                chargePower = batteryData.packCurrent / batteryData.maxChargeCurrent
                chargePower = chargePower * -500
            } else {
                chargePower = batteryData.packCurrent / batteryData.maxDischargeCurrent
                chargePower = chargePower * -1000
            }

            binding.speedViewSpeed.speedTo(chargePower, 1000)

            if (powerUsage < 0.0f) {
                binding.labelPower.text = "+${powerUsage.roundToInt() * -1}"
            } else {
                binding.labelPower.text = powerUsage.roundToInt().toString()
            }

            binding.labelCycle.text = String.format("Cycle:%s", batteryData.packDischargCycle.toString())

            // Cell Bar-Diagram
            val cellBars = ArrayList<BarEntry>()
            cellBars.add(BarEntry(0.toFloat(), batteryData.cellVoltage1))
            cellBars.add(BarEntry(1.toFloat(), batteryData.cellVoltage2))
            cellBars.add(BarEntry(2.toFloat(), batteryData.cellVoltage3))
            cellBars.add(BarEntry(3.toFloat(), batteryData.cellVoltage4))
            cellBars.add(BarEntry(4.toFloat(), batteryData.cellVoltage5))
            cellBars.add(BarEntry(5.toFloat(), batteryData.cellVoltage6))
            cellBars.add(BarEntry(6.toFloat(), batteryData.cellVoltage7))
            cellBars.add(BarEntry(7.toFloat(), batteryData.cellVoltage8))

            val barDataSetVoltage = BarDataSet(cellBars, "Cell Voltages")
            barDataSetVoltage.valueTextColor = requireActivity().getColor(R.color.white)
            barDataSetVoltage.valueTextSize = 12.0f
            barDataSetVoltage.valueFormatter = DefaultValueFormatter(3)
            barDataSetVoltage.setColors(requireActivity().getColor(R.color.primary))

            val barData = BarData(barDataSetVoltage)
            binding.barchartCells1.data = barData
            binding.barchartCells1.invalidate()

            val cellBars1 = ArrayList<BarEntry>()

            cellBars1.add(BarEntry(8.toFloat(), batteryData.cellVoltage9))
            cellBars1.add(BarEntry(9.toFloat(), batteryData.cellVoltage10))
            cellBars1.add(BarEntry(10.toFloat(), batteryData.cellVoltage11))
            cellBars1.add(BarEntry(11.toFloat(), batteryData.cellVoltage12))
            cellBars1.add(BarEntry(12.toFloat(), batteryData.cellVoltage13))
            cellBars1.add(BarEntry(13.toFloat(), batteryData.cellVoltage14))
            cellBars1.add(BarEntry(14.toFloat(), batteryData.cellVoltage15))
            cellBars1.add(BarEntry(15.toFloat(), batteryData.cellVoltage16))

            val barDataSetVoltage1 = BarDataSet(cellBars, "Cell Voltages")
            barDataSetVoltage1.valueTextColor = requireActivity().getColor(R.color.white)
            barDataSetVoltage1.valueTextSize = 12.0f
            barDataSetVoltage1.valueFormatter = DefaultValueFormatter(3)
            barDataSetVoltage1.setColors(requireActivity().getColor(R.color.primary))

            val barData1 = BarData(barDataSetVoltage)
            binding.barchartCells2.data = barData1
            binding.barchartCells2.invalidate()

            var totalPercentage = batteryData.packRSOC.toUByte()
            if(totalPercentage > 100U) {
                totalPercentage = 100U
            }

            binding.labelVoltage.text = roundTo(batteryData.packVoltage, 1).toString()
            binding.labelPercentage.text = totalPercentage.toString()

            uiBatteryCapacityBar(totalPercentage.toFloat())

            binding.labelCurrent.text = roundTo(batteryData.packCurrent, 1).toString()
            binding.labelCapacityWh.text = roundTo(batteryData.capacity, 1).toString()

            binding.labelTemperature.text = roundTo(batteryData.sysTemperature, 1).toString()
            binding.labelTemperatureMax.text = roundTo(batteryData.heatsinkTemperature, 1).toString()
        }
    }

    private fun uiColorCapacityProgressBar(value: Float, uiElement: ProgressBar) {
        // 80% skipped here because battery only gets charged to ~80 percent
        when {
            value < 20 -> {
                uiElement.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder20))
            }
            value < 40 -> {
                uiElement.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder40))
            }
            value < 70 -> {
                uiElement.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder70))
            }
            else -> {
                uiElement.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
            }
        }
    }

    private fun uiBatteryCapacityBar(value: Float) {
        when {
            value == 0.0f -> {
                binding.progressBarBattery1.progress = 0
                binding.progressBarBattery2.progress = 0
                binding.progressBarBattery3.progress = 0
                binding.progressBarBattery4.progress = 0
                binding.progressBarBattery5.progress = 0
            }
            value < 20.0f -> {
                val newValue = value * 5.0f
                uiColorCapacityProgressBar(newValue, binding.progressBarBattery1)
                binding.progressBarBattery1.progress = newValue.roundToInt()
                binding.progressBarBattery2.progress = 0
                binding.progressBarBattery3.progress = 0
                binding.progressBarBattery4.progress = 0
                binding.progressBarBattery5.progress = 0
            }
            value < 40.0f -> {
                binding.progressBarBattery1.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery1.progress = 100
                val newValue = (value - 20.0f) * 5.0f
                uiColorCapacityProgressBar(newValue, binding.progressBarBattery2)
                binding.progressBarBattery2.progress = newValue.roundToInt()
                binding.progressBarBattery3.progress = 0
                binding.progressBarBattery4.progress = 0
                binding.progressBarBattery5.progress = 0
            }
            value < 60.0f -> {
                binding.progressBarBattery1.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery1.progress = 100
                binding.progressBarBattery2.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery2.progress = 100
                val newValue = (value - 40.0f) * 5.0f
                uiColorCapacityProgressBar(newValue, binding.progressBarBattery3)
                binding.progressBarBattery3.progress = newValue.roundToInt()
                binding.progressBarBattery4.progress = 0
                binding.progressBarBattery5.progress = 0
            }
            value < 80.0f -> {
                binding.progressBarBattery1.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery1.progress = 100
                binding.progressBarBattery2.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery2.progress = 100
                binding.progressBarBattery3.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery3.progress = 100
                val newValue = (value - 60.0f) * 5.0f
                uiColorCapacityProgressBar(newValue, binding.progressBarBattery4)
                binding.progressBarBattery4.progress = newValue.roundToInt()
                binding.progressBarBattery5.progress = 0
            }
            value < 100.0f -> {
                binding.progressBarBattery1.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery1.progress = 100
                binding.progressBarBattery2.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery2.progress = 100
                binding.progressBarBattery3.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery3.progress = 100
                binding.progressBarBattery4.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery4.progress = 100
                val newValue = (value - 80.0f) * 5.0f
                uiColorCapacityProgressBar(newValue, binding.progressBarBattery5)
                binding.progressBarBattery5.progress = newValue.roundToInt()
            }
            else -> {
                binding.progressBarBattery1.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery1.progress = 100
                binding.progressBarBattery2.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery2.progress = 100
                binding.progressBarBattery3.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery3.progress = 100
                binding.progressBarBattery4.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery4.progress = 100
                binding.progressBarBattery5.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.percentUnder100))
                binding.progressBarBattery5.progress = 100
            }
        }
    }

    private fun roundTo(value: Float, decimals: Int): Float {
        if (value.isNaN() || value.isInfinite()) {
            return 0.0f
        }

        if (decimals == 0) {
            return value.roundToInt().toFloat()
        }

        val factor = 10.0.pow(decimals.toDouble()).toFloat()
        return (value * factor).roundToInt() / factor
    }
}