package tech.kshot.midnightprover

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the app alive while a proof runs (Android
 * 14/15 restrict background work, and the HTTP request to the proof server
 * must stay open). It shows an ongoing notification with a live timer and a
 * Cancel action, then a completion notification, and stops itself.
 */
class ProveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var watchJob: kotlinx.coroutines.Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as ProverApplication
        if (intent?.action == ACTION_CANCEL) {
            app.engine.cancel()
            return START_NOT_STICKY
        }
        val session = app.engine.session.value
        // A start while idle (or after the job already finished) must not
        // post a spurious "Proof failed" notification.
        if (!session.isRunning) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground(ongoing(session))
        acquireWakeLock(app.store.loadSettings().proveTimeoutMinutes)
        if (watchJob?.isActive != true) {
            watchJob = scope.launch {
                val done = app.engine.session.first { !it.isRunning }
                notifyFinished(done)
                releaseWakeLock()
                ServiceCompat.stopForeground(this@ProveService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startInForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        ServiceCompat.startForeground(this, ONGOING_ID, n, type)
    }

    private fun ongoing(s: ProveSession): Notification {
        val cancel = PendingIntent.getService(
            this, 1, Intent(this, ProveService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val circuit = s.info?.keyLocation ?: "proving payload"
        val host = runCatching { java.net.URI(s.serverUrl).host }.getOrNull() ?: "proof server"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(Copy.notificationTitle(host))
            .setContentText("$circuit · keep the proof server running")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(true)
            .setWhen(if (s.startedAtMillis > 0) s.startedAtMillis else System.currentTimeMillis())
            .setProgress(0, 0, true)
            .setContentIntent(openApp())
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, Copy.CANCEL, cancel)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun notifyFinished(s: ProveSession) {
        val (title, text) = when (s.state) {
            JobState.DONE -> Copy.doneTitle(JobHistoryFormat.formatDuration(s.elapsedSeconds)) to "${JobHistoryFormat.formatBytes(s.proofBytes)} proof. Tap to copy or share it."
            JobState.CANCELLED -> return
            else -> (s.failure?.title ?: "Proof failed") to (s.failure?.message ?: "")
        }
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (s.state == JobState.DONE) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp())
            .build()
        val allowed = Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (allowed) runCatching { NotificationManagerCompat.from(this).notify(DONE_ID, n) }
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun acquireWakeLock(timeoutMinutes: Int) {
        if (wakeLock?.isHeld == true) return
        releaseWakeLock()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MidnightProver:prove").apply {
            setReferenceCounted(false)
            acquire(timeoutMinutes * 60_000L + 30_000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "proving"
        const val ACTION_CANCEL = "tech.kshot.midnightprover.CANCEL"
        private const val ONGOING_ID = 6300
        private const val DONE_ID = 6301

        fun createChannel(context: Context) {
            val ch = NotificationChannel(CHANNEL_ID, "Proof jobs", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress and results of proofs"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        /** Starts the foreground service; falls back silently (the engine still runs) if Android refuses. */
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, ProveService::class.java))
            }
        }
    }
}
