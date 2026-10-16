package tv.own.owntv.mobile.ui.player

import tv.own.owntv.mobile.ui.stage.stageGlass
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.produceState
import org.koin.compose.koinInject
import tv.own.owntv.core.live.ProviderTags
import tv.own.owntv.core.parser.XtEpgEntry
import tv.own.owntv.mobile.ui.components.rememberClockTick
import tv.own.owntv.mobile.ui.screens.home.formatCount
import tv.own.owntv.mobile.ui.screens.home.formatYear
import tv.own.owntv.mobile.ui.screens.live.LiveChannelRow
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import tv.own.owntv.mobile.ui.stage.StageSheetHeader
import tv.own.owntv.mobile.ui.stage.stageSelectedBar
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.mobile.ui.screens.live.DirectTune
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileSlider
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileListRow
import tv.own.owntv.mobile.ui.components.SheetScroll
import tv.own.owntv.mobile.ui.components.sheetListHeight
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.player.PlaybackEngine
import tv.own.owntv.player.StreamInfoRow
import tv.own.owntv.player.ZoomMode
import tv.own.owntv.player.displayText
import tv.own.owntv.player.titleRes

/** Every picker the player's tool bar can open, chosen by [sheet]. */
@Composable
fun PlayerSheetHost(
    sheet: PlayerSheet,
    /**
     * The engine holding the stream. Every sheet here is about what is playing — its audio and
     * subtitle tracks, its volume, its zoom, its delays — so on a live channel running on ExoPlayer
     * they must come from that engine, not from a stopped mpv with no tracks to list.
     */
    player: PlaybackEngine,
    channels: List<ChannelEntity>,
    brightness: Float,
    onBrightness: (Float) -> Unit,
    onPickChannel: (ChannelEntity) -> Unit,
    /** Opens another sheet from inside this one — the subtitle sheet's way to the search. */
    onOpenSheet: (PlayerSheet) -> Unit,
    /** True for a film or an episode: only those can have a subtitle searched or attached. */
    canAddSubtitles: Boolean,
    /** Opens the phone's document picker for a subtitle file already on the device. */
    onPickLocalSubtitle: () -> Unit,
    /** Tunes a typed channel number, or null when the user has channel numbers turned off. */
    onTuneToNumber: (suspend (Int) -> DirectTune)?,
    /** "Go back to…" — the offsets on offer, and what to do with the chosen one. */
    catchup: CatchupOptions?,
    onDismiss: () -> Unit,
    /** When the programme on air ends, for the sleep timer's "End of programme"; null offers no such row. */
    programmeEndMs: Long? = null,
    /** The category the channel list shows, for its header (M3-10); null names it "Channels". */
    channelsTitle: String? = null,
) {
    when (sheet) {
        PlayerSheet.VOLUME -> VolumeSheet(player, onDismiss)
        PlayerSheet.BRIGHTNESS -> BrightnessSheet(brightness, onBrightness, onDismiss)
        PlayerSheet.SUBTITLES -> SubtitleSheet(
            player = player,
            canAddSubtitles = canAddSubtitles,
            onSearch = { onOpenSheet(PlayerSheet.SUBTITLE_SEARCH) },
            onPickLocalSubtitle = onPickLocalSubtitle,
            onDismiss = onDismiss,
        )
        PlayerSheet.SUBTITLE_SEARCH -> SubtitleSearchSheet(onDismiss)
        PlayerSheet.AUDIO -> AudioSheet(player, onDismiss)
        PlayerSheet.ASPECT -> AspectSheet(player, onDismiss)
        PlayerSheet.QUALITY -> QualitySheet(player, onDismiss)
        PlayerSheet.SPEED -> SpeedSheet(player, onDismiss)
        PlayerSheet.INFO -> StreamInfoSheet(player, onDismiss)
        PlayerSheet.CHANNELS -> ChannelSheet(channelsTitle, channels, onTuneToNumber, onPickChannel, onDismiss)
        PlayerSheet.CATCHUP -> catchup?.let { CatchupSheet(it, onDismiss) } ?: onDismiss()
        PlayerSheet.SLEEP_TIMER -> SleepTimerSheet(programmeEndMs = programmeEndMs, onDismiss = onDismiss)
    }
}

