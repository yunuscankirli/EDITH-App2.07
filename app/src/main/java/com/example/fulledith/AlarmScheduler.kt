package com.example.fulledith

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    fun kur(context: Context) {
        val prefs = context.getSharedPreferences("edith_prefs", Context.MODE_PRIVATE)
        val saat   = prefs.getInt("alarm_saat", 7)
        val dakika = prefs.getInt("alarm_dakika", 0)
        val aktif  = prefs.getBoolean("alarm_aktif", true)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.fulledith.MORNING_BRIEFING"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        if (!aktif) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, saat)
            set(Calendar.MINUTE, dakika)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent
            )
        }
    }

    fun iptalEt(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.fulledith.MORNING_BRIEFING"
        }
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    fun ayarla(context: Context, saat: Int, dakika: Int) {
        context.getSharedPreferences("edith_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("alarm_saat", saat)
            .putInt("alarm_dakika", dakika)
            .putBoolean("alarm_aktif", true)
            .apply()
        kur(context)
    }
}
