package com.personal.financetracker.ui.common

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.personal.financetracker.R
import com.personal.financetracker.util.Formatters

object ChartStyle {

    val compactCurrency = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String =
            if (value == 0f) "0" else Formatters.formatCompact(value.toDouble())
    }

    fun <T : BarLineChartBase<*>> base(chart: T): T {
        val ctx = chart.context
        chart.apply {
            setDrawGridBackground(false)
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            setBackgroundColor(0)
            setNoDataText("")
            setExtraOffsets(0f, 8f, 0f, 4f)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = ctx.color(R.color.text_secondary)
                textSize = 10f
                granularity = 1f
                yOffset = 6f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ctx.color(R.color.grid)
                gridLineWidth = 1f
                setDrawAxisLine(false)
                textColor = ctx.color(R.color.text_muted)
                textSize = 9f
                axisMinimum = 0f
                setLabelCount(4, false)
                valueFormatter = compactCurrency
                xOffset = 8f
            }
            axisRight.isEnabled = false
        }
        return chart
    }

    fun bar(chart: BarChart): BarChart = base(chart).apply {
        setDrawValueAboveBar(true)
        setFitBars(true)
        isHighlightFullBarEnabled = false
    }

    fun line(chart: LineChart): LineChart = base(chart).apply {
        isHighlightPerDragEnabled = true
        isHighlightPerTapEnabled = true
    }
}
