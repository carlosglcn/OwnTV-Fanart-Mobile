package tv.own.owntv.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * glcn: a placeholder's soft sweep of light — the grey shapes a page shows while it loads, so a slow
 * first start reads as "coming" rather than "empty".
 */
fun Modifier.shimmer(corner: Dp = 12.dp, base: Color = Color(0x1FFFFFFF)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    drawBehind {
        val w = size.width
        val brush = Brush.linearGradient(
            listOf(base, base.copy(alpha = base.alpha * 2.2f), base),
            start = Offset(w * x - w * 0.5f, 0f),
            end = Offset(w * x, size.height),
        )
        drawRoundRect(brush, cornerRadius = CornerRadius(corner.toPx(), corner.toPx()))
    }
}
