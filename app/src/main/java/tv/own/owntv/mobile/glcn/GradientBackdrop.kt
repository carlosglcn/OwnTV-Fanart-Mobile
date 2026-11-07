package tv.own.owntv.mobile.glcn

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import tv.own.owntv.core.theme.GLCN_MOTION_STILL
import tv.own.owntv.core.theme.GlcnStyle

/**
 * glcn: the backdrop of the gradient styles (the TV's), behind everything. Still styles draw their gradient once;
 * the others move three soft lights very slowly (a full round in about 40 s; still with animations off). The phone
 * has no focused picture, so "Fanart vivo" drifts the style's own colours.
 */
@Composable
fun GradientBackdrop(style: GlcnStyle, moving: Boolean, modifier: Modifier = Modifier) {
    val stops = style.gradient?.map { Color(it) } ?: return
    val drift = moving && style.motion != GLCN_MOTION_STILL
    val t = if (drift) {
        rememberInfiniteTransition(label = "aurora").animateFloat(
            0f, 1f, infiniteRepeatable(tween(DRIFT_MS, easing = LinearEasing), RepeatMode.Restart), label = "auroraT",
        ).value
    } else 0.18f
    Canvas(modifier) {
        if (style.motion == GLCN_MOTION_STILL) drawStill(stops)
        else drawLights(listOf(stops[0], stops[1], stops[2]), stops.last(), t)
    }
}

/** A still gradient, corner to corner, with a soft darkening at the foot so text there keeps its contrast. */
fun DrawScope.drawStill(stops: List<Color>) {
    drawRect(Brush.linearGradient(stops, Offset(0f, 0f), Offset(size.width, size.height)))
    drawRect(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f)))
}

/** Three large soft lights over a dark page, each on its own slow round (t in 0..1). */
fun DrawScope.drawLights(lights: List<Color>, page: Color, t: Float) {
    drawRect(page)
    val w = size.width
    val h = size.height
    val two = (2 * Math.PI).toFloat()
    val spots = listOf(
        Triple(0.18f, 0.12f, 0.0f),
        Triple(0.75f, 0.35f, 0.33f),
        Triple(0.3f, 0.7f, 0.66f),
    )
    lights.forEachIndexed { i, c ->
        val (bx, by, ph) = spots[i]
        val a = (t + ph) * two
        val cx = w * (bx + 0.08f * kotlin.math.cos(a))
        val cy = h * (by + 0.05f * kotlin.math.sin(a))
        val r = maxOf(w, h) * 0.42f
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.7f), c.copy(alpha = 0.25f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
    }
    drawRect(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.4f)))
}

private const val DRIFT_MS = 40_000
