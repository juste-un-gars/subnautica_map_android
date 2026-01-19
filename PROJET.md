# Subnautica Map Companion — Spécification Projet

## 🎯 Objectif

Créer un **mod pour Subnautica** qui expose les données de jeu via un serveur HTTP embarqué, permettant à une **application Android** (développée plus tard) d'afficher la position du joueur en temps réel sur une carte.

**Phase actuelle : MOD SUBNAUTICA uniquement**

---

## 📐 Architecture

```
┌────────────────────────────────────┐
│         MOD SUBNAUTICA             │
│  (BepInEx Plugin + Serveur HTTP)   │
│                                    │
│  • Collecte données toutes les 1s  │
│  • Serveur HTTP sur port 63030     │
│  • Expose GET /api/state (JSON)    │
└────────────────┬───────────────────┘
                 │ HTTP JSON (polling 1x/sec)
                 │ (même réseau WiFi local)
                 ▼
┌────────────────────────────────────┐
│         APP ANDROID (future)       │
│                                    │
│  • Affiche carte communautaire     │
│  • Overlay position + markers      │
└────────────────────────────────────┘
```

---

## 🔧 Environnement technique

### Prérequis (déjà installés chez l'utilisateur)
- **BepInEx** — Framework de modding Unity
- **Nautilus** — API de modding Subnautica

### Dépendances du mod
- **EmbedIO** (NuGet) — Serveur HTTP léger embarqué pour .NET
  - Package: `EmbedIO` version 3.x
  - Léger, sans dépendances lourdes, compatible Unity/Mono

### Cible
- **Subnautica 1** (version Steam actuelle)
- .NET Framework compatible avec Unity (Mono)

---

## 📁 Structure du projet

```
SubnauticaMapCompanion/
├── SubnauticaMapCompanion.csproj    # Projet C# (.NET Framework 4.7.2 ou net472)
├── Plugin.cs                         # Point d'entrée BepInEx [BepInPlugin]
├── GameDataCollector.cs              # Collecte des données du jeu (Update loop)
├── HttpServer/
│   ├── MapHttpServer.cs              # Serveur EmbedIO, démarrage/arrêt
│   └── ApiController.cs              # Contrôleur pour GET /api/state
├── Models/
│   ├── GameState.cs                  # DTO principal
│   ├── PlayerInfo.cs                 # Position, heading, depth, biome
│   ├── BeaconInfo.cs                 # Balises du joueur
│   ├── VehicleInfo.cs                # Seamoth, Cyclops, Prawn
│   └── TimeInfo.cs                   # Cycle jour/nuit
└── Config/
    └── PluginConfig.cs               # Configuration (port, intervalle refresh)
```

---

## 🎮 Accès aux données du jeu

### Classes Subnautica à utiliser

```csharp
// === JOUEUR ===
// Position du joueur (Vector3)
Player.main.transform.position
// x = est/ouest, y = altitude (négatif = profondeur), z = nord/sud

// Direction du regard (pour l'icône directionnelle sur la carte)
Player.main.transform.eulerAngles.y  // heading 0-360°

// Profondeur (valeur positive, plus pratique)
Ocean.GetDepthOf(Player.main.gameObject)

// === BIOME ===
// Nom du biome actuel
LargeWorldStreamer.main.GetBiome(Player.main.transform.position)
// Retourne des valeurs comme: "safeShallows", "kelpForest", "lostRiver", etc.

// === CYCLE JOUR/NUIT ===
DayNightCycle.main.IsDay()           // bool
DayNightCycle.main.timePassedAsFloat // float, temps écoulé

// === BALISES (Beacons) ===
// Toutes les balises sont gérées par PingManager
// PingManager.pings est un Dictionary<PingType, List<PingInstance>>
// 
// Pour itérer sur toutes les balises visibles:
foreach (var pingEntry in PingManager.pings)
{
    foreach (var ping in pingEntry.Value)
    {
        // ping.origin.position — Vector3 position
        // ping.GetLabel() — string nom/label
        // ping.colorIndex — int index couleur
        // ping.visible — bool visible sur HUD
    }
}

// === VÉHICULES ===
// Les véhicules héritent de Vehicle
// Types: SeaMoth, Cyclops, Exosuit (PRAWN Suit)

// Trouver tous les véhicules du joueur
Vehicle[] vehicles = UnityEngine.Object.FindObjectsOfType<Vehicle>();

foreach (var vehicle in vehicles)
{
    // vehicle.transform.position — Vector3
    // vehicle.GetName() — string nom custom ou défaut
    // vehicle.GetType().Name — "SeaMoth", "Cyclops", "Exosuit"
}

// Pour le Cyclops spécifiquement (c'est un SubRoot, pas Vehicle)
SubRoot[] cyclopsArray = UnityEngine.Object.FindObjectsOfType<SubRoot>();
// Filtrer ceux qui sont des Cyclops (pas des bases)
```

### Vérifications de sécurité

Toujours vérifier que les objets existent avant d'y accéder :

