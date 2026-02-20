# Project Vision: Syntropy

Syntropy is a Java-based, 2D colony automation-survival simulation game inspired by Factorio, RimWorld, and the Bobiverse book series. The player controls a single leader character who guides a growing colony of NPC colonists, automates resource chains, manages an evolving civilization, and ultimately spreads humanity across the stars — all while grappling with the environmental consequences of unchecked expansion.

The core narrative arc: **Survive → Automate → Expand → Cause Problems → Solve Them or Escape → Repeat on a New World.**

---

## Tech Stack
- **Language:** Java 17
- **Engine:** LibGDX (2D rendering, input)
- **Build:** Gradle
- **Architecture:** Modular Entity-Component-System (ECS)
- **Networking (future):** KryoNet

---

## Package & System Architecture
- `core` — Game loop, world management, time, saving/loading, event bus
- `entities` — Leader, Colonist, Child, Robot (Worker/Android), Building, Item
- `systems` — Needs, mood, AI/think tree, automation, pollution, research, combat, diplomacy, dynasty
- `world` — Chunk, Tile, WorldGenerator (procedural Perlin noise), biome, pollution map
- `ui` — Screens, HUD, colonist bar, tooltips, notifications, planet manager
- `input` — Leader controls, camera, selection, blueprint placement
- `factions` — Faction data, diplomacy state, relations, ideology

---

## Issue Classification (as of 2026-02-20)

### A — Logic Bugs (incorrect behavior violating stated invariants)
1. ✅ **`BuildingProductionSystem` did not fire `RESOURCE_PRODUCED`** — violated the invariant that all inter-system communication goes through the event bus. Fixed: fires `RESOURCE_PRODUCED` via `gameState.events.fireAndLog()` after each production tick.
2. ✅ **`ResearchSystem` did not fire `RESEARCH_COMPLETED`** — same invariant violation. Fixed: fires `RESEARCH_COMPLETED` via `events.fire()` when a tech's progress reaches its `researchTime`.
3. ✅ **Research system was a flat 4-tech tree** — the architecture specifies 5 eras with prerequisites. Fixed: replaced with 22 techs across 5 eras, each with `id`, `displayName`, `era`, `prerequisites`, `researchCost`. `startResearch()` enforces prerequisites before beginning.
4. ✅ **Colonist death did not drop carried items** — if a colonist died while carrying an item, the item was silently lost. Fixed: `NeedsSystem` now drops `inv.carriedItem` to the ground tile at the colonist's position on the first tick after death. `HealthComponent.deathItemsDropped` flag prevents duplicate drops.
5. ✅ **`ThinkNode_Haul` fired for ALL colonists regardless of role** — `ThinkNode_Haul` at priority 50 activated for any colonist who saw building output, conflicting with `ThinkNode_DoAssignedJob` at the same priority. Fixed: `ThinkNode_Haul.getPriority()` now checks `WorkSettingsComponent` for `HAULER` role (priority > 0) and returns 0 if not a HAULER.
6. ✅ **`successionCandidates` / `pendingSuccession` field name consistency** — verified in `GameApp`: `private boolean pendingSuccession` and `private List<Entity> successionCandidates` are used consistently throughout the file. No mismatch found.

### B — State-Transition Ambiguities (undefined/contradictory behavior in edge cases)
1. **HAULER carrying item on death** — if a HAULER dies mid-haul, the item is now dropped to their position tile. Whether hauling resumes for another colonist is undefined (no pending haul task system yet).
2. **Multiple colonists targeting same building output** — two HAULER colonists can both pick from the same building in the same tick before outputs are decremented. Acceptable given current single-output-per-tick design, but could cause race conditions with multiple haulers.
3. **Research interrupted by load** — if the game is saved mid-research and loaded, `currentResearch` resumes from `progress`. This is the intended behavior but is implicit.

### C — Acceptable Design Failures (intentional limitations or known tradeoffs)
1. **No `ThinkNode_ReactToEmergency`** — requires combat/threat system which is not yet implemented. Acceptable limitation.
2. **Single stockpile tile** — `World.getStockpileTile()` returns one tile. Multiple stockpiles would require a list, but single-stockpile is acceptable for current scope.
3. **Linear tech ordering in `startNextResearch()`** — picks first available tech in declaration order. Players may prefer to choose; this is deferred to a research UI screen.

