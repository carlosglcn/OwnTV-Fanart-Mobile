package tv.own.owntv.mobile.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.koin.compose.koinInject
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.live.EpgNowNext
import tv.own.owntv.core.live.NowArt
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.ProgrammeArt

/**
 * glcn: the zapping banner — a few seconds after a channel change, along the bottom of the picture:
 * the programme's picture, the channel, what is on with how far through it is, and what comes next.
 */
@Composable
internal fun ZapBanner(
    visible: Boolean,
    channel: ChannelEntity?,
    epg: EpgNowNext?,
    showNumber: Boolean,
    modifier: Modifier = Modifier,
    nowArt: NowArt = koinInject(),
) {
    AnimatedVisibility(
        visible = visible && channel != null,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 },
        modifier = modifier,
    ) {
        val ch = channel ?: return@AnimatedVisibility
        val art by produceState<String?>(null, ch.id, epg?.now?.startMs) {
            value = runCatching { nowArt.now(ch)?.imageUrl }.getOrNull()?.takeIf { it.isNotBlank() }
        }
        val context = LocalContext.current
        val fmt = remember { android.text.format.DateFormat.getTimeFormat(context) }
        val nowMs = remember(ch.id, epg) { System.currentTimeMillis() }
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xE6101418))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(width = 128.dp, height = 72.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E252B)),
                contentAlignment = Alignment.Center,
            ) {
                val url = art
                if (url != null) {
                    ProgrammeArt(url = url, modifier = Modifier.size(width = 128.dp, height = 72.dp))
                } else {
                    AsyncImage(model = ch.displayLogoUrl, contentDescription = null, modifier = Modifier.size(56.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (art != null) {
                        AsyncImage(model = ch.displayLogoUrl, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        listOfNotNull(ch.number?.takeIf { showNumber }?.toString(), ch.name).joinToString("  "),
                        color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                val now = epg?.now
                if (now != null) {
                    Text(now.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val span = (now.stopMs - now.startMs).toFloat()
                    if (span > 0f) {
                        val p = ((nowMs - now.startMs) / span).coerceIn(0f, 1f)
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.18f))) {
                            Box(Modifier.fillMaxWidth(p).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF4DA3FF)))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                epg?.next?.let { next ->
                    Text(
                        stringResource(R.string.content_live_next) + " · " + fmt.format(java.util.Date(next.startMs)) + "  " + next.title,
                        color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