```csharp
if (Player.main == null) return null;
if (LargeWorldStreamer.main == null) return null;
// etc.
```

---

## 📡 API HTTP

### Endpoint principal

```
GET http://<IP_PC>:63030/api/state
```

### Réponse JSON

```json
{
  "timestamp": 1705590000,
  "player": {
    "position": {
      "x": -234.5,
      "y": -89.2,
      "z": 156.7
    },
    "heading": 145.3,
    "depth": 89.2,
    "biome": "mushroomForest"
  },
  "time": {
    "dayNightValue": 0.35,
    "isDay": true
  },
  "beacons": [
    {
      "id": "beacon_001",
      "label": "Wreck Alpha",
      "position": {
        "x": -150.0,
        "y": -45.0,
        "z": 200.0
      },
      "colorIndex": 0,
      "visible": true
    }
  ],
  "vehicles": [
    {
      "id": "vehicle_001",
      "type": "SeaMoth",
      "name": "Seamoth",
      "position": {
        "x": -230.0,
        "y": -85.0,
        "z": 160.0
      }
    },
    {
      "type": "Cyclops",
      "id": "vehicle_002", 
      "name": "USS Enterprise",
      "position": {
        "x": -300.0,
        "y": -120.0,
        "z": 100.0
      }
    },
    {
      "type": "Exosuit",
      "id": "vehicle_003",
      "name": "Prawn Suit",
      "position": {
        "x": -310.0,
        "y": -125.0,
        "z": 95.0
      }
    }
  ]
}
```

### Endpoint de santé (optionnel mais utile)

```
GET http://<IP_PC>:63030/api/ping
→ { "status": "ok", "version": "1.0.0" }
```

---

## ⚙️ Configuration

Le mod doit être configurable via BepInEx Config :

```csharp
// Config/PluginConfig.cs
[BepInPlugin("com.subnautica.mapcompanion", "MapCompanion", "1.0.0")]
public class Plugin : BaseUnityPlugin
{
    public static ConfigEntry<int> ServerPort;
    public static ConfigEntry<float> RefreshInterval;
    public static ConfigEntry<bool> EnableServer;

    void Awake()
    {
        ServerPort = Config.Bind("Server", "Port", 63030, "Port HTTP du serveur");
        RefreshInterval = Config.Bind("Server", "RefreshInterval", 1.0f, "Intervalle de refresh en secondes");
        EnableServer = Config.Bind("Server", "Enabled", true, "Activer le serveur HTTP");
    }
}
```

Fichier généré : `BepInEx/config/com.subnautica.mapcompanion.cfg`

---

## 🔄 Cycle de vie du mod

```
┌─────────────────────────────────────────────────────────────┐
│  Plugin.Awake()                                             │
│  ├─ Charger configuration                                   │
│  ├─ Initialiser GameDataCollector                           │
│  └─ Démarrer HttpServer sur port configuré                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Plugin.Update() — appelé chaque frame                      │
│  └─ GameDataCollector.Tick() — throttled à 1x/sec          │
│      └─ Collecte Player, Beacons, Vehicles, Time            │
│      └─ Stocke dans GameState (thread-safe)                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  HttpServer (thread séparé)                                 │
│  └─ GET /api/state → retourne dernier GameState en JSON    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Plugin.OnDestroy()                                         │
│  └─ Arrêter proprement le serveur HTTP                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 🗺️ Ressources cartographiques

### Cartes interactives (pour référence et extraction de tiles)

| Ressource | URL | Notes |
|-----------|-----|-------|
| Subnautica Map.io | https://subnauticamap.io/ | Carte interactive avec layers Lost River, Lava Zones |
| Map Genie | https://mapgenie.io/subnautica | Carte interactive complète |
| Delta Calculator | https://www.deltacalculator.com/subnautica/map/ | Carte avec biomes et compass |

### Cartes PNG haute résolution

| Ressource | URL | Notes |
|-----------|-----|-------|
| Rocketsoup Blog | https://blog.rocketsoup.net/2024/07/16/subnautica-maps/ | **Excellent** - Cartes PNG multi-layers (surface, caves, Lost River, Lava), libres de droits |
| Subnautica Wiki | https://subnautica.fandom.com/wiki/Crater_Map | Cartes officielles + liens vers cartes des développeurs |
| Steam Community | https://steamcommunity.com/sharedfiles/filedetails/?id=2781543360 | Cartes annotées des biomes |

### Layers nécessaires pour l'app Android

1. **Surface** (0 à -500m) — Biomes principaux
2. **Jellyshroom Cave** (-100 à -300m)
3. **Lost River** (-500 à -900m) — Plusieurs sous-zones
4. **Inactive Lava Zone** (-900 à -1400m)
5. **Active Lava Zone / Lava Lakes** (-1400m+)

### App Android existante (référence)

- **Submaptica** sur Google Play : https://play.google.com/store/apps/details?id=com.hesanta.subnauticamap
  - Montre que le concept fonctionne
  - Mais pas de connexion temps réel au jeu

---

## 📊 Système de coordonnées Subnautica

```
        Nord (+Z)
           ▲
           │
