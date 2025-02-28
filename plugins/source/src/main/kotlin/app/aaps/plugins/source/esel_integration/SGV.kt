package app.aaps.plugins.source.esel_integration

import android.util.Log
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "SGV"

class SGV(var value: Int, var timestamp: Long, var record: Int) {
    var raw: Int = value
    var direction: String = "NONE"

    init {
        if (value < 0) {
            value = 38
        } else if (value < 40) {
            value = 39
        } else if (value > 1000) {
            value = 38
        } else if (value > 400) {
            value = 400
        }
    }

    companion object {
        fun convert(mmoll: Float): Int {
            val mgdl = mmoll * 18.0182f
            return mgdl.roundToInt()
        }
    }

    override fun toString(): String {
        val df: DateFormat = SimpleDateFormat.getDateTimeInstance()
        return df.format(Date(timestamp)) + ": " + value
    }

    fun setDirection(slopeByMinute: Double) {
        direction = when {
            slopeByMinute <= (-3.5) -> "DoubleDown"
            slopeByMinute <= (-2.0) -> "SingleDown"
            slopeByMinute <= (-1.0) -> "FortyFiveDown"
            slopeByMinute <= (1.0) -> "Flat"
            slopeByMinute <= (2.0) -> "FortyFiveUp"
            slopeByMinute <= (3.5) -> "SingleUp"
            slopeByMinute <= (40.0) -> "DoubleUp"
            else -> "NONE"
        }
    }

    fun smooth(last: Int, enableSmooth: Boolean) {
        var value = value.toDouble()
        var lastSmooth = last.toDouble()

        if (!enableSmooth) {
            SP.putInt("lastReadingRaw", this.value)
            SP.putFloat("readingSmooth", this.value.toFloat())
            return
        }

        try {
            lastSmooth = SP.getFloat("readingSmooth", lastSmooth.toFloat()).toDouble()
        } catch (e: Exception) {
            // first time: no value available, fallbacksolution is default value
        }

        val factor = SP.getDouble("smooth_factor", 0.3, 0.0, 1.0)
        val correction = SP.getDouble("correction_factor", 0.5, 0.0, 1.0)
        val descentFactor = SP.getDouble("descent_factor", 0.0, 0.0, 1.0)
        var lastRaw = SP.getInt("lastReadingRaw", this.value).toFloat()

        SP.putInt("lastReadingRaw", this.value)

        if (last < 39) { // no useful value, e.g. due to pause in transmitter usage
            lastRaw = this.value.toFloat()
            lastSmooth = this.value.toDouble()
        }

        // exponential smoothing, see https://en.wikipedia.org/wiki/Exponential_smoothing
        // y'[t]=y'[t-1] + (a*(y-y'[t-1])) = a*y+(1-a)*y'[t-1]
        // factor is a, value is y, lastSmooth y'[t-1], smooth y'
        // factor between 0 and 1, default 0.3
        // factor = 0: always last smooth (constant)
        // factor = 1: no smoothing
        var smooth = lastSmooth + (factor * (value - lastSmooth))

        // correction: average of delta between raw and smooth value, added to smooth with correction factor
        // correction between 0 and 1, default 0.5
        // correction = 0: no correction, full smoothing
        // correction > 0: less smoothing
        smooth += correction * ((lastRaw - lastSmooth + (value - smooth)) / 2.0)

        smooth -= descentFactor * (smooth - min(value, smooth))

        SP.putFloat("readingSmooth", smooth.toFloat())

        if (this.value > SP.getInt("lower_limit", 65)) {
            this.value = smooth.roundToInt()
        }

        Log.d(TAG, "readDataFromContentProvider called, result = ${this.value}")
    }
}