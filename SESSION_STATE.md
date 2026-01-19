# SESSION_STATE.md

**Last Updated:** 2026-01-19 15:30
**Current Session:** SESSION_004_fog_fixes_fullscreen
**Status:** Complete

---

## Current Session Summary

### SESSION_004: Fog of War Fixes, Fullscreen Mode & UX Improvements

**Goal:** Fix fog of war alignment, make it fully opaque, use circular reveals, enable fullscreen mode, and add double-back-to-exit

**Status:** Complete

**What was accomplished:**

**Fog of War Fixes:**
- Fixed fog alignment issue: now correctly accounts for square map in portrait container
- Added `effectiveMapSize` and `mapOffsetY` calculations for proper positioning
- Map image is hidden until first data is received from server
- Shows "Waiting for game data..." message while waiting for connection
- Made fog completely opaque - hidden terrain is now invisible
- Implemented **circular reveals** (50m radius) using `BlendMode.Clear` with `CompositingStrategy.Offscreen`
- New `worldToMapPositionWithOffset()` function for correct marker positioning
- New `drawFogOfWarCircles()` function for circular fog reveals

**Fullscreen Immersive Mode:**
- App now launches in true fullscreen mode (no status bar, no navigation bar)
- Added `FLAG_LAYOUT_NO_LIMITS` for full edge-to-edge display
- Updated themes.xml with fullscreen and cutout mode settings
- Uses immersive sticky mode - swipe from edge shows bars temporarily
- Re-hides bars on focus change and resume

**UX Improvements:**
- Added **double-back-to-exit** functionality
- First back press shows toast "Press back again to exit"
- Second back press within 2 seconds closes the app

---

## Quick Resume

To continue: `"continue"` or `"let's continue"`

**Next steps to implement:**
1. Add "About" screen with credits (rocketsoup.net, libraries)
2. Multiple map layers (caves, lost river, etc.)
3. Option to use detailed map vs blank map

---

## Recent Sessions

| Session | Date | Status | Summary |
|---------|------|--------|---------|
| SESSION_004 | 2026-01-19 | Complete | Fog fixes, fullscreen, circular reveals, double-back-to-exit |
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
- [x] Circular fog reveals (50m radius)
- [x] Fullscreen immersive mode
- [x] Double-back-to-exit

### Planned
- [ ] About screen with credits
- [ ] Map options:
  - [ ] Option to use detailed map (map-details.jpg) vs blank map (map-blank.jpg)
- [ ] Multiple map layers (caves, lost river, etc.)

---

## Files Modified This Session (SESSION_004)

### Modified Files
- `app/src/main/java/com/music/music/subnauticamap/MainActivity.kt`
  - Added setupFullscreen() with FLAG_LAYOUT_NO_LIMITS
  - Added hideSystemBars() with WindowInsetsControllerCompat
  - Added onWindowFocusChanged and onResume to maintain fullscreen
  - Added BackHandler for double-back-to-exit functionality
  - Added Toast message for first back press
- `app/src/main/java/com/music/music/subnauticamap/ui/map/MapViewModel.kt`
  - Added `hasReceivedData` flag to MapUiState
  - Set hasReceivedData = true when data is received successfully
- `app/src/main/java/com/music/music/subnauticamap/ui/map/MapScreen.kt`
  - Pass hasReceivedData to InteractiveMapView
- `app/src/main/java/com/music/music/subnauticamap/ui/map/components/InteractiveMapView.kt`
  - Added hasReceivedData parameter
  - Hide map content until data is received
  - Added "Waiting for game data..." loading state
  - Added effectiveMapSize and mapOffsetY for correct positioning
  - Added worldToMapPositionWithOffset() function
  - Added drawFogOfWarCircles() with BlendMode.Clear and CompositingStrategy.Offscreen
  - Fixed findTappedMarker to use correct coordinates
- `app/src/main/res/values/themes.xml`
  - Added windowFullscreen, windowLayoutInDisplayCutoutMode
  - Added windowTranslucentStatus/Navigation for true fullscreen

---

## Fog of War Technical Details

**Chunk System:**
- World size: 4000m x 4000m (-2000 to +2000 on X and Z)
- Chunk size: 50m x 50m (50m visibility radius)
- Grid: 80 x 80 = 6400 total chunks
- Storage: Binary file with packed Long keys (8 bytes per chunk)
- Circular reveals with radius = chunkPixelSize * 0.7 for smooth overlap

**Rendering:**
- Uses `CompositingStrategy.Offscreen` for BlendMode.Clear to work
- Draws black rectangle over map area first
- Then cuts out circles with `BlendMode.Clear` for explored chunks

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
- Could add mini-map in corner showing explored areas
- Consider option to switch between detailed and blank map

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