/** 0–150%: above 100 is the boost, the same range the television offers. */
@Composable
private fun VolumeSheet(player: PlaybackEngine, onDismiss: () -> Unit) {
    val volume by player.volume.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_volume)) {
        SliderRow(
            value = volume / 150f,
            label = stringResource(R.string.player_percent, volume),
            onChange = { player.adjustVolumeByUser((it * 150).toInt() - player.volume.value) },
        )
        // The two-finger tap mutes as well; this row is the button every gesture must have.
        MobileListRow(
            title = stringResource(if (volume > 0) R.string.player_mute else R.string.player_unmute),
            onClick = { player.toggleMute() },
        )
    }
}

@Composable
private fun BrightnessSheet(brightness: Float, onBrightness: (Float) -> Unit, onDismiss: () -> Unit) {
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_brightness)) {
        SliderRow(
            value = brightness,
            label = stringResource(R.string.player_percent, (brightness * 100).toInt()),
            onChange = onBrightness,
        )
    }
}

@Composable
private fun SliderRow(value: Float, label: String, onChange: (Float) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapMedium),
    ) {
        MobileSlider(value = value, onValueChange = onChange, modifier = Modifier.weight(1f))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * The subtitle tracks, plus the two ways of getting one the stream does not carry, plus the timing
 * nudge for a subtitle that runs ahead of or behind the speech.
 *
 * A picture-based track (PGS, VOBSUB, DVB) is labelled as one, because choosing it hands playback to
 * the other engine for a second and the user should know why the picture blinked.
 */
@Composable
private fun SubtitleSheet(
    player: PlaybackEngine,
    canAddSubtitles: Boolean,
    onSearch: () -> Unit,
    onPickLocalSubtitle: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tracks = remember { player.textTracks() }
    val timing = remember { player.subtitleTimingAvailable() }
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_subtitles)) {
        SheetScroll {
            MobileListRow(
                title = stringResource(R.string.common_off),
                onClick = { player.disableSubtitles(); onDismiss() },
            )
            if (tracks.isEmpty()) {
                EmptyNote(stringResource(R.string.player_no_tracks))
            }
            tracks.forEach { track ->
                val label = track.displayLabel()
                MobileListRow(
                    title = if (track.image) stringResource(R.string.player_image_track, label) else label,
                    subtitle = if (track.selected) stringResource(R.string.common_on) else null,
                    onClick = { player.selectSubtitle(track.mpvId); onDismiss() },
                )
            }
            if (canAddSubtitles) {
                SectionLabel(stringResource(R.string.player_add_subtitles))
                MobileListRow(title = stringResource(R.string.player_search_subtitles), onClick = onSearch)
                MobileListRow(
                    title = stringResource(R.string.player_select_local_subtitle),
                    onClick = { onPickLocalSubtitle(); onDismiss() },
                )
            }
            if (timing) {
                SectionLabel(stringResource(R.string.player_subtitle_timing))
                SubtitleTimingRow(player)
            }
        }
    }
}

/**
 * Subtitle timing, adjusted while the film keeps playing behind the sheet — the only way to tell
 * whether it now matches the speech is to hear the speech.
 */
@Composable
private fun SubtitleTimingRow(player: PlaybackEngine) {
    val delay by player.subDelayMs.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxWidth().padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
    ) {
        Text(
            text = when {
                delay == 0 -> stringResource(R.string.player_subtitle_delay_zero)
                delay > 0 -> stringResource(R.string.player_subtitle_delay_positive, delay / 1000.0)
                else -> stringResource(R.string.player_subtitle_delay_negative, -delay / 1000.0)
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = when {
                delay > 0 -> stringResource(R.string.player_subtitles_later)
                delay < 0 -> stringResource(R.string.player_subtitles_earlier)
                else -> stringResource(R.string.player_no_offset)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.padding(top = MobileDimens.GapTiny),
            horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapTiny),
        ) {
            TextButton(onClick = { player.adjustSubtitleDelay(-500) }) {
                Text(stringResource(R.string.player_subtitle_delay_negative, 0.5))
            }
            TextButton(onClick = { player.adjustSubtitleDelay(-100) }) {
                Text(stringResource(R.string.player_subtitle_delay_negative, 0.1))
            }
            TextButton(onClick = { player.resetSubtitleDelay() }) {
                Text(stringResource(R.string.common_reset))
            }
            TextButton(onClick = { player.adjustSubtitleDelay(100) }) {
                Text(stringResource(R.string.player_subtitle_delay_positive, 0.1))
            }
            TextButton(onClick = { player.adjustSubtitleDelay(500) }) {
                Text(stringResource(R.string.player_subtitle_delay_positive, 0.5))
            }
        }
    }
}

