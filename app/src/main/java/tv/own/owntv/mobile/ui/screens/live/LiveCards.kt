package tv.own.owntv.mobile.ui.screens.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.ChannelLogoImage
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.theme.MobileDimens
import java.text.DateFormat
import java.util.Date

/*
 * glcn fork — Live TV as picture cards (the phone's version of the TV app's Cinematic layout).
 *
 * Each channel is a 16:9 card with the picture of what is on now (XMLTV <programme><icon>), LIVE,
 * the logo, the title, the channel, the time and the progress; under it, what comes next. One
 * column on a phone, more as the screen widens.
 */

private const val WINDOW_HOURS = 6L

@Composable
internal fun LiveCardsGrid(
    channels: LazyPagingItems<ChannelEntity>,
    gridState: LazyGridState,
    loadProgrammes: suspend (List<ChannelEntity>, Long, Long) -> Map<Long, List<EpgProgrammeEntity>>,
    providerNames: Map<Long, String>,
    onClick: (ChannelEntity) -> Unit,
    onLongClick: (ChannelEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var programmes by remember { mutableStateOf<Map<Long, List<EpgProgrammeEntity>>>(emptyMap()) }
    var loadedMinute by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val t = System.currentTimeMillis()
            delay(60_000 - (t % 60_000))
            now = System.currentTimeMillis()
        }
    }
    // One batched read per scroll settle (and on the minute) for the cards on screen and a few beyond.
    LaunchedEffect(gridState, channels) {
        combine(
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.index } }.distinctUntilChanged(),
            snapshotFlow { now },
        ) { indices, t -> indices to t }.collect { (indices, t) ->
            if (indices.isEmpty()) return@collect
            delay(120)
            val minute = t / 60_000
            val from = (indices.first() - 2).coerceAtLeast(0)
            val to = (indices.last() + 6).coerceAtMost(channels.itemCount - 1)
            val want = (from..to).mapNotNull { if (it < channels.itemCount) channels.peek(it) else null }
                .filter { loadedMinute[it.id] != minute }
            if (want.isEmpty()) return@collect
            val found = runCatching { loadProgrammes(want, t - 60_000, t + WINDOW_HOURS * 3_600_000) }.getOrNull()
                ?: return@collect
            programmes = programmes + want.associate { it.id to found[it.id].orEmpty() }
            loadedMinute = loadedMinute + want.associate { it.id to minute }
        }
    }

    val times = rememberCardTimeFormat()
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 300.dp),
        contentPadding = PaddingValues(MobileDimens.ScreenPaddingH),
        horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapMedium),
        verticalArrangement = Arrangement.spacedBy(MobileDimens.GapLarge),
        modifier = modifier.fillMaxSize(),
    ) {
        items(count = channels.itemCount, key = channels.itemKey { it.id }) { index ->
            val channel = channels[index]
            if (channel != null) {
                val rows = programmes[channel.id].orEmpty()
                val current = rows.firstOrNull { now in it.startMs until it.stopMs }
                val next = rows.firstOrNull { it.startMs >= (current?.stopMs ?: now) }
                LiveCard(
                    channel = channel,
                    programme = current,
                    next = next,
                    now = now,
                    providerName = providerNames[channel.sourceId],
                    times = times,
                    onClick = { onClick(channel) },
                    onLongClick = { onLongClick(channel) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LiveCard(
    channel: ChannelEntity,
    programme: EpgProgrammeEntity?,
    next: EpgProgrammeEntity?,
    now: Long,
    providerName: String?,
    times: DateFormat,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val separator = stringResource(R.string.content_epg_bits_separator)
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(MobileDimens.CardCorner))
                .background(Color(0xFF15191C))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        ) {
            val art = programme?.imageUrl
            if (!art.isNullOrBlank()) {
                tv.own.owntv.mobile.ui.components.ProgrammeArt(url = art, modifier = Modifier.fillMaxSize())
            } else {
                ChannelLogoImage(
                    url = channel.displayLogoUrl,
                    modifier = Modifier.align(Alignment.Center).size(96.dp),
                    fallback = {
                        Icon(MobileIcons.LiveTv, null, tint = Color(0xFF8A9590), modifier = Modifier.size(48.dp))
                    },
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.25f to Color.Transparent, 0.6f to Color.Black.copy(alpha = 0.7f), 1f to Color.Black.copy(alpha = 0.95f)),
                ),
            )
            // Logo, top left — on a dark plate so a white logo never vanishes on a bright picture.
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(MobileDimens.GapSmall)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(4.dp),
            ) {
                ChannelLogoImage(
                    url = channel.displayLogoUrl,
                    modifier = Modifier.size(width = 44.dp, height = 30.dp),
                    fallback = { Icon(MobileIcons.LiveTv, null, tint = Color.White, modifier = Modifier.size(22.dp)) },
                )
            }
            // glcn: only a programme broadcast live (a match, an event), not every channel on air.
            if (programme != null && tv.own.owntv.core.live.SportsNow.isLive(programme)) {
                LiveBadge(Modifier.align(Alignment.TopEnd).padding(MobileDimens.GapSmall))
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    programme?.title ?: channel.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        if (programme != null) channel.name else stringResource(R.string.content_no_epg),
                        programme?.let {
                            stringResource(R.string.content_live_time_range_plain, times.format(Date(it.startMs)), times.format(Date(it.stopMs)))
                        },
                    ).joinToString(separator),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD5DDDA), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (programme != null) {
                    val span = (programme.stopMs - programme.startMs).coerceAtLeast(1)
                    LinearProgressIndicator(
                        progress = { ((now - programme.startMs).toFloat() / span).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }
            }
        }
        // What follows, then the details the feed carries for what is on now.
        val details = programme?.details
        val bits = listOfNotNull(details?.year?.toString(), details?.rating) + details?.categoryList.orEmpty().take(2)
        if (next != null) {
            Text(
                stringResource(R.string.content_live_next_label, times.format(Date(next.startMs))) + separator + next.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = MobileDimens.GapSmall, start = 2.dp, end = 2.dp),
            )
        }
        val sub = (bits + listOfNotNull(providerName)).joinToString(separator)
        if (sub.isNotBlank()) {
            Text(
                sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFF4D4D)))
        Text(
            stringResource(R.string.player_live),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White,
        )
    }
}

