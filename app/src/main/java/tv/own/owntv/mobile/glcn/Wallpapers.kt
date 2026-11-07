package tv.own.owntv.mobile.glcn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.ui.stage.LocalStagePalette
import tv.own.owntv.mobile.ui.stage.stageAccent

/**
 * glcn: the app's own wallpapers, the TV's set — photos downloaded once from the repository's `wallpapers` branch
 * when picked (the app carries small previews), and animated ones drawn live, stored as `anim:<kind>`.
 */
object Wallpapers {
    const val ANIM_PREFIX = "anim:"
    private const val BASE_URL = "https://raw.githubusercontent.com/carlosglcn/OwnTV-Fanart/wallpapers/"
    val animated = listOf("aurora", "ondas", "brillos", "nieve", "fugaces", "bokeh", "nacar", "amanecer", "petalos")
    /** The animated ones drawn on a light page — darkened under a dark theme so its white text still reads. */
    val lightAnimated = setOf("nacar", "amanecer", "petalos")
    val dark = listOf("via_lactea", "aurora_nieve", "aurora_pinos", "puerto", "bosque", "nieve_violeta", "siluetas", "pico_lila")
    val light = listOf("nubes_rosa", "duna", "mar_beige", "muelle", "valle")
    /** Offered for the profile screen first: a picture with more presence, nothing laid over it but names. */
    val gateFirst = listOf("nubes_rosa", "cordillera")

    fun animKind(path: String?): String? = path?.takeIf { it.startsWith(ANIM_PREFIX) }?.removePrefix(ANIM_PREFIX)

    fun thumbUri(name: String) = "file:///android_asset/wallpapers/thumb/$name.jpg"

    fun file(context: android.content.Context, name: String) = java.io.File(java.io.File(context.filesDir, "backgrounds"), "wall_$name.jpg")

    /** The photo a background path is, if it is one of ours. */
    fun nameOf(path: String?): String? = path?.let { java.io.File(it).name }
        ?.takeIf { it.startsWith("wall_") && it.endsWith(".jpg") }?.removePrefix("wall_")?.removeSuffix(".jpg")

    /** What to show of a photo now: the downloaded picture when it is here, else its preview. */
    fun model(context: android.content.Context, name: String): Any = file(context, name).takeIf { it.length() > 0 } ?: thumbUri(name)

