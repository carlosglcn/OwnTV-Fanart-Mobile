package tv.own.owntv.mobile.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.database.entity.EpgProgrammeEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.home.FeaturedItem
import tv.own.owntv.core.home.TonightItem
import tv.own.owntv.core.live.SportsNow
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.theme.MobileDimens
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.components.ProgrammeArt
import tv.own.owntv.mobile.ui.components.SectionHeader

/** glcn: Home's renewed pieces on the phone — greeting, featured carousel, picture cards, Tonight. */

private val Red = Color(0xFFE5243B)

@Composable
internal fun LiveBadgeSmall(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.glcn_live_badge),
        color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(RoundedCornerShape(4.dp)).background(Red).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** "Buenas noches, Carlos" over the date, with the weather chip beside it. */
@Composable
internal fun HomeGreetingHeader(name: String?, trailing: @Composable () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    val hour = remember(now) { java.util.Calendar.getInstance().apply { timeInMillis = now }.get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(
        when (hour) {
            in 6..13 -> R.string.glcn_greeting_morning
            in 14..20 -> R.string.glcn_greeting_afternoon
            else -> R.string.glcn_greeting_evening
        },
    )
    val date = remember(now) { java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.getDefault()).format(java.util.Date(now)) }
    Row(
        Modifier.fillMaxWidth().padding(start = MobileDimens.ScreenPaddingH, top = MobileDimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                listOfNotNull(greeting, name?.takeIf { it.isNotBlank() }).joinToString(", "),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                date.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

/**
 * A live programme as a picture card: the programme's fanart on top (the channel logo when the guide
 * has none, or only a blank placeholder), and under it — never over it, where the picture's own
 * lettering would fight it — the title, the channel and time, and the progress bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ArtLiveCardMobile(
    channel: ChannelEntity,
    programme: EpgProgrammeEntity?,
    now: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    width: Dp = 260.dp,
    startLabel: String? = null,
    bell: Boolean = false,
) {
    val rawArt = programme?.imageUrl?.takeIf { it.isNotBlank() }
    val art = rawArt?.takeUnless { tv.own.owntv.mobile.ui.components.rememberPlainArt(it) }
    Column(
        Modifier
            .width(width)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            if (art != null) {
                ProgrammeArt(url = art, modifier = Modifier.fillMaxSize())
                AsyncImage(
                    model = channel.displayLogoUrl, contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(3.dp),
                )
            } else {
                AsyncImage(
                    model = channel.displayLogoUrl, contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(80.dp),
                )
            }
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (programme != null && startLabel == null && SportsNow.isLive(programme)) LiveBadgeSmall()
                if (bell) {
                    Box(Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(MobileIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            if (programme != null && startLabel == null) {
                val span = (programme.stopMs - programme.startMs).coerceAtLeast(1L)
                val p = ((now - programme.startMs).toFloat() / span).coerceIn(0f, 1f)
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.Black.copy(alpha = 0.35f))) {
                    Box(Modifier.fillMaxWidth(p).height(3.dp).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                programme?.title?.let(SportsNow::displayTitle) ?: channel.name,
                color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val line = when {
                startLabel != null -> "$startLabel · ${channel.name}"
                else -> channel.name
            }
            Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** "Tonight": tap sets or clears a reminder, long-press opens the channel. */
@Composable
internal fun TonightRowMobile(
    items: List<TonightItem>,
    reminded: (TonightItem) -> Boolean,
    onToggleReminder: (TonightItem) -> Unit,
    onOpenChannel: (Long) -> Unit,
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val fmt = remember { android.text.format.DateFormat.getTimeFormat(context) }
    Column {
        SectionHeader(title = stringResource(R.string.glcn_tonight))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MobileDimens.GapSmall),
            contentPadding = PaddingValues(horizontal = MobileDimens.ScreenPaddingH),
        ) {
            items(items.size, key = { "${items[it].channel.id}:${items[it].programme.startMs}" }) { i ->
                val item = items[i]
                ArtLiveCardMobile(
                    channel = item.channel,
                    programme = item.programme,
                    now = 0L,
                    startLabel = fmt.format(java.util.Date(item.programme.startMs)),
                    bell = reminded(item),
                    onClick = { onToggleReminder(item) },
                    onLongClick = { onOpenChannel(item.channel.id) },
                )
            }
        }
    }
}

/** The featured carousel: a big card per slide, turning by itself every few seconds. */
@Composable
internal fun FeaturedCarousel(
    allItems: List<FeaturedItem>,
    onPage: (FeaturedItem, String) -> Unit,
    onOpenChannel: (Long) -> Unit,
    onOpenMovie: (Long) -> Unit,
    onOpenSeries: (Long) -> Unit,
) {
    // glcn: only slides whose picture can fill the card (see rememberBillboardSlides).
    val slides = tv.own.owntv.mobile.ui.components.rememberBillboardSlides(allItems)
    if (slides.isEmpty()) return
    val items = slides.map { it.first }
    val pager = rememberPagerState { items.size }
    LaunchedEffect(pager.currentPage, items) {
        slides.getOrNull(pager.currentPage)?.let { (it0, url) -> onPage(it0, url) }
        if (items.size < 2) return@LaunchedEffect
        delay(7_000)
        runCatching { pager.animateScrollToPage((pager.currentPage + 1) % items.size) }
    }
    val context = LocalContext.current
    val fmt = remember { android.text.format.DateFormat.getTimeFormat(context) }
    Column(Modifier.padding(top = MobileDimens.GapSmall)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = MobileDimens.ScreenPaddingH),
            pageSpacing = MobileDimens.GapSmall,
        ) { page ->
            val item = items[page]
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        when (item) {
                            is FeaturedItem.Live -> onOpenChannel(item.channel.id)
                            is FeaturedItem.Movie -> onOpenMovie(item.movie.id)
                            is FeaturedItem.Series -> onOpenSeries(item.series.id)
                        }
                    },
            ) {
                AsyncImage(model = slides[page].second, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.25f to Color.Transparent, 0.6f to Color.Black.copy(alpha = 0.75f), 1f to Color.Black.copy(alpha = 0.97f))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (item) {
                            is FeaturedItem.Live -> {
                                if (item.isLiveEvent) LiveBadgeSmall()
                                AsyncImage(model = item.channel.displayLogoUrl, contentDescription = null, modifier = Modifier.size(22.dp))
                                Text(item.channel.name, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            is FeaturedItem.Movie -> Text(stringResource(R.string.glcn_recent_movies).uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            is FeaturedItem.Series -> Text(stringResource(R.string.glcn_recent_series).uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        when (item) {
                            is FeaturedItem.Live -> SportsNow.displayTitle(item.programme.title)
                            is FeaturedItem.Movie -> item.movie.name
                            is FeaturedItem.Series -> item.series.name
                        },
                        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    val line = when (item) {
                        is FeaturedItem.Live -> listOfNotNull(
                            fmt.format(java.util.Date(item.programme.startMs)) + " – " + fmt.format(java.util.Date(item.programme.stopMs)),
                            tv.own.owntv.mobile.ui.components.rememberTmdbRating(item.programme.title),
                        ).joinToString("  ·  ")
                        is FeaturedItem.Movie -> listOfNotNull(item.movie.year?.toString(), item.movie.rating?.let { "★ %.1f".format(it) }).joinToString("  ·  ")
                        is FeaturedItem.Series -> listOfNotNull(item.series.year?.toString(), item.series.rating?.let { "★ %.1f".format(it) }).joinToString("  ·  ")
                    }
                    if (line.isNotBlank()) Text(line, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        }
        if (items.size > 1) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                items.indices.forEach { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (i == pager.currentPage) 18.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                    )
                }
            }
        }
    }
}