@Composable
private fun rememberCardTimeFormat(): DateFormat {
    val locales = LocalConfiguration.current.locales
    return remember(locales) { DateFormat.getTimeInstance(DateFormat.SHORT) }
}

/** glcn: "Sports now" — every channel showing sport right now, live events first. */
@Composable
internal fun SportsNowSheet(
    load: suspend () -> List<tv.own.owntv.core.live.SportsNow.Entry>,
    onPick: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var entries by remember { mutableStateOf<List<tv.own.owntv.core.live.SportsNow.Entry>?>(null) }
    LaunchedEffect(Unit) { entries = runCatching { load() }.getOrDefault(emptyList()) }
    val times = rememberCardTimeFormat()
    val separator = stringResource(R.string.content_epg_bits_separator)
    tv.own.owntv.mobile.ui.components.MobileBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.glcn_sports_now),
    ) {
        val list = entries
        when {
            list == null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            list.isEmpty() -> Text(
                stringResource(R.string.glcn_sports_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(MobileDimens.ScreenPaddingH),
            )
            else -> androidx.compose.foundation.lazy.LazyColumn(
                Modifier.heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.7f).dp),
            ) {
                items(list.size, key = { list[it].channel.id }) { i ->
                    val e = list[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onPick(e.channel) })
                            .padding(horizontal = MobileDimens.ScreenPaddingH, vertical = MobileDimens.GapSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapMedium),
                    ) {
                        Box(
                            Modifier
                                .width(128.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF15191C)),
                        ) {
                            val art = e.programme.imageUrl
                            if (!art.isNullOrBlank()) {
                                tv.own.owntv.mobile.ui.components.ProgrammeArt(url = art, modifier = Modifier.fillMaxSize())
                            } else {
                                ChannelLogoImage(
                                    url = e.channel.displayLogoUrl,
                                    modifier = Modifier.align(Alignment.Center).size(56.dp),
                                    fallback = { Icon(MobileIcons.LiveTv, null) },
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.programme.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                e.channel.name + separator + stringResource(
                                    R.string.content_live_time_range_plain,
                                    times.format(Date(e.programme.startMs)), times.format(Date(e.programme.stopMs)),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
