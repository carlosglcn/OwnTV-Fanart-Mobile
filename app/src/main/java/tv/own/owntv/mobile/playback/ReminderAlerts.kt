package tv.own.owntv.mobile.playback

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import tv.own.owntv.core.reminder.ReminderManager
import tv.own.owntv.mobile.MainActivity
import tv.own.owntv.mobile.R

/**
 * glcn: programme reminders on the phone. The television prompts on screen; a phone is usually in a
 * pocket, so each reminder whose moment comes also becomes a notification. Tapping it opens the app
 * on that channel. With the app open, the shell also asks (or switches straight away, per the
 * Programme reminders setting) — see MobileShell.
 */
object ReminderAlerts {

    private const val CHANNEL_ID = "glcn_reminders"
    const val EXTRA_OPEN_CHANNEL = "glcn_open_channel"
    const val EXTRA_REMINDER_ID = "glcn_reminder_id"

    /** A channel the user asked to open from a reminder notification; the shell navigates and clears it. */
    val openChannelRequested = MutableStateFlow<Long?>(null)

    private val notified = HashSet<Long>()

    fun start(context: Context, scope: CoroutineScope, manager: ReminderManager) {
        val app = context.applicationContext
        scope.launch {
            manager.due.collect { due ->
                val ids = due.map { it.id }.toSet()
                synchronized(notified) { notified.retainAll(ids) }
                for (r in due) {
                    val fresh = synchronized(notified) { notified.add(r.id) }
                    if (fresh) post(app, r.id, r.channelId, r.title, r.channelName)
                }
            }
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(tag(reminderId), 0)
        }
    }

    private fun tag(id: Long) = "reminder-$id"

    private fun post(context: Context, reminderId: Long, channelId: Long, title: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.glcn_reminder_channel), NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_CHANNEL, channelId)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
        val pending = PendingIntent.getActivity(
            context, reminderId.toInt(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.owntv_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.glcn_reminder_starts, channelName))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching { nm.notify(tag(reminderId), 0, notification) }
    }
}
