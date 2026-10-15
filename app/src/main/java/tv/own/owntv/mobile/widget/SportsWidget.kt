package tv.own.owntv.mobile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.live.GuideReader
import tv.own.owntv.core.live.SportsNow
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.repository.activeSourceIds
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.MainActivity
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.playback.ReminderAlerts

/**
 * glcn: "Sports now" on the home screen — up to four channels showing sport, live events first.
 * Tapping one opens that channel; the header opens the app. Refreshed every half hour by the
 * system and whenever the app goes to the background.
 */
class SportsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context.applicationContext, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val ROWS = intArrayOf(R.id.glcn_widget_row1, R.id.glcn_widget_row2, R.id.glcn_widget_row3, R.id.glcn_widget_row4)

        /** Re-draw every placed widget (no-op when there is none). */
        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = runCatching { manager.getAppWidgetIds(ComponentName(app, SportsWidget::class.java)) }.getOrNull()
            if (ids == null || ids.isEmpty()) return
            scope.launch { runCatching { render(app, manager, ids) } }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val entries = runCatching { load() }.getOrDefault(emptyList())
            val fmt = android.text.format.DateFormat.getTimeFormat(context)
            val views = RemoteViews(context.packageName, R.layout.glcn_widget_sports)
            views.setTextViewText(
                R.id.glcn_widget_updated,
                context.getString(R.string.glcn_widget_updated, fmt.format(java.util.Date())),
            )
            val openApp = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.glcn_widget_header, openApp)
            views.setViewVisibility(R.id.glcn_widget_empty, if (entries.isEmpty()) View.VISIBLE else View.GONE)
            ROWS.forEachIndexed { i, rowId ->
                val e = entries.getOrNull(i)
                if (e == null) {
                    views.setViewVisibility(rowId, View.GONE)
                } else {
                    val p = e.programme
                    val live = if (SportsNow.isLive(p)) "● " else ""
                    views.setTextViewText(
                        rowId,
                        "$live${p.title}\n${e.channel.name} · ${fmt.format(java.util.Date(p.startMs))}–${fmt.format(java.util.Date(p.stopMs))}",
                    )
                    views.setViewVisibility(rowId, View.VISIBLE)
                    val open = Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra(ReminderAlerts.EXTRA_OPEN_CHANNEL, e.channel.id)
                    views.setOnClickPendingIntent(
                        rowId,
                        PendingIntent.getActivity(
                            context, 1000 + i, open,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
            }
            manager.updateAppWidget(ids, views)
        }

        private suspend fun load(): List<SportsNow.Entry> {
            val koin = GlobalContext.getOrNull() ?: return emptyList()
            val settings = koin.get<SettingsRepository>()
            val pid = settings.activeProfileId.first()
            if (pid < 0) return emptyList()
            val ids = activeSourceIds(settings, koin.get<SourceDao>(), pid, MediaType.LIVE)
            val cust = koin.get<CustomizationStore>().observe(pid, MediaType.LIVE).first()
            return SportsNow(koin.get<ChannelDao>(), koin.get<GuideReader>())
                .find(ids, cust, settings.epgOffsetMinutes.first())
                .take(ROWS.size)
        }
    }
}
