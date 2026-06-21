# 2023 Charged Up Port — Design Spec

**Date:** 2026-06-21
**Status:** Approved for planning
**Goal:** Convert maple-sim from the 2026 (Rebuilt) default to the **2023 FRC game, CHARGED UP**, including field boundaries, obstacles, the charge station, grids (as enterable zones with non-passable dividers), and the cone/cube game pieces, with height-aware grid scoring.

---

## 1. Scope

In scope:

- New season package `chargedup2023` mirroring the structure of existing season packages.
- Full physical field: perimeter, grids (enterable bays + dividers), charge stations, barriers, double-substation protrusions.
- Game pieces: CONE and CUBE, both as on-field bodies and as in-flight projectiles.
- Height-aware grid scoring using the existing `Goal` abstraction (same mechanism as 2025 Reefscape), including LINK detection and a published match breakdown.
- Replace the existing season packages and retarget the default arena to 2023.

Out of scope (explicitly):

- Charge-station balance / dock / engage logic. The charge station is a 3D tilting ramp that the 2D engine cannot simulate. A **separate 3D layer will be developed later**; this port leaves a clean extension point for it.
- Cooperation/sustainability bonuses beyond what naturally falls out of LINK counting (can be a follow-up).
- Single-substation and double-substation human-player *throwing* simulation (substations are treated as piece-feed/spawn points, not active throwers).

---

## 2. Source of truth for geometry

Primary geometry source: **FRC 6328 (Mechanical Advantage) `FieldConstants.java`** for 2023, supplied by the user. It is machine-readable and exact (inches → meters). Values below use the standard (non-WPI) field variant, which matches the field size in the WPILib bundled `2023-chargedup.json`.

Supporting references:

