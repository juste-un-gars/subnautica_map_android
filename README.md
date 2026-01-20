# Subnautica Map Android

An Android companion app for Subnautica that displays an interactive real-time map with player position, beacons, vehicles, and fog of war exploration tracking.

## Features

- **Real-time player tracking** - See your position and heading on the map
- **Multi-layer maps** - 5 depth layers (Surface, Jellyshroom Cave, Lost River, Inactive Lava Zone, Lava Lakes)
- **Fog of war** - Reveals map as you explore, persistent between sessions
- **Beacons & vehicles** - Display all in-game beacons and vehicles (Seamoth, Cyclops, Prawn Suit)
- **Custom markers** - Save your own points of interest
- **Follow mode** - Auto-follow player with 5x zoom
- **Day/night indicator** - Shows current in-game time cycle
- **Depth & biome display** - Real-time depth and current biome info
- **Backup/restore** - Export and import your exploration data
- **Fullscreen immersive mode** - Distraction-free map viewing
- **Portrait & landscape support** - Works in any orientation

## Requirements

### On PC (Subnautica)
- **Subnautica** (Steam or Epic Games version)
- **BepInEx** mod loader installed
- **MapAPI mod** installed and running
  - Repository: [subnautica_map_windows](https://github.com/juste-un-gars/subnautica_map_windows)

### On Android
- Android 8.0 (API 26) or higher
- Same WiFi network as the PC running Subnautica

### Network
- Both devices must be on the **same local WiFi network**
- Windows Firewall must allow **port 63030** (TCP inbound)

## Installation

### 1. Install the Subnautica Mod (PC)

1. Install [BepInEx](https://github.com/BepInEx/BepInEx) in your Subnautica folder
2. Download the MapAPI mod from [releases](https://github.com/juste-un-gars/subnautica_map_windows/releases)
3. Extract to `Subnautica/BepInEx/plugins/`

### 2. Configure Windows Firewall

Open PowerShell as Administrator and run:
```powershell
New-NetFirewallRule -DisplayName "Subnautica MapAPI" -Direction Inbound -Port 63030 -Protocol TCP -Action Allow
```

Or manually:
1. Open Windows Firewall
2. Create new Inbound Rule
3. Port: 63030, Protocol: TCP
4. Action: Allow

### 3. Find Your PC's IP Address

In Command Prompt or PowerShell:
```bash
ipconfig
```
Look for **IPv4 Address** under your WiFi adapter (e.g., `192.168.1.100`)

### 4. Install the Android App

Download the APK from [Releases](../../releases) and install on your Android device.

## Usage

1. **Launch Subnautica** on your PC with the MapAPI mod loaded
2. **Load a save** or start a new game (the API only works when in-game)
3. **Open the Android app** and enter your PC's IP address
4. **Tap Connect** - the app will connect and display the map

### Connection Screen
- Enter your PC's IP address (e.g., `192.168.1.100`)
- The IP is saved for next time
- Tap "Connect" to establish connection

### Map Screen
- **Pinch to zoom** - Zoom in/out on the map
- **Drag to pan** - Move around the map
- **Tap markers** - View beacon/vehicle/custom marker details
- **Long press** - Add a custom marker at that location

### Menu Options (Top-right)
- **Follow Player** - Toggle auto-follow mode (5x zoom, centers on player)
- **Layer** - Manually select a depth layer or use Auto
- **Map Style** - Switch between detailed and blank map
- **Visibility** - Show/hide player, beacons, vehicles, custom markers
- **Fog of War** - Toggle fog visibility or reset exploration
- **Backup** - Export exploration data as JSON
- **Restore** - Import exploration data from backup
- **About** - App info and credits

## API Reference

The app connects to the mod's HTTP API:

| Endpoint | Description |
|----------|-------------|
| `GET /api/ping` | Health check |
| `GET /api/state` | Full game state (player, beacons, vehicles, time) |

Default port: **63030**

See [CONNECTION.md](CONNECTION.md) for full API documentation.

## Troubleshooting

| Problem | Solution |
|---------|----------|
| "Connection refused" | Subnautica not running or mod not loaded |
| "Network unreachable" | Devices not on same WiFi network |
| "Connection timeout" | Firewall blocking port 63030 |
| "Waiting for game..." | Player not in-game (at main menu) |
| Map not updating | Check WiFi connection, try reconnecting |

### Test Connection
On your Android device, open a browser and go to:
```
http://<PC_IP>:63030/api/ping
```
Should return: `{"status":"ok","version":"1.0.0"}`

## Building from Source

### Prerequisites
- Android Studio (latest)
- JDK 17+
- Android SDK 34

### Build Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
```bash
./gradlew assembleRelease -x lintVitalAnalyzeRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM with Repository pattern
- **HTTP:** Retrofit + OkHttp
- **Images:** Coil
- **Persistence:** DataStore (settings), JSON files (fog of war, markers)

## Map Layers

| Layer | Depth Range | Description |
|-------|-------------|-------------|
| Surface | 0 to -500m | Main biomes, starting area |
| Jellyshroom Cave | -100 to -300m | Underground cave system |
| Lost River | -500 to -900m | Bioluminescent caverns |
| Inactive Lava Zone | -900 to -1400m | Volcanic chambers |
| Lava Lakes | -1400m+ | Deepest area, final zone |

The app automatically switches layers based on player depth.

## Credits

- **Map images:** Adapted from community resources ([Subnautica Map.io](https://subnauticamap.io/), [Map Genie](https://mapgenie.io/subnautica))
- **Subnautica:** Unknown Worlds Entertainment
- **Inspiration:** [Submaptica](https://play.google.com/store/apps/details?id=com.candycoded.submaptica) (static map app)

## License

This project is for personal/educational use. Subnautica is a trademark of Unknown Worlds Entertainment.

---

**Version:** 1.0.0
**Minimum Android:** 8.0 (API 26)
**Mod Port:** 63030