---

## Design Decisions (as of 2026-02-20)

1. **Leader death → Game Over?** — No. Player chooses a successor from existing colonists. Only a total colony wipe (zero surviving colonists) is game over.
2. **`aiDisabled` flag** — Yes. The leader entity uses `AIComponent.aiDisabled = true` so `AITaskSystem` skips it. This is the canonical way to distinguish player-controlled entities from NPC entities.
3. **Carried items on colonist death** — Items are dropped to the ground tile at the colonist's last position (`PositionComponent.x`, `PositionComponent.y`). Items are not destroyed, not eaten. `HealthComponent.deathItemsDropped` prevents duplicate drops across ticks.
4. **Stuck colonists** — Any colonist stuck longer than their node's `STUCK_TIMEOUT_SECONDS` threshold clears their task and returns to the think tree. This is the canonical anti-freeze mechanism. All ThinkNodes implement this pattern.
5. **Event bus** — Always instance-based via `gameState.events`. Never static. Enforced by architecture. `GameEvents.clearListeners()` is called on load to prevent double-wiring.

---


---

## The Player — The Leader

The player controls **one single character**: the colony's leader. There is no possession-swapping. The leader IS the player.

### Leader Properties
- Has **stats**: charisma, engineering, science, combat — each affects colony efficiency
- **Ages in real time** relative to game progression (~60–80 in-game years lifespan)
- **Can be killed** by enemies, disasters, or accidents
- On death (natural or otherwise) → player **chooses a successor** from existing colonists
- Successors **inherit some stats** from the previous leader (especially if they are a child)
- This creates a **dynasty system** — your legacy is the colony, not just one life
- **No true game over** unless the entire colony is wiped out

---

## Colonists (NPCs)

Colonists are autonomous NPCs driven by an AI think tree. The player does not directly control them.

### Colonist Properties
- **Needs:** hunger, sleep, happiness, health
- **Jobs/Roles:** farmer, miner, medic, engineer, scientist, soldier, builder
- **Recruitment:** arrive naturally over time, attracted by colony size/reputation, or born
- **Children:** colonists form relationships and have children who grow up over time
- **Trait inheritance:** children inherit traits from parents (skills, personality)
- This creates emergent **dynasty and bloodline** mechanics

### Job Assignment Progression
1. Player assigns roles manually (like RimWorld work priorities)
2. **Blueprint automation** — place a blueprint in storage, colonists sense and build automatically
3. **Learned crafting** — experienced colonists craft buildings from raw resources without blueprints
4. Colonists improve at their jobs over time (skill progression)

---

## Robots

Two distinct tiers of robots, unlocked through research:

### Tier 1 — Worker Bots (Factorio-style)
- Purely functional: carry, build, mine, haul
- Powered by solar/battery — low pollution
- No needs, no social layer
- Cheap and mass-produceable
- Cannot do complex tasks (research, medicine, diplomacy)

### Tier 2 — Android Colonists (Hybrid)
- Can perform complex tasks like humans
- Require **charging stations** → produce **pollution/waste**
- Limited in number due to environmental cost
- Much faster builders and workers than humans
- The tradeoff: powerful but environmentally costly

**Design tension:** Androids are efficient but accelerate the global pollution meter. This is intentional.

---

## Automation & Building Progression

1. **Manual** — player and colonists build everything by hand
2. **Blueprint system** — player places blueprints in storage; colonists retrieve and construct
3. **Learned crafting** — experienced colonists craft from raw resources automatically
4. **Bot automation** — Worker Bots handle all logistics (Factorio-style)
5. **Android construction** — Androids build at high speed but increase pollution

---

## Eco-Friendly vs Industrial Machines

Every major machine type has two variants:

| | Industrial | Eco-Friendly |
|---|---|---|
| Output | High | Lower |
| Speed | Fast | Slower |
| Pollution | High | Minimal |
| Cost | Cheaper | More expensive |
| Unlock | Early | Later (research) |

---

## Pollution & Environment System

### Regional Pollution
- Each map region has a local pollution level
- Heavy industry in one area devastates that biome
- Forests stop growing and die
- Colonists in polluted areas get health/mood debuffs
- Spreading industry across regions delays local collapse

