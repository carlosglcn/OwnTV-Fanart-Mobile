package tv.own.owntv.mobile.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.theme.GlcnStyle
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.screens.settings.kind
import tv.own.owntv.mobile.ui.screens.settings.label
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.stageAccent
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * glcn: first-run "Choose your style". A tap applies the style at once, so the whole screen — this step
 * included — shows it; Continue keeps it, and "None" keeps the app's own colours.
 */
@Composable
internal fun GlcnStyleStep(onNext: () -> Unit, onBack: () -> Unit) {
    val settings: SettingsRepository = koinInject()
    val scope = rememberCoroutineScope()
    val current by settings.glcnStyle.collectAsStateWithLifecycle(null)
    WizardFrame(
        step = WizardStep.DISPLAY,
        title = stringResource(R.string.glcn_setup_style_title),
        sub = stringResource(R.string.glcn_style_desc),
        next = stringResource(R.string.setup_continue),
        onNext = onNext,
        onBack = onBack,
        extraLeft = listOf(stringResource(R.string.glcn_style_none) to { scope.launch { settings.applyGlcnStyle(null) }; Unit }),
    ) {
        GlcnStyle.entries.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { st ->
                    StyleTile(st, selected = st == current, modifier = Modifier.weight(1f)) {
                        scope.launch { settings.applyGlcnStyle(st) }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StyleTile(st: GlcnStyle, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalStagePalette.current
    val accent = Color(android.graphics.Color.parseColor(st.accentHex))
    val card = Color(st.glassTint)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(palette.ink.copy(alpha = 0.045f))
            .border(2.dp, if (selected) stageAccent.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        // A small picture of the style: its page, a billboard with the accent and three cards.
        Column(
            Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(10.dp)).background(Color(st.page)).padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF1C4A2C), RoundedCornerShape(6.dp))) {
                Box(Modifier.padding(6.dp).size(width = 26.dp, height = 7.dp).background(accent, CircleShape))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { Box(Modifier.weight(1f).height(22.dp).background(card, RoundedCornerShape(5.dp))) }
            }
        }
        Text(st.label(), style = stageText(13.5f, 800), color = palette.text, maxLines = 1, modifier = Modifier.padding(top = 7.dp, start = 2.dp))
        Text(st.kind(), style = stageText(11f, 500), color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
    }
}
