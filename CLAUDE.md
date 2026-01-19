# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Key Documentation Files:**
- **[CLAUDE.md](CLAUDE.md)** - Project overview, objectives, architecture, session management
- **[SESSION_STATE.md](SESSION_STATE.md)** - Current session status and recent work
- **[CONNECTION.md](CONNECTION.md)** - API documentation for connecting to the Subnautica mod
- **[PROJET.md](PROJET.md)** - Full project specification (mod + Android app)

---

## Project Context

**Project Name:** Subnautica Map Android
**Tech Stack:** Android (Kotlin), Jetpack Compose
**Primary Language(s):** Kotlin
**Key Dependencies:** Retrofit/OkHttp (HTTP), Coil (images), Jetpack Compose (UI)
**Architecture Pattern:** MVVM with Repository pattern
**Development Environment:** Android Studio

---

## Project Goal

Develop an **Android companion app** for Subnautica that:
- Connects to the **MapAPI mod** running on the PC (via local WiFi)
- Displays an **interactive map** of Subnautica with multiple depth layers
- Shows **real-time player position** overlaid on the map
- Displays **beacons** and **vehicles** (Seamoth, Cyclops, Prawn Suit)
- Supports **day/night cycle** indicator
- Shows current **biome** and **depth**

**Note:** The Subnautica mod (server side) is already complete. This repository focuses on the Android client only.

---

## Architecture Overview

```
┌────────────────────────────────────┐
│         MOD SUBNAUTICA             │
│  (Already done - separate repo)    │
│  • HTTP Server on port 63030       │
│  • GET /api/ping (health check)    │
│  • GET /api/state (game data)      │
└────────────────┬───────────────────┘
                 │ HTTP JSON (polling 1x/sec)
                 │ (same local WiFi network)
                 ▼
┌────────────────────────────────────┐
│         APP ANDROID (this repo)    │
│                                    │
│  • Connection screen (IP config)   │
│  • Interactive map with layers     │
│  • Player position overlay         │
│  • Beacons & vehicles markers      │
│  • Depth/biome info display        │
└────────────────────────────────────┘
```

---

## API Connection

### Base URL
```
http://<PC_IP>:63030
```

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/ping` | GET | Health check - returns `{"status":"ok","version":"1.0.0"}` |
| `/api/state` | GET | Full game state (player, beacons, vehicles, time) |

### Network Requirements
- Android device and PC must be on the **same WiFi network**
- Windows firewall must allow port **63030** (TCP inbound)
- Subnautica must be running with the mod loaded

### Error Handling
| HTTP Code | Meaning | App Behavior |
|-----------|---------|--------------|
| 200 | Success | Update map with data |
| 503 | Player not in game | Show "Waiting for game..." |
| Connection refused | Mod not running | Show connection screen |
| Timeout | Network issue | Retry with indicator |

---

## Data Models

### GameState (API Response)
```kotlin
data class GameState(
    val timestamp: Long,
    val player: PlayerInfo?,
    val time: TimeInfo,
    val beacons: List<BeaconInfo>,
    val vehicles: List<VehicleInfo>
)
```

### PlayerInfo
```kotlin
data class PlayerInfo(
    val position: Vector3,
    val heading: Float,    // 0-360° (0=North, 90=East, 180=South, 270=West)
    val depth: Float,      // Positive value in meters
    val biome: String
)
```

### Vector3
```kotlin
data class Vector3(
    val x: Float,  // East/West (-2000 to +2000)
    val y: Float,  // Depth (0 = surface, negative = underwater)
    val z: Float   // North/South (-2000 to +2000)
)
```

---

## Coordinate System

```
        North (+Z)
           ^
           |
West <-----+-----> East (+X)
(-X)       |
           v
        South (-Z)

Y axis (vertical):
  Y = 0      -> Water surface
  Y = -100   -> 100m depth
  Y = -1400  -> Lava Lakes (deepest point)
