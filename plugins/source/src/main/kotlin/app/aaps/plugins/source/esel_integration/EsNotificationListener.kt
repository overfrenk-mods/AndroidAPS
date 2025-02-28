// EselGlucoseSource.kt
package app.aaps.plugins.source.esel_integration

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.ArrayList
import app.aaps.plugins.source.esel_integration.*
import java.util.List

//import delle classi di AndroidAPS necessarie

class EselGlucoseSource : NotificationListenerService() {
    private var timestamp: Long = 0
    companion object {
        private var lastReadings: MutableList<SGV> = ArrayList()
        // private val rr = ReadReceiver()  // Rimuovi la dichiarazione di rr
        private var lastProcessedTimestamp: Long = 0 // Timestamp dell'ultima lettura elaborata
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        //Implementazione simile a EsNotificationListener.java, adattata a Kotlin
        //...
        Log.d("EselGlucoseSource", "Notification posted from: ${sbn.packageName}")
        val sgv = generateSGV(notification, lastReadings.size)
        if (sgv != null) {
            lastReadings.add(sgv)
            // rr.CallBroadcast(null)  // Rimuovi la chiamata a CallBroadcast
            lastProcessedTimestamp = System.currentTimeMillis()
        }
        //...
    }

    fun getData(number: Int, lastReadingTime: Long): MutableList<SGV> {
        //Implementazione simile a EsNotificationListener.java, adattata a Kotlin
        //...
        return lastReadings
    }

    fun generateSGV(notification: Notification, record: Int): SGV? {
        val glucoseValue = notification.extras.getDouble("glucose_value")
        val timestamp = notification.extras.getLong("timestamp")

        // You can now use the 'record' parameter, e.g., logging it
        //Log.d("EselGlucoseSource", "Generating SGV record #$record")

        return if (glucoseValue > 0 && timestamp > 0) {
            SGV(glucoseValue.toInt(), timestamp)
        } else {
            null
        }
    }


    private fun insertGlucoseIntoAndroidAPS(timestamp: Long, glucoseValue: Double) {
        // Usa le API di AndroidAPS per inserire i dati nel database
        // Esempio (da adattare):
        //val resolver = contentResolver
        //val values = ContentValues()
        //values.put(..., timestamp)
        //values.put(..., glucoseValue)
        //resolver.insert(..., values)
    }
}