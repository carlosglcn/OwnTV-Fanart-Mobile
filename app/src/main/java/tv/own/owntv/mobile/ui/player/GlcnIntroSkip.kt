package tv.own.owntv.mobile.ui.player

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import tv.own.owntv.mobile.R
import tv.own.owntv.player.PlaybackEngine

/**
 * glcn: "Skip intro" for series — the same rules as on the television, and the same memory, so a series
 * learned on one device's app is not relearned here: the first time the viewer jumps forward early in an
 * episode and keeps watching, that moment is remembered for the series; later episodes offer (or, with
 * Auto-skip on, make) the same jump. Before anything is learned the button jumps a typical intro.
 */
object GlcnIntroSkip {
    private const val DEFAULT_JUMP_MS = 80_000L
    private const val WINDOW_MS = 8 * 60_000L
    private const val SHOW_FROM_MS = 10_000L
    private const val SHOW_UNTIL_MS = 4 * 60_000L
    private const val JUMP_MS = 15_000L
    private const val SETTLE_MS = 12_000L

    private fun prefs(c: Context) = c.getSharedPreferences("glcn_intro", Context.MODE_PRIVATE)
    private fun key(series: String) = series.trim().lowercase()
    private fun learned(c: Context, series: String): Long = prefs(c).getLong(key(series), -1L)
    private fun learn(c: Context, series: String, endMs: Long) { prefs(c).edit().putLong(key(series), endMs).apply() }

    /** The button, bottom-right over the picture; lifted above the controls while they show. */
    @Composable
    fun Chip(engine: PlaybackEngine, enabled: Boolean, controlsVisible: Boolean, autoSkip: Boolean, modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val meta by engine.currentMeta.collectAsStateWithLifecycle()
        val series = meta.subtitle?.takeIf { enabled && meta.episodeNumber != null && it.isNotBlank() }
        val position by engine.position.collectAsStateWithLifecycle()
        var learnedEnd by remember(series) { mutableLongStateOf(series?.let { learned(context, it) } ?: -1L) }
        var autoDone by remember(meta) { mutableStateOf(false) }

        var last by remember(meta) { mutableLongStateOf(-1L) }
        var candidate by remember(meta) { mutableLongStateOf(-1L) }
        LaunchedEffect(position, series) {
            if (series == null) return@LaunchedEffect
            val prev = last
            last = position
            if (prev > 3_000L && position - prev > JUMP_MS && prev < WINDOW_MS && position < WINDOW_MS + DEFAULT_JUMP_MS) {
                candidate = position
            }
        }
        LaunchedEffect(candidate) {
            if (candidate < 0 || series == null) return@LaunchedEffect
            val at = candidate
            delay(SETTLE_MS)
            if (candidate == at && engine.position.value >= at) {
                learn(context, series, at)
                learnedEnd = at
            }
        }

        val inWindow = series != null && when {
            learnedEnd > 0 -> position in 2_000L until (learnedEnd - 3_000L)
            else -> position in SHOW_FROM_MS until SHOW_UNTIL_MS
        }
        LaunchedEffect(inWindow, autoSkip, learnedEnd) {
            if (autoSkip && inWindow && learnedEnd > 0 && !autoDone) {
                autoDone = true
                engine.seekBy(learnedEnd - engine.position.value)
            }
        }
        val visible = inWindow && !(autoSkip && learnedEnd > 0)
        val lift by animateDpAsState(if (controlsVisible) 132.dp else 36.dp, label = "introLift")
        Box(modifier.fillMaxSize()) {
            AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomEnd)) {
                Box(
                    Modifier
                        .padding(end = 20.dp, bottom = lift)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .clickable {
                            val end = learnedEnd
                            val pos = engine.position.value
                            engine.seekBy(if (end > pos) end - pos else DEFAULT_JUMP_MS)
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Text(stringResource(R.string.glcn_skip_intro), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
