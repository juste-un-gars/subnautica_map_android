# CONNECTION.md - MapAPI Integration Guide

Documentation pour connecter une application Android au mod MapAPI de Subnautica.

---

## Quick Start

```
Base URL: http://<PC_IP>:63030
Endpoints: /api/ping, /api/state
Method: GET
Format: JSON
Polling: 1 requête/seconde recommandé
```

---

## Configuration Réseau

### Prérequis
- Le PC et l'appareil Android doivent être sur le **même réseau WiFi**
- Le pare-feu Windows doit autoriser le port **63030** (TCP entrant)
- Subnautica doit être lancé avec le mod actif

### Trouver l'IP du PC
```bash
# Windows CMD/PowerShell
ipconfig
# Chercher "IPv4 Address" (ex: 192.168.1.100)
```

### URL de connexion
```
http://192.168.1.XXX:63030/api/state
```

---

## Endpoints API

### 1. Health Check - `/api/ping`

Vérifie si le serveur est actif.

**Request:**
```
GET http://<PC_IP>:63030/api/ping
```

**Response (200 OK):**
```json
{
  "status": "ok",
  "version": "1.0.0"
}
```

**Utilisation:** Appeler au démarrage de l'app pour vérifier la connexion.

---

### 2. Game State - `/api/state`

Retourne l'état complet du jeu.

**Request:**
```
GET http://<PC_IP>:63030/api/state
```

**Response (200 OK):**
```json
{
  "Timestamp": 1768810657,
  "Player": {
    "Position": {
      "X": "-83.81332",
      "Y": "-4.979864",
      "Z": "-289.9349"
    },
    "Heading": 145.3,
    "Depth": "4.979864",
    "Biome": "safeShallows"
  },
  "Time": {
    "DayNightValue": "39698.25",
    "IsDay": false
  },
  "Beacons": [
    {
      "Id": "beacon_0",
      "Label": "My Beacon",
      "Position": { "X": "100.0", "Y": "-20.0", "Z": "150.0" },
      "ColorIndex": 0,
      "Visible": true
    }
  ],
  "Vehicles": [
    {
      "Id": "vehicle_0",
      "Type": "SeaMoth",
      "Name": "SeaMoth(Clone)",
      "Position": { "X": "-107.2685", "Y": "-6.732635", "Z": "-304.4645" }
    },
    {
      "Id": "vehicle_1",
      "Type": "Cyclops",
      "Name": "Cyclops(Clone)",
      "Position": { "X": "-398.4311", "Y": "-37.25427", "Z": "-338.8511" }
    },
    {
      "Id": "vehicle_2",
      "Type": "Exosuit",
      "Name": "Exosuit(Clone)",
      "Position": { "X": "-65.97403", "Y": "-6.06743", "Z": "-275.9556" }
    }
  ]
}
```

**Response (503 Service Unavailable):**
Retourné quand le joueur n'est pas en jeu (menu principal, chargement).
```json
{
  "error": "Game not ready",
  "message": "Player not in game"
}
```

---

## Modèles de Données

### GameState
| Champ | Type | Description |
|-------|------|-------------|
| Timestamp | long | Unix timestamp (secondes) |
| Player | PlayerInfo | Infos joueur (null si pas en jeu) |
| Time | TimeInfo | Cycle jour/nuit |
| Beacons | BeaconInfo[] | Liste des balises |
| Vehicles | VehicleInfo[] | Liste des véhicules |

