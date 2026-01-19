# Session 001: Initial Android App Setup

## Date: 2026-01-19
## Duration: Start - Paused
## Goal: Create basic Android app to test connection with Subnautica MapAPI mod

---

## Completed Tasks

- [x] Read project documentation (PROJET.md, CONNECTION.md, model_CLAUDE.md)
- [x] Create CLAUDE.md customized for Android development
- [x] Create Android project structure (Gradle Kotlin DSL)
- [x] Set up dependencies (Retrofit, OkHttp, Compose, DataStore)
- [x] Create API data models matching mod JSON response
- [x] Create Retrofit API service interface
- [x] Create API client factory
- [x] Create SettingsRepository (IP, port, keep screen on)
- [x] Create GameStateRepository (API calls)
- [x] Create ConnectionViewModel
- [x] Create ConnectionScreen UI
- [x] Create MapViewModel with polling
- [x] Create MapScreen UI (data display)
- [x] Create Subnautica-themed colors
- [x] Set up navigation (Connection -> Map)
- [x] Add "Keep screen on" option
- [x] Update SESSION_STATE.md

---

## Current Status

**Status:** Complete - Ready for testing

The basic app structure is complete. The app can:
1. Accept IP address and port configuration
2. Persist settings between sessions
3. Test connection to the mod API
4. Display real-time game data (player, vehicles, beacons)
5. Keep the screen on while viewing the map

---

## Technical Decisions

- **Architecture:** MVVM with Repository pattern
  - Reason: Clean separation of concerns, testable, standard Android practice

- **UI Framework:** Jetpack Compose
  - Reason: Modern, declarative, easier to maintain

- **HTTP Client:** Retrofit + OkHttp
  - Reason: Industry standard, easy to use, good error handling

- **Persistence:** DataStore Preferences
  - Reason: Modern replacement for SharedPreferences, coroutine-friendly

- **Polling interval:** 1 second
  - Reason: Matches mod refresh rate, good balance of responsiveness/battery

---

## Files Created

### Configuration
| File | Description |
|------|-------------|
| settings.gradle.kts | Project settings |
| build.gradle.kts | Root build config |
| gradle.properties | Gradle properties |
| gradle/libs.versions.toml | Version catalog |
| app/build.gradle.kts | App module build config |

### Source Code
| File | Description |
|------|-------------|
| MainActivity.kt | Entry point + navigation setup |
| NavRoutes.kt | Navigation route constants |
| ApiModels.kt | Data classes for API responses |
| MapApiService.kt | Retrofit interface |
| ApiClient.kt | Retrofit factory |
| SettingsRepository.kt | Settings persistence |
| GameStateRepository.kt | API call management |
| Theme.kt | Subnautica colors |
| ConnectionScreen.kt | Connection UI |
| ConnectionViewModel.kt | Connection state |
| MapScreen.kt | Map data display UI |
| MapViewModel.kt | Map state + polling |

### Resources
| File | Description |
|------|-------------|
| AndroidManifest.xml | App manifest |
| strings.xml | String resources |
| colors.xml | Color definitions |
| themes.xml | Theme definition |
| ic_launcher_foreground.xml | App icon |

---

## Next Steps

1. [ ] Test app with actual Subnautica mod running
2. [ ] Download map PNG images from rocketsoup blog
3. [ ] Implement interactive map view (zoom/pan)
4. [ ] Add map layers for different depths
5. [ ] Implement fog of war system
6. [ ] Add player marker with rotation
7. [ ] Add vehicle/beacon markers

---

## User Requests (Noted for Later)

1. **Fog of War** - Reveal map areas as player explores
   - Will need to store visited coordinates locally
   - Consider grid-based approach (e.g., 50m x 50m cells)
   - Persist between sessions

---

## Session Summary

Created a complete Android application structure for the Subnautica Map Companion. The app connects to the MapAPI mod via HTTP, displays real-time game data including player position, vehicles, and beacons. Includes a connection configuration screen with IP/port settings and a "keep screen on" option. Ready for testing with the actual mod.

---

## Handoff Notes

- **Critical context:** App uses polling (1 req/sec), not WebSocket
- **Blockers:** None - ready to test
- **Next priority:** Test connection, then add actual map visualization
