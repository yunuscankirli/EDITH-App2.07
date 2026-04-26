package com.example.fulledith

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var ttsReady = false
    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var hudOverlay: HUDOverlay
    private lateinit var messaging: MessagingManager

    private var pendingAction: String? = null
    private var pendingNumber: String = ""
    private var pendingMessage: String = ""

    private var iokbsModAktif = false
    private var iokbsSoruBekle = false
    private var iokbsSoruAktif: IOKBSCoach.Soru? = null
    private var iokbsDogruSayisi = 0
    private var iokbsYanlisSayisi = 0
    private var iokbsSoruSayisi = 0

    companion object {
        const val PERMISSION_REQUEST = 200
        const val TAG = "EDITH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        commandText = findViewById(R.id.commandText)
        hudOverlay = findViewById(R.id.hudOverlay)
        tts = TextToSpeech(this, this)
        messaging = MessagingManager(this)
        checkPermissions()
        AlarmScheduler.kur(this)
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSION_REQUEST)
        } else {
            initVoice()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST) initVoice()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }
            tts.setPitch(0.52f)
            tts.setSpeechRate(1.05f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 600)
                }
                override fun onError(utteranceId: String?) {
                    Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 600)
                }
            })
            ttsReady = true
            speak("E.D.I.T.H. sistemi başlatıldı. Tüm sistemler aktif. Dinliyorum.")
        }
    }

    private fun initVoice() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread {
                    isListening = true
                    statusText.text = "🎙️ DİNLİYORUM..."
                    hudOverlay.setListening(true)
                }
            }
            override fun onBeginningOfSpeech() {
                runOnUiThread { statusText.text = "🎤 SES ALINIYOR..." }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                runOnUiThread {
                    commandText.text = "► $text"
                    isListening = false
                    hudOverlay.setListening(false)
                    processCommand(text.lowercase(Locale.getDefault()))
                }
            }
            override fun onError(error: Int) {
                runOnUiThread {
                    isListening = false
                    hudOverlay.setListening(false)
                    statusText.text = "⚡ HAZIR"
                }
                Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 1500)
            }
            override fun onRmsChanged(rmsdB: Float) {
                runOnUiThread { hudOverlay.setVolume(rmsdB) }
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                runOnUiThread { commandText.text = "... $partial" }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        if (isListening || !ttsReady) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Listening error: ${e.message}")
        }
    }

    private fun sorSonrakiIOKBS() {
        val soru = IOKBSCoach.rastgeleSoru()
        iokbsSoruAktif = soru
        iokbsSoruBekle = true
        speak(soru.metin)
    }

    private fun processCommand(cmd: String) {
        when (pendingAction) {
            "sms_num" -> { pendingNumber = cmd; pendingAction = "sms_msg"; speak("Mesajı söyleyin."); return }
            "sms_msg" -> { pendingMessage = cmd; pendingAction = null; speak(messaging.smsSend(pendingNumber, pendingMessage)); return }
            "wa_num" -> { pendingNumber = cmd; pendingAction = "wa_msg"; speak("Mesajı söyleyin."); return }
            "wa_msg" -> { pendingMessage = if (cmd == "geç") "" else cmd; pendingAction = null; speak(messaging.whatsappAc(pendingNumber, pendingMessage)); return }
            "tg_msg" -> { pendingMessage = cmd; pendingAction = null; messaging.telegramGonder(pendingMessage) { speak(it) }; speak("Telegram mesajı gönderiliyor."); return }
        }

        if (iokbsSoruBekle && iokbsSoruAktif != null) {
            iokbsSoruBekle = false
            val soru = iokbsSoruAktif!!
            val dogru = soru.dogruCevap.split("|").any { cmd.contains(it.lowercase()) }
            iokbsSoruSayisi++
            if (dogru) { iokbsDogruSayisi++; speak("Doğru! ${soru.aciklama} Toplam: $iokbsDogruSayisi doğru.") }
            else { iokbsYanlisSayisi++; speak("Yanlış. Doğru: ${soru.aciklama}") }
            Handler(Looper.getMainLooper()).postDelayed({ if (iokbsModAktif) sorSonrakiIOKBS() }, 2500)
            return
        }

        val response = when {
            cmd.contains("whatsapp") && cmd.contains("mesaj") -> { pendingAction = "wa_num"; "Hangi numaraya?" }
            cmd.contains("whatsapp") -> { messaging.whatsappAc(); "WhatsApp açılıyor." }
            cmd.contains("sms") -> { pendingAction = "sms_num"; "Numarayı söyleyin." }
            cmd.contains("telegram") && cmd.contains("oku") -> { messaging.telegramOku { speak(it) }; "Okunuyor." }
            cmd.contains("telegram") -> { pendingAction = "tg_msg"; "Ne yazmamı istersiniz?" }
            cmd.contains("merhaba") || cmd.contains("selam") -> "Merhaba efendim. E.D.I.T.H. hizmetinizde."
            cmd.contains("kimsin") -> "Ben E.D.I.T.H. Stark teknolojisi tabanlı yapay zeka."
            cmd.contains("saat") -> { val sdf = SimpleDateFormat("HH:mm", Locale("tr")); "Saat ${sdf.format(Date())}." }
            cmd.contains("tarih") || cmd.contains("bugün") -> { val sdf = SimpleDateFormat("d MMMM yyyy", Locale("tr")); "${sdf.format(Date())}." }
            cmd.contains("durum") || cmd.contains("rapor") -> "Tüm sistemler nominal."
            cmd.contains("test") -> "Tüm sistemler operasyonel."
            cmd.isBlank() -> { startListening(); return }
            else -> "Komut alındı: $cmd"
        }
        speak(response)
    }

    fun speak(text: String) {
        if (!ttsReady) return
        runOnUiThread { statusText.text = "🔊 KONUŞUYOR..." }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), "EDITH_${System.currentTimeMillis()}")
    }

    override fun onResume() {
        super.onResume()
        if (ttsReady && !isListening) Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 1000)
    }

    override fun onPause() {
        super.onPause()
        speechRecognizer?.stopListening()
        isListening = false
    }

    override fun onDestroy() {
        tts.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
