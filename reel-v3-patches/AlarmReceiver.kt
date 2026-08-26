package com.rushx.reelscheduler

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SEND_REEL) return
        val id = intent.getLongExtra(EXTRA_JOB_ID, -1L)
        val repo = JobRepository(context)
        val job = repo.get(id) ?: return

        wakeScreenBriefly(context)

        val keyguard = context.getSystemService(KeyguardManager::class.java)
        if (keyguard.isDeviceLocked) {
            val retries = job.retryCount + 1
            if (retries <= 20) {
                val retry = job.copy(
                    triggerAtMillis = System.currentTimeMillis() + 30_000L,
                    status = ScheduledReel.STATUS_WAITING_UNLOCK,
                    retryCount = retries
                )
                repo.upsert(retry)
                if (AlarmScheduler.canScheduleExact(context)) AlarmScheduler.schedule(context, retry)
                Notifier.show(
                    context,
                    "Reel waiting for unlock",
                    "Unlock the phone. I’ll retry @${job.recipient} automatically.",
                    (id % Int.MAX_VALUE).toInt()
                )
            } else {
                repo.upsert(job.copy(status = ScheduledReel.STATUS_FAILED))
                Notifier.show(context, "Reel not sent", "The phone stayed locked for too long.")
            }
            return
        }

        val active = job.copy(status = ScheduledReel.STATUS_ACTIVE)
        repo.upsert(active)
        repo.setActiveJobId(id)

        if (InstagramAccessibilityService.launchScheduledJob(active)) {
            Notifier.show(context, "Sending scheduled Reel", "Opening Instagram for @${job.recipient}…")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
            if (launchInstagram(context, active)) {
                Notifier.show(context, "Sending scheduled Reel", "Opening Instagram for @${job.recipient}…")
                return
            }
        }

        repo.upsert(job.copy(status = ScheduledReel.STATUS_FAILED))
        repo.setActiveJobId(null)
        Notifier.show(
            context,
            "Background launch blocked",
            "Enable Reel Scheduler → Appear on top, and keep its Accessibility service enabled."
        )
    }

    private fun launchInstagram(context: Context, job: ScheduledReel): Boolean {
        val reelIntent = Intent(Intent.ACTION_VIEW, Uri.parse(job.url)).apply {
            setPackage("com.instagram.android")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return try {
            context.startActivity(reelIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun wakeScreenBriefly(context: Context) {
        try {
            val pm = context.getSystemService(PowerManager::class.java)
            val lock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "ReelScheduler:sendWake"
            )
            lock.acquire(15_000L)
        } catch (_: Throwable) {
        }
    }

    companion object {
        const val ACTION_SEND_REEL = "com.rushx.reelscheduler.SEND_REEL"
        const val EXTRA_JOB_ID = "jobId"
    }
}