    /** Downloads the photo once (to a part file, then renamed) and returns its path; null when it could not. */
    suspend fun download(context: android.content.Context, name: String, onProgress: (Float) -> Unit): String? = withContext(Dispatchers.IO) {
        runCatching {
            val out = file(context, name)
            if (out.length() > 0) return@runCatching out.absolutePath
            out.parentFile?.mkdirs()
            val part = java.io.File(out.parentFile, out.name + ".part")
            val conn = java.net.URL("$BASE_URL$name.jpg").openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = NET_TIMEOUT_MS
            conn.readTimeout = NET_TIMEOUT_MS
            try {
                check(conn.responseCode == java.net.HttpURLConnection.HTTP_OK)
                val total = conn.contentLengthLong
                conn.inputStream.use { inp ->
                    part.outputStream().use { o ->
                        val buf = ByteArray(BUF_BYTES)
                        var done = 0L
                        while (true) {
                            val n = inp.read(buf)
                            if (n < 0) break
                            o.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress(done.toFloat() / total)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            check(part.length() > 0 && part.renameTo(out))
            out.absolutePath
        }.getOrNull()
    }

    private const val NET_TIMEOUT_MS = 15_000
    private const val BUF_BYTES = 16 * 1024
}


/** glcn: a wallpaper full screen — a picture (path, uri or file) or an animated one (`anim:<kind>`). */
@Composable
fun WallLayer(model: Any, modifier: Modifier = Modifier) {
    val anim = (model as? String)?.let { Wallpapers.animKind(it) }
    if (anim != null) AnimatedWallpaper(anim, modifier)
    else coil3.compose.AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}

/** glcn: an animated wallpaper, full screen behind the app. */
@Composable
fun AnimatedWallpaper(kind: String, modifier: Modifier = Modifier) {
    val moving = tv.own.owntv.mobile.ui.theme.LocalAnimations.current != tv.own.owntv.core.theme.AnimationLevel.OFF
    val t = if (moving) rememberInfiniteTransition(label = "wall").animateFloat(
        0f, 1f, infiniteRepeatable(tween(WALL_ROUND_MS, easing = LinearEasing), RepeatMode.Restart), label = "wallT",
    ).value else 0.2f
    val pageDark = !LocalStagePalette.current.isLight
    val lightKind = kind in Wallpapers.lightAnimated
    Canvas(modifier) {
        drawWallpaper(kind, t)
        if (pageDark && lightKind) drawRect(Color.Black.copy(alpha = 0.5f))
        else if (!pageDark && !lightKind) drawRect(Color.White.copy(alpha = 0.35f))
    }
}

fun DrawScope.drawWallpaper(kind: String, t: Float) {
    when (kind) {
        "ondas" -> drawWaves(t)
        "brillos" -> drawSparkles(t)
        "nieve" -> drawSnow(t)
        "fugaces" -> drawStars(t)
        "bokeh" -> drawBokeh(t)
        "nacar" -> drawPearl(t)
        "amanecer" -> drawDawn(t)
        "petalos" -> drawPetals(t)
        else -> drawLights(listOf(Color(0xFF0F766E), Color(0xFF6D28D9), Color(0xFF1D4ED8)), Color(0xFF070B14), t)
    }
}

/** Slow layered waves in deep blue to violet, each layer on its own phase. */
private fun DrawScope.drawWaves(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF060A18), Color(0xFF0B1230))))
    val cols = listOf(Color(0xFF1B2A6B), Color(0xFF3A2A85), Color(0xFF5B2A86), Color(0xFF7A2E6E))
    val two = (2 * Math.PI).toFloat()
    cols.forEachIndexed { k, c ->
        val path = androidx.compose.ui.graphics.Path()
        val base = h * (0.45f + 0.13f * k)
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            val y = base + h * 0.05f * kotlin.math.sin(x / w * two * (1.1f + 0.25f * k) + (t + k * 0.21f) * two)
            path.lineTo(x, y)
            x += w / 48f
        }
        path.lineTo(w, h); path.close()
        drawPath(path, c.copy(alpha = 0.55f))
    }
    drawRect(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f)))
}

/** Soft points of light rising very slowly over a dark plum page. */
private fun DrawScope.drawSparkles(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.linearGradient(listOf(Color(0xFF120A1E), Color(0xFF0A1022)), Offset.Zero, Offset(w, h)))
    val rnd = java.util.Random(11)
    repeat(46) {
        val bx = rnd.nextFloat()
        val by = rnd.nextFloat()
        val r = (6f + rnd.nextFloat() * 22f) * (w / 1920f)
        val sp = 0.4f + rnd.nextFloat() * 0.8f
        val y = ((by - t * sp) % 1f + 1f) % 1f
        val warm = rnd.nextBoolean()
        val c = if (warm) Color(0xFFFFB37A) else Color(0xFF8FB8FF)
        val a = 0.10f + rnd.nextFloat() * 0.22f
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), Offset(bx * w, y * h), r * 3), r * 3, Offset(bx * w, y * h))
    }
}

private val TWO_PI = (2 * Math.PI).toFloat()

/** Snow falling slowly over a winter night, each flake swaying on its own. Whole laps per round, so it loops clean. */
private fun DrawScope.drawSnow(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0A1222), Color(0xFF1A2B47), Color(0xFF2B3D5C))))
    val rnd = java.util.Random(23)
    repeat(110) {
        val bx = rnd.nextFloat()
        val by = rnd.nextFloat()
        val laps = 2 + rnd.nextInt(4)
        val r = (1.5f + rnd.nextFloat() * 4.5f) * (w / 1920f)
        val y = ((by + t * laps) % 1f) * (h + 20f) - 10f
        val x = bx * w + kotlin.math.sin((t * laps + bx) * TWO_PI) * 26f * (w / 1920f)
        drawCircle(Color.White.copy(alpha = 0.35f + r / (w / 1920f) / 12f), r, Offset(x, y))
    }
    drawRect(Brush.verticalGradient(0.7f to Color.Transparent, 1f to Color.White.copy(alpha = 0.06f)))
}

