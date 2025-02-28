package app.aaps.plugins.source.esel_integration

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import org.json.JSONArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.ArrayList


class ReadReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReadReceiver"
        private const val REPEAT_TIME = 20 * 1000L // 20 seconds
    }

    private var suppressBroadcast = false
    private val output = JSONArray()

    override fun onReceive(context: Context, intent: Intent) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Esel:ReadReceiver:Broadcast")
        wl.acquire()

        Log.v(TAG, "onReceive called")
        setAlarm(context) // Use context directly

        callBroadcast(context)

        wl.release()
    }

    private fun callBroadcast(context: Context) {
        var sync = 8
        try {
            sync = SP.getInt("max-sync-hours", sync)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val syncTime = sync * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        try {
            SP.putLong("readReceiver-called", currentTime)

            var lastReadingTime = SP.getLong("lastReadingTime", currentTime)
            if (lastReadingTime + syncTime < currentTime) {
                lastReadingTime = currentTime - syncTime
            }

            broadcastData(context, lastReadingTime, true)

        } catch (e: Exception) {
            val msg = e.message
            ToastUtils.makeToast("Exception: $msg")
            Log.e(TAG, msg)
        }

        // Auto full sync in specific time intervals
        val autoSyncInterval = SP.getInt("auto-sync-interval", 3) * 60 * 60 * 1000L
        val lastFullSync = SP.getLong("last_full_sync", currentTime - autoSyncInterval - 1)

        if (autoSyncInterval > 0 && (currentTime - lastFullSync) > autoSyncInterval) {
            fullSync(context, sync)
        }

        val useEsdms = SP.getBoolean("use_esdms", false)
        if (useEsdms) {
            EsNowDatareader.updateLogin()
        }
    }

    private fun fullSync(context: Context, syncHours: Int) {
        val currentTime = System.currentTimeMillis()
        val syncTime = syncHours * 60 * 60 * 1000L
        val lastTimestamp = currentTime - syncTime

        val useEsdms = SP.getBoolean("use_esdms", false)
        if (useEsdms) {
            val dataHandler = object : EsNowDatareader.ProcessResultI {
                override fun processResult(data: List<SGV>) {
                    try {
                        val written = processValues(false, data)
                        val msg = "Full Sync done: Read $written values from DB\n(last $syncHours hours)"
                        Log.i(TAG, msg, true)
                    } catch (e: Exception) {
                        Log.e(TAG, "No access to eversensedms", true)
                    }
                }
            }

            val reader = EsNowDatareader()
            reader.queryLastValues(dataHandler, syncHours)

        } else {
            val written = broadcastData(context, lastTimestamp, false)
            val msg = "Full Sync done: Read $written values from DB\n(last $syncHours hours)"
            Log.i(TAG, msg, true)
        }

        SP.putLong("last_full_sync", currentTime)
    }

    fun fullExport(context: Context, file: File, syncHours: Int) {
        val currentTime = System.currentTimeMillis()
        val syncTime = syncHours * 60 * 60 * 1000L
        val lastTimestamp = currentTime - syncTime

        var written = 0
        suppressBroadcast = true
        val useEsdms = SP.getBoolean("use_esdms", false)

        if (useEsdms) {
            val dataHandler = object : EsNowDatareader.ProcessResultI {
                override fun processResult(data: List<SGV>) {
                    written = processValues(false, data)
                    val msg = "Full Sync done: Read $written values from DB\n(last $syncHours hours)"
                    Log.i(TAG, msg, true)
                    writeData(file, output.toString())
                    output.put("") // Clear the JSONArray
                    suppressBroadcast = false
                }
            }

            val reader = EsNowDatareader()
            reader.queryLastValues(dataHandler, syncHours)

        } else {
            written = broadcastData(context, lastTimestamp, false)
            suppressBroadcast = false
            writeData(file, output.toString())
            output.put("") // Clear the JSONArray
            val msg = "Full Sync done: Read $written values from DB\n(last $syncHours hours)"
            Log.i(TAG, msg, true)
        }

        SP.putLong("last_full_sync", currentTime)
    }

    private fun writeData(file: File, data: String) {
        if (!file.parentFile.exists()) {
            file.parentFile.mkdir()
        }
        if (!file.parentFile.canWrite()) {
            val msg = "Error: can not write data. Please enable the storage access permission for Esel."
            Log.e(TAG, msg, true)
        }
        if (!file.exists()) {
            try {
                file.createNewFile()
                FileWriter(file.absoluteFile).use { fileWriter ->
                    BufferedWriter(fileWriter).use { bufferedWriter ->
                        bufferedWriter.write(data)
                    }
                }
            } catch (err: IOException) {
                val msg = "Error creating file: ${err.toString()} occurred at: ${err.stackTraceToString()}"
                Log.e(TAG, msg, true)
            }
        }
    }

    private fun broadcastData(context: Context, lastReadingTime: Long, isContinuousRun: Boolean): Int {
        var result = 0
        try {
            val currentTime = System.currentTimeMillis()
            SP.putLong("readReceiver-called", currentTime)

            val size = 2
            var updatedReadingTime = lastReadingTime

            val usePatchedEs = SP.getBoolean("use_patched_es", true)

            do {
                lastReadingTime = updatedReadingTime
                val valueArray = ArrayList<SGV>()

                if (SP.getBoolean("overwrite_bg", false)) {
                    val bg = SP.getInt("bg_value", 120)
                    valueArray.add(SGV(bg, lastReadingTime, 1))
                    valueArray.add(SGV(bg, currentTime, 2))
                } else if (usePatchedEs) {
                    valueArray.addAll(Datareader.readDataFromContentProvider(context, size, lastReadingTime))
                    if (valueArray.isEmpty()) {
                        Log.e(TAG, "DB not readable!", true)
                    }
                } else {
                    val readFromNl = true
                    if (readFromNl) {
                        valueArray.addAll(EsNotificationListener.getData(size, lastReadingTime))
                    }
                }

                if (valueArray.size != size) {
                    return result
                }

                result += processValues(isContinuousRun, valueArray)
                updatedReadingTime = SP.getLong("lastReadingTime", lastReadingTime)
            } while (updatedReadingTime != lastReadingTime)

        } catch (eb: android.database.CursorIndexOutOfBoundsException) {
            eb.printStackTrace()
            Log.w(TAG, "DB is empty! It can take up to 15min with running Eversense App until values are available!", true)
        } catch (e: Exception) {
            e.printStackTrace()
            SP.putInt("lastReadingValue", 120)
        }

        return result
    }

    private fun processValues(isContinuousRun: Boolean, valueArray: List<SGV>): Int {
        var result = 0
        val currentTime = System.currentTimeMillis()

        for (i in valueArray.indices) {
            val sgv = valueArray[i]
            val oldTime = SP.getLong("lastReadingTime", -1L)
            val newValue = oldTime != sgv.timestamp
            var futureValue = false

            if (sgv.timestamp - currentTime > 60 * 1000) {
                // sgv is from future
                val shiftValue = sgv.timestamp - currentTime
                val sec = shiftValue / 1000f
                Log.w(TAG, "broadcastData called, value is in future by [sec] $sec")
                futureValue = true
            }

            if (newValue && !futureValue) {
                val oldValue = SP.getInt("lastReadingValue", -1)
                val sgvTime = sgv.timestamp
                val hasTimeGap = (sgvTime - oldTime) > 12 * 60 * 1000L

                if (sgv.value >= 39) {
                    if (isContinuousRun) {
                        val enableSmooth = SP.getBoolean("smooth_data", false) && !hasTimeGap
                        sgv.smooth(oldValue, enableSmooth)
                    }

                    val slopeByMinute = if (oldTime != sgvTime) {
                        (sgv.value - oldValue) * 60000.0 / (sgvTime - oldTime)
                    } else {
                        0.0
                    }

                    if (!hasTimeGap) {
                        sgv.setDirection(slopeByMinute)
                    }

                    try {
                        if (!suppressBroadcast) {
                            LocalBroadcaster.broadcast(sgv, isContinuousRun)
                        } else {
                            LocalBroadcaster.addSgvEntry(output, sgv)
                        }
                        result++
                    } catch (e: Exception) {
                        Log.e(TAG, "LocalBroadcaster.broadcast exception, result = ${e.message}")
                    }
                } else {
                    ToastUtils.makeToast("NOT A READING!")
                }

                SP.putLong("lastReadingTime", sgvTime)
                SP.putInt("lastReadingValue", sgv.value)
            }
        }
        return result
    }

    private fun setAlarm(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, ReadReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
        am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + REPEAT_TIME, pi)
    }

    fun cancelAlarm(context: Context) {
        val intent = Intent(context, ReadReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }
}