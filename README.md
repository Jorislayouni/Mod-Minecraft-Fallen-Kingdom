# Fallen Kingdom — Mod Minecraft 1.8.8

Mode de jeu **Fallen Kingdom** pour Minecraft 1.8.8 (Forge), inspiré des séries Fallen Kingdom jouées par Siphano, Aypierre et d'autres. Chaque équipe construit sa base autour d'un **cœur**, puis les équipes s'affrontent : la dernière équipe avec un cœur intact gagne.

## Règles du jeu

- **2 équipes minimum** (plus si tu veux), chacune avec sa zone de base.
- Chaque équipe a un **cœur** (un bloc désigné, typiquement un beacon) qu'elle doit protéger.
- **Phase de préparation** : tout le monde en **survie**, PvP désactivé. Chaque joueur ne peut poser des blocs que **dans sa propre base**. Les cœurs sont invulnérables.
- **Phase de combat** : PvP activé. On ne peut poser des blocs que dans sa base, **sauf la TNT** qui est autorisée partout (c'est l'outil principal pour casser les bases adverses).
- Les joueurs ont **3 vies** par défaut (configurable). À 0 vies, le joueur passe en mode spectateur jusqu'à la fin de la partie.
- Une équipe est éliminée quand **son cœur est détruit** ou quand **tous ses joueurs sont à 0 vies**.
- **Pas de timer** : c'est l'admin qui décide quand passer de la prep au combat avec une commande.

## Installation

### Prérequis

- Minecraft **1.8.8**
- **Forge 1.8.8** installé dans le launcher

### Pour les joueurs