/** 25 ms a step, the same as the setting: what this corrects lands in the tens of milliseconds, and
 *  50 could bracket it without ever hitting it. */
private const val AV_SYNC_STEP_MS = 25
private const val AV_SYNC_LIMIT_MS = 5_000

@Composable
private fun AudioSheet(player: PlaybackEngine, onDismiss: () -> Unit) {
    val tracks = remember { player.audioTracks() }
    val delay by player.audioDelayMs.collectAsStateWithLifecycle()
    val remembered by player.audioDelayRemembered.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_audio_track)) {
        SheetScroll {
            if (tracks.isEmpty()) EmptyNote(stringResource(R.string.player_no_tracks))
            tracks.forEach { track ->
                MobileListRow(
                    title = track.displayLabel(),
                    subtitle = if (track.selected) stringResource(R.string.common_on) else null,
                    onClick = { player.selectAudio(track.mpvId); onDismiss() },
                )
            }
            // Lip sync, for a badly muxed file where the voices arrive before or after the mouths.
            // Offered on everything the full-screen player plays, exactly as the television offers it.
            SectionLabel(stringResource(R.string.player_av_sync))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapTiny),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
            ) {
                TextButton(
                    onClick = { player.adjustAudioDelay(-AV_SYNC_STEP_MS) },
                    enabled = delay > -AV_SYNC_LIMIT_MS,
                ) { Text(stringResource(R.string.common_minus)) }
                Text(
                    text = formatDelay(delay),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    onClick = { player.adjustAudioDelay(AV_SYNC_STEP_MS) },
                    enabled = delay < AV_SYNC_LIMIT_MS,
                ) { Text(stringResource(R.string.common_plus)) }
            }
            // The error belongs to the stream, not to the user: remembering it keeps the correction on
            // this film alone rather than carrying it onto everything watched afterwards.
            MobileListRow(
                title = stringResource(R.string.player_av_sync_remember),
                subtitle = if (remembered) stringResource(R.string.common_on) else null,
                onClick = { player.toggleRememberAudioDelay() },
            )
        }
    }
}

@Composable
private fun formatDelay(ms: Int): String = when {
    ms == 0 -> stringResource(R.string.player_delay_zero)
    ms > 0 -> stringResource(R.string.player_delay_positive, ms)
    else -> stringResource(R.string.player_delay_negative, ms)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = MobileDimens.ScreenPaddingH,
            top = MobileDimens.GapSmall,
            bottom = MobileDimens.GapTiny,
        ),
    )
}

@Composable
private fun AspectSheet(player: PlaybackEngine, onDismiss: () -> Unit) {
    val current by player.zoomMode.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_aspect)) {
        // Six modes: one row more than a landscape sheet fits, so "Force 4:3" was off the end of it.
        SheetScroll {
            ZoomMode.entries.forEach { mode ->
                MobileListRow(
                    title = stringResource(mode.labelRes),
                    subtitle = if (mode == current) stringResource(R.string.common_on) else null,
                    onClick = { player.setZoomModeByUser(mode); onDismiss() },
                )
            }
        }
    }
}

