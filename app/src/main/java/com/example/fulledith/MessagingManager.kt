package com.example.fulledith

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MessagingManager(private val context: Context) {

    private val TELEGRAM_BOT_TOKEN = "YOUR_BOT_TOKEN_HERE"
    private val TELEGRAM_CHAT_ID   = "YOUR_CHAT_ID_HERE"

    fun whatsappAc(numara: String = "", mesaj: String = ""): String {
        return try {
            val uri = if (numara.isNotBlank()) {
                val temizNumara = numara.replace(Regex("[^0-9+]"), "")
                val encodedMsg  = URLEncoder.encode(mesaj, "UTF-8")
                Uri.parse("https://wa.me/$temizNumara?text=$encodedMsg")
            } else {
                Uri.parse("whatsapp://send")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (numara.isNotBlank()) "WhatsApp açıldı." else "WhatsApp açıldı."
        } catch (e: Exception) {
            "WhatsApp bulunamadı."
        }
    }

    fun smsSend(numara: String, mesaj: String): String {
        return try {
            val temizNumara = numara.replace(Regex("[^0-9+]"), "")
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(mesaj)
            smsManager.sendMultipartTextMessage(temizNumara, null, parts, null, null)
            "SMS gönderildi: $temizNumara"
        } catch (e: Exception) {
            "SMS gönderilemedi."
        }
    }

    fun telegramGonder(mesaj: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val sonuc = try {
                val url = URL("https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage")
                val params = "chat_id=${URLEncoder.encode(TELEGRAM_CHAT_ID, "UTF-8")}" +
                             "&text=${URLEncoder.encode(mesaj, "UTF-8")}"
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    outputStream.write(params.toByteArray())
                    connectTimeout = 8000
                    readTimeout    = 8000
                }
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(response)
                if (json.getBoolean("ok")) "Telegram mesajı gönderildi." else "Telegram hatası."
            } catch (e: Exception) {
                "Telegram bağlantısı kurulamadı."
            }
            withContext(Dispatchers.Main) { callback(sonuc) }
        }
    }

    fun telegramOku(callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val sonuc = try {
                val url = URL("https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/getUpdates?limit=3")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout    = 8000
                }
                val response = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json    = JSONObject(response)
                val results = json.getJSONArray("result")
                if (results.length() == 0) {
                    "Yeni Telegram mesajı yok."
                } else {
                    val sb = StringBuilder("Son mesajlar: ")
                    for (i in 0 until results.length()) {
                        val msg  = results.getJSONObject(i).optJSONObject("message") ?: continue
                        val from = msg.optJSONObject("from")?.optString("first_name", "Bilinmiyor") ?: "Bilinmiyor"
                        val text = msg.optString("text", "")
                        if (text.isNotBlank()) sb.append("$from: $text. ")
                    }
                    sb.toString()
                }
            } catch (e: Exception) {
                "Telegram mesajları okunamadı."
            }
            withContext(Dispatchers.Main) { callback(sonuc) }
        }
    }
}