1. Télécharger le `.jar` du mod :
   - Soit depuis une [release GitHub](https://github.com/jorislayouni/mod-minecraft-fallen-kingdom/releases) (lien direct, recommandé)
   - Soit depuis l'onglet [Actions](https://github.com/jorislayouni/mod-minecraft-fallen-kingdom/actions) → dernier run vert → section *Artifacts* (nécessite un compte GitHub)
2. Placer le `.jar` dans le dossier `mods` de Minecraft :
   - **Windows** : `%appdata%\.minecraft\mods\`
   - **macOS** : `~/Library/Application Support/minecraft/mods/`
   - **Linux** : `~/.minecraft/mods/`
3. Lancer Minecraft avec le profil **Forge 1.8.8**.

### Pour le serveur

Le mod doit être installé **à la fois sur le serveur et sur chaque client**. Copier le `.jar` dans le dossier `mods/` du serveur Forge 1.8.8.

## Comment lancer une partie

L'admin (opérateur / joueur en créatif avec les permissions) configure tout via la commande `/fk`.

### 1. Créer les équipes

```
/fk team create rouge RED 3
/fk team create bleu BLUE 3
```

Format : `/fk team create <nom> <couleur> [vies]`. Couleurs disponibles : `RED`, `BLUE`, `GREEN`, `YELLOW`, `GOLD`, `AQUA`, `LIGHT_PURPLE`, `WHITE`, `DARK_RED`, `DARK_BLUE`, `DARK_GREEN`, `DARK_AQUA`, `DARK_PURPLE`, `DARK_GRAY`, `GRAY`, `BLACK`.

### 2. Affecter les joueurs

```
/fk team add Alice rouge
/fk team add Bob bleu
```

### 3. Définir la zone de chaque base

La zone est une boîte rectangulaire (AABB). Pendant la prep, les joueurs ne pourront poser des blocs qu'à l'intérieur de cette zone.

```
/fk setbase rouge 100 60 100 130 90 130
/fk setbase bleu -100 60 -100 -130 90 -130
```

Format : `/fk setbase <equipe> <x1> <y1> <z1> <x2> <y2> <z2>` (coordonnées des deux coins opposés).

### 4. Poser le cœur de chaque équipe

Se placer **sur** le bloc à désigner comme cœur (typiquement un beacon bien visible) puis :

```
/fk setcore rouge
```

Ou en spécifiant les coordonnées : `/fk setcore rouge 115 61 115`.

### 5. Définir le spawn de chaque équipe

C'est ici que les joueurs apparaîtront au démarrage du combat et à chaque respawn.

```
/fk setspawn rouge
```

Se placer devant la base à l'endroit voulu, puis exécuter la commande.

### 6. Démarrer la préparation

```
/fk start
```

Tous les joueurs sont téléportés dans leur base en mode survie. PvP désactivé. Ils peuvent construire/miner dans leur zone de base pour renforcer leurs murs, creuser des tunnels, préparer des pièges, etc.

> Note : la prep est en survie, donc les équipes doivent récolter leurs matériaux. Prévois de placer des coffres avec du stock, ou bien un accès à une carrière dans la zone de base.

### 7. Lancer le combat

Quand l'admin estime que la préparation est terminée (il n'y a pas de timer automatique) :

```
/fk game
```

Tous les joueurs sont téléportés à leur spawn. PvP activé. Que le meilleur gagne !

### 8. Suivi et arrêt

- `/fk status` — affiche la phase courante et l'état des équipes
- `/fk stop` — arrête la partie (retour au lobby)

## Référence des commandes

| Commande | Description |
|---|---|
| `/fk team create <nom> <couleur> [vies]` | Crée une équipe |
| `/fk team add <joueur> <équipe>` | Affecte un joueur à une équipe |
| `/fk team remove <joueur>` | Retire un joueur de son équipe |
| `/fk team list` | Liste les équipes |
| `/fk setbase <équipe> <x1 y1 z1 x2 y2 z2>` | Définit la zone de base |
| `/fk setcore <équipe> [x y z]` | Définit le bloc cœur (défaut : position du sender) |
| `/fk setspawn <équipe> [x y z]` | Définit le spawn (défaut : position du sender) |
| `/fk start` | Lance la phase de préparation (survie, PvP off) |
| `/fk game` | Lance la phase de combat (PvP on, TNT partout) |
| `/fk stop` | Arrête la partie |
| `/fk status` | Affiche l'état de la partie |

Alias : `/fallenkingdom` fonctionne aussi.

Permission requise : niveau OP 2 (admin).

## Compilation depuis les sources

Prérequis : **JDK 8** (pas une version plus récente, ForgeGradle 1.2 ne le supporte pas).

```bash
git clone https://github.com/jorislayouni/mod-minecraft-fallen-kingdom.git
cd mod-minecraft-fallen-kingdom
./gradlew setupDecompWorkspace
./gradlew build
```

Le `.jar` sera produit dans `build/libs/FallenKingdomMod-1.0.0.jar`.

Le premier build peut prendre 5 à 15 minutes (téléchargement de Forge + mappings MCP). Les builds suivants sont quasi instantanés.

## Limitations connues

- Pas de persistance : si le serveur redémarre en pleine partie, l'état des équipes/vies est perdu. À relancer depuis le lobby.
- Pas de scoreboard latéral : le suivi se fait via `/fk status`.
- Un joueur sans vie passe en mode aventure (pas spectateur) pour rester compatible avec les mappings 1.8.8.
- Pas de world border automatique : il faut gérer la zone de jeu manuellement (ex. via `/worldborder`).

## Structure du projet

```
src/main/java/fr/fallenkingdom/
├── FallenKingdom.java                  Classe @Mod, point d'entrée
├── Reference.java                      Constantes (modid, version)
├── game/
│   ├── GameManager.java                État global, phases, vies
│   ├── GamePhase.java                  Enum LOBBY / PREPARATION / GAME / ENDED
│   ├── FKTeam.java                     Une équipe (joueurs, base, cœur, spawn)
│   └── BaseRegion.java                 Zone rectangulaire
├── handlers/
│   ├── BlockPlaceHandler.java          Restriction pose de blocs (TNT exception)
│   ├── CoreBreakHandler.java           Détection destruction du cœur
│   └── PlayerEventHandler.java         PvP, friendly fire, vies, respawn
├── commands/CommandFK.java             Commande /fk
└── util/ChatUtil.java                  Helpers de chat
```

## Licence

À définir.
