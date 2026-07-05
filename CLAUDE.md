# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Torque is a vehicle physics mod/plugin for Minecraft, built as a Gradle multi-project:

- `torque` — platform-agnostic core: vehicle physics, components, model/resource-pack processing. Pure Java, no Minecraft dependencies (they're `compileOnly` for types only).
- `torque-paper` — Paper plugin adapter (`io.papermc.paper`), depends on `torque`.
- `torque-fabric` — Fabric mod adapter (uses `fabric-loom` + Mojang mappings + mixins), depends on `torque`.

Both platform modules implement the `Platform` interface (`torque/src/main/java/ca/bkaw/torque/platform/Platform.java`) and its associated abstractions (`World`, `ItemStack`, `BlockState`, `entity/*`, `DataInput`/`DataOutput`) so the core module never touches Paper or Fabric APIs directly. When adding a feature that needs engine access, add/extend the abstraction in `torque/.../platform/` first, then implement it in both `torque-paper/.../platform/` and `torque-fabric/.../platform/`.

## Commands

Build everything:
```
./gradlew build
```

Build/assemble a single module (produces the shaded jar via `shadowJar`):
```
./gradlew :torque-paper:build
./gradlew :torque-fabric:build
```

Run unit tests (JUnit 5, only exist in `torque`):
```
./gradlew :torque:test
```

Run a single test class or method:
```
./gradlew :torque:test --tests "ca.bkaw.torque.SomeTest"
./gradlew :torque:test --tests "ca.bkaw.torque.SomeTest.someMethod"
```

Launch a local dev server with the plugin/mod installed (used to manually test in-game behavior):
```
./gradlew :torque-paper:runServer
./gradlew :torque-fabric:runServer
```
Both `runServer` tasks set `-Dtorque.assets=../../torque/src/main/resources`, which makes `Torque.reload()` load assets directly from source instead of from the built jar — edits to files under `torque/src/main/resources` take effect on `/torque reload` without rebuilding.

Java target is 21 (`gradle.properties`: `java_version`). Versions for Minecraft/Paper/Fabric/dependencies are centralized in `gradle.properties`.

## Architecture

### Vehicle / component model

A `Vehicle` (`torque/.../vehicle/Vehicle.java`) is a plain container of `VehicleComponent`s — an ECS-style composition, not inheritance. All behavior (physics, wheels, seats, gravity, drag, collision, steering, turn signals, hitboxes, etc.) lives in individual components under `torque/.../components/`. `Vehicle.tick()` just ticks every component; components look up sibling components via `Vehicle.getComponent(Class)`.

- `VehicleComponentType` (record) pairs an `Identifier`, a JSON config parser, and a constructor (`(Vehicle, config, DataInput) -> VehicleComponent`). Built-in types are registered in `VehicleManager.registerBuiltIns()`.
- `VehicleType` (record) is the immutable "class" of a vehicle: its model, mass/inertia tensor, and the ordered list of component configurations, parsed from a `torque_vehicle` JSON data file (see `VehicleType.fromJson`).
- Components serialize their own state via `VehicleComponent.save(Vehicle, DataOutput)`; construction reads it back via the `DataInput` passed to the constructor. Each component gets its own namespaced data section (keyed by component type identifier) so components can't clash — see `VehicleManager.saveVehicle`/`loadVehicle`.
- Some components implement extra interfaces to opt into cross-cutting behavior, e.g. `PartTransformationProvider` lets a component (like `SteeringWheelComponent`) contribute a rotation/translation to a named model part; `VehicleRenderer` polls all components implementing it each render.

`VehicleManager` (`torque/.../vehicle/VehicleManager.java`) owns the registries (component types, vehicle types), the list of loaded vehicles/renderers, the player→vehicle and entity→vehicle maps, and the tick loop (registered via `Platform.runEachTick`). It also supports freezing/stepping ticks for debugging (`freezeTicking`/`stepTicks`).

### Rendering

Vehicles are rendered as a "primary" `ItemDisplay` entity (which is also where vehicle state is persisted, via `DataInput`/`DataOutput`) plus additional `ItemDisplay` entities per model part, all mounted on the primary entity. `VehicleRenderer` computes each part's transformation matrix every tick from the `RigidBodyComponent`'s position/orientation, the part's base translation/scale (from the model), and any `PartTransformationProvider` contributions. Seats are handled separately (`SeatsComponent` + `SeatTags`), mounting passengers onto either the primary entity (if it's the viewport seat) or a dedicated invisible display entity.

### Assets pipeline (`torque/.../assets/`)

`TorqueAssets` builds a Minecraft resource pack (`torque_resource_pack.zip`) at startup/reload:
1. Loads a base resource pack (bundled in the jar, or overridden via `-Dtorque.assets=<dir>` for dev iteration).
2. For each vehicle model referenced by a `VehicleType`, `getOrCreateVehicleModel` reads the raw Blockbench-style model JSON (`assets/model/*`), recenters it on its center of mass, runs registered `TagHandler`s (`torque/.../tags/*`, e.g. `SeatTags`, `WheelTags`, `SteeringWheelTags`, `LightTags`) to extract tagged elements/groups into separate named model parts (`ModelExtractor`), rescales anything over the 3-block display limit, and writes out a `primary` model plus one model+item-model JSON per extracted part.
3. The finished pack is hashed (SHA-1) and served to joining/connected players via `ResourcePackSender` (`BuiltInTcpResourcePackSender` injects a Netty channel handler through `Platform.injectChannelHandler` to push the pack over the vanilla protocol).

`VehicleType.fromJson` also computes the vehicle's inertia tensor from the model's geometry (`InertiaTensor`), so mass distribution follows the model shape rather than being configured by hand.

### Identifiers, registries, data I/O

- `Identifier` (namespace:key) is the addressing scheme for vehicle types, component types, and models — mirrors Minecraft's resource location convention.
- `Registry<T>` (`torque/.../util/Registry.java`) is a small generic name→value map keyed by an `Identifier` extractor function; used for component types, vehicle types, and vehicle models.
- `DataInput`/`DataOutput` abstract persistent per-entity storage (PDC on Paper, NBT on Fabric — see `PdcInputOutput` / `NbtInputOutput`) with nested named sections, used for save/load of vehicle and component state.

### Fabric specifics

`torque-fabric` uses Mojang mappings and mixins (`torque.mixins.json`, `torque.accesswidener`) to hook into vanilla classes not exposed by a public API (e.g. `ServerConnectionListenerMixin`, `ItemDisplayMixin`, `InteractionMixin`) — check these before assuming Fabric-side behavior can be implemented without a mixin.

### Debugging

`Debug` (`torque/.../util/Debug.java`) provides opt-in print/tick hooks used throughout components and the assets pipeline; `VehicleManager.freezeTicking()`/`stepTicks(int)` let you pause simulation and single-step it, useful when working on physics.
