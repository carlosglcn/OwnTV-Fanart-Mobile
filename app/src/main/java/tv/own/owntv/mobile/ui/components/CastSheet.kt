package tv.own.owntv.mobile.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import tv.own.owntv.core.database.entity.MovieEntity
import tv.own.owntv.core.metadata.CastLibrary
import tv.own.owntv.core.metadata.CastMember
import tv.own.owntv.core.metadata.MetadataImages
import tv.own.owntv.core.metadata.Wiki
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.StageButton
import tv.own.owntv.mobile.ui.stage.StagePoster
import tv.own.owntv.mobile.ui.stage.stageAccent
import tv.own.owntv.mobile.ui.stage.stageText

/**
 * glcn: a cast member, from a film's page — their photo and name, Wikipedia's line on them, when and where they were
 * born (and their age), the summary with a button to the whole article, and their other films in your playlists
 * (a tap opens one). The television's cast page, as a sheet.
 */
@Composable
fun CastSheet(person: CastMember, exceptFilm: Long?, onOpenFilm: (MovieEntity) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalStagePalette.current
    val context = LocalContext.current
    val wiki by produceState<Pair<Boolean, Wiki.Summary?>>(false to null, person.name) { value = true to Wiki.person(person.name) }
    val qid = wiki.second?.qid
    val facts by produceState<Wiki.Facts?>(null, qid) { value = qid?.let { Wiki.facts(it) } }
    val films by produceState<List<MovieEntity>?>(null, person.name, facts) {
        val credits = facts?.films.orEmpty().map { CastLibrary.Credit(it.tmdb, it.titles, it.year) }
        value = CastLibrary.filmsWith(person.name, except = exceptFilm, credits = credits)
    }
    MobileBottomSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AsyncImage(
                MetadataImages.profile(person.profilePath, "w342"), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.width(96.dp).height(144.dp).clip(RoundedCornerShape(14.dp)).background(palette.cardSolid),
            )
            Column(Modifier.weight(1f)) {
                Text(person.name, style = stageText(22f, 800), color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val s = wiki.second
                s?.description?.let { Text(it.replaceFirstChar { c -> c.uppercase() }, style = stageText(13f, 700), color = stageAccent.accent, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)) }
                factsLine(facts)?.let { Text(it, style = stageText(12.5f, 600), color = palette.body, modifier = Modifier.padding(top = 6.dp)) }
                if (wiki.first && s == null) Text(stringResource(R.string.glcn_castpage_wiki_none), style = stageText(12.5f, 500), color = palette.muted, modifier = Modifier.padding(top = 6.dp))
            }
        }
        wiki.second?.let { s ->
            Text(s.extract, style = stageText(13.5f, 500, lineHeight = 1.45f), color = palette.body, maxLines = 8, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp).padding(top = 14.dp))
            StageButton(
                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(s.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                text = stringResource(R.string.glcn_castpage_wiki_open),
                icon = MobileIcons.OpenInNew,
                small = true,
                modifier = Modifier.padding(horizontal = 18.dp).padding(top = 10.dp),
            )
        }
        Text(stringResource(R.string.glcn_castpage_films), style = stageText(15f, 800), color = palette.text, modifier = Modifier.padding(horizontal = 18.dp).padding(top = 18.dp, bottom = 8.dp))
        val list = films
        when {
            list == null -> Box(Modifier.height(170.dp))
            list.isEmpty() -> Text(stringResource(R.string.glcn_cast_none, person.name), style = stageText(13f, 500), color = palette.muted, modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 18.dp))
            else -> LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                items(list, key = { it.id }) { f ->
                    StagePoster(imageUrl = f.posterUrl, title = f.name, subtitle = f.year?.toString(), onClick = { onDismiss(); onOpenFilm(f) }, modifier = Modifier.width(96.dp))
                }
            }
        }
        Box(Modifier.size(1.dp), contentAlignment = Alignment.Center) {}
    }
}

/** "Born 18 December 1963 in Shawnee · 62 years old", or with the death date and age at death. */
@Composable
private fun factsLine(f: Wiki.Facts?): String? {
    val born = f?.born ?: return null
    val fmt = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)
    val sep = stringResource(R.string.content_epg_bits_separator)
    val first = buildString {
        append(stringResource(R.string.glcn_castpage_born, born.format(fmt)))
        f.place?.let { append(' ').append(stringResource(R.string.glcn_castpage_born_in, it)) }
    }
    val died = f.died
    val second = if (died != null) stringResource(R.string.glcn_castpage_died, died.format(fmt), java.time.Period.between(born, died).years)
    else stringResource(R.string.glcn_castpage_age, java.time.Period.between(born, java.time.LocalDate.now()).years)
    return first + sep + second
}
