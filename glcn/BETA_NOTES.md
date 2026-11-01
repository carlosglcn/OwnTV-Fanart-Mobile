## Beta

Versión de pruebas de OwnTV Fanart. Se instala al lado de la normal.

- 🎬 Films and series get their cast, title lettering, genres and trailer back: last version's trailer-language update made the cached TMDB details of every title with a trailer expire, and the cache clean-up after the next playlist sync deleted them; the titles then refetched with only the search result. Those incomplete titles are cleared once, so the next look fetches them whole (within the hourly allowance), and a failed fetch no longer replaces full details. The first beta of this fix asked for them again from every screen and ran out of memory
- 🏷️ Quality marks: the picture format before REMUX, and Dolby Vision spelled out on title pages
- ⚡ Faster matching of sport and film programmes and provider tags (prepared once instead of on every call)
- 🧯 Syncing several playlists no longer runs the app out of memory: on boxes with a small app memory (most TV boxes, the SHIELD included) the playlists sync one after another instead of all at once — a large list and Emby side by side filled it and the app froze
