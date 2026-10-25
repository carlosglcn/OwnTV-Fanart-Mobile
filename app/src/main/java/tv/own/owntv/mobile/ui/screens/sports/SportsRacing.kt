package tv.own.owntv.mobile.ui.screens.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tv.own.owntv.core.sports.model.Match
import tv.own.owntv.core.sports.model.MatchState
import tv.own.owntv.core.sports.model.RaceSession
import tv.own.owntv.core.sports.model.Ranking
import tv.own.owntv.core.sports.model.RankingKind
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.StageTool
import tv.own.owntv.mobile.ui.stage.stageAccent
import tv.own.owntv.mobile.ui.stage.stageText

/** glcn: a racing session's name in the user's language ("Libres 1", "Clasificación", "Carrera"). */
@Composable
internal fun raceSessionLabel(m: Match): String? = RaceSession.of(m.subtitle)?.let {
    stringResource(
        when (it) {
            RaceSession.PRACTICE_1 -> R.string.glcn_race_practice_1
            RaceSession.PRACTICE_2 -> R.string.glcn_race_practice_2
            RaceSession.PRACTICE_3 -> R.string.glcn_race_practice_3
            RaceSession.PRACTICE -> R.string.glcn_race_practice
            RaceSession.WARM_UP -> R.string.glcn_race_warm_up
            RaceSession.SPRINT_QUALIFYING -> R.string.glcn_race_sprint_qualifying
            RaceSession.SPRINT -> R.string.glcn_race_sprint
            RaceSession.QUALIFYING -> R.string.glcn_race_qualifying
            RaceSession.RACE -> R.string.glcn_race_race
        },
    )
}

/**
 * glcn: the score box of a racing session. A session has no home and away, so the box names the session
 * and the Grand Prix, then the time and circuit, and lists the first three below.
 */
@Composable
internal fun RaceScoreBox(m: Match, hidden: Boolean, onReveal: () -> Unit) {
    val palette = LocalStagePalette.current
    val sep = stringResource(R.string.content_epg_bits_separator)
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.ink.copy(alpha = 0.05f))
            .clickable(enabled = hidden, onClick = onReveal)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(raceSessionLabel(m) ?: m.subtitle.orEmpty(), style = stageText(26f, 800), color = palette.text, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(m.title, style = stageText(14f, 600), color = palette.body, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(10.dp))
        if (m.isLive) LiveMark(stringResource(R.string.player_live) + sep + clockText(m))
        else Text(
            if (m.state == MatchState.PRE) listOfNotNull(dayOf(m.startMs, locale) ?: stringResource(R.string.content_epg_today), timeText(m.startMs)).joinToString(sep) else m.statusText,
            style = stageText(15f, 700), color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        m.venue?.let { Text(it, style = stageText(11.5f, 500), color = palette.muted, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)) }
        val grid = m.state == MatchState.PRE
        if (m.competitors.isNotEmpty() && (!grid || RaceSession.of(m.subtitle) == RaceSession.RACE) && !(hidden && !grid)) {
            if (grid) Text(stringResource(R.string.glcn_race_grid), style = stageText(11f, 700), color = palette.dim, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
            else Spacer(Modifier.height(12.dp))
            m.competitors.forEachIndexed { i, c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text((i + 1).toString(), style = stageText(13f, 800), color = palette.muted, modifier = Modifier.width(18.dp))
                    Crest(c.team, 26)
                    Text(c.team.name, style = stageText(13.5f, 700), color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    c.team.country?.let { Text(it, style = stageText(11f, 500), color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }
}

@Composable
private fun rankLabel(kind: RankingKind): String = stringResource(
    when (kind) {
        RankingKind.DRIVERS -> R.string.glcn_rank_drivers
        RankingKind.CONSTRUCTORS -> R.string.glcn_rank_constructors
        RankingKind.PLAYERS -> R.string.glcn_rank_players
    },
)

/** glcn: a racing series' (or tennis tour's) ranking: a switch between drivers and constructors, then # · name · points. */
@Composable
internal fun RankingTable(ranking: Ranking, highlight: Set<String> = emptySet()) {
    val palette = LocalStagePalette.current
    val groups = ranking.groups
    var sel by rememberSaveable { mutableIntStateOf(0) }
    val g = groups.getOrNull(sel.coerceIn(0, (groups.size - 1).coerceAtLeast(0))) ?: return
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        if (groups.size > 1) Row(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEachIndexed { i, x -> StageTool(onClick = { sel = i }, label = rankLabel(x.kind), on = sel == i) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Spacer(Modifier.width(28.dp))
            Text(stringResource(R.string.glcn_rank_name), style = stageText(11f, 700), color = palette.dim, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.sports_table_points), style = stageText(11f, 700), color = palette.dim, textAlign = TextAlign.End)
        }
        g.rows.forEach { r ->
            val on = r.team.id in highlight
            Row(
                Modifier.fillMaxWidth()
                    .then(if (on) Modifier.background(stageAccent.accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp)) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(r.rank.toString(), style = stageText(12.5f, 700), color = palette.muted, modifier = Modifier.width(28.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Crest(r.team, 22)
                    Text(if (r.team.isAthlete) r.team.name else teamLabel(r.team), style = stageText(13f, if (on) 800 else 600), color = palette.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    r.team.country?.let { Text(it, style = stageText(11f, 500), color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Text(r.points.toString(), style = stageText(13f, 800), color = palette.text, textAlign = TextAlign.End)
            }
        }
    }
}
