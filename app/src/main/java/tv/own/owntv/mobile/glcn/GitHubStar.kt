package tv.own.owntv.mobile.glcn

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.screens.settings.SheetText
import tv.own.owntv.mobile.ui.stage.StageButton
import tv.own.owntv.mobile.ui.stage.StageButtonStyle
import tv.own.owntv.mobile.ui.stage.StageSheetHeader

/**
 * glcn: the occasional "a star on GitHub?" prompt. It waits for a week and ten launches, comes back
 * at most once a month later, and never again after "Open GitHub" or "Already done". The answer
 * lives in the app's own preferences, so updates keep it; only uninstalling or clearing data resets it.
 */
object GitHubStar {
    const val URL = "https://github.com/carlosglcn/OwnTV-Fanart-Mobile"

    private const val FILE = "glcn_star"
    private const val KEY_DONE = "done"
    private const val KEY_LAUNCHES = "launches"
    private const val KEY_FIRST = "first_at"
    private const val KEY_LAST = "last_at"
    private const val KEY_SHOWN = "shown"

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val MIN_LAUNCHES = 10
    private const val MIN_AGE_MS = 7 * DAY_MS
    private const val AGAIN_AFTER_MS = 30 * DAY_MS
    private const val MAX_SHOWN = 2

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Counts this launch and says whether the prompt is due now. Call once per launch. */
    fun launchAndCheck(c: Context, now: Long = System.currentTimeMillis()): Boolean {
        val p = prefs(c)
        if (p.getBoolean(KEY_DONE, false)) return false
        val first = p.getLong(KEY_FIRST, 0L).takeIf { it > 0 } ?: now
        val launches = p.getInt(KEY_LAUNCHES, 0) + 1
        p.edit().putLong(KEY_FIRST, first).putInt(KEY_LAUNCHES, launches).apply()
        val shown = p.getInt(KEY_SHOWN, 0)
        val last = p.getLong(KEY_LAST, 0L)
        return shown < MAX_SHOWN &&
            launches >= MIN_LAUNCHES &&
            now - first >= MIN_AGE_MS &&
            (last == 0L || now - last >= AGAIN_AFTER_MS)
    }

    /** "Not now" (or the sheet swiped away): counts as one showing. */
    fun later(c: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(c)
        p.edit().putLong(KEY_LAST, now).putInt(KEY_SHOWN, p.getInt(KEY_SHOWN, 0) + 1).apply()
    }

    /** "Open GitHub" or "Already done": never asked again. */
    fun done(c: Context) {
        prefs(c).edit().putBoolean(KEY_DONE, true).apply()
    }

    fun open(c: Context) {
        runCatching {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
fun GitHubStarSheet(onClose: () -> Unit) {
    val context = LocalContext.current
    MobileBottomSheet(onDismissRequest = { GitHubStar.later(context); onClose() }) {
        StageSheetHeader(stringResource(R.string.glcn_star_title))
        SheetText(stringResource(R.string.glcn_star_body))
        Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StageButton(
                onClick = { GitHubStar.done(context); GitHubStar.open(context); onClose() },
                text = stringResource(R.string.glcn_star_open),
                icon = MobileIcons.Star,
                style = StageButtonStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StageButton(onClick = { GitHubStar.later(context); onClose() }, text = stringResource(R.string.glcn_star_later), modifier = Modifier.weight(1f))
                StageButton(onClick = { GitHubStar.done(context); onClose() }, text = stringResource(R.string.glcn_star_done), icon = MobileIcons.Check, modifier = Modifier.weight(1f))
            }
        }
    }
}
