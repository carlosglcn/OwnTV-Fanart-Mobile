package tv.own.owntv.mobile.ui.shell

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.ui.stage.PlaylistMark
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.StageMenuItem
import tv.own.owntv.mobile.ui.stage.StagePlaylistMark
import tv.own.owntv.mobile.ui.stage.StageSheetHeader
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * Show playlist, the television's sheet: All playlists, then each playlist with its mark
 * and its channel count ("55K channels"; Settings › Layout › Navigation › Show counts). The current
 * one is filled with a ✓ and carries no count, as on the television. `All playlists` is id -1, the value Settings stores.
 */
@Composable
fun PlaylistSheet(
    playlists: List<SourceEntity>,
    activeId: Long,
    names: List<String>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    activeSet: Set<Long> = emptySet(),
    onSelectMany: (Set<Long>) -> Unit = {},
) {
    val settings: SettingsRepository = koinInject()
    val channelDao: ChannelDao = koinInject()
    val showCounts by settings.sheetCounts.collectAsStateWithLifecycle(true)
    val counts by produceState(emptyMap<Long, Int>(), playlists) {
        value = playlists.associate { it.id to runCatching { channelDao.countForSourceOnce(it.id) }.getOrDefault(0) }
    }
    // glcn: what each playlist holds beyond channels (Emby: films and series), and when an Xtream line ends — read once
    // from the provider and kept, as the television does.
    val movieDao: tv.own.owntv.core.database.dao.MovieDao = koinInject()
    val seriesDao: tv.own.owntv.core.database.dao.SeriesDao = koinInject()
    val xtream: tv.own.owntv.core.parser.XtreamClient = koinInject()
    val vod by produceState(emptyMap<Long, Pair<Int, Int>>(), playlists) {
        value = playlists.associate { it.id to (runCatching { movieDao.countForSourceOnce(it.id) }.getOrDefault(0) to runCatching { seriesDao.countForSourceOnce(it.id) }.getOrDefault(0)) }
    }
    val expiries by produceState(playlists.associate { it.id to tv.own.owntv.core.repository.SourceGlcnPrefs.expiry(it.id) }, playlists) {
        val known = value.toMutableMap()
        playlists.filter { it.type == tv.own.owntv.core.model.SourceType.XTREAM && known[it.id] == null }.forEach { s ->
            runCatching { xtream.accountExpiryMs(s) }.getOrNull()?.let { ms ->
                tv.own.owntv.core.repository.SourceGlcnPrefs.setExpiry(s.id, ms)
                known[s.id] = ms
                value = known.toMap()
            }
        }
    }
    val disabled by tv.own.owntv.core.repository.SourceGlcnPrefs.disabled.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val compact = remember(locale) { CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT) }
    val title = stringResource(R.string.content_playlist_picker_title)

    // glcn: a tap ticks or unticks a playlist and the sheet stays open; the choice applies at once and
    // none ticked means all playlists.
    var picked by remember {
        mutableStateOf(if (activeSet.isNotEmpty()) activeSet else if (activeId > 0) setOf(activeId) else emptySet())
    }
    fun toggle(id: Long) {
        picked = if (id in picked) picked - id else picked + id
        onSelectMany(picked)
    }

    MobileBottomSheet(onDismissRequest = onDismiss, title = title) {
        StageSheetHeader(title = title, subtitle = stringResource(R.string.content_playlist_picker_subtitle))
        StageMenuItem(
            label = stringResource(R.string.content_all_playlists),
            onClick = { picked = emptySet(); onSelect(-1L); onDismiss() },
            icon = MobileIcons.Layers,
            filled = picked.isEmpty(),
            checked = picked.isEmpty(),
        )
        playlists.forEachIndexed { i, source ->
            val current = source.id in picked
            val count = counts[source.id]
            val off = source.id in disabled
            val (films, shows) = vod[source.id] ?: (0 to 0)
            // glcn: the count says what the playlist holds — its films and series when it has no channels (Emby).
            val holds = when {
                count != null && count > 0 -> pluralStringResource(R.plurals.sync_count_channels, count, compact.format(count))
                films > 0 || shows > 0 -> listOfNotNull(
                    films.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.glcn_deck_films_count, it, compact.format(it)) },
                    shows.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.content_vod_series_count, it, it) },
                ).joinToString(stringResource(R.string.content_epg_bits_separator))
                else -> null
            }
            val ends = expiries[source.id]?.let { ms ->
                val days = kotlin.math.floor((ms - System.currentTimeMillis()) / 86_400_000.0).toInt()
                when {
                    days < 0 -> stringResource(R.string.glcn_pl_expired)
                    days == 0 -> stringResource(R.string.glcn_pl_expires_today)
                    days <= EXPIRY_SOON_DAYS -> pluralStringResource(R.plurals.glcn_pl_expires_days, days, days)
                    else -> stringResource(R.string.settings_sources_expiry, java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, locale).format(java.util.Date(ms)))
                }
            }
            StageMenuItem(
                label = names[i],
                onClick = { toggle(source.id) },
                // glcn: a long press switches the playlist off (or back on) without going to Settings.
                onLongClick = { tv.own.owntv.core.repository.SourceGlcnPrefs.setDisabled(source.id, !off) },
                leading = { StagePlaylistMark(PlaylistMark.of(names[i], i)) },
                filled = current,
                checked = current,
                enabled = true,
                value = listOfNotNull(if (off) stringResource(R.string.glcn_pl_off) else null, ends).joinToString(stringResource(R.string.content_epg_bits_separator)).ifBlank { null },
                count = if (showCounts && !current) holds else null,
                modifier = if (off) Modifier.alpha(0.5f) else Modifier,
            )
        }
        Text(
            stringResource(R.string.content_playlist_marks_note) + " " + stringResource(R.string.glcn_pl_long_press_note),
            style = stageText(11.5f, 500),
            color = LocalStagePalette.current.dim,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp),
        )
    }
}

/** glcn: a line ending within this many days says so in days. */
private const val EXPIRY_SOON_DAYS = 15
