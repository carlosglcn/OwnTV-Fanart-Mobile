package tv.own.owntv.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import tv.own.owntv.mobile.ui.stage.StageChevron
import tv.own.owntv.mobile.ui.stage.StageRowValue
import tv.own.owntv.mobile.ui.stage.StageSettingRow
import tv.own.owntv.mobile.ui.stage.StageSwitch

/**
 * One line of a settings page in the Stage look (`.srow`, M9): a switch, a value (accent, with › when it opens
 * something), or nothing. The settings pages that are not built from [SettingItem] rows draw with this, so
 * every page reads the same.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    checked: Boolean? = null,
    value: String? = null,
    leading: (@Composable () -> Unit)? = null,
    /** The 34 dp icon square, so the row lines up with the icon rows around it (#10). */
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    StageSettingRow(
        title = title,
        modifier = modifier.then(if (enabled) Modifier else Modifier.alpha(0.45f)),
        subtitle = subtitle,
        icon = icon,
        leading = leading,
        onClick = {
            if (!enabled) return@StageSettingRow
            // Tapping anywhere on a switch row toggles it; the switch itself is not a target.
            if (checked != null && onCheckedChange != null) onCheckedChange(!checked) else onClick()
        },
    ) {
        when {
            checked != null -> StageSwitch(checked)
            value != null || showChevron -> StageRowValue(value.orEmpty(), if (showChevron) StageChevron.FORWARD else StageChevron.DOWN)
        }
    }
}
