package tv.own.owntv.mobile.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.sheetListHeight
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.StageButton
import tv.own.owntv.mobile.ui.stage.StageField
import tv.own.owntv.mobile.ui.stage.StageSettingRow
import tv.own.owntv.mobile.ui.stage.StageSheetHeader
import tv.own.owntv.mobile.ui.stage.StageSwitch
import tv.own.owntv.mobile.ui.stage.stageAccent
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * "Find in folder": the subtitle files in the folder chosen once with the system picker, best match for
 * the film or episode playing first, narrowed by the search box, with the automatic-loading switch.
 * Choosing another folder is the player's job ([onChangeFolder]), which owns the picker.
 */
@Composable
fun FolderSubtitleSheet(
    onChangeFolder: () -> Unit,
    onFailed: () -> Unit,
    onDismiss: () -> Unit,
    vm: FolderSubtitleViewModel = koinViewModel(),
) {
    val shown by vm.shown.collectAsStateWithLifecycle()
    val hasFiles by vm.hasFiles.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val autoLoad by vm.autoLoad.collectAsStateWithLifecycle()
    val applying by vm.applying.collectAsStateWithLifecycle()
    val accent = stageAccent.accent
    // The view model outlives the sheet, so the folder is read again on each open.
    LaunchedEffect(Unit) { vm.start() }
    LaunchedEffect(Unit) { vm.picked.collect { ok -> if (ok) onDismiss() else onFailed() } }

    val title = stringResource(R.string.player_subtitle_folder_title)
    MobileBottomSheet(onDismissRequest = onDismiss, title = title) {
        StageSheetHeader(title)
        val files = shown
        when {
            files == null -> Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), color = accent)
            }
            !hasFiles -> Note(stringResource(R.string.player_subtitle_folder_empty))
            else -> {
                StageField(
                    value = query,
                    onValueChange = vm::setQuery,
                    label = stringResource(R.string.common_search),
                    placeholder = stringResource(R.string.common_search_hint),
                    imeAction = ImeAction.Search,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                StageSettingRow(
                    title = stringResource(R.string.player_subtitle_folder_auto),
                    subtitle = stringResource(R.string.player_subtitle_folder_auto_description),
                    minHeight = 48.dp,
                    onClick = { vm.setAutoLoad(!autoLoad) },
                ) { StageSwitch(autoLoad) }
                if (files.isEmpty()) {
                    Note(stringResource(R.string.search_nothing_found, query.trim()))
                } else {
                    LazyColumn(Modifier.heightIn(max = sheetListHeight())) {
                        items(files, key = { it.location }) { file ->
                            StageSettingRow(
                                title = file.name,
                                minHeight = 48.dp,
                                onClick = { if (applying == null) vm.select(file) },
                            ) {
                                if (applying == file.location) CircularProgressIndicator(Modifier.size(20.dp), color = accent)
                            }
                        }
                    }
                }
            }
        }
        StageButton(
            onClick = onChangeFolder,
            text = stringResource(R.string.player_subtitle_folder_change),
            small = true,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = stageText(11.5f, 500, lineHeight = 1.4f),
        color = LocalStagePalette.current.muted,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