```

**World dimensions:** ~4000m x 4000m (from -2000 to +2000 on X and Z)

### Coordinate Conversion (World -> Map Pixels)
```kotlin
fun worldToMap(worldX: Float, worldZ: Float, mapWidth: Int, mapHeight: Int): Pair<Int, Int> {
    val mapX = ((worldX + 2000) / 4000 * mapWidth).toInt()
    val mapY = ((2000 - worldZ) / 4000 * mapHeight).toInt() // Z inverted
    return Pair(mapX, mapY)
}
```

---

## Map Layers

The app should support multiple depth layers:

| Layer | Depth Range | Description |
|-------|-------------|-------------|
| Surface | 0 to -500m | Main biomes |
| Jellyshroom Cave | -100 to -300m | Underground cave |
| Lost River | -500 to -900m | Multiple sub-zones |
| Inactive Lava Zone | -900 to -1400m | Volcanic area |
| Lava Lakes | -1400m+ | Deepest area |

---

## App Screens

### 1. Connection Screen
- Input field for PC IP address
- "Connect" button
- Connection status indicator
- Remember last used IP (SharedPreferences)

### 2. Map Screen
- Interactive zoomable/pannable map
- Layer selector (depth levels)
- Player marker with heading indicator
- Beacon markers with labels
- Vehicle markers (icons per type)
- Info overlay: biome, depth, day/night

### 3. Settings Screen (optional)
- Polling interval
- Map appearance options
- Clear saved IP

---

## Project Structure (Recommended)

```
app/
├── src/main/
│   ├── java/com/example/subnauticamap/
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   ├── MapApiService.kt      # Retrofit interface
│   │   │   │   └── ApiModels.kt          # Data classes
│   │   │   └── repository/
│   │   │       └── GameStateRepository.kt
│   │   ├── ui/
│   │   │   ├── connection/
│   │   │   │   ├── ConnectionScreen.kt
│   │   │   │   └── ConnectionViewModel.kt
│   │   │   ├── map/
│   │   │   │   ├── MapScreen.kt
│   │   │   │   ├── MapViewModel.kt
│   │   │   │   └── components/
│   │   │   │       ├── MapView.kt
│   │   │   │       ├── PlayerMarker.kt
│   │   │   │       ├── BeaconMarker.kt
│   │   │   │       └── VehicleMarker.kt
│   │   │   └── theme/
│   │   │       └── Theme.kt
│   │   └── util/
│   │       └── CoordinateConverter.kt
│   └── res/
│       ├── drawable/
│       │   ├── map_surface.png
│       │   ├── map_lost_river.png
│       │   ├── ic_player.xml
│       │   ├── ic_beacon.xml
│       │   ├── ic_seamoth.xml
│       │   ├── ic_cyclops.xml
│       │   └── ic_prawn.xml
│       └── values/
│           └── strings.xml
```

---

## File Encoding Standards

- **All files:** UTF-8 with LF (Unix) line endings
- **Timestamps:** ISO 8601 (YYYY-MM-DD HH:mm)
- **Time format:** 24-hour (HH:mm)

---

## Claude Code Session Management

### Quick Start (TL;DR)

**Continue work:** `"continue"` or `"let's continue"`
**New session:** `"new session: Feature Name"` or `"start new session"`

**Claude handles everything automatically** - no need to manage session numbers or files manually.

---

### Session File Structure

**Two-Tier System:**
1. **SESSION_STATE.md** (root) - Overview and index of all sessions
2. **.claude/sessions/SESSION_XXX_[name].md** - Detailed session files

**Naming:** `SESSION_001_project_setup.md` (three digits, 001-999)

**Session Limits (Recommendations):**
- Max tasks: 20-25 per session
- Max files modified: 15-20 per session
- Recommended duration: 2-4 hours

---

### Session Management Rules

#### MANDATORY Actions:
1. Always read CLAUDE.md first for context
2. Always read current session file
3. Update session in real-time as tasks complete
4. Document all code (headers, functions)
5. Never lose context between messages
6. Auto-save progress every 10-15 minutes

#### When to Create New Session:
- New major feature/module
- Completed session goal
- Different project area
- After long break
- Approaching session limits

---

## Documentation Standards

### File Header (All Files)
```kotlin
/**
 * @file filename.kt
 * @description Brief file purpose
 * @session SESSION_XXX
 * @created YYYY-MM-DD
 */
```

### Function Documentation
```kotlin
/**
 * Brief function description
 *
 * @param paramName Parameter description
 * @return Return description
 * @throws Exception Error conditions
 */
```

---

## Git Workflow

### Branch Naming
**Format:** `feature/session-XXX-brief-description`
**Examples:** `feature/session-001-connection-screen`, `feature/session-002-map-view`

### Commit Messages
```
Session XXX: [Brief summary]

Changes:
- Change 1
- Change 2

Session: SESSION_XXX
```

---

## Map Resources

### Community Maps (for tile extraction)
| Resource | URL | Notes |
|----------|-----|-------|
| Subnautica Map.io | https://subnauticamap.io/ | Interactive with layers |
| Map Genie | https://mapgenie.io/subnautica | Complete interactive map |
| Rocketsoup Blog | https://blog.rocketsoup.net/2024/07/16/subnautica-maps/ | PNG maps multi-layers |

### Reference App
- **Submaptica** on Google Play (static map, no real-time connection)

---

## Common Commands

**Continue:** "continue", "let's continue", "keep going"
**New session:** "new session: [name]", "start new session"
**Save:** "save progress", "checkpoint"
**Update:** "update session", "update SESSION_STATE.md"

---

## MVP Criteria

- [ ] Connection screen with IP input
- [ ] Successful connection to mod API
- [ ] Display map with at least surface layer
- [ ] Show player position in real-time
- [ ] Show player heading/direction
- [ ] Display beacons on map
- [ ] Display vehicles on map
- [ ] Show depth and biome info
- [ ] Handle connection errors gracefully

---

## Technical Notes

### Polling Strategy
- Use 1-second interval for real-time updates
- Use Kotlin coroutines for async operations
- Handle lifecycle (stop polling when app in background)

### Performance
- Cache map images
- Reuse views/composables
- Minimize allocations in update loop

### Testing
- Test with mod running on PC
- Test connection recovery (mod restart)
- Test on different screen sizes

---

**Last Updated:** 2026-01-19
**Version:** 1.0.0
