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
        GlcnStyle.RIA_NOCHE -> R.string.glcn_style_ria_noche
        GlcnStyle.RIA_DIA -> R.string.glcn_style_ria_dia
        GlcnStyle.ESTUDIO_DIA -> R.string.glcn_style_estudio_dia
        GlcnStyle.NEON_DIA -> R.string.glcn_style_neon_dia
        GlcnStyle.BRASA -> R.string.glcn_style_brasa
        GlcnStyle.ALBA -> R.string.glcn_style_alba
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
        GlcnStyle.AURORA -> R.string.glcn_style_aurora
        GlcnStyle.CREPUSCULO -> R.string.glcn_style_crepusculo
        GlcnStyle.OCEANO -> R.string.glcn_style_oceano
        GlcnStyle.FANART_VIVO -> R.string.glcn_style_fanart_vivo
    },
)

/** glcn: a theme's name (Appearance). */
@Composable
fun tv.own.owntv.core.theme.GlcnTheme.label(): String = stringResource(
    when (this) {
        tv.own.owntv.core.theme.GlcnTheme.RIA -> R.string.glcn_theme_ria
        tv.own.owntv.core.theme.GlcnTheme.BRASA -> R.string.glcn_theme_brasa
        tv.own.owntv.core.theme.GlcnTheme.LUMEN -> R.string.glcn_theme_lumen
        tv.own.owntv.core.theme.GlcnTheme.AURORA -> R.string.glcn_theme_aurora
        tv.own.owntv.core.theme.GlcnTheme.CREPUSCULO -> R.string.glcn_theme_crepusculo
        tv.own.owntv.core.theme.GlcnTheme.ESTUDIO -> R.string.glcn_theme_estudio
        tv.own.owntv.core.theme.GlcnTheme.NEON -> R.string.glcn_theme_neon
        tv.own.owntv.core.theme.GlcnTheme.VITRINA -> R.string.glcn_theme_vitrina
    },
)

/** glcn: what kind a style is ("Dark", "Glass · stronger devices"). */
@Composable
fun GlcnStyle.kind(): String = stringResource(
    when {
        motion == tv.own.owntv.core.theme.GLCN_MOTION_DRIFT -> R.string.glcn_style_kind_drift
        motion == tv.own.owntv.core.theme.GLCN_MOTION_ART -> R.string.glcn_style_kind_art
        isGradient -> R.string.glcn_style_kind_gradient
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
    // glcn: Appearance, as on the TV — the theme (each with a night and a day side) and the mode; the styles of
    // earlier versions stay under their own row for whoever had one.
    val theme = tv.own.owntv.core.theme.GlcnTheme.of(style)
    val mode = s.glcnStyleMode.pref(null) ?: if (style?.light == true) tv.own.owntv.core.theme.GLCN_MODE_LIGHT else tv.own.owntv.core.theme.GLCN_MODE_DARK
    val look = listOfNotNull(
        choiceItem(
            "glcn_theme", MobileIcons.Palette, g, stringResource(R.string.glcn_look_theme),
            stringResource(R.string.glcn_look_desc), null,
            listOf(SettingsChoice<tv.own.owntv.core.theme.GlcnTheme?>(null, none, stringResource(R.string.glcn_style_none_desc))) +
                tv.own.owntv.core.theme.GlcnTheme.entries.map { SettingsChoice<tv.own.owntv.core.theme.GlcnTheme?>(it, it.label()) },
            theme, null,
            { t -> vm.edit { if (t == null) applyGlcnStyle(null) else applyGlcnTheme(t) } },
            valueText = theme?.label() ?: style?.label() ?: none,
        ),
        if (style == null) null else choiceItem(
            "glcn_mode", MobileIcons.AutoAwesome, g, stringResource(R.string.glcn_look_mode),
            stringResource(R.string.glcn_look_mode_auto_desc), null,
            listOf(
                SettingsChoice(tv.own.owntv.core.theme.GLCN_MODE_AUTO, stringResource(R.string.glcn_look_mode_auto), stringResource(R.string.glcn_look_mode_auto_desc)),
                SettingsChoice(tv.own.owntv.core.theme.GLCN_MODE_DARK, stringResource(R.string.glcn_look_mode_dark)),
                SettingsChoice(tv.own.owntv.core.theme.GLCN_MODE_LIGHT, stringResource(R.string.glcn_look_mode_light)),
            ),
            mode, tv.own.owntv.core.theme.GLCN_MODE_AUTO, { m -> vm.edit { setGlcnStyleMode(m) } },
        ),
    )
    return listOf(
        SettingSection(
            stringResource(R.string.glcn_look_title),
            look,
        ),
        SettingSection(
            stringResource(R.string.glcn_part_home),
            listOf(
                choiceItem(
                    "glcn_style", MobileIcons.Palette, g, stringResource(R.string.glcn_look_older),
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
