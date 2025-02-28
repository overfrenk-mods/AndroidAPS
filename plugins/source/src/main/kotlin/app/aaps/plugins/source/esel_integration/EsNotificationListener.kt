// EselGlucoseSource.kt
package app.aaps.plugins.source.esel_integration

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.ArrayList
import java.util.List

//import delle classi di AndroidAPS necessarie

class EselGlucoseSource : NotificationListenerService() {
    val notification = sbn.notification

    companion object {
        private var lastReadings: MutableList<SGV> = ArrayList()
        private val rr = ReadReceiver()
        private var lastProcessedTimestamp: Long = 0 // Timestamp dell'ultima lettura elaborata
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        //Implementazione simile a EsNotificationListener.java, adattata a Kotlin
        //...
        val sgv = generateSGV(notification, lastReadings.size)
        if (sgv != null) {
            lastReadings.add(sgv)
            rr.CallBroadcast(null)
            lastProcessedTimestamp = currentTimestamp // Aggiorna il timestamp dell'ultima lettura elaborata
        }
        //...
    }

    fun getData(number: Int, lastReadingTime: Long): List<SGV> {
        //Implementazione simile a EsNotificationListener.java, adattata a Kotlin
        //...
    }

    fun generateSGV(notification: Notification, record: Int): SGV? {
        //Implementazione simile a EsNotificationListener.java, adattata a Kotlin
        //...
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