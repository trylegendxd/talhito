package com.rushx.reelscheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    fun canScheduleExact(context: Context): Boolean {
        val alarm = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()
    }

    fun schedule(context: Context, job: ScheduledReel) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_SEND_REEL)
            .putExtra(AlarmReceiver.EXTRA_JOB_ID, job.id)

        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(job.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, job.triggerAtMillis, pi)
        } else {
            alarm.setExact(AlarmManager.RTC_WAKEUP, job.triggerAtMillis, pi)
        }
    }

    fun cancel(context: Context, id: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_SEND_REEL)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(id),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) alarm.cancel(pi)
    }

    private fun requestCode(id: Long): Int = (id xor (id ushr 32)).toInt()
}
