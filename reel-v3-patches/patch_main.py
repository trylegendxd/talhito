from pathlib import Path

p = Path('ReelSchedulerAndroid/app/src/main/java/com/rushx/reelscheduler/MainActivity.kt')
s = p.read_text()
s = s.replace(
    'findViewById<Button>(R.id.alarmButton).setOnClickListener { requestExactAlarmAccess() }',
    'findViewById<Button>(R.id.alarmButton).setOnClickListener { requestExactAlarmAccess() }\n        findViewById<Button>(R.id.overlayButton).setOnClickListener { requestOverlayAccess() }'
)
s = s.replace(
    '        val job = ScheduledReel(',
    '        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {\n            toast("Allow Appear on top so Android can open Instagram while Reel Scheduler is closed")\n            requestOverlayAccess()\n            return\n        }\n\n        val job = ScheduledReel('
)
old = '''    private fun updateSetupStatus() {
        val access = if (isAccessibilityEnabled()) "✓ Instagram automation" else "✗ Instagram automation"
        val alarm = if (AlarmScheduler.canScheduleExact(this)) "✓ Exact timing" else "✗ Exact timing"
        setupStatus.text = "$access\\n$alarm\\nInstagram must already be logged in."
    }
'''
new = '''    private fun updateSetupStatus() {
        val access = if (isAccessibilityEnabled()) "✓ Instagram automation" else "✗ Instagram automation"
        val alarm = if (AlarmScheduler.canScheduleExact(this)) "✓ Exact timing" else "✗ Exact timing"
        val overlay = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) "✓ Background launch" else "✗ Background launch (Appear on top)"
        setupStatus.text = "$access\\n$alarm\\n$overlay\\nInstagram must already be logged in."
    }

    private fun requestOverlayAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            toast("Background launch is already allowed")
        }
    }
'''
s = s.replace(old, new)
p.write_text(s)

p = Path('ReelSchedulerAndroid/app/src/main/res/layout/activity_main.xml')
s = p.read_text()
marker = '''        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="QUEUE"'''
button = '''        <Button
            android:id="@+id/overlayButton"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:background="@drawable/button_secondary_bg"
            android:text="Allow background launch (Appear on top)"
            android:textColor="@color/textPrimary" />

'''
s = s.replace(marker, button + marker)
p.write_text(s)
