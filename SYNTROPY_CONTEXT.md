Project Vision: Syntropy
Syntropy is a Java-based, 2D colony automation-survival simulation game, inspired by Factorio and RimWorld. Players automate resource chains, manage evolving needs and relationships of colonists, build and optimize their colony, and defend against threats—all while shaping emergent stories.

Unique Twist: Player Possession Mechanic
Unlike traditional simulations, Syntropy lets you "possess" any colonist at any time by pressing a key. While possessing, you directly control that colonist with WASD, disabling their AI, while the rest of the colony continues on autopilot. You can seamlessly swap between direct action and high-level management, blending hands-on play with deep simulation.

Tech Stack
Language: Java 17
Engine: LibGDX (2D rendering, input)
Build: Gradle
Architecture: Modular Entity-Component-System (ECS)
Package & System Architecture
core: Game loop, world management, time, saving/loading
entities: Colonist, Building, Item, Robot, and other core game objects
systems: Automation, needs/mood, social/relationships, combat, events, research
world: Chunk, Tile, WorldGenerator (procedural Perlin noise)
ui: Screens, HUD, tooltips, notifications
input: Possession controls, camera, selection
Key Class Responsibilities
Colonist: Holds metadata (name, age), needs (hunger, energy, mood), skills, relationships, inventory, AI state, and possession flag. update() method manages needs, tasks, and switches input modes.
Building / Miner: Abstract base for all buildings; handles position, rotation, input/output buffers, construction status, and update logic. Miner produces stone periodically for hauling.
Tile: Stores terrain/resource types, quantities, and references to buildings/colonists/items at a location.
World: 2D array of Tiles, chunked for performance.
PlayerController: Handles the possession mechanic, toggling WASD control and AI states for colonists.
TaskSystem: Assigns and prioritizes jobs such as hauling, eating, and working for unpossessed colonists.
Core Gameplay Loop
Manage: Oversee colonists, assign goals, optimize layouts
Automate: Build and improve supply chains (mining, hauling, storage)
Possess: Press 'P' to control a colonist directly at any time for fine-grained action
Survive: Manage needs, defend from threats, adapt to random events
Expand: Research, build, and face more complex colony challenges over time
MVP Feature Set
Renders a 10x10 tile grid
Includes at least one colonist (rectangle), movable by AI (random wandering) or WASD possession
One building (“Miner”) produces stone
Colonist hauls stone from miner to stockpile
Simple hunger mechanic — hunger decreases over time, colonist seeks/eats food
Next Steps
After completing the MVP, Syntropy’s development should continue in the following areas:

1. Polish and Bugfix MVP
   Thorough playtesting for stability and retention of gameplay flow
   Fix bugs in core loop: possession, AI, hauling, and needs
2. UI/UX Improvements
   Add visual indicators for colonist needs (e.g., hunger/mood bars)
   HUD for resource counters, colonist lists, and active possession
   Input feedback (highlight possessed colonist, prompts, tooltips)
3. Save & Load System
   Implement saving/loading of world and entity state (suggest JSON for MVP)
   Enables persistent colonies and faster iteration
4. Expand Gameplay Features
   Job System: Multiple concurrent tasks, job priorities, advanced stockpile logic
   Research & Progression: Simple tech tree and new building unlocks (e.g., conveyor belts, smelters)
   Additional Buildings: More automation options or logistics support (e.g., assembler, food grower)
   World Expansion: Larger or procedurally generated worlds, new resources
   Threats & Events: Randomized events and basic combat
   Social/Story: Simple relationships, events, and emergent stories
5. Refine ECS & Modular Design
   Extract and document ECS components for scalability
   Ensure new features follow ECS best practices
6. Documentation & Planning
   Update README and in-code docs to reflect new systems and features
   Create a GitHub Project board for task tracking and future planning
7. Gather Feedback
   Share for early playtesting and feedback from users or developers
   Prioritize enhancements and fixes based on user experience
   Syntropy fuses deep colony AI, direct player control, robust automation, and emergent storytelling—built on a clean, scalable Java ECS foundation and ready to grow.