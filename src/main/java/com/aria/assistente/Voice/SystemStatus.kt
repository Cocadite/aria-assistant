package com.aria.assistente.voice

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlin.math.roundToInt

object SystemStatus {
    fun summary(ctx: Context): String {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val bytesAvail = stat.availableBytes
        val bytesTotal = stat.totalBytes
        val usedPct = (((bytesTotal - bytesAvail).toDouble() / bytesTotal.toDouble()) * 100.0).roundToInt()

        return "Bateria ${level}%. Armazenamento ${usedPct}% usado. Android ${Build.VERSION.RELEASE}."
    }
}
