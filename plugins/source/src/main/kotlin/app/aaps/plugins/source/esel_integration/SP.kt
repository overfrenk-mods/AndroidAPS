package app.aaps.plugins.source.esel_integration

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object SP {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var context: Context

    fun init(context: Context) {
        SP.context = context
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    fun getString(resourceID: Int, defaultValue: String): String {
        return sharedPreferences.getString(context.getString(resourceID), defaultValue) ?: defaultValue
    }

    fun getString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun getBoolean(resourceID: Int, defaultValue: Boolean): Boolean {
        return try {
            sharedPreferences.getBoolean(context.getString(resourceID), defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            sharedPreferences.getBoolean(key, defaultValue)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun getDouble(resourceID: Int, defaultValue: Double): Double {
        return SafeParse.stringToDouble(
            sharedPreferences.getString(
                context.getString(resourceID),
                defaultValue.toString()
            )
        )
    }

    fun getDouble(key: String, defaultValue: Double): Double {
        return SafeParse.stringToDouble(sharedPreferences.getString(key, defaultValue.toString()))
    }

    fun getDouble(key: String, defaultValue: Double, min: Double, max: Double): Double {
        var value = SafeParse.stringToDouble(sharedPreferences.getString(key, defaultValue.toString()))
        if (value < min) {
            value = min
        }
        if (value > max) {
            value = max
        }
        return value
    }

    fun getInt(resourceID: Int, defaultValue: Int): Int {
        return try {
            sharedPreferences.getInt(context.getString(resourceID), defaultValue)
        } catch (e: Exception) {
            SafeParse.stringToInt(
                sharedPreferences.getString(
                    context.getString(resourceID),
                    defaultValue.toString()
                )
            )
        }
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return try {
            sharedPreferences.getInt(key, defaultValue)
        } catch (e: Exception) {
            SafeParse.stringToInt(sharedPreferences.getString(key, defaultValue.toString()))
        }
    }

    fun getLong(resourceID: Int, defaultValue: Long): Long {
        return SafeParse.stringToLong(
            sharedPreferences.getString(
                context.getString(resourceID),
                defaultValue.toString()
            )
        )
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return try {
            sharedPreferences.getLong(key, defaultValue)
        } catch (e: Exception) {
            SafeParse.stringToLong(sharedPreferences.getString(key, defaultValue.toString()))
        }
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            sharedPreferences.getFloat(key, defaultValue)
        } catch (e: Exception) {
            SafeParse.stringToInt(sharedPreferences.getString(key, defaultValue.toString())).toFloat()
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun putBoolean(resourceID: Int, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(context.getString(resourceID), value)
        editor.apply()
    }

    fun removeBoolean(resourceID: Int) {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(resourceID))
        editor.apply()
    }

    fun putLong(key: String, value: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun putLong(resourceID: Int, value: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(context.getString(resourceID), value)
        editor.apply()
    }

    fun putInt(key: String, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun putInt(resourceID: Int, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(context.getString(resourceID), value)
        editor.apply()
    }

    fun putFloat(key: String, value: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat(key, value)
        editor.apply()
    }

    fun putString(resourceID: Int, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(context.getString(resourceID), value)
        editor.apply()
    }

    fun putString(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun removeString(resourceID: Int) {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(resourceID))
        editor.apply()
    }
}