/** A clear night: stars twinkling, and now and then a shooting star crossing. */
private fun DrawScope.drawStars(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF03050D), Color(0xFF0B1230), Color(0xFF1B1840))))
    val rnd = java.util.Random(5)
    repeat(160) {
        val x = rnd.nextFloat() * w
        val y = rnd.nextFloat() * h * 0.9f
        val r = (0.8f + rnd.nextFloat() * 1.8f) * (w / 1920f)
        val tw = 0.5f + 0.5f * kotlin.math.sin((t * (3 + rnd.nextInt(6)) + rnd.nextFloat()) * TWO_PI)
        drawCircle(Color.White.copy(alpha = 0.25f + 0.6f * tw), r, Offset(x, y))
    }
    // Three shooting stars a round, each lit for a short stretch of it.
    for (k in 0 until 3) {
        val start = k / 3f + 0.05f
        val p = (t - start) / 0.04f
        if (p in 0f..1f) {
            val sx = w * (0.25f + 0.25f * k)
            val sy = h * (0.08f + 0.1f * k)
            val head = Offset(sx + p * w * 0.22f, sy + p * h * 0.14f)
            val tail = Offset(head.x - w * 0.08f, head.y - h * 0.05f)
            val a = kotlin.math.sin(p * Math.PI.toFloat())
            drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.9f * a)), tail, head), tail, head, strokeWidth = 2.5f * (w / 1920f))
        }
    }
}

/** Big soft circles of colour drifting out of focus over a deep page, like city lights through a lens. */
private fun DrawScope.drawBokeh(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.linearGradient(listOf(Color(0xFF0D0716), Color(0xFF061A1F)), Offset.Zero, Offset(w, h)))
    val cols = listOf(Color(0xFFFF8FB1), Color(0xFF7DD3FC), Color(0xFFFCD34D), Color(0xFFA78BFA), Color(0xFF5EEAD4))
    val rnd = java.util.Random(31)
    repeat(26) {
        val bx = rnd.nextFloat()
        val by = rnd.nextFloat()
        val laps = 1 + rnd.nextInt(2)
        val ph = rnd.nextFloat()
        val r = (50f + rnd.nextFloat() * 130f) * (w / 1920f)
        val x = (bx + 0.04f * kotlin.math.sin((t * laps + ph) * TWO_PI)) * w
        val y = (by + 0.05f * kotlin.math.cos((t * laps + ph) * TWO_PI)) * h
        val c = cols[it % cols.size]
        val a = 0.08f + 0.1f * (0.5f + 0.5f * kotlin.math.sin((t * (laps + 1) + ph) * TWO_PI))
        drawCircle(Brush.radialGradient(0f to c.copy(alpha = a), 0.75f to c.copy(alpha = a * 0.8f), 1f to Color.Transparent, center = Offset(x, y), radius = r), r, Offset(x, y))
    }
}

/** Light: mother-of-pearl — pastel waves rolling slowly over a pale page. */
private fun DrawScope.drawPearl(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.linearGradient(listOf(Color(0xFFF7F2FB), Color(0xFFEAF3FA), Color(0xFFFBEFF3)), Offset.Zero, Offset(w, h)))
    val cols = listOf(Color(0xFFF9C6D9), Color(0xFFD9C8F5), Color(0xFFBFE0F5), Color(0xFFC9EEDF))
    cols.forEachIndexed { k, c ->
        val path = androidx.compose.ui.graphics.Path()
        val base = h * (0.5f + 0.12f * k)
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            val y = base + h * 0.06f * kotlin.math.sin(x / w * TWO_PI * (0.9f + 0.2f * k) + (t + k * 0.25f) * TWO_PI)
            path.lineTo(x, y)
            x += w / 48f
        }
        path.lineTo(w, h); path.close()
        drawPath(path, c.copy(alpha = 0.55f))
    }
}

