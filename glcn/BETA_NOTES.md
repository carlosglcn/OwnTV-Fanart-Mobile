## Beta

Versión de pruebas de OwnTV Fanart. Se instala al lado de la normal.
- 🧯 Emby / Jellyfin sync reads the library a page at a time instead of holding every episode in memory at once; series are grouped by genre like films
- 🇪🇸 Emby / Jellyfin genres in Spanish when the app is in Spanish (Ciencia ficción, Miniseries, Documental…), so a library tagged in English and one in Spanish share their categories
- 🏷️ Emby / Jellyfin series by platform: variants joined (HBO Nordic → HBO…), small networks in "Other networks"
- 🧠 Memory: now/next caches bounded, Home reads the live channels once
- 🔄 Newest core (Emby TMDB ids, Xtream title hints, platform order and logos, TMDB logos of your country) and the original OwnTV Mobile's latest (Stalker login fields, playlist counts); builds on Java 25
- 🧩 Newest core: guide refresh only when it changed, ASS/SSA subtitles on mpv, disc folders on network shares, chapters from mpv
- 📺 **Live TV, quicker to run through:** the picture cards go two to a row on a phone (one card used to fill half the screen), and the tools sit under the search with their names — Provider, Guide, Sports now, List / Cards
- 🏠 **Home:** the cover is lower (Keep watching shows under it), "Sports now" leaves out the match already in the cover, and Starting soon / Tonight have the same heading as the other rows
- 🔄 **Sync notice at the top, small:** "Syncing Emby" no longer sits over the cards at the foot of the screen
- 🎞️ **Library posters without the playlist mark** ("EMBY" on every poster sat over the title art); the list view still says where each title comes from
- ⚙️ **Settings in fewer cards:** Picture, Sound & subtitles and Watching & recording now live inside Playback, each under its own heading (10 cards instead of 13), and the cards lose the setting counts
- 🧠 Emby/Jellyfin sync uses far less memory: the library is written to a file as it is read instead of being held whole in memory.
- 📋 Playback report: the diagnostic lines come from the saved file, so what happened just before a freeze or a crash is in the report.
- ⭐ Occasional "Star on GitHub?" prompt (after a week and 10 launches, at most twice; "Already done" or "Open GitHub" hides it for good) and the same option in About
- 🔑 Lists that refuse the usual player identity now play without touching anything: OwnTV Fanart tries another identity by itself and remembers the one that works for that list. If every one is refused, the error points you to the User-Agent setting.
- 📂 Live TV: a "Categories" button at the start of the tools row (the green "‹ All channels" still opens them too).
- 🔥 Home: "Trending films" and "Trending series" rows of their own, switched on under Now trending in Home settings (off by default; the mixed row stays).
- 🗂️ Library categories in sections — Platforms, Genres and More categories — and no playlist mark when everything comes from one playlist.
- ⏭️ Skip intro: each episode's real intro times from TheIntroDB; with nothing known the button no longer appears (no blind jump).
- ⭐ Star prompt: notes you can sign in to GitHub with your Google account.
