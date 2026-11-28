package tv.own.owntv.mobile.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Whether there is room to show a list and what it opens side by side.
 *
 * 840dp is Material's medium/expanded boundary — the width at which a list pane and a detail pane
 * both stay usable rather than becoming two cramped columns. The rail already arrives at 600dp,
 * which is the compact/medium boundary and a separate decision.
 *
 * The width is the *window*, not the display: in split screen and on a folded device it is
 * already the smaller number, which is what makes step 7 of the phase work without a device check.
 */
@Composable
@ReadOnlyComposable
fun isExpandedWidth(): Boolean = windowWidthDp() >= EXPANDED_DP

/**
 * The window's width and height in dp, converted with the display's own density rather than the
 * composition's: UI zoom scales `LocalDensity`, and a size class must not change with the zoom.
 */
@Composable
@ReadOnlyComposable
fun windowWidthDp(): Int = LocalWindowInfo.current.containerSize.width * 160 / LocalConfiguration.current.densityDpi

@Composable
@ReadOnlyComposable
fun windowHeightDp(): Int = LocalWindowInfo.current.containerSize.height * 160 / LocalConfiguration.current.densityDpi

private const val EXPANDED_DP = 840
