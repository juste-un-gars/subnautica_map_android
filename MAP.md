# Cartes Subnautica — Logique d'affichage

## 🎯 Principe

L'application affiche **une carte différente selon le biome** dans lequel se trouve le joueur.

- **Carte globale (surface)** → Affichée par défaut quand le joueur est dans un biome de surface
- **Cartes spécifiques** → Affichées automatiquement quand le joueur entre dans une zone souterraine

La sélection se fait par le **nom du biome**, pas par la profondeur seule.

---

## 🗺️ Les 5 cartes disponibles

### 1. Carte Surface (par défaut)

| Propriété | Valeur |
|-----------|--------|
| **Nom** | Surface / Carte globale |
| **Profondeur typique** | 0 à -500m |
| **URL** | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-blank.jpg |
| **URL alternative (avec biomes)** | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-biomes.jpg |

**Biomes concernés** : Tous les biomes de surface (voir liste ci-dessous)

---

### 2. Jellyshroom Caves

| Propriété | Valeur |
|-----------|--------|
| **Nom** | Jellyshroom Caves |
| **Profondeur typique** | -100 à -300m |
| **URL** | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-jellyshroom-cave.jpg |

**Biomes déclencheurs** :
```
jellyShroom
jellyShroomCaves
```

---

### 3. Lost River

| Propriété | Valeur |
|-----------|--------|
| **Nom** | Lost River |
| **Profondeur typique** | -500 à -900m |
| **URL** | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-1-lost-river.jpg |

**Biomes déclencheurs** :
```
lostRiver
LostRiver_TreeCove
LostRiver_BonesField
LostRiver_Canyon
LostRiver_Junction
LostRiver_GhostTree
LostRiver_Corridor
lostRiverCorridor
```

---

### 4. Inactive Lava Zone

| Propriété | Valeur |
|-----------|--------|
| **Nom** | Inactive Lava Zone |
| **Profondeur typique** | -900 à -1400m |
| **URL** | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-2-inactive-lava-zone.jpg |

**Biomes déclencheurs** :
```
ilzChamber
ilzCorridor
inactiveLavaZone
intactiveLava
lavaCastle
lavaPit
```

---

### 5. Lava Lakes (Active Lava Zone)

| Propriété | Valeur |
|-----------|--------|
| **Nom** | Lava Lakes / Active Lava Zone |
| **Profondeur typique** | -1400m et plus |
| **URL** | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-3-lava-lakes.jpg |

**Biomes déclencheurs** :
```
introlava
lavaLakes
activeLavaZone
introlava
introlava_Corridor
```

---

## 📋 Liste des biomes de surface

Ces biomes utilisent la **carte globale (surface)** :

```
safeShallows
kelpForest
grassyPlateaus
mushroomForest
kooshZone (Bulb Zone)
bloodKelp
bloodKelpTwo
underwaterIslands
floatingIsland
mountainIsland
mountains
dunes
crashZone
crashedShip
cragField
grandReef
deepGrandReef
sparseReef
seaTreaderPath
void (Dead Zone)
```

---

## 🔄 Algorithme de sélection de carte

```
FONCTION getActiveMap(biome: string) -> MapLayer:
    
    // Priorité 1: Lava Lakes (zone la plus profonde)
    SI biome CONTIENT "lavaLakes" 
       OU biome CONTIENT "introlava"
       OU biome CONTIENT "activeLava":
        RETOURNER "lava-lakes"
    
    // Priorité 2: Inactive Lava Zone
    SI biome CONTIENT "ilz" 
       OU biome CONTIENT "inactiveLava"
       OU biome CONTIENT "intactiveLava"
       OU biome CONTIENT "lavaCastle"
       OU biome CONTIENT "lavaPit":
        RETOURNER "inactive-lava-zone"
    
    // Priorité 3: Lost River
    SI biome CONTIENT "lostRiver":
        RETOURNER "lost-river"
    
    // Priorité 4: Jellyshroom Caves
    SI biome CONTIENT "jellyShroom":
        RETOURNER "jellyshroom-caves"
    
    // Défaut: Surface
    RETOURNER "surface"
```

### Implémentation C# (pour le mod)

