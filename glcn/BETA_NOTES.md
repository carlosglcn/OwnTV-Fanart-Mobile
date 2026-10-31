## Beta

Versión de pruebas de OwnTV Fanart. Se instala al lado de la normal.

- 🔄 Merged the latest from the original OwnTV Mobile (thanks, ahXN00): per-playlist "Clean channel names", guide priority with Move up / Move down in EPG sources, and starting on the last channel in Live TV or the TV guide
- 🧩 Updated to the current shared core (the same as the TV beta): sports additions (more handball leagues, tennis rankings, cycling, snooker, country presets), the newer guide and database, and faster playlist writes
- 💬 About: the OwnTV Fanart Telegram group with its QR code (t.me/owntvfanart), above the original project's
- ⚡ Player: the list button at the top (and a swipe up) opens the new quick zap panel — the channels of the list you are watching with logo, programme on air and progress, and what comes next — along the side of the picture when held sideways, rising from the bottom when upright. A search on top finds any channel by name across your playlists; a tap changes channel
- 📁 Player: the button on the bar now reads "Categories" with a folder icon (it was a second, identical list button)
- 🖼️ TV guide pictures: when a guide offers a wide picture for a programme (XMLTV <image type="backdrop">, a still or a landscape icon), it is used instead of the upright poster, whose printed title used to clash with the programme's own title. Applies from the next guide update
- ¹² TV guide and zap panels: a channel's feed number written as a superscript (M+ Terror ¹, M+ Terror ²) is kept in its name, so two feeds of one channel no longer look like the same row
- 🏷️ Movies and Series: quality badges read from the title — on the posters the best picture mark (DV in gold, HDR10+, HDR10, HDR, else 4K) and REMUX (also when written as [4K R] / [DV R]); on the title's page every mark (resolution, REMUX, HDR, Atmos/7.1/5.1)
- 🎨 About: the OwnTV Fanart Telegram QR is now drawn like Telegram's own group codes — rounded dots in the Fanart colours, the Telegram plane in the middle and @OWNTVFANART below
- 🖼️ TV guide: programme pictures are stored again on every guide update (they had been left out since the core update)
- 📺 Sports › Watch live: channels ordered by how surely their guide names the match, then by picture quality
- 🖼️ Films and series with a TMDB id typed by hand: the TMDB poster replaces the provider's on every card, also after re-syncs
- 🆕 Movies and Series open sorted by date added (newest first) unless you chose another order
- 🏎️ Racing: while a session runs, the match page lists the whole live order (the follow sheet still offers the first three)
- 📺 Player: "Watch on TV" button in the top bar — the channel or film on screen opens on the paired TV.
- 🎮 More › TV remote: the phone as the paired TV's remote (arrows, OK, Back, play/pause, channel, volume) and keyboard (typed words open Search on the TV).
- 🖼️ Home: with Featured first, its slides become a tall edge-to-edge fanart cover with a slow push-in, fading into the page.
- 🧩 New home-screen widget "On now in your favourites": what four favourite channels are showing; tap to watch.
- 📁 Live TV: the Favourites groups made on the TV show as folders under Favourites.
- ⏰ Home: new "Starting soon" row (what starts within the half hour on your favourite and recent channels).
- 🗣️ Trailers in your language first (metadata language, else the app's or the system's), English as the fallback.
- 🔒 Beta: test builds are now published to a private repository, so only the beta can see them. Paste the read-only access token once in About › Beta access to keep updating from the app
- 🔄 Merged the latest from the original OwnTV (thanks, ahXN00 and its contributors):
  - 🔒 Lock a category behind the Customize PIN, and ask for the PIN to open Settings — per profile; reminders on a locked channel wait until it is opened
  - 📝 Playlists: edits re-sync through the shared core; a backup restored without passwords keeps the playlists' names
  - ↕️ Moved items keep their place, deleting asks first, and no "0 min" after Mark as watched
- 🧠 Now on the same core as the TV app:
  - 💬 Live TV: teletext subtitles (page 888) — Movistar+, La 1 and most Spanish channels — on the standard player
  - 🔈 Live TV on mpv in stereo (Dolby 5.1 channels froze on one frame)
  - 🎞️ Films: each file opens on the player that suits it (4K/HDR/Dolby Vision on ExoPlayer, unusual containers on mpv); Dolby Vision profile 7 plays as 8.1; a film stuck on its first frame moves to the other player
  - ⏰ Habit reminders one at a time: the most watched keeps the slot
- 📺 Live TV: when a guide lists two programmes at once, every screen names the same one, and "Up next" is what starts after it ends
- 🔐 Emby / Jellyfin and WebDAV: they now use the app's own connection settings — the newer Let's Encrypt certificates the TV does not know, your custom DNS and proxy. A server whose certificate moved to the new roots said "Could not reach the server" on the TV while the phone reached it
- 🔐 Secure connections: a server that sends its certificate without the intermediate one (common behind a NAS reverse proxy) is now accepted — the app fetches the missing certificate itself, as browsers do. Emby on such a server said "Trust anchor for certification path not found"