Ouest ◄────┼────► Est (+X)
    (-X)   │
           ▼
        Sud (-Z)

Profondeur : Y négatif = plus profond
             Y = 0 → surface de l'eau
             Y = -1400 → Lava Lakes
```

### Dimensions du monde
- Environ **4000m x 4000m** (de -2000 à +2000 sur X et Z)
- Profondeur max : environ **-1500m** (Lava Lakes)
- Centre (0, 0, 0) : proche du Lifepod 5

### Conversion coordonnées → pixels carte

Pour une carte de 2048x2048 pixels centrée sur (0,0) :

```csharp
// Monde: -2000 à +2000 → Pixels: 0 à 2048
float worldToPixel(float worldCoord) {
    return (worldCoord + 2000f) / 4000f * 2048f;
}

int pixelX = (int)worldToPixel(position.x);
int pixelY = (int)worldToPixel(-position.z); // Z inversé pour avoir Nord en haut
```

---

## 🚀 Instructions de build

### 1. Créer le projet

```bash
dotnet new classlib -n SubnauticaMapCompanion -f net472
cd SubnauticaMapCompanion
```

### 2. Ajouter les références

Dans le `.csproj`, ajouter les références aux DLLs de Subnautica :

```xml
<ItemGroup>
  <!-- BepInEx -->
  <Reference Include="BepInEx">
    <HintPath>$(SUBNAUTICA_PATH)\BepInEx\core\BepInEx.dll</HintPath>
  </Reference>
  <Reference Include="0Harmony">
    <HintPath>$(SUBNAUTICA_PATH)\BepInEx\core\0Harmony.dll</HintPath>
  </Reference>
  
  <!-- Unity -->
  <Reference Include="UnityEngine">
    <HintPath>$(SUBNAUTICA_PATH)\Subnautica_Data\Managed\UnityEngine.dll</HintPath>
  </Reference>
  <Reference Include="UnityEngine.CoreModule">
    <HintPath>$(SUBNAUTICA_PATH)\Subnautica_Data\Managed\UnityEngine.CoreModule.dll</HintPath>
  </Reference>
  
  <!-- Subnautica -->
  <Reference Include="Assembly-CSharp">
    <HintPath>$(SUBNAUTICA_PATH)\Subnautica_Data\Managed\Assembly-CSharp.dll</HintPath>
  </Reference>
</ItemGroup>

<ItemGroup>
  <!-- Serveur HTTP -->
  <PackageReference Include="EmbedIO" Version="3.5.2" />
</ItemGroup>
```

### 3. Build et déploiement

```bash
dotnet build -c Release
# Copier SubnauticaMapCompanion.dll vers:
# $(SUBNAUTICA_PATH)/BepInEx/plugins/SubnauticaMapCompanion/
```

---

## ✅ Critères de validation

### MVP Fonctionnel
- [ ] Le serveur HTTP démarre quand le jeu se lance
- [ ] GET /api/state retourne du JSON valide
- [ ] Position du joueur mise à jour toutes les secondes
- [ ] Profondeur et biome corrects
- [ ] Liste des balises avec positions
- [ ] Liste des véhicules avec positions
- [ ] Le serveur s'arrête proprement à la fermeture du jeu

### Tests manuels
1. Lancer Subnautica avec le mod
2. Ouvrir un navigateur sur `http://localhost:63030/api/state`
3. Vérifier que le JSON se met à jour
4. Vérifier depuis un autre appareil sur le même réseau (utiliser l'IP du PC)

---

## 📝 Notes importantes

### Thread Safety
Le serveur HTTP tourne dans un thread séparé. L'accès aux données du jeu (Unity) doit se faire depuis le thread principal. Solution :
- Collecter les données dans `Update()` (thread principal)
- Stocker dans un objet thread-safe (lock ou Interlocked)
- Le serveur HTTP lit cet objet en lecture seule

### Gestion des erreurs
- Si `Player.main` est null (menu principal, chargement), retourner un état vide ou un code HTTP 503
- Logger les erreurs dans la console BepInEx

### Performance
- Ne pas collecter les données à chaque frame
- Utiliser un timer pour limiter à 1 collecte/seconde
- Éviter les allocations dans la boucle Update (réutiliser les objets)

---

## 🔮 Évolutions futures (hors scope MVP)

- [ ] WebSocket pour push temps réel (au lieu de polling)
- [ ] Endpoint pour les bases construites
- [ ] Endpoint pour l'inventaire
- [ ] Santé/O2/Faim/Soif du joueur
- [ ] Support Below Zero (projet séparé)
- [ ] Communication bidirectionnelle (créer beacon depuis l'app)

---

## 📚 Références

- Documentation Nautilus : https://subnauticamodding.github.io/Nautilus/
- Guide modding Subnautica : https://mroshaw.github.io/Subnautica/
- Projet similaire (archivé) : https://github.com/MartinSGill/SubnauticaWatcher
- EmbedIO documentation : https://github.com/unosquare/embedio
