package tv.own.owntv.mobile.auto

import android.media.MediaDescription
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import tv.own.owntv.core.customize.CustomizationStore
import tv.own.owntv.core.database.dao.ChannelDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.entity.ChannelEntity
import tv.own.owntv.core.epg.displayLogoUrl
import tv.own.owntv.core.live.GuideReader
import tv.own.owntv.core.live.NowArt
import tv.own.owntv.core.live.SportsNow
import tv.own.owntv.core.model.MediaType
import tv.own.owntv.core.repository.activeSourceIds
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.mobile.R
import tv.own.owntv.mobile.playback.PipController
import tv.own.owntv.mobile.ui.screens.live.LiveTuner
import java.text.Normalizer

/**
 * glcn: OwnTV in Android Auto. The car shows Favourites, Recent, Sports now and All channels, each
 * channel with what it is showing; picking one plays it as sound (a car never shows the picture while
 * driving). Next/previous on the steering wheel change channel, and "Ok Google, play <channel> on
 * OwnTV" finds it by name.
 *
 * The car talks to its own media session here; the player itself is the app's usual one, so the
 * phone's notification, the lock screen and the car all show and control the same stream.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoBrowserService : MediaBrowserService() {

    private val tuner: LiveTuner by inject()
    private val pip: PipController by inject()
    private val settings: SettingsRepository by inject()
    private val sourceDao: SourceDao by inject()
    private val channelDao: ChannelDao by inject()
    private val customize: CustomizationStore by inject()
    private val guide: GuideReader by inject()
    private val nowArt: NowArt by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession

    override fun onCreate() {
        super.onCreate()
        session = MediaSession(this, "OwnTV-Auto").apply {
            setCallback(callback)
            isActive = true
        }
        sessionToken = session.sessionToken
        publishState(playing = false, title = null, subtitle = null, art = null)
        // Mirror whatever the app's player is doing.
        tuner.activeEngine
            .flatMapLatest { engine -> combine(engine.isPlaying, tuner.channel) { playing, ch -> playing to ch } }
            .onEach { (playing, ch) ->
                val now = ch?.let { runCatching { nowArt.now(it) }.getOrNull() }
                publishState(
                    playing = playing,
                    title = ch?.name,
                    subtitle = now?.title?.let(SportsNow::displayTitle),
                    art = now?.imageUrl?.takeIf { it.isNotBlank() } ?: ch?.displayLogoUrl,
                )
            }
            .launchIn(scope)
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { session.isActive = false; session.release() }
        super.onDestroy()
    }

    // --- Browsing ------------------------------------------------------------------------------

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot =
        BrowserRoot(ROOT, Bundle().apply {
            // Folders as a list, channels as a grid of logos.
            putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
            putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 2)
        })

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowser.MediaItem>>) {
        result.detach()
        scope.launch {
            val items = runCatching { withContext(Dispatchers.IO) { children(parentId) } }.getOrDefault(emptyList())
            result.sendResult(items.toMutableList())
        }
    }

    private suspend fun children(parentId: String): List<MediaBrowser.MediaItem> {
        if (parentId == ROOT) {
            return listOf(
                folder(FAVOURITES, getString(R.string.glcn_auto_favorites)),
                folder(RECENT, getString(R.string.glcn_auto_recent)),
                folder(SPORTS, getString(R.string.glcn_sports_now)),
                folder(ALL, getString(R.string.glcn_auto_all)),
            )
        }
        val pid = settings.activeProfileId.first().takeIf { it >= 0 } ?: return emptyList()
        val ids = activeSourceIds(settings, sourceDao, pid, MediaType.LIVE)
        if (ids.isEmpty()) return emptyList()
        val cust = customize.observe(pid, MediaType.LIVE).first()
        val channels: List<ChannelEntity> = when (parentId) {
            FAVOURITES -> channelDao.favoritesListAlpha(pid).first().filter { it.sourceId in ids }
            RECENT -> channelDao.recentlyWatchedWithTimestampFiltered(pid, ids, 20).first().map { it.channel }
            SPORTS -> SportsNow(channelDao, guide).find(ids, cust, settings.epgOffsetMinutes.first()).map { it.channel }
            ALL -> channelDao.allForSources(ids, 300)
            else -> emptyList()
        }.filter { CustomizeKeysCompat.visible(it, cust) }.take(MAX_ITEMS)
        val now = runCatching { nowArt.many(channels) }.getOrDefault(emptyMap())
        return channels.map { ch -> ch.toItem(now[ch.id]?.title) }
    }

    private suspend fun search(query: String): List<ChannelEntity> {
        val pid = settings.activeProfileId.first().takeIf { it >= 0 } ?: return emptyList()
        val ids = activeSourceIds(settings, sourceDao, pid, MediaType.LIVE)
        val want = fold(query)
        if (want.isEmpty()) return emptyList()
        val all = channelDao.allForSources(ids, 5_000)
        return all.filter { fold(it.name) == want }.ifEmpty { all.filter { fold(it.name).contains(want) } }.take(MAX_ITEMS)
    }

    private fun folder(id: String, title: String) = MediaBrowser.MediaItem(
        MediaDescription.Builder().setMediaId(id).setTitle(title).build(),
        MediaBrowser.MediaItem.FLAG_BROWSABLE,
    )

    private fun ChannelEntity.toItem(nowTitle: String?) = MediaBrowser.MediaItem(
        MediaDescription.Builder()
            .setMediaId(CHANNEL_PREFIX + id)
            .setTitle(name)
            .setSubtitle(nowTitle?.let(SportsNow::displayTitle))
            .setIconUri(ArtProvider.uriFor(this@AutoBrowserService, displayLogoUrl))
            .build(),
        MediaBrowser.MediaItem.FLAG_PLAYABLE,
    )

    // --- Playing -------------------------------------------------------------------------------

    private val callback = object : MediaSession.Callback() {
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            val id = mediaId?.removePrefix(CHANNEL_PREFIX)?.toLongOrNull() ?: return
            play(id)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            scope.launch {
                val q = query.orEmpty()
                val target = if (q.isBlank()) {
                    // "Play OwnTV": carry on with the last channel.
                    val pid = settings.activeProfileId.first()
                    val ids = activeSourceIds(settings, sourceDao, pid, MediaType.LIVE)
                    withContext(Dispatchers.IO) { channelDao.recentlyWatchedWithTimestampFiltered(pid, ids, 1).first().firstOrNull()?.channel }
                } else withContext(Dispatchers.IO) { search(q).firstOrNull() }
                target?.let { play(it.id) }
            }
        }

        override fun onPlay() {
            if (!tuner.currentEngine.isPlaying.value) tuner.currentEngine.togglePlayPause()
        }

        override fun onPause() {
            if (tuner.currentEngine.isPlaying.value) tuner.currentEngine.togglePlayPause()
        }

        override fun onSkipToNext() = tuner.step(1)
        override fun onSkipToPrevious() = tuner.step(-1)
        override fun onStop() = tuner.stop()
    }

    private fun play(channelId: Long) {
        tuner.tune(channelId)
        // No picture in the car: once the channel is open, drop its video decoder unless the phone's
        // own player is on screen.
        scope.launch {
            delay(1_500)
            if (!pip.playerOnScreen.value) tuner.currentEngine.enterAudioOnly()
        }
    }

    private fun publishState(playing: Boolean, title: String?, subtitle: String?, art: String?) {
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_PLAY_FROM_MEDIA_ID or PlaybackState.ACTION_PLAY_FROM_SEARCH
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build(),
        )
        session.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, subtitle ?: title.orEmpty())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, title.orEmpty())
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, subtitle ?: title.orEmpty())
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, title.orEmpty())
                .apply {
                    ArtProvider.uriFor(this@AutoBrowserService, art)?.toString()?.let {
                        putString(MediaMetadata.METADATA_KEY_ART_URI, it)
                        putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, it)
                    }
                }
                .build(),
        )
    }

    private object CustomizeKeysCompat {
        fun visible(ch: ChannelEntity, cust: tv.own.owntv.core.customize.SectionCustomizations): Boolean =
            tv.own.owntv.core.customize.CustomizeKeys.channel(ch) !in cust.hiddenItems
    }

    private companion object {
        const val ROOT = "root"
        const val FAVOURITES = "favourites"
        const val RECENT = "recent"
        const val SPORTS = "sports"
        const val ALL = "all"
        const val CHANNEL_PREFIX = "ch:"
        const val MAX_ITEMS = 100

        fun fold(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase().replace(Regex("[^a-z0-9+]+"), " ").trim()
    }
}