### Global Pollution (Planetary Health Meter)
- All colonies on the planet (including rival factions) contribute to a global meter
- Even if YOU are eco-friendly, a neighbor industrializing can doom the planet
- As meter rises: crop failures, extreme weather, health crises, ecosystem collapse
- At critical levels: planet becomes uninhabitable → must expand to space

### Consequences
- Forests die and do not regrow
- Colonist mood and health debuffs in polluted zones
- Crop yield reduction
- Eventually: uninhabitable zones expand across the map

---

## Factions & Diplomacy

Other colonies exist on the planet with their own ideologies.

### Faction Types
- **Eco colonies** — sustainable, slow growth, friendly
- **Industrial colonies** — fast expansion, high pollution, aggressive
- **Hostile factions** — raiders, pirates, competitors

### Diplomacy Options
- Trade, Alliance, Annexation by diplomacy, Annexation by force, Coexistence

### Key Mechanic
Rival faction pollution contributes to the global meter. You may need to stop a neighbor — by diplomacy or force — to save the planet. Moral dilemmas are intentional.

---

## Research Eras

### Era 1 — Survival
Basic farming, mining, storage, simple buildings, colonist needs management

### Era 2 — Automation
Blueprints, basic bots, conveyor systems, improved farming, early medicine

### Era 3 — Industrial
Advanced machines, androids, eco-friendly alternatives, pollution management, diplomacy

### Era 4 — Space
Rocketry, off-world communication, terraforming probe, multi-colony management

### Era 5 — Legacy
Advanced terraforming, inter-colony networks, planetary stewardship, new world colonization

---

## Space Expansion & Multi-Colony Management

### Multiple Colonies
- Player manages multiple colonies simultaneously
- Switch between colonies instantly (tab/map view)
- Each colony runs in real time — automation is critical
- Colonies can trade resources and share research

### Terraforming Probe (Bobiverse-inspired)
- Built on current planet before departure
- Player controls the probe directly as a **second gameplay mode**
- Probe must automate: oxygen generation, temperature regulation, soil preparation
- Probe gameplay: place machines, automate processes, prepare world for human arrival
- Once terraformed sufficiently → colonists can be sent

### Planet Hopping
- Leaving a planet does not mean abandoning it
- Bring key resources and technology with you
- Both colonies managed simultaneously in real time
- The old planet may still be dying — watch it collapse while building anew

---

## AI System — Think Tree

Colonist AI uses a **priority-based think tree**. Each node returns a priority float. Highest valid priority wins.

### Think Node Chain (priority order)
1. `ReactToEmergency` — fire, attack, injury (priority: 999)
2. `EatIfStarving` — seek food if hunger critical (priority: 100)
3. `SleepIfExhausted` — rest if energy critical (priority: 80)
4. `DoAssignedJob` — perform assigned role (priority: 50)
5. `ExecuteBlueprint` — pick up and place blueprints (priority: 30)
6. `Socialize` — interact with colonists if needs met (priority: 10)
7. `Wander` — idle behavior (priority: 1)

---

## Design Patterns & Reference Implementations

These are original Java patterns written for Syntropy, derived from studying
RimWorld and Factorio design concepts. No code has been copied — all is original.

### Pattern 1 — Tiered Needs (HungerCategory)
Instead of tracking hunger as a raw float, derive a named category from it.
Each category drives different AI priority, speed, efficiency, and mood offsets.

```java
public enum HungerCategory {
    FED, HUNGRY, URGENTLY_HUNGRY, STARVING;

    public static HungerCategory fromLevel(float level) {
        if (level > 0.6f) return FED;
        if (level > 0.3f) return HUNGRY;
        if (level > 0.1f) return URGENTLY_HUNGRY;
        return STARVING;
    }
}

// Same pattern applies to energy:
public enum EnergyCategory {
    RESTED, TIRED, EXHAUSTED, COLLAPSED;

    public static EnergyCategory fromLevel(float level) {
        if (level > 0.6f) return RESTED;
        if (level > 0.3f) return TIRED;
        if (level > 0.1f) return EXHAUSTED;
        return COLLAPSED;
    }
}
```

### Pattern 2 — Decoupled Mood System
Needs NEVER touch mood directly. ThoughtWorkers observe need state
and inject mood modifiers. Keeps all systems independent and extensible.