/** Light: a dawn sky — peach to pink to pale blue, soft clouds drifting across. */
private fun DrawScope.drawDawn(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFFBFD9F2), Color(0xFFF6D1DC), Color(0xFFFFE1C7))))
    val rnd = java.util.Random(9)
    repeat(9) {
        val by = 0.1f + rnd.nextFloat() * 0.6f
        val bx = rnd.nextFloat()
        val laps = 1 + rnd.nextInt(2)
        val cw = (260f + rnd.nextFloat() * 340f) * (w / 1920f)
        val x = ((bx + t * laps) % 1f) * (w + cw * 2) - cw
        val y = by * h
        for (j in 0 until 4) {
            val cx = x + (j - 1.5f) * cw * 0.32f
            val cy = y + (if (j % 2 == 0) 0f else -cw * 0.08f)
            val rr = cw * (0.22f + 0.06f * (j % 2))
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.5f), Color.Transparent), Offset(cx, cy), rr), rr, Offset(cx, cy))
        }
    }
}

/** Light: cherry petals falling and turning over a cream page. */
private fun DrawScope.drawPetals(t: Float) {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF8F3), Color(0xFFFCEFF1))))
    val rnd = java.util.Random(17)
    repeat(46) {
        val bx = rnd.nextFloat()
        val by = rnd.nextFloat()
        val laps = 1 + rnd.nextInt(3)
        val s = (10f + rnd.nextFloat() * 14f) * (w / 1920f)
        val y = ((by + t * laps) % 1f) * (h + 40f) - 20f
        val x = bx * w + kotlin.math.sin((t * laps * 2 + bx) * TWO_PI) * 40f * (w / 1920f)
        val turn = (t * laps * 360f * 2 + bx * 360f)
        val c = if (it % 3 == 0) Color(0xFFF4A7BE) else if (it % 3 == 1) Color(0xFFF9C4D2) else Color(0xFFEE93AD)
        rotate(turn, Offset(x, y)) {
            drawOval(c.copy(alpha = 0.8f), topLeft = Offset(x - s, y - s * 0.55f), size = androidx.compose.ui.geometry.Size(s * 2, s * 1.1f))
        }
    }
}

private sealed interface PhoneWall {
    data class Anim(val kind: String) : PhoneWall
    data class Photo(val name: String) : PhoneWall
}

/**
 * glcn: the phone's wallpaper gallery — the whole screen shows the one you tap, the panel at the foot lists them
 * (Animated / Dark / Light); "Set it" keeps it (a photo is downloaded once), Back leaves yours.
 */
