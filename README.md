# Syntropy — A 2D Colony Automation Game

> *Inspired by Factorio and RimWorld*

Syntropy is a Java-based, 2D colony automation-survival simulation. You control a **leader character** who guides NPC colonists, automates resource chains, manages buildings, and keeps the colony alive. The AI handles routine tasks — eating, sleeping, hauling — while you focus on strategy.

## Gameplay

### The Leader (You)
- **WASD** to move around the colony
- **Double-click a colonist** to possess them (direct control) — double-click again to release
- **E** to pick up/drop items at your feet
- Build beds for colonists to rest on

### The Colonists (AI)
Each colonist has needs:
- **Hunger** (green) — they eat when food appears in the stockpile
- **Energy** (blue) — they sleep in beds
- **Mood** (yellow) — affected by work conditions and social interaction
- **Health** (red) — damaged by starvation; slowly regenerates when well-fed and rested

Colonists autonomously:
- **Haul** resources from buildings to the stockpile (capped at 5 per item type)
- **Eat** from the stockpile when hungry
- **Rest** in beds when tired
- **Wander** and socialize when needs are met

### Buildings
- **Miner** (×2) — produces stone every 5 seconds
- **Food Grower** — produces food every 6 seconds
- **Beds** (×4) — colonists sleep here to recover energy

### The Stockpile
The stockpile tile displays a live count of each item type with a small icon:
```
  [stone]x5
  [food]x9
  [wood]x3
```

### Terrain & Collision
- **Water** & **Stone** — impassable
- **Grass**, **Dirt**, **Sand** — walkable
- Collision is **feet-based**: characters can stand at the very edge of water/stone, and only get blocked when their feet would step onto it

## HUD Layout

**Top-Left Panel:**
- Stockpile resource summary
- Leader name, age, and status
- Pickup help text

**Top-Right Panel:**
- Full colonist list with stats
- `Name (Task)` with `HP:X% Food:X% Zzz:X% Mood:X%`
- Color-coded percentages: green (healthy), orange (warning), red (critical)

**Bottom Panel:**
- Horizontal colonist bar showing quick-stats for all colonists
- Event log (last 3 events)

## Controls

| Input | Action |
|---|---|
| **WASD** | Move leader / Pan camera |
| **Double-click colonist** | Possess / unpossess |
| **E** | Pick up / drop item (when possessed) |
| **B** | Enter/exit build mode (place beds — costs 3 wood each) |
| **Scroll** | Zoom in/out |
| **F5** | Save game (JSON) |
| **F9** | Load game |

## Save & Load
- Press **F5** to save — creates `syntropy_save.json` with full world state
- Press **F9** to load — restores all tiles, buildings, colonists, items, and positions

## Architecture

### Entity-Component-System (ECS)
All colonists and buildings are **entities** composed of small, reusable components:
- `PositionComponent` — x, y location (tile center + 0.5)
- `NeedsComponent` — hunger, energy, health (0.0–1.0 scale)
- `HealthComponent` — dead flag, death timer
- `InventoryComponent` — carried item
- `AIComponent` — task, target, stuck timer
- `IdentityComponent` — name, age

### Systems
- **NeedsSystem** — ticks hunger/energy decay, applies starvation damage, regens health when well-fed
- **AITaskSystem** — runs the think tree (eat → rest → haul → wander)
- **BuildingProductionSystem** — generates items from buildings
- **ThinkNode_*** — individual decision nodes (hunger check, path-to-stockpile, etc.)
- **EventSystem** — instance-based event bus (no static state)

### World
- **50×50 tile grid** procedurally generated with Perlin/Simplex noise
- Tile types: Water, Sand, Grass, Dirt, Stone
- Each tile stores: terrain type, building entity, ground items
- **Collision** checks character feet (narrow box at sprite bottom) against passable terrain

## Tech Stack

- **Language**: Java 21 (via Gradle toolchain)
- **Engine**: LibGDX 1.12.1 (LWJGL3 backend)
- **Build**: Gradle 8.8
- **Testing**: JUnit 5
- **Serialization**: Gson (for save/load)

## Project Structure

```
src/main/java/com/haraldsson/syntropy/
├── core/              GameApp, GameMain, GameState, GameHud, SpriteManager
├── ecs/               ECSWorld, Entity, Component base classes
├── ecs/components/    Position, Needs, Health, Inventory, AI, Identity, etc.
├── ecs/systems/       NeedsSystem, AITaskSystem, BuildingProductionSystem
├── ai/nodes/          ThinkNode_Eat, ThinkNode_Haul, ThinkNode_Rest, etc.
├── entities/          ItemType, TaskType, ColonistRole, Item
├── input/             PlayerController (movement, possession, camera)
├── world/             World, Tile, TerrainType, WorldGenerator, Pathfinder
└── systems/           EventSystem, EventType, SaveLoadSystem, SaveData
```

## Run

```bash
./gradlew run
```

Or use the green ▶ play button in IntelliJ next to `main()` in `GameMain.java`.

## Build

```bash
./gradlew build
```

Output JAR: `build/libs/syntropy-1.0-SNAPSHOT.jar`

## Recent Improvements (2026-02-21)

- **Feet-based collision** — Characters only blocked when their feet touch impassable terrain; can stand at water's edge
- **Better HUD panels** — Compact, dark-background panels that wrap content (not full-screen black boxes)
- **Readable stats** — All text bright white; colonist bars show color-coded percentages
- **Balanced hunger** — Slower drain (slower starvation), faster food production, bigger meals
- **Stockpile display** — Visual item counts with icons instead of tiny sprites
- **Centered sprites** — Character sprites centered on their logical position for accurate collision
- **Improved AI hauling** — Stops hauling when stockpile reaches 5 of any item type