- [2023 FRC Game Manual — Section 5 ARENA](https://firstfrc.blob.core.windows.net/frc2023/Manual/Sections/2023FRCGameManual-05.pdf)
- [2023 Layout & Marking Diagram](https://firstfrc.blob.core.windows.net/frc2023/FieldAssets/2023LayoutMarkingDiagram.pdf) (image-based CAD; visual cross-check only)
- [WPILib `2023-chargedup.json` field size](https://www.chiefdelphi.com/t/2023-chargedup-json/422689)
- [REV Game Elements — Charged Up](https://docs.revrobotics.com/frc-kickoff-concepts/charged-up-2023/game-elements)
- Local PDF: `2023FRCGameManual.pdf`, field image `charged-up-field-1100.webp`.

**Coordinate system:** origin at the rightmost point of the BLUE alliance wall; X increases into the field (away from blue wall), Y increases to the left (NWU). Blue elements live at low X. Red elements are the 180° rotational flip: `(x, y) -> (FIELD_LENGTH - x, FIELD_WIDTH - y)`.

**Field dimensions:** length (X) = 651.25 in = **16.542 m**, width (Y) = 315.5 in = **8.014 m**.

---

## 3. Architecture

```
org.ironmaple.simulation.seasonspecific.chargedup2023/
├── Arena2023ChargedUp.java          # extends SimulatedArena; inner ChargedUpFieldObstaclesMap (FieldMap)
├── ChargedUpConeOnField.java        # extends GamePieceOnFieldSimulation, type "Cone"
├── ChargedUpCubeOnField.java        # extends GamePieceOnFieldSimulation, type "Cube"
├── ChargedUpConeOnFly.java          # extends GamePieceProjectile (scoring projectile)
├── ChargedUpCubeOnFly.java          # extends GamePieceProjectile (scoring projectile)
├── ChargedUpGridSimulation.java     # implements SimulatedArena.Simulatable; one per alliance; owns nodes
└── ChargedUpNode.java               # extends Goal; one per scoring location (27 per alliance)
```

No changes to the core physics engine beyond the default-instance swap and the intake stack-handling removal. The `Goal`, `GamePieceProjectile`, `GamePieceOnFieldSimulation`, and `FieldMap` abstractions are reused as-is.

### Packages removed

- `seasonspecific/crescendo2024/` (all files)
- `seasonspecific/reefscape2025/` (all files)
- `seasonspecific/rebuilt2026/` (all files)

### Packages kept

- `seasonspecific/evergreen/` — generic, non-season test arena. Removing it risks breaking tests/templates.

---

## 4. Core wiring changes

1. **`SimulatedArena.java`**
   - Line ~111 (`getInstance`): default instance `new Arena2026Rebuilt()` → `new Arena2023ChargedUp()`.
   - Line ~130 (`overrideInstance`): the stray `if (instance != null) instance = new Arena2026Rebuilt();` references a deleted class; remove that line (it is a latent bug — it overwrites the new instance with a default before the real assignment).
2. **`IntakeSimulation.java`**
   - Remove the `import ...reefscape2025.ReefscapeCoralAlgaeStack;` (line ~24).
   - Remove the two `instanceof ReefscapeCoralAlgaeStack` branches (lines ~320-323). No stack concept exists in 2023, so collisions fall through to the normal game-piece path.
3. **`FieldMirroringUtils.java`**
   - `FIELD_WIDTH` (length, X) `17.548` → `16.542`.
   - `FIELD_HEIGHT` (Y) `8.052` → `8.014`.
   - `flip()` stays 180° rotational (already correct for the 2023 rotationally-symmetric field).
4. **`CLAUDE.md`** — update the season section to reflect `chargedup2023` as current and the removed packages.

---

## 5. Field obstacle map (`ChargedUpFieldObstaclesMap`)

All values below are the blue-side layout in meters. Red obstacles are produced by the 180° flip. The map is built from `FieldMap` primitives: `addBorderLine(a, b)` (segment wall), `addRectangularObstacle(w, h, pose)`, `addCustomObstacle(convex, pose)`.

### 5.1 Perimeter

Four border lines forming the field rectangle: corners (0,0), (16.542,0), (16.542,8.014), (0,8.014). No angled corners.

### 5.2 Grid — enterable bays with non-passable dividers

The grid is **not** a solid block. It is a set of 9 column-bays a robot can nose into from the field side, separated by non-passable dividers running the full grid depth.

- Grid depth: X from 0 (alliance wall) to `outerX` = 54.25 in = **1.378 m**.
- 9 column centers, Y = 20.19 + 22·i in (i = 0..8):
  0.513, 1.071, 1.630, 2.189, 2.748, 3.307, 3.865, 4.424, 4.983 m.
- **Back wall:** border line along X ≈ 0 across the grid Y span (or rely on the alliance-wall perimeter line; back wall added explicitly to keep the bays closed at the rear).
- **8 dividers:** non-passable segments at the midpoints between adjacent column centers, each running X[0, 1.378]. Divider thickness ≈ 0.05–0.13 m (model as thin rectangles so they are true colliders, not zero-width segments, to reliably block a bumper). Bay boundaries (Y): 0.792, 1.351, 1.909, 2.468, 3.027, 3.586, 4.145, 4.704 m.
- Bays open toward the field; the two outer bays are bounded by the guardrail (perimeter) and the barrier respectively.

### 5.3 Charge station

- Rectangle: X[2.919, 4.858] (length 1.939 m), Y[1.508, 3.988] (width 2.480 m), center (3.889, 2.748).
- Toggleable via constructor flag, **default solid** (impassable), following the `rebuilt2026` pattern.
- Added via a dedicated isolated method `addChargeStationCollider(boolean solid)` so the future 3D charge-station layer can override or replace it without touching the rest of the map.

### 5.4 Barrier

- One per alliance, separating the community from the opposing alliance's loading zone.
- Rectangle: X[0, 2.236] (length 7 ft 4 in = 2.236 m), centered on Y ≈ 5.499 (loading-zone `rightY`), width 0.41 m.

### 5.5 Double substation protrusion

- One per alliance, attached to and in-line with the opponent's alliance wall, extending 14 in = 0.356 m into the field.
- Blue's double substation sits at the red end; the red double substation (the one physically present at the blue end) is the flip of the blue definition.
- Rectangle: length (X) 0.356 m, width (Y) ≈ 2.44 m (8 ft), center Y = `fieldWidth - 49.76 in` = 6.750 m (blue-frame value, flipped for placement at the appropriate end).

### 5.6 Intentionally non-collider elements

| Element | Reason |
|---|---|
| Cable bump / cable protector (`cableBumpCorners`, ~3.8 m, 7 in wide, 19–22 mm tall) | Robots roll over it; below any meaningful 2D collider. |
| Single substation | Field-facing wall recessed 3 in behind the guardrail; no protrusion. Used only as a piece-feed point. |
| Vision targets, AprilTags, reflective tape, LED strings, charge-station lighting | Non-physical. |

---

## 6. Game pieces

### 6.1 On-field bodies

Both modeled with **square** bases (per design decision), as `GamePieceOnFieldSimulation` subclasses with a `GamePieceInfo`:

| Piece | Type string | Footprint (square) | Height | Mass | Notes |
|---|---|---|---|---|---|
| Cone | `"Cone"` | ~0.20 m (8 in base) | ~0.33 m | 0.653 kg | yellow marker cone |
| Cube | `"Cube"` | ~0.24 m | ~0.24 m | 0.071 kg | purple inflated cube |

Damping and restitution follow the conventions used by existing on-field pieces (tune for plausible sliding/settling). The cube's lighter mass and softness can be approximated with higher linear damping.

### 6.2 Projectiles (scoring mechanism)

`ChargedUpConeOnFly` and `ChargedUpCubeOnFly` extend `GamePieceProjectile`. Robot code ejects a piece toward a node as a projectile (same usage pattern as `ReefscapeCoralOnFly`). The projectile's 3D pose is what the grid `Goal`s test against. Provide convenience constructors mirroring the existing OnFly classes (launch position on robot, robot pose, chassis speeds, launch height, launch velocity, launch angle).

### 6.3 Placement (`placeGamePiecesOnField`)

Per alliance, 4 staged pieces at the staging marks:

- X = `fieldLength/2 - 47.36 in` = **7.068 m**.
- Y = {0.919, 2.138, 3.357, 4.576} m (firstY 36.19 in, separation 48 in).
- Pattern (Y ascending): cube, cone, cone, cube.

Mirrored for the red alliance via `FieldMirroringUtils.flip`.

---

## 7. Grid scoring (height-aware, `Goal`-based)

### 7.1 Node model

`ChargedUpNode extends Goal`. One node per scoring location: 9 columns × 3 rows = **27 nodes per alliance**, 54 total.

For each column i (0..8), `isCube = (i == 1 || i == 4 || i == 7)`:

| Row | Type | Node X (m) | Node Z (m) | `allowGrounded` | Capacity |
|---|---|---|---|---|---|
| Hybrid (low) | accepts Cone or Cube | 1.197 | 0.0 | true | 1 |
| Mid | cube columns: Cube; else Cone | 0.800 | cube 0.521 / cone 0.864 | false | 1 |
| High | cube columns: Cube; else Cone | 0.368 | cube 0.826 / cone 1.168 | false | 1 |

- The `Goal` 3D box (`xyBox` + `minZ`/`maxZ`) is positioned at each node's `(x, y, z)`. A piece scores when its `getPose3d()` falls inside the box (and passes type + rotation + grounded checks), exactly as `ReefscapeReefBranch` does.
- **Type filtering:** cone nodes filter `"Cone"`, cube nodes filter `"Cube"`. Hybrid nodes accept either type — implemented as two co-located `Goal`s (one Cone, one Cube) sharing the bay zone, or a node subclass that checks both types. (Decision deferred to the plan; two co-located goals is the lower-risk option since it reuses `Goal` unchanged.)
- **Rotation:** start with `anyRotation()` for all nodes. Leave a tunable hook (`setNeededAngle`) so cones-must-be-upright can be enabled later without API change.

### 7.2 Grid orchestrator

`ChargedUpGridSimulation implements SimulatedArena.Simulatable`, one per alliance, owns the node array. Responsibilities mirror `ReefscapeReefSimulation`:

- `simulationSubTick` → tick all nodes.
- `draw(List<Pose3d>)` → render scored pieces for AdvantageScope.
- Publish node poses to NetworkTables under `/SmartDashboard/MapleSim/Goals/{BlueGrid|RedGrid}`.
- Expose scored-state accessors (e.g. a per-row, per-column count array) analogous to `getBranches()`.

### 7.3 Scoring & LINKs

- Points (2023 manual, per scored game piece): low/hybrid = 3 (auto) / 2 (teleop); mid = 4 (auto) / 3 (teleop); high = 6 (auto) / 5 (teleop). Applied in each node's `addPoints()` via `arena.addToScore` and `arena.addValueToMatchBreakdown`, with the auto-vs-teleop branch on `DriverStation.isAutonomous()` (same pattern as `ReefscapeReefBranch`).
- **LINK:** 3 adjacent scored nodes in the same row. Detected in the grid orchestrator from the scored-state array each sub-tick; published to the match breakdown (e.g. `BlueLinks`, `RedLinks`).
- Reset hooks (`clearGrid`) called from `placeGamePiecesOnField` / field reset.

---

## 8. Build phases

Each phase compiles green and is independently testable.

1. **Foundation:** delete the three season packages; retarget the `SimulatedArena` default and fix the stray `overrideInstance` line; remove the `ReefscapeCoralAlgaeStack` handling from `IntakeSimulation`; update `FieldMirroringUtils` field dimensions; add a minimal stub `Arena2023ChargedUp` (empty obstacle map) so the project builds.
2. **Obstacle map:** implement `ChargedUpFieldObstaclesMap` — perimeter, grid back wall + 8 dividers, charge station (toggleable), barrier, double-substation protrusion. Verify robot collisions (enters bays, bumps dividers/charge station).
3. **Game pieces:** `ChargedUpConeOnField`, `ChargedUpCubeOnField`, `ChargedUpConeOnFly`, `ChargedUpCubeOnFly`; implement `placeGamePiecesOnField` staging; verify intake collection and pose publishing (`"Cone"` / `"Cube"`).
4. **Scoring:** `ChargedUpNode`, `ChargedUpGridSimulation`; node placement at real 3D heights; LINK detection; match-breakdown publishing; verify scoring via projectiles and grounded hybrid pieces.

---

## 9. Known limitations / future work

- **Charge station** is a static collider, not a balanceable ramp. A dedicated 3D layer is planned; `addChargeStationCollider` is the seam it will hook into.
- Grid scoring approximates placement with the projectile/3D-pose mechanism (same compromise as 2025). Real-arm placement is not physically simulated.
- Cone upright-orientation scoring is disabled by default (tunable hook left in place).
- Cooperation/sustainability bonus logic beyond LINK counting is deferred.
- WPI vs non-WPI field micro-variations (~cm, plus AprilTag layout differences) are not parameterized; the standard non-WPI dims are used to match WPILib's bundled field size.
