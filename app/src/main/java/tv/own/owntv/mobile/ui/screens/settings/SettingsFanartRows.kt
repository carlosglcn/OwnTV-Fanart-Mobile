package tv.own.owntv.mobile.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.core.theme.GlcnStyle
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.components.MobileIcons

/** glcn: a style's name. */
@Composable
fun GlcnStyle.label(): String = stringResource(
    when (this) {
        GlcnStyle.LUMEN_NOCHE -> R.string.glcn_style_lumen_noche
        GlcnStyle.LUMEN_DIA -> R.string.glcn_style_lumen_dia
        GlcnStyle.ATARDECER -> R.string.glcn_style_atardecer
        GlcnStyle.ESTUDIO -> R.string.glcn_style_estudio
        GlcnStyle.VITRINA -> R.string.glcn_style_vitrina
        GlcnStyle.PULSO -> R.string.glcn_style_pulso
        GlcnStyle.NEON -> R.string.glcn_style_neon
        GlcnStyle.NIEVE -> R.string.glcn_style_nieve
        GlcnStyle.PAPEL -> R.string.glcn_style_papel
        GlcnStyle.ARENA -> R.string.glcn_style_arena
    },
)

/** glcn: what kind a style is ("Dark", "Glass · stronger devices"). */
@Composable
fun GlcnStyle.kind(): String = stringResource(
    when {
        glass -> R.string.glcn_style_kind_glass
        light -> R.string.glcn_style_kind_light
        else -> R.string.glcn_style_kind_dark
    },
)

/**
 * glcn: Settings › Fanart — everything this fork adds, in one page, so the upstream pages stay as they
 * are: the style, seasonal themes, clearer dialogue and skipping intros.
 */
@Composable
internal fun fanartSections(vm: SettingsViewModel): List<SettingSection> {
    val g = SettingsGroup.FANART
    val s = vm.settings
    val style = s.glcnStyle.pref(null)
    val none = stringResource(R.string.glcn_style_none)
    return listOf(
        SettingSection(
            stringResource(R.string.glcn_part_home),
            listOf(
                choiceItem(
                    "glcn_style", MobileIcons.Palette, g, stringResource(R.string.glcn_style_title),
                    stringResource(R.string.glcn_style_desc), stringResource(R.string.glcn_style_none_desc),
                    listOf(SettingsChoice<GlcnStyle?>(null, none, stringResource(R.string.glcn_style_none_desc))) +
                        GlcnStyle.entries.map { SettingsChoice<GlcnStyle?>(it, it.label(), it.kind()) },
                    style, null, { st -> vm.edit { applyGlcnStyle(st) } },
                ),
                choiceItem(
                    "glcn_logo_plate", MobileIcons.Image, g, stringResource(R.string.glcn_logo_plate_title),
                    stringResource(R.string.glcn_logo_plate_desc), null,
                    listOf(
                        SettingsChoice(0, stringResource(R.string.glcn_logo_plate_classic)),
                        SettingsChoice(1, stringResource(R.string.glcn_logo_plate_none)),
                    ),
                    s.logoPlate.pref(0), 0, { m -> vm.edit { setLogoPlate(m) } },
                ),
                choiceItem(
                    "glcn_season", MobileIcons.AutoAwesome, g, stringResource(R.string.glcn_season_title),
                    stringResource(R.string.glcn_season_desc), null,
                    listOf(
                        SettingsChoice(0, stringResource(R.string.common_off)),
                        SettingsChoice(1, stringResource(R.string.glcn_season_details)),
                        SettingsChoice(2, stringResource(R.string.glcn_season_animated)),
                    ),
                    s.seasonMode.pref(0), 2, { m -> vm.edit { setSeasonMode(m) } },
                ),
            ),
        ),
        SettingSection(
            stringResource(R.string.glcn_part_player),
            listOf(
                switchItem(
                    "glcn_auto_skip_intro", MobileIcons.PlayCircle, stringResource(R.string.glcn_auto_skip_title),
                    stringResource(R.string.glcn_auto_skip_desc), s.autoSkipIntro.pref(false),
                ) { vm.edit { setAutoSkipIntro(it) } },
                switchItem(
                    "glcn_habits", MobileIcons.Bell, stringResource(R.string.glcn_habits_title),
                    stringResource(R.string.glcn_habits_desc), s.habitLearning.pref(true),
                ) { vm.edit { setHabitLearning(it) } },
                switchItem(
                    "glcn_dialogue", MobileIcons.Tune, stringResource(R.string.glcn_dialogue_title),
                    stringResource(R.string.glcn_dialogue_desc), s.dialogueBoost.pref(false),
                ) { vm.edit { setDialogueBoost(it) } },
            ),
        ),
    )
}
