package tv.own.owntv.mobile.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import tv.own.owntv.core.ui.stage.PlaylistMark
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileBottomSheet
import tv.own.owntv.mobile.ui.components.MobileIcons
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.StageButton
import tv.own.owntv.mobile.ui.stage.StageButtonStyle
import tv.own.owntv.mobile.ui.stage.StageChevron
import tv.own.owntv.mobile.ui.stage.StageGroupLabel
import tv.own.owntv.mobile.ui.stage.StageLiveBadge
import tv.own.owntv.mobile.ui.stage.StageMenuItem
import tv.own.owntv.mobile.ui.stage.StagePill
import tv.own.owntv.mobile.ui.stage.StagePlaylistMark
import tv.own.owntv.mobile.ui.stage.StagePoster
import tv.own.owntv.mobile.ui.stage.StageProgress
import tv.own.owntv.mobile.ui.stage.StageRadio
import tv.own.owntv.mobile.ui.stage.StageRadii
import tv.own.owntv.mobile.ui.stage.StageRecommended
import tv.own.owntv.mobile.ui.stage.StageRow
import tv.own.owntv.mobile.ui.stage.StageRowValue
import tv.own.owntv.mobile.ui.stage.StageSearchField
import tv.own.owntv.mobile.ui.stage.StageSectionHead
import tv.own.owntv.mobile.ui.stage.StageSegmented
import tv.own.owntv.mobile.ui.stage.StageSettingRow
import tv.own.owntv.mobile.ui.stage.StageSheetHeader
import tv.own.owntv.mobile.ui.stage.StageSpecChips
import tv.own.owntv.mobile.ui.stage.StageStepper
import tv.own.owntv.mobile.ui.stage.StageStill
import tv.own.owntv.mobile.ui.stage.StageSwitch
import tv.own.owntv.mobile.ui.stage.StageTag
import tv.own.owntv.mobile.ui.stage.StageTagTone
import tv.own.owntv.mobile.ui.stage.StageTool
import tv.own.owntv.mobile.ui.stage.stageBackground
import tv.own.owntv.mobile.ui.stage.stageGlass
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * Every Stage component (plan M0) on the Stage page background, in whichever theme is set — the gallery's
 * theme chips switch Dark / Light. Core strings only, like the rest of the gallery.
 */
