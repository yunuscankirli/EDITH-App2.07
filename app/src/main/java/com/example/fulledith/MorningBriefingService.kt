package com.example.fulledith

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MorningBriefingService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private val briefingQueue = mutableListOf<String>()
    private var currentIndex  = 0

    companion object {
        const val CHANNEL_ID = "edith_morning"
        const val NOTIF_ID   = 1001
        const val OWM_API_KEY = "BURAYA_API_KEY_YAZIN"
        const val INEGOL_LAT = 40.0767
        const val INEGOL_LON = 29.5092
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("tr", "TR")
            tts.setPitch(0.52f)
            tts.setSpeechRate(1.0f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(uid: String?) {}
                override fun onDone(uid: String?) {
                    currentIndex++
                    if (currentIndex < briefingQueue.size) speakNext() else stopSelf()
                }
                override fun onError(uid: String?) { stopSelf() }
            })
            fetchWeatherThenBuild()
        }
    }

    private fun fetchWeatherThenBuild() {
        CoroutineScope(Dispatchers.IO).launch {
            val havaCumlesi = try {
                val url = URL("https://api.openweathermap.org/data/2.5/weather?lat=$INEGOL_LAT&lon=$INEGOL_LON&appid=$OWM_API_KEY&units=metric&lang=tr")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
                }
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                conn.disconnect()
                val sicaklik = json.getJSONObject("main").getDouble("temp").toInt()
                val nem      = json.getJSONObject("main").getInt("humidity")
                val tanim    = json.getJSONArray("weather").getJSONObject(0).getString("description")
                val ruzgar   = json.getJSONObject("wind").getDouble("speed").toInt()
                "İnegöl'de $tanim. Sıcaklık $sicaklik derece, nem yüzde $nem, rüzgar $ruzgar metre."
            } catch (e: Exception) {
                "İnegöl'de bugün hava güzel bekleniyor."
            }
            withContext(Dispatchers.Main) { buildBriefing(havaCumlesi); speakNext() }
        }
    }

    private fun buildBriefing(havaCumlesi: String) {
        val cal   = Calendar.getInstance()
        val gun   = SimpleDateFormat("EEEE", Locale("tr")).format(cal.time)
        val tarih = SimpleDateFormat("d MMMM yyyy", Locale("tr")).format(cal.time)
        val saat  = SimpleDateFormat("HH:mm", Locale("tr")).format(cal.time)
        val isim  = getSharedPreferences("edith_prefs", Context.MODE_PRIVATE).getString("kullanici_adi", "efendim") ?: "efendim"
        briefingQueue.clear(); currentIndex = 0
        briefingQueue.add("Günaydın $isim. Bugün $gun, $tarih. Saat $saat.")
        briefingQueue.add(havaCumlesi)
        briefingQueue.add("Bugün harika şeyler başarabilirsiniz. Sistemler hazır.")
        briefingQueue.add("Sabah brifingim tamamlandı. İyi günler.")
    }

    private fun speakNext() {
        if (currentIndex >= briefingQueue.size) return
        tts.speak(briefingQueue[currentIndex], TextToSpeech.QUEUE_FLUSH, null, "brief_$currentIndex")
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EDITH — Sabah Brifing")
            .setContentText("Başlatılıyor...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pi).build()
    }

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "EDITH Sabah", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