```csharp
public enum MapLayer
{
    Surface,
    JellyshroomCaves,
    LostRiver,
    InactiveLavaZone,
    LavaLakes
}

public static MapLayer GetActiveMapLayer(string biome)
{
    if (string.IsNullOrEmpty(biome))
        return MapLayer.Surface;
    
    string biomeLower = biome.ToLower();
    
    // Lava Lakes (le plus profond)
    if (biomeLower.Contains("lavalakes") || 
        biomeLower.Contains("introlava") ||
        biomeLower.Contains("activelava"))
        return MapLayer.LavaLakes;
    
    // Inactive Lava Zone
    if (biomeLower.Contains("ilz") || 
        biomeLower.Contains("inactivelava") ||
        biomeLower.Contains("intactivelava") ||
        biomeLower.Contains("lavacastle") ||
        biomeLower.Contains("lavapit"))
        return MapLayer.InactiveLavaZone;
    
    // Lost River
    if (biomeLower.Contains("lostriver"))
        return MapLayer.LostRiver;
    
    // Jellyshroom Caves
    if (biomeLower.Contains("jellyshroom"))
        return MapLayer.JellyshroomCaves;
    
    // Défaut: Surface
    return MapLayer.Surface;
}
```

---

## 📡 Données exposées par l'API

L'API du mod inclut le layer de carte recommandé :

```json
{
  "player": {
    "position": { "x": -234.5, "y": -750.0, "z": 156.7 },
    "biome": "LostRiver_Junction",
    "depth": 750.0
  },
  "map": {
    "activeLayer": "lost-river",
    "availableLayers": ["surface", "jellyshroom-caves", "lost-river", "inactive-lava-zone", "lava-lakes"]
  }
}
```

L'app Android peut :
1. Utiliser `activeLayer` pour afficher automatiquement la bonne carte
2. Permettre à l'utilisateur de forcer un layer manuellement (toggle)

---

## 🖼️ Récapitulatif des URLs

| Layer | Fichier | URL |
|-------|---------|-----|
| `surface` | map-blank.jpg | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-blank.jpg |
| `surface` (alt) | map-biomes.jpg | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-biomes.jpg |
| `jellyshroom-caves` | map-jellyshroom-cave.jpg | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-jellyshroom-cave.jpg |
| `lost-river` | map-1-lost-river.jpg | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-1-lost-river.jpg |
| `inactive-lava-zone` | map-2-inactive-lava-zone.jpg | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-2-inactive-lava-zone.jpg |
| `lava-lakes` | map-3-lava-lakes.jpg | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-3-lava-lakes.jpg |

### Cartes bonus (pour features futures)

| Contenu | URL |
|---------|-----|
| Carte complète (tout) | https://rocketsoup.net/blogassets/subnautica/2024-07-20/map-details.jpg |
| Wrecks numérotés | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-wrecks.jpg |
| Lifepods | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-lifepods.jpg |
| Leviathans | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-leviathans.jpg |
| Bases Degasi | https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-degasi.jpg |
| Bases Alien | https://rocketsoup.net/blogassets/subnautica/2024-07-20/map-architect.jpg |

---

## 📐 Système de coordonnées

Toutes les cartes utilisent le même système de coordonnées :

```
Dimensions du monde : 4096m x 4096m
Centre (0, 0) : Proche du Lifepod 5
Plage X : -2048 à +2048
Plage Z : -2048 à +2048

Orientation :
  Nord = +Z
  Sud  = -Z
  Est  = +X
  Ouest = -X
```

### Conversion coordonnées jeu → pixels carte

Pour une image de **1024x1024 pixels** (taille des cartes Rocketsoup) :

```csharp
// Monde: -2048 à +2048 → Pixels: 0 à 1024
public static (int x, int y) WorldToPixel(float worldX, float worldZ)
{
    int pixelX = (int)((worldX + 2048f) / 4096f * 1024f);
    int pixelY = (int)((2048f - worldZ) / 4096f * 1024f); // Z inversé (Nord en haut)
    return (pixelX, pixelY);
}
```

---

## 📝 Notes

- Les noms de biomes peuvent varier légèrement selon les versions du jeu
- La détection par `Contains()` (insensible à la casse) est plus robuste que l'égalité stricte
- Les cartes des caves montrent aussi une partie du terrain de surface pour le contexte
- Les flèches sur les cartes caves : **vert** = vers la surface, **bleu** = vers les profondeurs

---

## 📚 Source des cartes

**Auteur** : Rocketsoup  
**Article** : https://blog.rocketsoup.net/2024/07/16/subnautica-maps/  
**Licence** : Libre d'utilisation (avec ou sans crédit)  
**Date** : Juillet 2024 (version finale du jeu)