```java
public interface ThoughtWorker {
    float getMoodOffset(Colonist c);
}

public class HungerThoughtWorker implements ThoughtWorker {
    public float getMoodOffset(Colonist c) {
        return switch (c.getHungerCategory()) {
            case FED            ->   0f;
            case HUNGRY         ->  -5f;
            case URGENTLY_HUNGRY-> -15f;
            case STARVING       -> -40f;
        };
    }
}

public class MoodSystem {
    private final List<ThoughtWorker> workers = List.of(
        new HungerThoughtWorker(),
        new SleepThoughtWorker(),
        new SocialThoughtWorker()
        // add more without touching existing code
    );

    public float calculateMood(Colonist c) {
        return workers.stream()
            .mapToFloat(w -> w.getMoodOffset(c))
            .sum();
    }
}
```

### Pattern 3 — Think Tree AI
Colonist AI is a chain of ThinkNodes. Highest priority valid node wins each tick.

```java
public interface ThinkNode {
    float getPriority(Colonist c);
    Task tryIssueTask(Colonist c);  // returns null if cannot issue
}

public class ThinkNode_EatIfStarving implements ThinkNode {
    public float getPriority(Colonist c) {
        return switch (c.getHungerCategory()) {
            case STARVING        -> 100f;
            case URGENTLY_HUNGRY ->  70f;
            default              ->   0f;
        };
    }

    public Task tryIssueTask(Colonist c) {
        Food food = c.getWorld().findNearestFood(c.getPosition());
        return food != null ? new EatTask(c, food) : null;
    }
}

public class ColonistAI {
    private final List<ThinkNode> nodes = List.of(
        new ThinkNode_ReactToEmergency(),
        new ThinkNode_EatIfStarving(),
        new ThinkNode_SleepIfExhausted(),
        new ThinkNode_DoAssignedJob(),
        new ThinkNode_ExecuteBlueprint(),
        new ThinkNode_Socialize(),
        new ThinkNode_Wander()
    );

    public void tick(Colonist c) {
        nodes.stream()
            .filter(n -> n.getPriority(c) > 0)
            .max(Comparator.comparingDouble(n -> n.getPriority(c)))
            .map(n -> n.tryIssueTask(c))
            .filter(Objects::nonNull)
            .ifPresent(c::assignTask);
    }
}
```

### Pattern 4 — Event Bus
Systems never call each other directly. All communication goes through events.
This keeps systems decoupled and makes multiplayer/save-load much easier.

```java
public enum EventType {
    RESOURCE_PRODUCED,
    RESOURCE_CONSUMED,
    COLONIST_DIED,
    COLONIST_BORN,
    POLLUTION_INCREASED,
    BUILDING_PLACED,
    RESEARCH_COMPLETED,
    LEADER_DIED,
    FACTION_RELATION_CHANGED
}

public class GameEvents {
    private static final Map<EventType, List<Consumer<Object>>> listeners = new HashMap<>();

    public static void on(EventType type, Consumer<Object> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public static void fire(EventType type, Object payload) {
        listeners.getOrDefault(type, List.of())
            .forEach(l -> l.accept(payload));
    }
}

// Usage — firing:
GameEvents.fire(EventType.RESOURCE_PRODUCED, new ResourcePayload(ResourceType.STONE, 10));

// Usage — subscribing:
GameEvents.on(EventType.RESOURCE_PRODUCED, payload -> {
    ResourcePayload p = (ResourcePayload) payload;
    pollutionSystem.onResourceProduced(p);
    storageSystem.onResourceProduced(p);
});
```

### Pattern 5 — Serializable GameState Root Object
ALL game state lives in one root object. No statics, no singletons for game data.
This is the foundation for save/load AND future multiplayer.

```java
public class GameState {
    public World world;
    public Leader leader;
    public List<Colonist> colonists;
    public List<Robot> robots;
    public List<Faction> factions;
    public PollutionData pollution;
    public ResearchState research;
    public List<Colony> colonies;       // for multi-planet
    public DynastyHistory dynasty;      // leader succession history
    // Serialized via Gson — everything must be serializable
}

// Save:
String json = new Gson().toJson(gameState);
Files.writeString(Path.of("save.json"), json);

// Load:
GameState gameState = new Gson().fromJson(json, GameState.class);
```

### Pattern 6 — Per-Colonist Work Priority Settings
Each colonist has their own job priorities (1–4 scale, 0 = disabled).
Used by the ThinkNode_DoAssignedJob node to pick the right task.

