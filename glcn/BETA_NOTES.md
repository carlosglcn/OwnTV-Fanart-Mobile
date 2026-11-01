## Beta

Versión de pruebas de OwnTV Fanart. Se instala al lado de la normal.
- 🧯 Emby / Jellyfin sync reads the library a page at a time instead of holding every episode in memory at once; series are grouped by genre like films
- 🇪🇸 Emby / Jellyfin genres in Spanish when the app is in Spanish (Ciencia ficción, Miniseries, Documental…), so a library tagged in English and one in Spanish share their categories
- 🏷️ Emby / Jellyfin series by platform: variants joined (HBO Nordic → HBO…), small networks in "Other networks"
- 🧠 Memory: now/next caches bounded, Home reads the live channels once
- 🔄 Newest core (Emby TMDB ids, Xtream title hints, platform order and logos, TMDB logos of your country) and the original OwnTV Mobile's latest (Stalker login fields, playlist counts); builds on Java 25
- 🧩 Newest core: guide refresh only when it changed, ASS/SSA subtitles on mpv, disc folders on network shares, chapters from mpv
