package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tv.own.owntv.mobile.ui.theme.MobileDimens

/** How much weight a button carries on the screen it sits on. */
enum class MobileButtonStyle { PRIMARY, SECONDARY, TEXT }

/**
 * A button at the accessibility floor of 48 dp, whatever the label.
 *
 * The three styles exist because a screen usually has one action worth pressing, one worth
 * offering, and one worth mentioning — Play, Download, Cancel.
 */
@Composable
fun MobileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MobileButtonStyle = MobileButtonStyle.PRIMARY,
    enabled: Boolean = true,
) {
    // Drawn as the Stage `.btn` (M9): primary = accent with its glow, secondary = ink, text = the small one.
    tv.own.owntv.mobile.ui.stage.StageButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = MobileDimens.TouchTarget),
        text = text,
        style = if (style == MobileButtonStyle.PRIMARY) tv.own.owntv.mobile.ui.stage.StageButtonStyle.PRIMARY else tv.own.owntv.mobile.ui.stage.StageButtonStyle.SECONDARY,
        small = style == MobileButtonStyle.TEXT,
        enabled = enabled,
    )
}