```java
public class WorkSettings {
    private final Map<JobType, Integer> priorities = new HashMap<>();

    public void setPriority(JobType job, int priority) {
        if (priority < 0 || priority > 4)
            throw new IllegalArgumentException("Priority must be 0–4");
        priorities.put(job, priority);
    }

    public int getPriority(JobType job) {
        return priorities.getOrDefault(job, 0);
    }

    public List<JobType> getActiveJobsSorted() {
        return priorities.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .sorted(Map.Entry.<JobType, Integer>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();
    }
}
```

### Pattern 7 — HUD Colonist Bar
Each colonist in the HUD bar shows a portrait + status icons.
Status is derived from current AI state and needs — never stored separately.

```java
public enum ColonistStatus {
    IDLE, EATING, SLEEPING, WORKING, FLEEING, ATTACKING, INJURED
}

public class ColonistBarEntry {
    private final Colonist colonist;

    public ColonistStatus getStatus() {
        if (colonist.isInCombat())        return ColonistStatus.ATTACKING;
        if (colonist.isFleeing())         return ColonistStatus.FLEEING;
        if (colonist.isInjured())         return ColonistStatus.INJURED;
        if (colonist.isSleeping())        return ColonistStatus.SLEEPING;
        if (colonist.isEating())          return ColonistStatus.EATING;
        if (colonist.hasActiveJob())      return ColonistStatus.WORKING;
        return ColonistStatus.IDLE;
    }

    public void render(SpriteBatch batch, float x, float y) {
        // Draw portrait
        batch.draw(colonist.getPortraitTexture(), x, y, 32, 32);
        // Draw status icon on top
        batch.draw(getStatusIcon(), x + 20, y + 20, 12, 12);
        // Draw hunger bar below
        renderNeedBar(batch, x, y - 6, colonist.getHunger(), Color.ORANGE);
        // Draw energy bar below hunger
        renderNeedBar(batch, x, y - 12, colonist.getEnergy(), Color.CYAN);
    }
}
```

---

## Architecture Rules (Non-Negotiable)

1. **No static game state** — all world/entity/faction data lives in `GameState`
2. **No singletons for game data** — pass `GameState` explicitly or use dependency injection
3. **ECS components must be serializable** — use Gson-compatible types only
4. **All inter-system communication via event bus** — no direct system-to-system calls
5. **Multiplayer-ready from day one** — KryoNet when implemented; architecture must support it now

---

## Current Development Status (as of 2026-02-20)

