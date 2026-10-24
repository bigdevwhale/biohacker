package app.biohacker.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.biohacker.MainActivity
import app.biohacker.R
import app.biohacker.container
import app.biohacker.domain.date
import app.biohacker.localized
import app.biohacker.ru
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The only reminder: once a day at the chosen time, if today's readings aren't in yet, the lab asks for
 * them and names what is on trial. One a day, never more.
 */
object Bell {
    private const val CHANNEL = "readings"
    private const val WORK = "bell"

    fun channel(c: Context) {
        val ch = NotificationChannel(CHANNEL, c.localized().let { if (it.ru) "Показания дня" else "Daily readings" }, NotificationManager.IMPORTANCE_DEFAULT)
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** The next ring: today at [minute] if that's still ahead, otherwise tomorrow. */
    fun due(minute: Int, now: Long, zone: ZoneId): Long? {
        if (minute < 0) return null
        fun at(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli() + minute * 60_000L
        val today = now.date(zone)
        return at(today).takeIf { it > now + 60_000L } ?: at(today.plusDays(1))
    }

    suspend fun plan(c: Context) {
        val app = c.applicationContext
        val st = app.container.settings.now()
        val wm = WorkManager.getInstance(app)
        val now = System.currentTimeMillis()
        val t = if (st.onboarded) due(st.bell, now, ZoneId.systemDefault()) else null
        if (t == null) { wm.cancelUniqueWork(WORK); return }
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<BellWorker>().setInitialDelay(t - now, TimeUnit.MILLISECONDS).build())
    }

    suspend fun ring(c: Context) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val k = c.container
        val s = k.repo.refresh()
        val today = k.repo.today()
        if (s.reading(today) != null) return
        val ru = c.localized().ru
        val names = s.running.mapNotNull { k.catalog[it.mod]?.name(ru) }
        val text = if (names.isEmpty()) (if (ru) "Пять приборов, десять секунд" else "Five gauges, ten seconds")
            else (if (ru) "На испытании: " else "On trial: ") + names.joinToString(", ")
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_lab)
            .setContentTitle(if (ru) "Время снять показания" else "Time for today's readings")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(1, n)
    }
}

class BellWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Bell.ring(applicationContext)
        Bell.plan(applicationContext)
        return Result.success()
    }
}