/** N11 — Auto (Settings → Maximum video quality), then every height this stream offers, highest first. */
@Composable
private fun QualitySheet(player: PlaybackEngine, onDismiss: () -> Unit) {
    val heights by player.videoQualities.collectAsStateWithLifecycle()
    val current by player.videoQualityPick.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_quality)) {
        SheetScroll {
            (listOf<Int?>(null) + heights).forEach { height ->
                MobileListRow(
                    title = if (height == null) stringResource(R.string.settings_auto) else stringResource(R.string.settings_video_quality_lines, height),
                    subtitle = if (height == current) stringResource(R.string.common_on) else null,
                    onClick = { player.selectVideoQuality(height); onDismiss() },
                )
            }
        }
    }
}

private val SPEEDS = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0)

@Composable
private fun SpeedSheet(player: PlaybackEngine, onDismiss: () -> Unit) {
    val current by player.speed.collectAsStateWithLifecycle()
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_tool_speed)) {
        val locale = LocalConfiguration.current.locales[0]
        // Seven speeds, and 2.0x is the last of them — the one a landscape sheet dropped.
        SheetScroll {
            SPEEDS.forEach { speed ->
                val number = remember(speed, locale) {
                    java.text.NumberFormat.getNumberInstance(locale).apply {
                        minimumFractionDigits = 1
                        maximumFractionDigits = 2
                    }.format(speed)
                }
                MobileListRow(
                    title = if (speed == 1.0) {
                        stringResource(R.string.player_speed_normal)
                    } else {
                        stringResource(R.string.player_speed, number)
                    },
                    subtitle = if (speed == current) stringResource(R.string.common_on) else null,
                    onClick = { player.setSpeed(speed); onDismiss() },
                )
            }
        }
    }
}

/** The technical readout, rendered from core's own table so it matches the television's line for line. */
@Composable
private fun StreamInfoSheet(player: PlaybackEngine, onDismiss: () -> Unit) {
    var rows by remember { mutableStateOf(emptyList<StreamInfoRow>()) }
    LaunchedEffect(player) { rows = player.streamInfo() }
    val res = LocalResources.current
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.player_stream_info)) {
        // Scrollable, not merely capped: this is the longest table in the app — eleven rows, one of
        // them a whole stream URL that wraps to three lines — and half a screen never holds it.
        Column(
            Modifier
                .heightIn(max = sheetListHeight())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MobileDimens.ScreenPaddingH),
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        text = stringResource(row.label.titleRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.38f),
                    )
                    Text(
                        text = row.value.displayText(res),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.62f),
                    )
                }
            }
        }
    }
}

/**
 * The other channels in this folder — the swipe-up list, and its button on the tool bar.
 *
 * With channel numbers turned on it also takes one typed in: a remote control has a keypad, and a
 * phone's equivalent is a field. Every way it can fail says so, because a number that quietly does
 * nothing is indistinguishable from a field that is not working.
 */
@Composable
private fun ChannelSheet(
    title: String?,
    channels: List<ChannelEntity>,
    onTuneToNumber: (suspend (Int) -> DirectTune)?,
    onPick: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    // M3-10 (the TV's Phase D): the category · count · Live TV, "Now playing · 256 · name", then Live TV's
    // own rows with what is on now; the playing channel carries the accent bar.
    val tuner: LiveTuner = koinInject()
    val playing by tuner.channel.collectAsStateWithLifecycle()
    val now by produceState(emptyMap<Long, XtEpgEntry>(), channels) { value = tuner.nowProgrammes(channels) }
    val nowMs by rememberClockTick()
    val sep = stringResource(R.string.content_epg_bits_separator)
    val heading = title?.let { ProviderTags.parse(it).name } ?: stringResource(R.string.content_channel_overlay_title)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val body: @Composable (Boolean) -> Unit = { side -> ChannelSheetBody(heading, channels, playing, now, nowMs, sep, side, onTuneToNumber, onPick, onDismiss) }
    // Held sideways (M3-10): a glass panel down the left of the picture, over a wash from that side, instead of
    // a sheet that would cover the whole short screen.
    if (configuration.screenWidthDp > configuration.screenHeightDp) {
        androidx.activity.compose.BackHandler(onBack = onDismiss)
        // The panel sits at the start edge, so in right-to-left the wash comes from the right.
        val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
        val wash = if (rtl) arrayOf(0.3f to Color.Transparent, 1f to Color(0xBF020506)) else arrayOf(0f to Color(0xBF020506), 0.7f to Color.Transparent)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(*wash))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        ) {
            Column(
                Modifier
                    .padding(14.dp)
                    .width(380.dp)
                    .fillMaxHeight()
                    .stageGlass(RoundedCornerShape(24.dp))
                    // The panel's own taps stay in it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) { body(true) }
        }
        return
    }
    MobileBottomSheet(onDismissRequest = onDismiss, title = heading, stage = true) { body(false) }
}