@Composable
fun StageGallerySection() {
    val palette = LocalStagePalette.current
    // Three marks in the first three colours, named from core strings (MOVI, SERI, LIVE).
    val marks = listOf(R.string.common_nav_movies, R.string.common_nav_series, R.string.common_nav_live_tv)
        .mapIndexed { i, name -> PlaylistMark.of(stringResource(name), i) }
    val rating = remember { NumberFormat.getInstance().apply { maximumFractionDigits = 1; minimumFractionDigits = 1 }.format(7.8) }
    var on by remember { mutableStateOf(true) }
    var segment by remember { mutableIntStateOf(0) }
    var step by remember { mutableIntStateOf(100) }
    var choice by remember { mutableIntStateOf(1) }
    var query by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    val gap = Arrangement.spacedBy(8.dp)

    Column(
        Modifier.fillMaxWidth().stageBackground().padding(horizontal = 6.dp, vertical = 16.dp),
        verticalArrangement = gap,
    ) {
        Text(stringResource(R.string.common_nav_settings), style = stageText(28f, 800, lineHeight = 1.05f), color = palette.text, modifier = Modifier.padding(horizontal = 10.dp))

        // Top cluster pills, tools, segmented.
        Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StagePill(onClick = {}, text = stringResource(R.string.content_all_playlists), icon = MobileIcons.Folder, trailingIcon = MobileIcons.KeyboardArrowDown)
            StagePill(onClick = {}, icon = MobileIcons.Search, contentDescription = stringResource(R.string.common_search))
        }
        Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StageTool(onClick = {}, icon = MobileIcons.Search)
            StageTool(onClick = {}, icon = MobileIcons.FormatListBulleted, value = stringResource(R.string.content_provider))
            StageTool(onClick = { on = !on }, label = stringResource(R.string.settings_view_grid), on = on)
        }
        StageSegmented(
            options = listOf(stringResource(R.string.settings_view_grid), stringResource(R.string.settings_view_list)),
            icons = listOf(MobileIcons.GridView, MobileIcons.ViewList),
            selected = segment,
            onSelect = { segment = it },
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        StageSearchField(query, { query = it }, stringResource(R.string.common_search_hint), Modifier.padding(horizontal = 10.dp))

        // Buttons.
        FlowRow(Modifier.padding(horizontal = 10.dp), horizontalArrangement = gap, verticalArrangement = gap) {
            StageButton(onClick = { sheet = true }, text = stringResource(R.string.content_key_watch), icon = MobileIcons.PlayArrow, style = StageButtonStyle.PRIMARY)
            StageButton(onClick = {}, text = stringResource(R.string.common_cancel))
            StageButton(onClick = {}, icon = MobileIcons.FavoriteBorder)
            StageButton(onClick = {}, text = stringResource(R.string.common_delete), style = StageButtonStyle.DANGER, small = true)
        }

        // Tags, marks, badges, progress.
        FlowRow(Modifier.padding(horizontal = 10.dp), horizontalArrangement = gap, verticalArrangement = gap, itemVerticalAlignment = Alignment.CenterVertically) {
            StageTag(stringResource(R.string.player_stream_hdr))
            StageTag(stringResource(R.string.common_on), tone = StageTagTone.ACCENT)
            StageTag(stringResource(R.string.common_never), tone = StageTagTone.WARN)
            marks.forEach { StagePlaylistMark(it) }
            StageRecommended()
            StageLiveBadge(stringResource(R.string.player_live))
            StageSpecChips(listOf(stringResource(R.string.player_stream_hdr), stringResource(R.string.player_stream_hdr)))
        }
        Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = gap) {
            StageProgress(0.6f)
            StageProgress(0.35f, Modifier.width(200.dp), thin = true)
        }

        // Rows: idle, current (fx), selected (bar).
        StageSectionHead(stringResource(R.string.common_nav_live_tv), count = 3)
        listOf(false to false, true to false, false to true).forEach { (current, selected) ->
            StageRow(onClick = {}, onLongClick = { sheet = true }, current = current, selected = selected, height = 66.dp) {
                Text(stringResource(R.string.common_nav_live_tv), style = stageText(14.5f, 700), color = palette.text, modifier = Modifier.weight(1f))
                StagePlaylistMark(marks[0])
            }
        }

        // Setting rows: sw / ch / go / st / seg / act.
        StageSectionHead(stringResource(R.string.common_nav_settings), count = 6)
        StageSettingRow(stringResource(R.string.settings_glass_effect), icon = MobileIcons.AutoAwesome, subtitle = stringResource(R.string.settings_theme_description), onClick = { on = !on }) { StageSwitch(on) }
        StageSettingRow(stringResource(R.string.settings_theme), icon = MobileIcons.Palette, onClick = { sheet = true }) { StageRowValue(stringResource(R.string.settings_theme_dark), StageChevron.DOWN) }
        StageSettingRow(stringResource(R.string.common_nav_downloads), icon = MobileIcons.Download, current = true, onClick = {}) { StageRowValue("", StageChevron.FORWARD, plain = true) }
        StageSettingRow(stringResource(R.string.settings_ui_zoom), icon = MobileIcons.OpenInFull) {
            StageStepper(stringResource(R.string.common_percent, step), onMinus = { step -= 5 }, onPlus = { step += 5 })
        }
        StageSettingRow(stringResource(R.string.settings_theme), icon = MobileIcons.GridView) {
            StageSegmented(listOf(stringResource(R.string.settings_view_grid), stringResource(R.string.settings_view_list)), segment, { segment = it })
        }
        StageSettingRow(stringResource(R.string.more_backup_now), icon = MobileIcons.Save, onClick = {}) {
            StageRowValue(stringResource(R.string.more_backup_now), plain = true, accentPart = stringResource(R.string.common_never))
        }

        // Posters, a still, a glass panel.
        Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StagePoster(null, stringResource(R.string.common_nav_movies), Modifier.weight(1f), rating = rating, mark = marks[1], progress = 0.4f)
            StagePoster(null, stringResource(R.string.common_nav_series), Modifier.weight(1f), rating = rating, watched = true, selected = true)
            StagePoster(null, stringResource(R.string.common_nav_movies), Modifier.weight(1f), subtitle = stringResource(R.string.common_nav_series), rating = rating)
        }
        StageStill(null, Modifier.padding(horizontal = 10.dp), progress = 0.5f)
        Box(
            Modifier.padding(horizontal = 10.dp).fillMaxWidth().height(64.dp).stageGlass(RoundedCornerShape(StageRadii.BottomBar)),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.settings_glass_effect), style = stageText(13f, 700), color = palette.text) }
    }

    if (sheet) {
        MobileBottomSheet(onDismissRequest = { sheet = false }, title = stringResource(R.string.common_nav_live_tv), stage = true) {
            StageSheetHeader(
                title = stringResource(R.string.common_nav_live_tv),
                subtitle = stringResource(R.string.common_nav_guide),
                extra = stringResource(R.string.common_never),
            )
            StageGroupLabel(stringResource(R.string.content_menu_group_watch))
            StageMenuItem(stringResource(R.string.content_key_watch), { sheet = false }, icon = MobileIcons.PlayArrow, value = stringResource(R.string.common_on))
            StageMenuItem(stringResource(R.string.content_provider), { sheet = false }, icon = MobileIcons.FormatListBulleted, count = stringResource(R.string.common_percent, 42), selected = true)
            StageMenuItem(stringResource(R.string.content_all_playlists), { sheet = false }, icon = MobileIcons.Folder, checked = true, tags = { StagePlaylistMark(marks[0]) })
            StageGroupLabel(stringResource(R.string.content_menu_group_channel))
            listOf(R.string.settings_theme_dark, R.string.settings_theme_light).forEachIndexed { i, label ->
                StageMenuItem(stringResource(label), { choice = i }, leading = { StageRadio(choice == i) }, selected = choice == i, tags = if (i == 0) ({ StageRecommended() }) else null)
            }
            StageMenuItem(stringResource(R.string.common_delete), { sheet = false }, icon = MobileIcons.Delete, danger = true)
        }
    }
}
