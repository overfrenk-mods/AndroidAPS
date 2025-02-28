package app.aaps.plugins.source.esel_integration

object SafeParse {

    fun stringToInt(value: String?, defaultValue: Int = 0): Int {
        return try {
            value?.toInt() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun stringToLong(value: String?, defaultValue: Long = 0L): Long {
        return try {
            value?.toLong() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun stringToDouble(value: String?, defaultValue: Double = 0.0): Double {
        return try {
            value?.toDouble() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun stringToFloat(value: String?, defaultValue: Float = 0.0f): Float {
        return try {
            value?.toFloat() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }
}