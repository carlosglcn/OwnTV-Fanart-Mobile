package tv.own.owntv.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.compose.koinInject
import tv.own.owntv.core.launcher.LauncherDeepLink
import tv.own.owntv.core.launcher.RemoteKeys
import tv.own.owntv.core.sync.local.AutoSyncManager
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.StageIcon
import tv.own.owntv.mobile.ui.stage.stageAccent
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * glcn: the phone as the paired TV's remote — the arrows and OK, Back, play/pause, channel and volume, and a field
 * whose words the TV searches for. Each key goes straight to the TV that took the last one, in the order pressed.
 */
@Composable
fun RemotePage(modifier: Modifier = Modifier) {
    val auto = koinInject<AutoSyncManager>()
    val paired by auto.devices.collectAsStateWithLifecycle(initialValue = emptyList())
    val palette = LocalStagePalette.current
    val accent = stageAccent
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val order = remember { Mutex() }
    var status by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    val failText = stringResource(R.string.glcn_tv_unreachable)
    fun send(link: LauncherDeepLink) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch {
            order.withLock {
                val r = auto.sendQuick(link.toUri().toString())
                failed = r.isFailure
                status = r.getOrNull()?.name
            }
        }
    }
    fun key(name: String) = send(LauncherDeepLink.RemoteKey(name))

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (paired.isEmpty()) {
            Text(stringResource(R.string.glcn_remote_none), style = stageText(14f, 500, lineHeight = 1.4f), color = palette.muted)
            return@Column
        }
        Text(
            if (failed) failText else status?.let { stringResource(R.string.glcn_remote_connected, it) } ?: paired.first().name,
            style = stageText(13f, 600), color = if (failed) palette.warn else palette.muted,
            modifier = Modifier.padding(bottom = 18.dp),
        )
        // The arrows round OK, as on the remote.
        Box(Modifier.size(260.dp).clip(CircleShape).background(palette.controlFill), contentAlignment = Alignment.Center) {
            RemoteButton(MobileIcons.KeyboardArrowUp, stringResource(R.string.glcn_remote_up), Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) { key(RemoteKeys.UP) }
            RemoteButton(MobileIcons.KeyboardArrowDown, stringResource(R.string.glcn_remote_down), Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) { key(RemoteKeys.DOWN) }
            RemoteButton(MobileIcons.KeyboardArrowRight, stringResource(R.string.glcn_remote_left), Modifier.align(Alignment.CenterStart).padding(start = 8.dp), rotation = 180f) { key(RemoteKeys.LEFT) }
            RemoteButton(MobileIcons.KeyboardArrowRight, stringResource(R.string.glcn_remote_right), Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { key(RemoteKeys.RIGHT) }
            Box(
                Modifier.size(100.dp).clip(CircleShape).background(accent.accent).clickable { key(RemoteKeys.OK) },
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.glcn_remote_ok), style = stageText(20f, 800), color = accent.onAccent) }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RemoteButton(MobileIcons.ArrowBack, stringResource(R.string.common_back), filled = true) { key(RemoteKeys.BACK) }
            RemoteButton(MobileIcons.PlayArrow, stringResource(R.string.glcn_remote_play_pause), filled = true) { key(RemoteKeys.PLAY_PAUSE) }
            RemoteButton(MobileIcons.Info, stringResource(R.string.glcn_remote_info), filled = true) { key(RemoteKeys.INFO) }
            RemoteButton(MobileIcons.Guide, stringResource(R.string.common_nav_guide), filled = true) { key(RemoteKeys.GUIDE) }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RemoteButton(MobileIcons.KeyboardArrowUp, stringResource(R.string.glcn_remote_channel_up), filled = true, label = stringResource(R.string.glcn_remote_ch)) { key(RemoteKeys.CHANNEL_UP) }
            RemoteButton(MobileIcons.KeyboardArrowDown, stringResource(R.string.glcn_remote_channel_down), filled = true, label = stringResource(R.string.glcn_remote_ch)) { key(RemoteKeys.CHANNEL_DOWN) }
            RemoteButton(MobileIcons.VolumeUp, stringResource(R.string.glcn_remote_volume_up), filled = true) { key(RemoteKeys.VOLUME_UP) }
            RemoteButton(MobileIcons.KeyboardArrowDown, stringResource(R.string.glcn_remote_volume_down), filled = true, label = stringResource(R.string.glcn_remote_vol)) { key(RemoteKeys.VOLUME_DOWN) }
            RemoteButton(MobileIcons.VolumeOff, stringResource(R.string.glcn_remote_mute), filled = true) { key(RemoteKeys.MUTE) }
        }
        Spacer(Modifier.height(26.dp))
        // The keyboard: what is typed here, the TV searches for.
        var text by remember { mutableStateOf("") }
        val submit = {
            val t = text.trim()
            if (t.isNotEmpty()) { send(LauncherDeepLink.RemoteText(t)); text = "" }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.take(RemoteKeys.TEXT_MAX) },
            placeholder = { Text(stringResource(R.string.glcn_remote_type_hint)) },
            singleLine = true,
            trailingIcon = {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { submit() }, contentAlignment = Alignment.Center) {
                    StageIcon(MobileIcons.Search, 20.dp, palette.text)
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RemoteButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    rotation: Float = 0f,
    label: String? = null,
    size: Dp = if (filled) 56.dp else 64.dp,
    onClick: () -> Unit,
) {
    val palette = LocalStagePalette.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(if (filled) Modifier.background(palette.controlFill) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            label?.let { Text(it, style = stageText(10f, 800), color = palette.muted) }
            StageIcon(icon, if (filled) 22.dp else 34.dp, palette.text, Modifier.rotate(rotation))
        }
    }
}