### PlayerInfo
| Champ | Type | Description |
|-------|------|-------------|
| Position | Vector3 | Position (X=Est/Ouest, Y=Profondeur, Z=Nord/Sud) |
| Heading | float | Direction 0-360° (0=Nord, 90=Est, 180=Sud, 270=Ouest) |
| Depth | float | Profondeur en mètres (positif = sous l'eau) |
| Biome | string | Nom du biome actuel |

### TimeInfo
| Champ | Type | Description |
|-------|------|-------------|
| DayNightValue | float | Temps écoulé en jeu |
| IsDay | bool | true = jour, false = nuit |

### VehicleInfo
| Champ | Type | Description |
|-------|------|-------------|
| Id | string | Identifiant unique (ex: "vehicle_0") |
| Type | string | "SeaMoth", "Cyclops", "Exosuit", ou mod |
| Name | string | Nom du véhicule |
| Position | Vector3 | Position dans le monde |

### BeaconInfo
| Champ | Type | Description |
|-------|------|-------------|
| Id | string | Identifiant unique |
| Label | string | Texte de la balise |
| Position | Vector3 | Position dans le monde |
| ColorIndex | int | Index de couleur (0-7) |
| Visible | bool | Visibilité de la balise |

### Vector3
| Champ | Type | Description |
|-------|------|-------------|
| X | string | Coordonnée Est/Ouest |
| Y | string | Coordonnée verticale (négatif = profondeur) |
| Z | string | Coordonnée Nord/Sud |

---

## Système de Coordonnées

```
        Nord (+Z)
           ^
           |
Ouest <----+----> Est (+X)
(-X)       |
           v
        Sud (-Z)

Axe Y (vertical):
  Y = 0      -> Surface de l'eau
  Y = -100   -> 100m de profondeur
  Y = -1400  -> Lacs de lave (point le plus bas)
```

**Dimensions du monde:** ~4000m x 4000m (de -2000 à +2000 sur X et Z)

---

## Recommandations d'Implémentation

### Polling
```kotlin
// Kotlin/Android - Exemple avec coroutines
val POLL_INTERVAL = 1000L // 1 seconde

fun startPolling() {
    viewModelScope.launch {
        while (isActive) {
            try {
                val state = api.getState()
                updateUI(state)
            } catch (e: Exception) {
                handleError(e)
            }
            delay(POLL_INTERVAL)
        }
    }
}
```

### Gestion des erreurs
| Situation | Action recommandée |
|-----------|-------------------|
| Connexion refusée | Vérifier IP/port, afficher écran config |
| Timeout (>3s) | Réessayer, afficher indicateur "connexion..." |
| 503 Service Unavailable | Afficher "En attente du jeu..." |
| JSON invalide | Logger l'erreur, ignorer cette réponse |

### Conversion coordonnées -> carte
```kotlin
// Convertir position monde -> position carte (pixels)
fun worldToMap(worldX: Float, worldZ: Float, mapWidth: Int, mapHeight: Int): Pair<Int, Int> {
    // Monde: -2000 à +2000 sur X et Z
    val mapX = ((worldX + 2000) / 4000 * mapWidth).toInt()
    val mapY = ((2000 - worldZ) / 4000 * mapHeight).toInt() // Z inversé pour la carte
    return Pair(mapX, mapY)
}
```

---

## Test de Connexion

### Depuis Android (navigateur)
1. Ouvrir Chrome sur Android
2. Aller à `http://<PC_IP>:63030/api/ping`
3. Doit afficher `{"status":"ok","version":"1.0.0"}`

### Depuis PC (vérifier que le serveur tourne)
```
http://localhost:63030/api/ping
http://localhost:63030/api/state
```

---

## Troubleshooting

| Problème | Solution |
|----------|----------|
| "Connection refused" | Subnautica n'est pas lancé ou mod non chargé |
| "Network unreachable" | Vérifier que les appareils sont sur le même WiFi |
| "Connection timeout" | Pare-feu bloque le port 63030, ajouter exception |
| Données null | Le joueur est dans le menu, pas en jeu |

### Ouvrir le port dans le pare-feu Windows
```powershell
# PowerShell (Admin)
New-NetFirewallRule -DisplayName "Subnautica MapAPI" -Direction Inbound -Port 63030 -Protocol TCP -Action Allow
```

---

## Code Source du Mod

Repository: https://github.com/juste-un-gars/subnautica_map_windows

Structure:
```
MapAPI/
├── Plugin.cs              # Point d'entrée BepInEx
├── GameDataCollector.cs   # Collecte des données (thread principal Unity)
├── HttpServer/
│   ├── MapHttpServer.cs   # Serveur EmbedIO
│   └── ApiController.cs   # Endpoints API
└── Models/                # DTOs JSON
```

---

**Version:** 1.0.0
**Port:** 63030
**Refresh Rate:** 1 seconde
