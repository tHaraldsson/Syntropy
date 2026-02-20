# Syntropy

A 2D colony automation-survival simulation game, inspired by Factorio and RimWorld. Built with Java 21 and LibGDX.

## Unique Twist: Possession Mechanic

Double-click any colonist to **possess** them — you directly control that colonist with WASD while the rest of the colony runs on AI. Double-click again to release. Seamlessly swap between hands-on play and high-level management.

## Features

### Core Gameplay
- **Procedurally generated** 30×30 world using simplex noise (water, sand, grass, dirt, stone terrain)
- **2 colonists** with full AI: wandering, hauling, eating, resting
- **3 need systems**: Hunger (green), Energy (blue), Mood (yellow) — all visible as bars
- **Death**: colonists die when hunger reaches zero; auto-unpossess on death

### Buildings
- **Miner** (×2) — produces stone every 5 seconds
- **Food Grower** — produces food every 10 seconds

### Automation
- Colonists autonomously haul resources from buildings to the stockpile
- AI priority system: Eat → Rest → Haul → Wander

### Research & Progression
- Press **R** to begin researching the next technology
- Tech tree: Fast Mining → Advanced Agriculture → Smelting → Conveyor Belts
- Research progresses passively over time

### Random Events
- Events trigger every 30–60 seconds
- Bountiful harvest, heat wave, morale boost, food spoilage, exhaustion
- Event log displayed on screen

### Save & Load
- **F5** to save, **F9** to load (JSON format)
- Full world state serialized: tiles, buildings, colonists, items

### HUD
- Stockpile resource counts
- Building output counts
- Colonist list with needs and current task
- Research progress
- Event log
- Controls help bar

## Controls

| Input | Action |
|---|---|
| **Double-click** colonist | Possess / unpossess |
| **WASD** | Move colonist (possessed) / Pan camera (free) |
| **E** | Pick up / drop item at your feet (possessed) |
| **R** | Start next research |
| **Scroll** | Zoom in/out |
| **F5** | Save game |
| **F9** | Load game |

## Tech Stack

- **Language**: Java 21
- **Engine**: LibGDX 1.12.1 (LWJGL3 backend)
- **Build**: Gradle 8.8

## Project Structure

```
src/main/java/com/haraldsson/syntropy/
├── core/          GameMain, GameApp, SaveLoadSystem, SaveData
├── entities/      Colonist, Building, Miner, FoodGrower, Item, ItemType, TaskType
├── input/         PlayerController (possession, camera, pickup)
├── systems/       TaskSystem, ResearchSystem, EventSystem, Technology
└── world/         World, Tile, TerrainType, WorldGenerator, SimplexNoise
```

## Run

```bash
./gradlew run
```

Or click the green ▶ play button next to `main()` in `GameMain.java` in IntelliJ.

## Build

```bash
./gradlew build
```