@Composable
fun WallpaperGallery(current: String?, onPicked: (path: String, picture: Boolean) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalStagePalette.current
    val accent = stageAccent
    val currentItem: PhoneWall? = Wallpapers.animKind(current)?.let { PhoneWall.Anim(it) } ?: Wallpapers.nameOf(current)?.let { PhoneWall.Photo(it) }
    var sel by remember { mutableStateOf(currentItem ?: PhoneWall.Anim(Wallpapers.animated.first())) }
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var failed by remember { mutableStateOf(false) }
    fun modelOf(w: PhoneWall): Any = when (w) {
        is PhoneWall.Anim -> Wallpapers.ANIM_PREFIX + w.kind
        is PhoneWall.Photo -> Wallpapers.model(context, w.name)
    }
    fun apply() {
        if (loading) return
        when (val w = sel) {
            is PhoneWall.Anim -> { onPicked(Wallpapers.ANIM_PREFIX + w.kind, false); onDismiss() }
            is PhoneWall.Photo -> {
                loading = true; failed = false; progress = 0f
                scope.launch {
                    val path = Wallpapers.download(context, w.name) { progress = it }
                    loading = false
                    if (path == null) failed = true else { onPicked(path, true); onDismiss() }
                }
            }
        }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            Crossfade(targetState = sel, animationSpec = tween(PREVIEW_FADE_MS), label = "wallPreview", modifier = Modifier.fillMaxSize()) { w ->
                WallLayer(modelOf(w), Modifier.fillMaxSize())
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(palette.page.copy(alpha = 0.86f), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .navigationBarsPadding().padding(top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.glcn_wall_title), color = palette.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 20.dp))
                Text(
                    when {
                        loading -> stringResource(R.string.glcn_wall_downloading, (progress * 100).toInt())
                        failed -> stringResource(R.string.glcn_wall_failed)
                        else -> stringResource(R.string.glcn_wall_desc_phone)
                    },
                    color = if (failed) Color(0xFFFF6B6B) else palette.muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp),
                )
                listOf(
                    stringResource(R.string.glcn_wall_animated) to Wallpapers.animated.map { PhoneWall.Anim(it) },
                    stringResource(R.string.glcn_wall_dark) to Wallpapers.dark.map { PhoneWall.Photo(it) },
                    stringResource(R.string.glcn_wall_light) to Wallpapers.light.map { PhoneWall.Photo(it) },
                ).forEach { (label, items) ->
                    Text(label, color = palette.dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(items) { w ->
                            val chosen = w == sel
                            Column(Modifier.width(TILE_W.dp).clickable { if (!loading) { sel = w; failed = false } }) {
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(12.dp))
                                        .border(if (chosen) 2.5.dp else 0.dp, if (chosen) accent.accent else Color.Transparent, RoundedCornerShape(12.dp)),
                                ) {
                                    WallLayer(if (w is PhoneWall.Photo) Wallpapers.thumbUri(w.name) else modelOf(w), Modifier.fillMaxSize())
                                    if (loading && chosen) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0.03f, 1f)).height(4.dp).background(accent.accent))
                                }
                                Text(wallName(w), color = if (chosen) accent.accent else palette.text, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(palette.ink.copy(alpha = 0.08f)).clickable(onClick = onDismiss).padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.common_cancel), color = palette.text, fontWeight = FontWeight.Bold) }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(accent.accent).clickable { apply() }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(stringResource(R.string.glcn_wall_use), color = accent.onAccent, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun wallName(w: PhoneWall): String = stringResource(
    when (w) {
        is PhoneWall.Anim -> when (w.kind) {
            "ondas" -> R.string.glcn_wall_ondas
            "brillos" -> R.string.glcn_wall_brillos
            "nieve" -> R.string.glcn_wall_nieve
            "fugaces" -> R.string.glcn_wall_fugaces
            "bokeh" -> R.string.glcn_wall_bokeh
            "nacar" -> R.string.glcn_wall_nacar
            "amanecer" -> R.string.glcn_wall_amanecer
            "petalos" -> R.string.glcn_wall_petalos
            else -> R.string.glcn_style_aurora
        }
        is PhoneWall.Photo -> when (w.name) {
            "via_lactea" -> R.string.glcn_wall_via_lactea
            "aurora_nieve" -> R.string.glcn_wall_aurora_nieve
            "aurora_pinos" -> R.string.glcn_wall_aurora_pinos
            "puerto" -> R.string.glcn_wall_puerto
            "bosque" -> R.string.glcn_wall_bosque
            "nieve_violeta" -> R.string.glcn_wall_nieve_violeta
            "siluetas" -> R.string.glcn_wall_siluetas
            "pico_lila" -> R.string.glcn_wall_pico_lila
            "nubes_rosa" -> R.string.glcn_wall_nubes_rosa
            "duna" -> R.string.glcn_wall_duna
            "mar_beige" -> R.string.glcn_wall_mar_beige
            "muelle" -> R.string.glcn_wall_muelle
            "valle" -> R.string.glcn_wall_valle
            else -> R.string.glcn_wall_cordillera
        }
    },
)

private const val WALL_ROUND_MS = 45_000
private const val PREVIEW_FADE_MS = 260
private const val TILE_W = 84
