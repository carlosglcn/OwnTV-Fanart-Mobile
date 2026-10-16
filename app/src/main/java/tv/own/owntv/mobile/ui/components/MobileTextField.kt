package tv.own.owntv.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import tv.own.owntv.mobile.ui.stage.drawInnerRing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch

/**
 * A single-line text field — the playlist URL, a username, a search box.
 *
 * On a phone the keyboard covers half the screen, so this stays one line and hands the caller an
 * [ImeAction] instead of growing.
 */
@Composable
fun MobileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onSearch: (() -> Unit)? = null,
    onImeDone: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    // The Stage field (M9): the label above in 11.5 muted, the 42 dp ink field with its focus ring (a danger ring
    // when [isError]), the dim placeholder, and the supporting line under it.
    val palette = tv.own.owntv.mobile.ui.stage.LocalStagePalette.current
    val accent = tv.own.owntv.mobile.ui.stage.stageAccent
    var focused by remember { mutableStateOf(false) }
    val textStyle = tv.own.owntv.mobile.ui.stage.stageText(13.5f, 500)
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = tv.own.owntv.mobile.ui.stage.stageText(11.5f, 600), color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoView)
                .onFocusChanged { focused = it.isFocused }
                .onFocusEvent { state ->
                    if (state.isFocused) {
                        scope.launch { runCatching { bringIntoView.bringIntoView() } }
                    }
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            textStyle = textStyle.copy(color = if (enabled) palette.text else palette.dim),
            cursorBrush = SolidColor(accent.accent),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            // A password gets the password keyboard: the text one auto-corrects and capitalises, which
            // turned a correct backup password into a wrong one.
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword && keyboardType == KeyboardType.Text) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
                autoCorrectEnabled = if (isPassword) false else null,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = {
                    onImeDone?.invoke()
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
                onSearch = {
                    if (onSearch != null) onSearch() else keyboard?.hide()
                    if (onSearch == null) focusManager.clearFocus()
                },
                onGo = {
                    onImeDone?.invoke()
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
                onSend = {
                    onImeDone?.invoke()
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
            ),
            decorationBox = { field ->
                val ring = when {
                    isError -> palette.danger
                    focused -> accent.focus
                    else -> null
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(tv.own.owntv.mobile.ui.stage.StageRadii.Search))
                        .background(palette.controlFill)
                        .then(
                            if (ring != null) {
                                Modifier.drawBehind { drawInnerRing(ring, 1.5.dp.toPx(), tv.own.owntv.mobile.ui.stage.StageRadii.Search.toPx()) }
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = textStyle, color = palette.dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    field()
                }
            },
        )
        supportingText?.let {
            Text(
                it,
                style = tv.own.owntv.mobile.ui.stage.stageText(11.5f, 500, lineHeight = 1.3f),
                color = if (isError) palette.danger else palette.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}