@Composable
private fun ChannelSheetBody(
    heading: String,
    channels: List<ChannelEntity>,
    playing: ChannelEntity?,
    now: Map<Long, XtEpgEntry>,
    nowMs: Long,
    sep: String,
    side: Boolean,
    onTuneToNumber: (suspend (Int) -> DirectTune)?,
    onPick: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    run {
        StageSheetHeader(
            title = heading,
            extra = formatCount(channels.size) + sep + stringResource(R.string.common_nav_live_tv),
            subtitle = playing?.let { ch ->
                listOfNotNull(
                    stringResource(R.string.shell_now_playing),
                    ch.number?.takeIf { it > 0 }?.let(::formatYear),
                    ProviderTags.parse(ch.name).name,
                ).joinToString(sep)
            },
        )
        if (onTuneToNumber != null) DirectTuneField(onTuneToNumber, onDismiss)
        LazyColumn(if (side) Modifier.fillMaxHeight() else Modifier.heightIn(max = sheetListHeight())) {
            items(channels, key = { it.id }) { channel ->
                val current = channel.id == playing?.id
                Box(if (current) Modifier.stageSelectedBar() else Modifier) {
                    LiveChannelRow(
                        channel = channel,
                        showNumber = true,
                        now = now[channel.id],
                        nowMs = nowMs,
                        mark = null,
                        isFavorite = false,
                        onClick = { onPick(channel); onDismiss() },
                        onLongClick = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun DirectTuneField(onTune: suspend (Int) -> DirectTune, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val notFound = stringResource(R.string.player_channel_not_found)
    val failed = stringResource(R.string.player_tune_failed)
    val ambiguous = stringResource(R.string.player_multiple_channels)

    fun submit() {
        val number = text.toIntOrNull() ?: return
        scope.launch {
            when (onTune(number)) {
                is DirectTune.Found -> onDismiss()
                DirectTune.NotFound -> message = notFound
                is DirectTune.Ambiguous -> message = ambiguous
                DirectTune.Failed -> message = failed
            }
        }
    }

    val palette = tv.own.owntv.mobile.ui.stage.LocalStagePalette.current
    Column(Modifier.padding(start = 6.dp, end = 6.dp, bottom = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // `.srch` (36 dp): digits only, filtered rather than merely hinted — a pasted "BBC 101" would
            // otherwise sit there looking like a number that will not tune.
            androidx.compose.foundation.text.BasicTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(6); message = null },
                singleLine = true,
                textStyle = tv.own.owntv.mobile.ui.stage.stageText(12.5f, 600).copy(color = palette.text),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(tv.own.owntv.mobile.ui.stage.stageAccent.accent),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions { submit() },
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(tv.own.owntv.mobile.ui.stage.StageRadii.Search))
                            .background(palette.controlFill)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                stringResource(R.string.player_channel_number_entry),
                                style = tv.own.owntv.mobile.ui.stage.stageText(12.5f, 500),
                                color = palette.dim,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        field()
                    }
                },
            )
            tv.own.owntv.mobile.ui.stage.StageButton(
                onClick = { submit() },
                text = stringResource(R.string.common_ok),
                small = true,
                enabled = text.isNotEmpty(),
            )
        }
        message?.let {
            Text(
                text = it,
                style = tv.own.owntv.mobile.ui.stage.stageText(11.5f, 600),
                color = palette.danger,
                modifier = Modifier.padding(start = 8.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(MobileDimens.ScreenPaddingH),
    )
}
