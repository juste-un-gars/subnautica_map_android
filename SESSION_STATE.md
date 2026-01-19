# SESSION_STATE.md

**Last Updated:** 2026-01-19 12:00
**Current Session:** SESSION_003_markers_fogofwar
**Status:** Complete - Ready for testing

---

## Current Session Summary

### SESSION_003: Marker Info Popups & Fog of War

**Goal:** Add clickable markers with info popups and fog of war system with persistence

**Status:** Complete - Ready for testing

**What was accomplished:**

**Marker Info Popups:**
- Added tap detection on map markers (player, beacons, vehicles)
- Created info popup card showing details when marker is tapped
- Added selection highlight ring around tapped markers
- Popup shows: type, name, position, depth, and other relevant info

**Fog of War System:**
- Implemented chunk-based fog of war (50x50 world units per chunk = 50m)
- Created FogOfWarRepository for persistence (saves to file)
- Fog state saved automatically as player explores
- Toggle fog on/off via cloud icon in toolbar
- "Reset Exploration" option to clear all discovered areas
- Exploration percentage displayed in info panel (e.g., "Explored: 2.3%")
- Total 6400 chunks (80x80 grid covering 4000x4000 world)

---

## Quick Resume

To continue: `"continue"` or `"let's continue"`

**Next steps to implement:**
1. Test marker popups and fog of war
2. Add "About" screen with credits (rocketsoup.net, libraries)
3. Multiple map layers (caves, lost river, etc.)

---

## Recent Sessions

| Session | Date | Status | Summary |
|---------|------|--------|---------|
| SESSION_003 | 2026-01-19 | Complete | Marker popups & fog of war |
| SESSION_002 | 2026-01-19 | Complete | Interactive map with zoom/pan |
| SESSION_001 | 2026-01-19 | Complete | Initial Android app setup |

---

## Project Progress

### Completed
- [x] Android project structure
- [x] API models (GameState, PlayerInfo, etc.)
- [x] Retrofit API service
- [x] Settings persistence (DataStore)
- [x] Connection screen UI
- [x] Map screen (data display)
- [x] Keep screen on option
- [x] Test with actual Subnautica mod
- [x] Interactive map with zoom/pan
- [x] Player marker with heading
- [x] Vehicle markers
- [x] Beacon markers
- [x] Clickable markers with info popups
- [x] Fog of war with persistence

### Planned
- [ ] About screen with credits
- [ ] Map options:
  - [ ] Option to use detailed map (map-details.jpg) vs blank map (map-blank.jpg)
  - [ ] Easy toggle for fog of war (already exists in menu, maybe add to settings)
- [ ] Multiple map layers (caves, lost river, etc.)

---

## Files Modified This Session

### New Files
- `app/src/main/java/com/music/music/subnauticamap/data/repository/FogOfWarRepository.kt`

### Modified Files
- `app/src/main/java/com/music/music/subnauticamap/ui/map/components/InteractiveMapView.kt`
  - Added tap detection for markers
  - Added SelectedMarker sealed class
  - Added MarkerInfoCard popup
  - Added fog of war rendering
  - Added chunk utility functions
- `app/src/main/java/com/music/music/subnauticamap/ui/map/MapViewModel.kt`
  - Added FogOfWarRepository integration
  - Added toggleFogOfWar() and resetFogOfWar()
  - Added exploration tracking in polling
- `app/src/main/java/com/music/music/subnauticamap/ui/map/MapScreen.kt`
  - Added fog of war menu in toolbar
  - Added exploration stats to info panel
  - Updated InteractiveMapView call with fog parameters
- `app/src/main/java/com/music/music/subnauticamap/MainActivity.kt`
  - Added FogOfWarRepository creation

---

## Fog of War Technical Details

**Chunk System:**
- World size: 4000m x 4000m (-2000 to +2000 on X and Z)
- Chunk size: 50m x 50m (user requested ~50m visibility)
- Grid: 80 x 80 = 6400 total chunks
- Storage: Binary file with packed Long keys (8 bytes per chunk)

**Chunk Key Encoding:**
```kotlin
chunkKey = (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)
```

**World to Chunk Conversion:**
```kotlin
chunkX = ((worldX + 2000) / 50).toInt()
chunkZ = ((2000 - worldZ) / 50).toInt()
```

**Persistence:**
- File: `explored_chunks.dat` in app internal storage
- Fog enabled state: DataStore preferences
- Saved automatically on each new chunk explored

---

## Notes for Next Session

- User wants "About" screen to credit:
  - Map creator: rocketsoup.net/blog
  - Libraries used (Retrofit, Coil, etc.)
- Consider adding smooth fog edges (gradient instead of hard edges)
- Could add mini-map in corner showing explored areas

---

## Map Resources

**Available Maps (from rocketsoup.net):**
| Map | URL | Description |
|-----|-----|-------------|
| Blank | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-blank.jpg | No labels, for fog of war |
| Detailed | https://rocketsoup.net/blogassets/subnautica/2024-07-20/map-details.jpg | With biome labels & points of interest |

---

## Credits to Add (About Screen)

**Map:**
- Source: https://blog.rocketsoup.net/2024/07/16/subnautica-maps/
- Author: rocketsoup.net

**Libraries:**
- Retrofit (com.squareup.retrofit2) - HTTP client
- OkHttp (com.squareup.okhttp3) - HTTP networking
- Coil (io.coil-kt) - Image loading
- Gson (com.google.code.gson) - JSON parsing
- Jetpack Compose - UI framework
- Navigation Compose - Navigation
- DataStore - Preferences storage
