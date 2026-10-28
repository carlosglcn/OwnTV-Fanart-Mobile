package tv.own.owntv.mobile.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.live.EpgNowNext
import tv.own.owntv.core.live.ProviderTags
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.ChannelLogoImage
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.stage.stageAccent
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * glcn: the quick zap panel — the channels of the list being watched, along the side of the picture (held
 * sideways) or rising from the bottom (upright), each with its logo, what is on with how far through it is,
 * and what follows. A search field on top finds any channel by name across the playlists. A tap changes
 * channel; a tap on the picture, the cross or Back closes it. It is always over video, so it is always dark;
 * the highlight is the user's accent.
 */
@Composable
internal fun QuickZapPanel(
    visible: Boolean,
    channels: List<ChannelEntity>,
    currentId: Long?,
    showNumber: Boolean,
    landscape: Boolean,
    loadNowNext: suspend (ChannelEntity) -> EpgNowNext?,
    search: suspend (String) -> List<ChannelEntity>,
    onPick: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = visible) { onDismiss() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(180)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            // The picture beside the panel: dimmed a little, and a tap on it closes.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
            val slide = tween<androidx.compose.ui.unit.IntOffset>(320, easing = FastOutSlowInEasing)
            androidx.compose.animation.AnimatedVisibility(
                visibleState = remember { androidx.compose.animation.core.MutableTransitionState(false) }.apply { targetState = true },
                enter = if (landscape) slideInHorizontally(slide) { it } else slideInVertically(slide) { it },
                exit = if (landscape) slideOutHorizontally(slide) { it } else slideOutVertically(slide) { it },
                modifier = Modifier.align(if (landscape) Alignment.CenterEnd else Alignment.BottomCenter),
            ) {
                PanelBody(
                    channels = channels,
                    currentId = currentId,
                    showNumber = showNumber,
                    loadNowNext = loadNowNext,
                    search = search,
                    onPick = onPick,
                    onDismiss = onDismiss,
                    modifier = if (landscape) {
                        Modifier.fillMaxHeight().width(400.dp).statusBarsPadding().navigationBarsPadding().padding(8.dp)
                    } else {
                        Modifier.fillMaxWidth().fillMaxHeight(0.62f).navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp)
                    },
                )
            }
        }
    }
}

@Composable
private fun PanelBody(
    channels: List<ChannelEntity>,
    currentId: Long?,
    showNumber: Boolean,
    loadNowNext: suspend (ChannelEntity) -> EpgNowNext?,
    search: suspend (String) -> List<ChannelEntity>,
    onPick: (ChannelEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    val accent = stageAccent.accent
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<ChannelEntity>?>(null) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) { found = null; return@LaunchedEffect }
        delay(250)
        found = runCatching { search(query) }.getOrDefault(emptyList())
    }
    val shown = found ?: channels
    val epg = remember { mutableStateMapOf<Long, EpgNowNext?>() }
    val startAt = remember(channels, currentId) { (channels.indexOfFirst { it.id == currentId } - 2).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startAt)
    LaunchedEffect(found) { if (found != null) listState.scrollToItem(0) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF00D1115))
            // Taps inside the panel stay inside it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus() },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.content_channel_overlay_title), style = stageText(18f, 800), color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(
                pluralStringResource(R.plurals.content_live_channel_count, shown.size, shown.size),
                style = stageText(12.5f, 500), color = Color.White.copy(alpha = 0.55f), modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) { Icon(MobileIcons.Close, stringResource(R.string.content_close), tint = Color.White) }
        }
        // The search: any channel by name, across the playlists.
        Row(
            Modifier
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(MobileIcons.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.glcn_quick_zap_search), style = stageText(14.5f, 500), color = Color.White.copy(alpha = 0.45f))
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = stageText(14.5f, 600).copy(color = Color.White),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    MobileIcons.Close, stringResource(R.string.content_close), tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp).clickable { query = "" },
                )
            }
        }
        if (found != null && shown.isEmpty()) {
            Text(
                stringResource(R.string.glcn_quick_zap_none), style = stageText(14f, 500), color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(shown, key = { it.id }) { ch ->
                LaunchedEffect(ch.id) { if (ch.id !in epg) epg[ch.id] = runCatching { loadNowNext(ch) }.getOrNull() }
                QuickZapRow(
                    channel = ch,
                    epg = epg[ch.id],
                    playing = ch.id == currentId,
                    showNumber = showNumber,
                    onClick = { onPick(ch) },
                )
            }
        }
    }
}

@Composable
private fun QuickZapRow(channel: ChannelEntity, epg: EpgNowNext?, playing: Boolean, showNumber: Boolean, onClick: () -> Unit) {
    val accent = stageAccent.accent
    val context = LocalContext.current
    val fmt = remember { android.text.format.DateFormat.getTimeFormat(context) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (playing) accent.copy(alpha = 0.20f) else Color.Transparent)
            .then(
                if (playing) Modifier.drawBehind {
                    val w = 4.dp.toPx()
                    drawRect(accent, Offset(0f, size.height * 0.22f), Size(w, size.height * 0.56f))
                } else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(width = 62.dp, height = 44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            ChannelLogoImage(
                url = channel.displayLogoUrl,
                fallback = {
                    Text(
                        ProviderTags.parse(channel.name).name.take(3), style = stageText(13f, 800),
                        color = Color.White.copy(alpha = 0.7f), maxLines = 1,
                    )
                },
                modifier = Modifier.padding(4.dp).fillMaxSize(),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showNumber) channel.number?.let {
                    Text(it.toString(), style = stageText(12f, 700), color = if (playing) accent else Color.White.copy(alpha = 0.5f), maxLines = 1)
                }
                Text(
                    ProviderTags.displayName(channel.name), style = stageText(15f, if (playing) 800 else 700), color = Color.White,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            val now = epg?.now
            if (now != null) {
                Text(now.title, style = stageText(13f, 500), color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val span = (now.stopMs - now.startMs).coerceAtLeast(1L)
                val f = ((System.currentTimeMillis() - now.startMs).toFloat() / span).coerceIn(0f, 1f)
                Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.14f))) {
                    Box(Modifier.fillMaxWidth(f).height(3.dp).background(accent))
                }
            }
            epg?.next?.let { n ->
                Text(
                    stringResource(R.string.content_live_next) + "  " + fmt.format(java.util.Date(n.startMs)) + "  " + n.title,
                    style = stageText(11.5f, 500), color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
