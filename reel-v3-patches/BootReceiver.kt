package com.rushx.reelscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        if (!AlarmScheduler.canScheduleExact(context)) {
            Notifier.show(context, "Reel Scheduler needs attention", "Allow Alarms & reminders again so scheduled Reels can run in the background.")
            return
        }

        val repo = JobRepository(context)
        val now = System.currentTimeMillis()
        repo.all()
            .filter { it.status == ScheduledReel.STATUS_SCHEDULED || it.status == ScheduledReel.STATUS_WAITING_UNLOCK }
            .forEach { job ->
                val trigger = if (job.triggerAtMillis > now) job.triggerAtMillis else now + 5_000L
                val restored = job.copy(triggerAtMillis = trigger, status = ScheduledReel.STATUS_SCHEDULED)
                repo.upsert(restored)
                AlarmScheduler.schedule(context, restored)
            }
    }
}