### Completed ✅
- MVP: 50x50 tile grid (simplex noise terrain), colonist movement, miner/food grower buildings, hauling, hunger mechanic
- Factorio-inspired procedural sprite rendering (SpriteManager — textured terrain, industrial buildings, humanoid colonists, item icons)
- Scene2D HUD (GameHud — resource counters, colonist list, leader info, event log, controls, status messages)
- Save/Load system (full ECS serialization — all components, tiles, buildings, entities)
- Permanent leader character — WASD movement, camera follow, E to pickup/drop, no possession swapping
- Leader stats: charisma, engineering, science, combat (affects colony efficiency, research, diplomacy)
- **Pattern 1 — Tiered Needs:** `HungerCategory` (FED/HUNGRY/URGENTLY_HUNGRY/STARVING), `EnergyCategory` (RESTED/TIRED/EXHAUSTED/COLLAPSED). Needs on 0.0–1.0 scale.
- **Pattern 2 — Decoupled Mood System:** `MoodComponent` separate from needs. `ThoughtWorker` interface + `HungerThoughtWorker`, `SleepThoughtWorker`, `HealthThoughtWorker`, `SocialThoughtWorker`. `MoodSystem` calculates mood as sum of all ThoughtWorker offsets.
- **Pattern 3 — Think Tree AI:** `ThinkNode` base class with `getPriority()` + `execute()`. `ThinkTreeRoot` picks highest-priority node. Implemented nodes: `ThinkNode_EatFood` (priority 100), `ThinkNode_Rest` (priority 80), `ThinkNode_Haul` (priority 50), `ThinkNode_Wander` (priority 1). `AITaskSystem` skips leader entity.
- **Pattern 4 — Event Bus:** `GameEvents` class with `EventType` enum, `on()`/`fire()`/`fireAndLog()`. Instance-based (not static). Lives in `GameState`.
- **Pattern 5 — GameState Root Object:** `GameState` class holding `World`, `ECSWorld`, `GameEvents`, `ResearchSystem`, `PollutionSystem`, dynasty tracking. No statics for game data.
- **Pattern 6 — Per-Colonist Work Priorities:** `WorkSettingsComponent` with `Map<ColonistRole, Integer>` priorities (0–4 scale). `getActiveJobsSorted()` for Think Tree.
- Global pollution system (`PollutionSystem` — building pollution rates, natural decay, colonist health debuffs, severity labels)
- Aging system (`AgingComponent` + `AgingSystem` — real-time aging, natural death, leader succession with stat inheritance)
- Dynasty/succession — player chooses successor via pause UI (keys 1–5); auto-picks if only one candidate
- Colonist roles (`ColonistRole` enum + `RoleComponent`)
- Random event system (heat waves, food blessings, exhaustion events)
- Basic research system (4 techs — needs expansion to 5 eras)
- `.gitignore` for save files
- **GameState root wired into GameApp** — all systems reference `gameState.world`, `gameState.ecsWorld`, `gameState.pollution`, `gameState.research`
- **Event bus wired** — `GameEvents` fires `COLONIST_DIED`, `LEADER_DIED`, `LEADER_SUCCEEDED`, `RESEARCH_COMPLETED`, `BUILDING_COMPLETED` events. Listeners log to event bus.
- **ThinkNode_DoAssignedJob** — uses `WorkSettingsComponent` priorities (Pattern 6). Colonists perform their assigned role (HAULER, MINER, FARMER) based on priority settings.
- **ThinkNode_Socialize** — colonists seek out nearby colonists to socialize when needs are met (priority 10). Stays for 5s then clears (oscillation fix).
- **Think Tree now has 6 nodes:** EatFood (100), Rest (80), DoAssignedJob (50), Haul (50), Socialize (10), Wander (1)
- `HealthComponent.deathEventFired` flag — prevents duplicate death events
- **Terrain collision for leader** — `PlayerController` checks X/Y independently so the leader slides along walls
- **NPC terrain collision (axis-split sliding)** — `AIComponent.moveTowardTarget` slides along walls instead of freezing; colonists cannot walk through impassable tiles
- **EatFood stuck timeout** — `ThinkNode_EatFood` uses `ai.stuckTimer` (6s) to clear stuck `MOVE_TO_FOOD`/`MOVE_TO_FOOD_GROWER` tasks
- **Haul stuck timeout (5s)** — `ThinkNode_Haul` clears task if colonist cannot reach building or stockpile within 5s
- **DoAssignedJob stuck timeout (5s)** — `ThinkNode_DoAssignedJob` clears task if colonist cannot reach target within 5s
- **Rest bed-stuck timeout (10s)** — `ThinkNode_Rest` falls back to sleeping on ground if colonist can't reach owned bed within 10s
- **Wander timer guard** — `ThinkNode_Wander` no longer stomps `wanderTimer` while a colonist is `RESTING`
- **Shared stuckTimer moved to `AIComponent`** — `ai.stuckTimer`, `ai.stuckTargetX`, `ai.stuckTargetY` are per-colonist fields; no more shared state across ThinkNode instances
- **Nearest-building hauling** — `ThinkNode_Haul`, `ThinkNode_DoAssignedJob`, and `ThinkNode_EatFood` all pick the **nearest** building with output (by distance via PositionComponent) instead of the first in iteration order
- **SocialThoughtWorker** — grants +15f mood boost when a colonist has `SOCIALIZING` task; grants +8f when `WANDER` task and within range 3f of another colonist. Wired into `MoodSystem` (Pattern 2)
- **`SOCIALIZING` task type** — distinct from `WANDER`; used by `ThinkNode_Socialize` when colonist is in social range; colonist stays for 5s then clears
- **HUD colonist bar (Pattern 7)** — horizontal scrollable row at the bottom of the screen. Each non-leader colonist entry shows: name (truncated), status label (IDLE/EATING/SLEEPING/WORKING/HAULING/SOCIALIZING), hunger bar (orange, ASCII), energy bar (cyan, ASCII). Dead colonists shown in grey with "DEAD". Updated every frame in `GameHud.update()`. Text-only, no portraits.
- **Energy bar** — fourth bar (blue/cyan) added to world-space colonist need bars in `GameApp.renderColonists()`. Y offsets adjusted so all 4 bars (hunger/health/energy/mood) fit.
- **Status message rendering** — `statusMessage` now drawn centered at top of viewport using `smallFont` (screen-space projection); shown when `statusTimer > 0`.
- **Dead colonist despawn** — `HealthComponent.deathTimer` increments after death; entity removed from `ECSWorld` after 30 seconds via `NeedsSystem`.
- **Passive health regen** — `NeedsSystem` heals at `HEALTH_REGEN` rate when colonist is `FED` and `RESTED`.
- **Succession selection UI** — when succession is needed and multiple candidates exist, game pauses and shows overlay with up to 5 candidates + stats; player presses 1–5 to choose. Single candidate auto-selected with message.
- **World reset (Ctrl+R)** — regenerates a fresh 50×50 world without restarting the application. Shown in HUD controls hint.
- **World size 50×50** — `WORLD_WIDTH` and `WORLD_HEIGHT` constants updated from 30 to 50.
- **Event bus wired into BuildingProductionSystem** — `BuildingProductionSystem` holds a `GameEvents` reference (set via `setEvents()` in `wireEventBus()`). Fires `RESOURCE_PRODUCED` via `fireAndLog` each time a building outputs an item.
- **Event bus wired into ResearchSystem** — `ResearchSystem` accepts `GameEvents` in its constructor (passed from `GameState`). Fires `RESEARCH_COMPLETED` (via `fire()`) when a tech finishes; the existing `wireEventBus()` listener logs the completion message.
- **5-era research system** — `Technology` gains `era` (1–5) and `List<String> prerequisites` fields. `ResearchSystem` now contains 22 techs across 5 eras (Survival → Automation → Industrial → Space → Legacy) with prerequisite enforcement. `startResearch(String techId)`, `isCompleted(String techId)`, and `prerequisitesMet(Technology)` added. `startNextResearch()` respects prerequisites.

