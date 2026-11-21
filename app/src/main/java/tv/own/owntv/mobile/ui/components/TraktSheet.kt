package tv.own.owntv.mobile.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.own.owntv.core.trakt.Trakt
import tv.own.owntv.core.trakt.TraktSync
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.StageButton
import tv.own.owntv.mobile.ui.stage.StageButtonStyle
import tv.own.owntv.mobile.ui.stage.StageMenuItem
import tv.own.owntv.mobile.ui.stage.stageAccent
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * glcn: Trakt for a profile, as on the television. Signed out: the device sign-in — the code, a button that opens
 * trakt.tv/activate on this phone (the code is copied, ready to paste), and the sheet checks on its own until the
 * account says yes. Signed in: the account, "Sync now" and "Sign out".
 */
@Composable
fun TraktSheet(profileId: Long, onDismiss: () -> Unit) {
    val version by Trakt.version.collectAsState()
    val scope = rememberCoroutineScope()
    val palette = LocalStagePalette.current
    val context = LocalContext.current
    MobileBottomSheet(onDismissRequest = onDismiss, title = stringResource(R.string.glcn_trakt_title)) {
        version // read: a sign-in or sign-out redraws the sheet
        if (Trakt.connected(profileId)) {
            var syncing by remember { mutableStateOf(false) }
            Text(
                stringResource(R.string.glcn_trakt_connected_as, Trakt.userName(profileId) ?: "Trakt"),
                style = stageText(13.5f, 500), color = palette.body, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
            StageMenuItem(
                label = stringResource(if (syncing) R.string.glcn_trakt_syncing else R.string.glcn_trakt_sync_now),
                value = stringResource(R.string.glcn_trakt_sync_line),
                icon = MobileIcons.Refresh,
                enabled = !syncing,
                onClick = {
                    syncing = true
                    scope.launch { runCatching { TraktSync.refresh(profileId, force = true) }; syncing = false }
                },
            )
            StageMenuItem(label = stringResource(R.string.glcn_trakt_sign_out), danger = true, onClick = { Trakt.disconnect(profileId); onDismiss() })
            Notice()
            return@MobileBottomSheet
        }
        var code by remember { mutableStateOf<Trakt.DeviceCode?>(null) }
        var state by remember { mutableStateOf(SignIn.WAITING) }
        var attempt by remember { mutableIntStateOf(0) }
        LaunchedEffect(attempt) {
            state = SignIn.WAITING
            val c = Trakt.startDevice() ?: run { state = SignIn.FAILED; return@LaunchedEffect }
            code = c
            var interval = c.intervalS.coerceAtLeast(1) * 1000L
            val deadline = System.currentTimeMillis() + c.expiresInS * 1000L
            while (System.currentTimeMillis() < deadline) {
                delay(interval)
                when (Trakt.pollDevice(profileId, c)) {
                    Trakt.Poll.Pending -> Unit
                    Trakt.Poll.SlowDown -> interval += 1000L
                    Trakt.Poll.Done -> { state = SignIn.DONE; TraktSync.refreshLater(profileId); return@LaunchedEffect }
                    Trakt.Poll.Expired -> { state = SignIn.EXPIRED; return@LaunchedEffect }
                    Trakt.Poll.Denied -> { state = SignIn.DENIED; return@LaunchedEffect }
                    is Trakt.Poll.Failed -> Unit
                }
            }
            state = SignIn.EXPIRED
        }
        Text(
            stringResource(
                when (state) {
                    SignIn.WAITING -> R.string.glcn_trakt_sign_in_line
                    SignIn.DONE -> R.string.glcn_trakt_signed_in
                    SignIn.EXPIRED -> R.string.glcn_trakt_expired
                    SignIn.DENIED -> R.string.glcn_trakt_denied
                    SignIn.FAILED -> R.string.glcn_trakt_failed
                },
            ),
            style = stageText(13.5f, 500), color = palette.body, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
        val c = code
        if (state == SignIn.WAITING && c != null) {
            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.userCode, style = stageText(34f, 800), color = stageAccent.accent, textAlign = TextAlign.Center)
                Text(c.url.removePrefix("https://"), style = stageText(13f, 600), color = palette.muted, textAlign = TextAlign.Center)
                StageButton(
                    onClick = {
                        runCatching {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Trakt", c.userCode))
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(c.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    },
                    text = stringResource(R.string.glcn_trakt_open_activate),
                    icon = MobileIcons.OpenInNew,
                    style = StageButtonStyle.PRIMARY,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
            when (state) {
                SignIn.DONE -> StageButton(onClick = onDismiss, text = stringResource(R.string.glcn_trakt_close))
                SignIn.EXPIRED, SignIn.DENIED, SignIn.FAILED -> StageButton(onClick = { code = null; attempt++ }, text = stringResource(R.string.glcn_trakt_retry))
                SignIn.WAITING -> StageButton(onClick = onDismiss, text = stringResource(R.string.glcn_trakt_cancel))
            }
        }
        Notice()
    }
}

/** Trakt's required notice, small, at the foot of the sheet. */
@Composable
private fun Notice() {
    Text(
        stringResource(R.string.glcn_trakt_notice), style = stageText(11.5f, 500), color = LocalStagePalette.current.dim, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

private enum class SignIn { WAITING, DONE, EXPIRED, DENIED, FAILED }
