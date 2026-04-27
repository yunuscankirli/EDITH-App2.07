package com.example.fulledith

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.example.fulledith.MORNING_BRIEFING" -> {
                val serviceIntent = Intent(context, MorningBriefingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                AlarmScheduler.kur(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                AlarmScheduler.kur(context)
            }
        }
    }
}
