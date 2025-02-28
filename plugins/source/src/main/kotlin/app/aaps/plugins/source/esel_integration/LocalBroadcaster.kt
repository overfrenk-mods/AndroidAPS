package app.aaps.plugins.source.esel_integration

import android.content.Intent
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log // Importa la classe Log

// Importa le classi SGV e SP da AndroidAPS (o definiscile se necessario)
// import app.aaps.SGV
// import app.aaps.SP

object LocalBroadcaster {

    const val XDRIP_PLUS_NS_EMULATOR = "com.eveningoutpost.dexdrip.NS_EMULATOR"
    const val ACTION_DATABASE = "info.nightscout.client.DBACCESS"

    private const val TAG = "LocalBroadcaster"
    private val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    fun broadcast(context: Context, sgv: SGV, log: Boolean) {
        try {
            val timeShift = SP.getInt("shift_days", 0) // Supponiamo che SP sia accessibile tramite context
            if (timeShift > 0) {
                val timeShiftMillis = timeShift * 24 * 60 * 60 * 1000 // days to ms
                sgv.timestamp = sgv.timestamp + timeShiftMillis
            }

            if (SP.getBoolean("send_to_AAPS", true)) { // Supponiamo che SP sia accessibile tramite context
                val entriesBody = JSONArray()
                addSgvEntry(entriesBody, sgv)
                sendBundle(context, "add", "entries", entriesBody, XDRIP_PLUS_NS_EMULATOR, log)
            }

            if (SP.getBoolean("send_to_NS", true)) { // Supponiamo che SP sia accessibile tramite context
                sendBundle(context, "dbAdd", "entries", generateSgvEntry(sgv), ACTION_DATABASE, log)
            }

            if (log) {
                Log.i(TAG, "${sgv.value} ${sgv.direction}") // Usa Log di Android
            }
        } catch (e: Exception) {
            val msg = "Unable to send bundle: $e"
            Log.e(TAG, msg) // Usa Log di Android
        }
    }

    fun addSgvEntry(entriesArray: JSONArray, sgv: SGV) {
        val json = generateSgvEntry(sgv)
        entriesArray.put(json)
    }

    private fun generateSgvEntry(sgv: SGV): JSONObject {
        val json = JSONObject()
        json.put("sgv", sgv.value)
        json.put("rawbg", sgv.raw)
        if (sgv.direction == null) {
            json.put("direction", "NONE")
        } else {
            json.put("direction", sgv.direction)
        }
        json.put("device", "ESEL")
        json.put("type", "sgv")
        json.put("date", sgv.timestamp)
        json.put("dateString", format.format(sgv.timestamp))

        return json
    }

    private fun sendBundle(context: Context, action: String, collection: String, json: Any, intentIdAction: String, log: Boolean) {
        val bundle = Bundle()
        bundle.putString("action", action)
        bundle.putString("collection", collection)
        bundle.putString("data", json.toString())
        val intent = Intent(intentIdAction)
        intent.putExtras(bundle).addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)

        val packageManager = context.packageManager
        val receivers = packageManager.queryBroadcastReceivers(intent, PackageManager.GET_RESOLVED_FILTER)

        for (resolveInfo in receivers) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName != null) {
                intent.setPackage(packageName)
                context.sendBroadcast(intent)
                if (log) {
                    Log.i(TAG, "send to: $packageName") // Usa Log di Android
                }
            }
        }
    }
}