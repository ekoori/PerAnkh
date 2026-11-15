package com.krbavcic.pentalingo

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.krbavcic.pentalingo.data.Release
import com.krbavcic.pentalingo.data.ReleaseCheck
import com.krbavcic.pentalingo.data.compareVersions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * "Tell me when a new version is out", in Per Ankh from GitHub: about once a day, when there is a
 * network, the same question the About page asks (see [ReleaseCheck] and PRIVACY.md), and one
 * notification per new release. Off until the learner turns it on.
 */
object UpdateNotices {
    /** The GitHub build looks for releases itself; Google Play's keeps itself up to date. */
    const val AVAILABLE = true

    private const val JOB = 4201
    internal const val CHANNEL = "releases"

    /** Schedules the daily look, or cancels it. Scheduling again leaves a pending look as it is. */
    fun set(context: Context, on: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!on) {
            scheduler.cancel(JOB)
            return
        }
        if (scheduler.getPendingJob(JOB) != null) return
        scheduler.schedule(
            JobInfo.Builder(JOB, ComponentName(context, UpdateCheckJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(TimeUnit.DAYS.toMillis(1))
                .setPersisted(true)
                .build(),
        )
    }

    /** Whether Android lets the app post notifications (asked for on Android 13 and later). */
    fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    internal suspend fun check(context: Context) {
        val release = ReleaseCheck(
            listOf(BuildConfig.LATEST_RELEASE_API, BuildConfig.LATEST_RELEASE_MIRROR),
            pages = "${BuildConfig.PROJECT_URL}/releases",
            userAgent = "PerAnkh/${BuildConfig.VERSION_NAME}",
        ).latest() ?: return
        if (compareVersions(release.version, BuildConfig.VERSION_NAME) <= 0) return
        val seen = context.getSharedPreferences("update-notices", Context.MODE_PRIVATE)
        if (seen.getString("notified", null) == release.version) return
        if (!allowed(context)) return
        notify(context, release)
        seen.edit().putString("notified", release.version).apply()
    }

    private fun notify(context: Context, release: Release) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "New versions", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "When a new version of Per Ankh is out"
            },
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(Intent.ACTION_VIEW, Uri.parse(release.url)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        manager.notify(
            JOB,
            Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_ankh)
                .setContentTitle("Per Ankh ${release.version} is out")
                .setContentText("Tap for what’s new, and the download.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build(),
        )
    }
}

/** The daily look, run by Android's job scheduler when there is a network. */
class UpdateCheckJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            try {
                UpdateNotices.check(applicationContext)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    /** Stopped early (the network went): try again later. */
    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