- **ThinkNode_Haul restricted to HAULER-role colonists** — `ThinkNode_Haul.getPriority()` now returns 0 for any colonist whose `WorkSettingsComponent` has HAULER priority = 0. Only colonists with an active HAULER role (priority > 0) activate this node. ✅ (FIX: 2026-02-20)
- **Colonist death drops carried items** — `NeedsSystem` drops `inv.carriedItem` to the ground tile at `(pos.x, pos.y)` on the first tick after death. `HealthComponent.deathItemsDropped` flag prevents duplicate drops. ✅ (FIX: 2026-02-20)
- **Regression tests (13 headless JUnit 5 tests)** — `BuildingProductionSystemTest` (2), `ResearchSystemTest` (4), `ColonistDeathTest` (3), `ThinkNodeHaulTest` (4). All pass without LibGDX rendering. ✅ (2026-02-20)

### Current Focus 🔨
- Add `ThinkNode_ReactToEmergency` (fire, attack, injury — priority 999). Needs combat/threat system first.
- Basic faction system (at least one rival AI colony contributing to global pollution)
- Regional pollution (per-tile pollution levels, biome degradation)

### Near-Term Goals 📋
- Blueprint placement system (place ghost → colonists auto-build)
- Eco-friendly vs industrial building variants

### Future Goals 🚀
- Children and trait inheritance (colonist relationships, bloodlines)
- Robot tiers (Worker Bots + Androids with pollution tradeoff)
- Diplomacy system (trade, alliance, annexation)
- Space expansion + terraforming probe gameplay
- Multi-colony management (tab between colonies)
- Multiplayer (KryoNet)

### Open Questions ❓
1. ~~**Successor selection UI:**~~ **RESOLVED** — pause overlay with up to 5 candidates + stats; player presses 1–5. Auto-picks if only one candidate.
2. ~~**Event bus wiring:**~~ **RESOLVED** — `GameEvents` is instance-based, lives in `GameState`, passed implicitly through `gameState.events`. Systems access it through GameState.
3. **Regional pollution granularity:** Per-tile or per-chunk (e.g., 5x5 tile regions)?
4. **Research eras:** Should techs within an era be researchable in any order, or strictly linear? **Current:** any order within an era as long as prerequisites are met.
5. **Combat system:** What triggers `ThinkNode_ReactToEmergency`? Do we need hostile entities first, or can we start with natural disasters (fire)?