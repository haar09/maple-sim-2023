# 2023 Charged Up Port Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert maple-sim from the 2026 Rebuilt default to the 2023 FRC game CHARGED UP — field boundaries, grids as enterable bays with non-passable dividers, charge stations, cone/cube game pieces, and height-aware grid scoring.

**Architecture:** New `chargedup2023` season package mirroring the existing season packages. The three other season packages are deleted and the default arena is retargeted. Field obstacles are built from the `FieldMap` primitives; scoring reuses the existing `Goal` 3D-box abstraction exactly as 2025 Reefscape does. No core-engine changes beyond the default-instance swap, the `IntakeSimulation` stack-handling removal, and the `FieldMirroringUtils` field-dimension update.

**Tech Stack:** Java 17, WPILib 2025.3.2 units/geometry, dyn4j 5.0.2, Gradle 8.11, Spotless (Palantir Java Format, auto-applied on compile).

**Spec:** `docs/superpowers/specs/2026-06-21-chargedup2023-port-design.md`

## Global Constraints

- **Geometry source:** FRC 6328 `FieldConstants` (2023), standard non-WPI variant. All numeric values below are pre-computed meters (`inches × 0.0254`) and must be used verbatim.
- **Field dimensions:** length (X) = 16.54175 m, width (Y) = 8.0137 m. Origin = blue alliance wall, X into field, Y to the left (NWU). Red = 180° flip `(x,y) -> (L-x, W-y)` via `FieldMirroringUtils.flip`.
- **Code style:** descriptive names, no Hungarian notation, never-nester (early returns), WPILib units in public APIs, SI + unit-suffixed names internally, JavaDoc on all public classes/methods with `<h2>` title. Files under 600 lines. Spotless auto-formats on compile — do not hand-format.
- **No Java test framework exists in this repo** (only the C++ `google-test` plugin). Per-task verification = `./gradlew spotlessApply compileJava` returns `BUILD SUCCESSFUL`, plus visual check of coordinates against the tables in this plan. The full runtime sim smoke-test is performed by the user in a WPILib robot-project environment, not in this repo.
- **Build/verify is run by the user in the WPILib environment.** Do not assume a local gradle run succeeded; mark verification steps complete only after the user confirms.
- **Commit after every task.** End commit bodies with the `Co-Authored-By` trailer.

---

## File Structure

**Created (new `chargedup2023` package):**
- `Arena2023ChargedUp.java` — arena + inner `ChargedUpFieldObstaclesMap`, wiring, placement, scoring orchestration hooks.
- `ChargedUpConeOnField.java` — cone on-field body + shared `GamePieceInfo`.
- `ChargedUpCubeOnField.java` — cube on-field body + shared `GamePieceInfo`.
- `ChargedUpConeOnFly.java` — cone projectile.
- `ChargedUpCubeOnFly.java` — cube projectile.
- `ChargedUpNode.java` — single scoring location (`extends Goal`).
- `ChargedUpGridSimulation.java` — per-alliance grid: builds nodes, draws, publishes, LINK detection.

**Modified:**
- `SimulatedArena.java` — default instance + stray `overrideInstance` line.
- `IntakeSimulation.java` — remove `ReefscapeCoralAlgaeStack` import + two `instanceof` branches.
- `FieldMirroringUtils.java` — field dimensions.
- `CLAUDE.md` — season section.

**Deleted:**
- `seasonspecific/crescendo2024/`, `seasonspecific/reefscape2025/`, `seasonspecific/rebuilt2026/` (all files).

**Kept:** `seasonspecific/evergreen/`.

---

## Pre-computed constants (use verbatim)

Base path for all new files: `project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/`

**Grid node X by row (m):** low/hybrid `1.196975`, mid `0.800100`, high `0.368300`
**Grid node Y by column i=0..8 (m):** `0.512826, 1.071626, 1.630426, 2.189226, 2.748026, 3.306826, 3.865626, 4.424426, 4.983226`
**Cube columns:** i ∈ {1, 4, 7}. **Cone columns:** i ∈ {0, 2, 3, 5, 6, 8}.
**Node Z (m):** hybrid `0.0`; mid cone `0.863600`, mid cube `0.520700`; high cone `1.168400`, high cube `0.825500`
**Grid depth (X):** `1.377950`
**Divider Y midpoints (m):** `0.792226, 1.351026, 1.909826, 2.468626, 3.027426, 3.586226, 4.145026, 4.703826`
**Charge station:** center `(3.888625, 2.748030)`, size X `1.938275` × Y `2.479044`
**Barrier:** center `(1.117600, 5.499100)`, size X `2.235200` × Y `0.406400`
**Double substation (blue's, at red end):** center `(16.363950, 6.749796)`, size X `0.355600` × Y `2.438400`
**Staging (blue):** X `7.067931`; Y `0.919226, 2.138426, 3.357626, 4.576826`; types by Y-index = cube, cone, cone, cube

---

# Phase 1 — Foundation (green build on 2023)

### Task 1: Add cone & cube on-field game pieces

**Files:**
- Create: `.../chargedup2023/ChargedUpConeOnField.java`
- Create: `.../chargedup2023/ChargedUpCubeOnField.java`

**Interfaces:**
- Consumes: `GamePieceOnFieldSimulation(GamePieceInfo, Pose2d)`; `GamePieceOnFieldSimulation.GamePieceInfo(String type, Convex shape, Distance height, Mass mass, double linearDamping, double angularDamping, double coefficientOfRestitution)`.
- Produces: `ChargedUpConeOnField.CHARGED_UP_CONE_INFO`, `ChargedUpCubeOnField.CHARGED_UP_CUBE_INFO` (public static `GamePieceInfo`); constructors `ChargedUpConeOnField(Pose2d)`, `ChargedUpCubeOnField(Pose2d)`.

- [ ] **Step 1: Create `ChargedUpConeOnField.java`**

```java
package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.math.geometry.Pose2d;
import org.dyn4j.geometry.Rectangle;
import org.ironmaple.simulation.gamepieces.GamePieceOnFieldSimulation;

/**
 *
 *
 * <h1>Represents a CONE in the 2023 Charged Up game.</h1>
 *
 * <p>A CONE is a yellow rubber marker cone ~13 in (~33 cm) tall with an 8 in (~20 cm) square base, weighing ~653 g.
 * Modeled with a square footprint matching the base.
 */
public class ChargedUpConeOnField extends GamePieceOnFieldSimulation {
    public static final GamePieceInfo CHARGED_UP_CONE_INFO =
            new GamePieceInfo("Cone", new Rectangle(0.203, 0.203), Meters.of(0.33), Kilograms.of(0.653), 2.0, 4.0, 0.2);

    public ChargedUpConeOnField(Pose2d initialPose) {
        super(CHARGED_UP_CONE_INFO, initialPose);
    }
}
```

- [ ] **Step 2: Create `ChargedUpCubeOnField.java`**

```java
package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.math.geometry.Pose2d;
import org.dyn4j.geometry.Rectangle;
import org.ironmaple.simulation.gamepieces.GamePieceOnFieldSimulation;

/**
 *
 *
 * <h1>Represents a CUBE in the 2023 Charged Up game.</h1>
 *
 * <p>A CUBE is a purple inflated PVC-fabric cube ~9.5 in (~24 cm) per side, weighing ~71 g. Soft and light; modeled
 * with a square footprint and high damping to approximate its low-energy behavior.
 */
public class ChargedUpCubeOnField extends GamePieceOnFieldSimulation {
    public static final GamePieceInfo CHARGED_UP_CUBE_INFO =
            new GamePieceInfo("Cube", new Rectangle(0.241, 0.241), Meters.of(0.241), Kilograms.of(0.071), 3.0, 5.0, 0.1);

    public ChargedUpCubeOnField(Pose2d initialPose) {
        super(CHARGED_UP_CUBE_INFO, initialPose);
    }
}
```

- [ ] **Step 3: Verify (user, WPILib env)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/ChargedUpConeOnField.java \
        project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/ChargedUpCubeOnField.java
git commit -m "feat: add 2023 cone and cube on-field game pieces"
```

---

### Task 2: Add stub arena with perimeter-only obstacle map

Creates a compilable `Arena2023ChargedUp` so the default can be retargeted in Task 3. Obstacle map starts as perimeter-only; filled out in Task 4.

**Files:**
- Create: `.../chargedup2023/Arena2023ChargedUp.java`

**Interfaces:**
- Consumes: `SimulatedArena(FieldMap)`; `FieldMap.addBorderLine(Translation2d, Translation2d)`; `SimulatedArena.placeGamePiecesOnField()` (abstract, must override); `addGamePiece`, `setupValueForMatchBreakdown`.
- Produces: `Arena2023ChargedUp()` no-arg constructor; `Arena2023ChargedUp(boolean chargeStationSolid)`; inner `public static final class ChargedUpFieldObstaclesMap extends FieldMap`. Field length/width constants `FIELD_LENGTH = 16.54175`, `FIELD_WIDTH = 8.0137`.

- [ ] **Step 1: Create `Arena2023ChargedUp.java` (stub)**

```java
package org.ironmaple.simulation.seasonspecific.chargedup2023;

import edu.wpi.first.math.geometry.Translation2d;
import org.ironmaple.simulation.SimulatedArena;

/**
 *
 *
 * <h2>The Simulation Arena for the 2023 FRC Game: CHARGED UP.</h2>
 *
 * <p>Models the 2023 field: perimeter, grids (enterable bays with non-passable dividers), charge stations, barriers,
 * and double-substation protrusions. The charge station is a static collider; balance/dock simulation is intentionally
 * out of scope (a dedicated 3D layer is planned).
 */
public class Arena2023ChargedUp extends SimulatedArena {
    public static final double FIELD_LENGTH = 16.54175;
    public static final double FIELD_WIDTH = 8.0137;

    /** The obstacles on the 2023 Charged Up competition field. */
    public static final class ChargedUpFieldObstaclesMap extends FieldMap {
        public ChargedUpFieldObstaclesMap(boolean chargeStationSolid) {
            super();

            // Perimeter
            addBorderLine(new Translation2d(0, 0), new Translation2d(FIELD_LENGTH, 0));
            addBorderLine(new Translation2d(0, FIELD_WIDTH), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));
            addBorderLine(new Translation2d(0, 0), new Translation2d(0, FIELD_WIDTH));
            addBorderLine(new Translation2d(FIELD_LENGTH, 0), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));
        }
    }

    /**
     *
     *
     * <h2>Creates the 2023 Charged Up arena with a solid charge-station collider.</h2>
     */
    public Arena2023ChargedUp() {
        this(true);
    }

    /**
     *
     *
     * <h2>Creates the 2023 Charged Up arena.</h2>
     *
     * @param chargeStationSolid whether the charge-station footprint is an impassable collider
     */
    public Arena2023ChargedUp(boolean chargeStationSolid) {
        super(new ChargedUpFieldObstaclesMap(chargeStationSolid));
    }

    @Override
    public void placeGamePiecesOnField() {}
}
```

- [ ] **Step 2: Verify (user)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/Arena2023ChargedUp.java
git commit -m "feat: add stub Arena2023ChargedUp with perimeter obstacle map"
```

---

### Task 3: Retarget default, update field dims, delete old seasons, fix intake

Big-bang switch to 2023-only. Order within the task matters: edit `FieldMirroringUtils` and `SimulatedArena`/`IntakeSimulation` to stop referencing the old packages, THEN delete the packages, so the tree is never left referencing deleted code at the end of the task.

**Files:**
- Modify: `.../utils/FieldMirroringUtils.java:12-13`
- Modify: `.../simulation/SimulatedArena.java` (`getInstance` ~111, `overrideInstance` ~130)
- Modify: `.../simulation/IntakeSimulation.java` (import ~24, branches ~318-325)
- Modify: `CLAUDE.md`
- Delete: `.../seasonspecific/crescendo2024/`, `.../seasonspecific/reefscape2025/`, `.../seasonspecific/rebuilt2026/`

**Interfaces:**
- Consumes: `Arena2023ChargedUp()` from Task 2.
- Produces: `SimulatedArena.getInstance()` returns an `Arena2023ChargedUp`; `FieldMirroringUtils.FIELD_WIDTH = 16.54175`, `FieldMirroringUtils.FIELD_HEIGHT = 8.0137`.

- [ ] **Step 1: Update `FieldMirroringUtils` field dimensions**

Replace lines 12-13:

```java
    public static final double FIELD_WIDTH = 16.54175;
    public static final double FIELD_HEIGHT = 8.0137;
```

(`FIELD_WIDTH` is the field length/X axis, `FIELD_HEIGHT` the Y axis, per existing naming. `flip()` stays 180° rotational — unchanged.)

- [ ] **Step 2: Retarget the default instance in `SimulatedArena.getInstance()`**

Replace the line:

```java
        if (instance == null) instance = new org.ironmaple.simulation.seasonspecific.rebuilt2026.Arena2026Rebuilt();
```

with:

```java
        if (instance == null)
            instance = new org.ironmaple.simulation.seasonspecific.chargedup2023.Arena2023ChargedUp();
```

- [ ] **Step 3: Remove the stray line in `SimulatedArena.overrideInstance()`**

Delete this line (it references the deleted class and is a latent bug — it overwrites the incoming instance with a default before the real assignment):

```java
        if (instance != null) instance = new Arena2026Rebuilt();
```

The method body becomes just:

```java
    public static void overrideInstance(SimulatedArena newInstance) {
        instance = newInstance;
    }
```

- [ ] **Step 4: Remove `ReefscapeCoralAlgaeStack` from `IntakeSimulation`**

Delete the import (line ~24):

```java
import org.ironmaple.simulation.seasonspecific.reefscape2025.ReefscapeCoralAlgaeStack;
```

Delete the stack-handling block (lines ~318-325), leaving only the two general `GamePieceOnFieldSimulation` branches above it:

```java
            boolean coralOrAlgaeIntake = "Coral".equals(IntakeSimulation.this.targetedGamePieceType)
                    || "Algae".equals(IntakeSimulation.this.targetedGamePieceType);
            if (collisionBody1 instanceof ReefscapeCoralAlgaeStack stack
                    && coralOrAlgaeIntake
                    && fixture2 == IntakeSimulation.this) flagGamePieceForRemoval(stack);
            else if (collisionBody2 instanceof ReefscapeCoralAlgaeStack stack
                    && coralOrAlgaeIntake
                    && fixture1 == IntakeSimulation.this) flagGamePieceForRemoval(stack);
```

- [ ] **Step 5: Delete the three season packages**

```bash
git rm -r project/src/main/java/org/ironmaple/simulation/seasonspecific/crescendo2024 \
          project/src/main/java/org/ironmaple/simulation/seasonspecific/reefscape2025 \
          project/src/main/java/org/ironmaple/simulation/seasonspecific/rebuilt2026
```

- [ ] **Step 6: Update `CLAUDE.md` season section** — replace the `crescendo2024` / `reefscape2025` / `rebuilt2026` package listings and the "Default instance: `Arena2025Reefscape`" / rebuilt references with a single `chargedup2023` section noting it is the current default. (Documentation edit; keep it brief and accurate.)

- [ ] **Step 7: Verify (user)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`, no references to deleted packages.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: switch default arena to 2023 Charged Up, remove other seasons"
```

---

# Phase 2 — Obstacle map

### Task 4: Implement the full Charged Up obstacle map

Fills in `ChargedUpFieldObstaclesMap`: grid bays + dividers, charge station (toggleable), barrier, double substation — blue side plus the 180° flip for red.

**Files:**
- Modify: `.../chargedup2023/Arena2023ChargedUp.java` (inner `ChargedUpFieldObstaclesMap`)

**Interfaces:**
- Consumes: `FieldMap.addBorderLine`, `FieldMap.addRectangularObstacle(double width, double height, Pose2d)`; `FieldMirroringUtils.flip(Translation2d)`.
- Produces: a fully populated obstacle map; no new public API.

- [ ] **Step 1: Replace the `ChargedUpFieldObstaclesMap` constructor body**

```java
        public ChargedUpFieldObstaclesMap(boolean chargeStationSolid) {
            super();

            // Perimeter
            addBorderLine(new Translation2d(0, 0), new Translation2d(FIELD_LENGTH, 0));
            addBorderLine(new Translation2d(0, FIELD_WIDTH), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));
            addBorderLine(new Translation2d(0, 0), new Translation2d(0, FIELD_WIDTH));
            addBorderLine(new Translation2d(FIELD_LENGTH, 0), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));

            addBlueObstacles();
            addRedObstacles();
        }

        private static final double GRID_DEPTH_X = 1.377950;
        private static final double[] DIVIDER_Y = {
            0.792226, 1.351026, 1.909826, 2.468626, 3.027426, 3.586226, 4.145026, 4.703826
        };
        private static final double DIVIDER_THICKNESS_Y = 0.127000;

        private static final double CHARGE_STATION_CENTER_X = 3.888625;
        private static final double CHARGE_STATION_CENTER_Y = 2.748030;
        private static final double CHARGE_STATION_SIZE_X = 1.938275;
        private static final double CHARGE_STATION_SIZE_Y = 2.479044;

        private static final double BARRIER_CENTER_X = 1.117600;
        private static final double BARRIER_CENTER_Y = 5.499100;
        private static final double BARRIER_SIZE_X = 2.235200;
        private static final double BARRIER_SIZE_Y = 0.406400;

        private static final double DOUBLE_SUBSTATION_CENTER_X = 16.363950;
        private static final double DOUBLE_SUBSTATION_CENTER_Y = 6.749796;
        private static final double DOUBLE_SUBSTATION_SIZE_X = 0.355600;
        private static final double DOUBLE_SUBSTATION_SIZE_Y = 2.438400;

        private final boolean chargeStationSolid;

        private void addBlueObstacles() {
            addGridDividers(false);
            addChargeStationCollider(false);
            addRectangularObstacle(
                    BARRIER_SIZE_X, BARRIER_SIZE_Y, new Pose2d(BARRIER_CENTER_X, BARRIER_CENTER_Y, new Rotation2d()));
            // Red's double substation physically sits at the blue end (flip of blue's definition).
            Translation2d redDsCenter =
                    FieldMirroringUtils.flip(new Translation2d(DOUBLE_SUBSTATION_CENTER_X, DOUBLE_SUBSTATION_CENTER_Y));
            addRectangularObstacle(
                    DOUBLE_SUBSTATION_SIZE_X,
                    DOUBLE_SUBSTATION_SIZE_Y,
                    new Pose2d(redDsCenter, new Rotation2d()));
        }

        private void addRedObstacles() {
            addGridDividers(true);
            addChargeStationCollider(true);
            Translation2d barrierCenter =
                    FieldMirroringUtils.flip(new Translation2d(BARRIER_CENTER_X, BARRIER_CENTER_Y));
            addRectangularObstacle(BARRIER_SIZE_X, BARRIER_SIZE_Y, new Pose2d(barrierCenter, new Rotation2d()));
            // Blue's double substation physically sits at the red end (un-flipped blue definition).
            addRectangularObstacle(
                    DOUBLE_SUBSTATION_SIZE_X,
                    DOUBLE_SUBSTATION_SIZE_Y,
                    new Pose2d(DOUBLE_SUBSTATION_CENTER_X, DOUBLE_SUBSTATION_CENTER_Y, new Rotation2d()));
        }

        private void addGridDividers(boolean isRed) {
            for (double dividerY : DIVIDER_Y) {
                Translation2d center = new Translation2d(GRID_DEPTH_X / 2.0, dividerY);
                if (isRed) center = FieldMirroringUtils.flip(center);
                addRectangularObstacle(
                        GRID_DEPTH_X, DIVIDER_THICKNESS_Y, new Pose2d(center, new Rotation2d()));
            }
        }

        /**
         * Adds the charge-station collider. Isolated so the future 3D charge-station layer can override or replace it
         * without touching the rest of the obstacle map.
         */
        private void addChargeStationCollider(boolean isRed) {
            if (!chargeStationSolid) return;
            Translation2d center = new Translation2d(CHARGE_STATION_CENTER_X, CHARGE_STATION_CENTER_Y);
            if (isRed) center = FieldMirroringUtils.flip(center);
            addRectangularObstacle(
                    CHARGE_STATION_SIZE_X, CHARGE_STATION_SIZE_Y, new Pose2d(center, new Rotation2d()));
        }
```

Set the `chargeStationSolid` field in the constructor (add `this.chargeStationSolid = chargeStationSolid;` right after `super();`). Add imports: `edu.wpi.first.math.geometry.Pose2d`, `edu.wpi.first.math.geometry.Rotation2d`, `org.ironmaple.utils.FieldMirroringUtils`.

> Note on field naming: `FieldMirroringUtils.flip(Translation2d)` uses `FIELD_WIDTH`/`FIELD_HEIGHT` (set to 16.54175/8.0137 in Task 3), so the flip is consistent with `Arena2023ChargedUp.FIELD_LENGTH`/`FIELD_WIDTH`.

- [ ] **Step 2: Verify coordinates (user/reviewer)** — confirm against the constants table: 8 dividers at the listed Y, full grid depth in X; charge station center (3.89, 2.75); barrier at Y≈5.50; double substations at both ends.

- [ ] **Step 3: Verify build (user)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/Arena2023ChargedUp.java
git commit -m "feat: implement full 2023 field obstacle map (grids, charge station, barriers, substations)"
```

---

# Phase 3 — Game pieces in flight & placement

### Task 5: Add cone & cube projectiles

**Files:**
- Create: `.../chargedup2023/ChargedUpConeOnFly.java`
- Create: `.../chargedup2023/ChargedUpCubeOnFly.java`

**Interfaces:**
- Consumes: `GamePieceProjectile(GamePieceInfo, Translation2d robotPosition, Translation2d shooterPositionOnRobot, ChassisSpeeds, Rotation2d shooterFacing, Distance initialHeight, LinearVelocity launchingSpeed, Angle shooterAngle)`; `enableBecomesGamePieceOnFieldAfterTouchGround()`, `withTouchGroundHeight(double)`; `addGamePieceAfterTouchGround(SimulatedArena)`; the `*_INFO` constants from Task 1.
- Produces: `ChargedUpConeOnFly(...)`, `ChargedUpCubeOnFly(...)` with the 7-arg launch constructor signature above.

- [ ] **Step 1: Create `ChargedUpConeOnFly.java`**

```java
package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.LinearVelocity;
import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.simulation.gamepieces.GamePieceOnFieldSimulation;
import org.ironmaple.simulation.gamepieces.GamePieceProjectile;

/**
 *
 *
 * <h1>Represents a CONE launched into the air in the 2023 Charged Up game.</h1>
 *
 * <p>Used as the scoring mechanism: a robot ejects a cone toward a grid node, and the node {@link
 * org.ironmaple.simulation.Goal} detects it by 3D pose.
 */
public class ChargedUpConeOnFly extends GamePieceProjectile {
    public ChargedUpConeOnFly(
            Translation2d robotPosition,
            Translation2d shooterPositionOnRobot,
            ChassisSpeeds chassisSpeeds,
            Rotation2d shooterFacing,
            Distance initialHeight,
            LinearVelocity launchingSpeed,
            Angle shooterAngle) {
        super(
                ChargedUpConeOnField.CHARGED_UP_CONE_INFO,
                robotPosition,
                shooterPositionOnRobot,
                chassisSpeeds,
                shooterFacing,
                initialHeight,
                launchingSpeed,
                shooterAngle);
        super.enableBecomesGamePieceOnFieldAfterTouchGround();
        super.withTouchGroundHeight(0.2);
    }

    @Override
    public void addGamePieceAfterTouchGround(SimulatedArena simulatedArena) {
        if (!super.becomesGamePieceOnGroundAfterTouchGround) return;
        simulatedArena.addGamePiece(new GamePieceOnFieldSimulation(
                ChargedUpConeOnField.CHARGED_UP_CONE_INFO,
                () -> Math.max(
                        ChargedUpConeOnField.CHARGED_UP_CONE_INFO.gamePieceHeight().in(Meters) / 2,
                        getPositionAtTime(super.launchedTimer.get()).getZ()),
                new Pose2d(
                        getPositionAtTime(launchedTimer.get()).toTranslation2d(),
                        initialLaunchingVelocityMPS.getAngle()),
                super.initialLaunchingVelocityMPS));
    }
}
```

- [ ] **Step 2: Create `ChargedUpCubeOnFly.java`** — identical structure, swapping `Cone` → `Cube` in class name, JavaDoc, and the two `CHARGED_UP_CUBE_INFO` references:

```java
package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.LinearVelocity;
import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.simulation.gamepieces.GamePieceOnFieldSimulation;
import org.ironmaple.simulation.gamepieces.GamePieceProjectile;

/**
 *
 *
 * <h1>Represents a CUBE launched into the air in the 2023 Charged Up game.</h1>
 *
 * <p>Used as the scoring mechanism: a robot ejects a cube toward a grid node, and the node {@link
 * org.ironmaple.simulation.Goal} detects it by 3D pose.
 */
public class ChargedUpCubeOnFly extends GamePieceProjectile {
    public ChargedUpCubeOnFly(
            Translation2d robotPosition,
            Translation2d shooterPositionOnRobot,
            ChassisSpeeds chassisSpeeds,
            Rotation2d shooterFacing,
            Distance initialHeight,
            LinearVelocity launchingSpeed,
            Angle shooterAngle) {
        super(
                ChargedUpCubeOnField.CHARGED_UP_CUBE_INFO,
                robotPosition,
                shooterPositionOnRobot,
                chassisSpeeds,
                shooterFacing,
                initialHeight,
                launchingSpeed,
                shooterAngle);
        super.enableBecomesGamePieceOnFieldAfterTouchGround();
        super.withTouchGroundHeight(0.2);
    }

    @Override
    public void addGamePieceAfterTouchGround(SimulatedArena simulatedArena) {
        if (!super.becomesGamePieceOnGroundAfterTouchGround) return;
        simulatedArena.addGamePiece(new GamePieceOnFieldSimulation(
                ChargedUpCubeOnField.CHARGED_UP_CUBE_INFO,
                () -> Math.max(
                        ChargedUpCubeOnField.CHARGED_UP_CUBE_INFO.gamePieceHeight().in(Meters) / 2,
                        getPositionAtTime(super.launchedTimer.get()).getZ()),
                new Pose2d(
                        getPositionAtTime(launchedTimer.get()).toTranslation2d(),
                        initialLaunchingVelocityMPS.getAngle()),
                super.initialLaunchingVelocityMPS));
    }
}
```

- [ ] **Step 3: Verify build (user)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/ChargedUpConeOnFly.java \
        project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/ChargedUpCubeOnFly.java
git commit -m "feat: add 2023 cone and cube projectiles"
```

---

### Task 6: Stage game pieces on the field

**Files:**
- Modify: `.../chargedup2023/Arena2023ChargedUp.java` (`placeGamePiecesOnField`)

**Interfaces:**
- Consumes: `addGamePiece(GamePieceOnFieldSimulation)`; `ChargedUpConeOnField(Pose2d)`, `ChargedUpCubeOnField(Pose2d)`; `FieldMirroringUtils.flip(Translation2d)`; `setupValueForMatchBreakdown(String)`.
- Produces: populated `placeGamePiecesOnField()` staging 4 pieces per alliance.

- [ ] **Step 1: Replace `placeGamePiecesOnField()`**

```java
    private static final double STAGING_X = 7.067931;
    private static final double[] STAGING_Y = {0.919226, 2.138426, 3.357626, 4.576826};
    // Per Y-index: true = cube, false = cone  (cube, cone, cone, cube)
    private static final boolean[] STAGING_IS_CUBE = {true, false, false, true};

    @Override
    public void placeGamePiecesOnField() {
        for (int i = 0; i < STAGING_Y.length; i++) {
            Translation2d bluePos = new Translation2d(STAGING_X, STAGING_Y[i]);
            Translation2d redPos = FieldMirroringUtils.flip(bluePos);
            if (STAGING_IS_CUBE[i]) {
                addGamePiece(new ChargedUpCubeOnField(new Pose2d(bluePos, new Rotation2d())));
                addGamePiece(new ChargedUpCubeOnField(new Pose2d(redPos, new Rotation2d())));
            } else {
                addGamePiece(new ChargedUpConeOnField(new Pose2d(bluePos, new Rotation2d())));
                addGamePiece(new ChargedUpConeOnField(new Pose2d(redPos, new Rotation2d())));
            }
        }

        setupValueForMatchBreakdown("BlueLinks");
        setupValueForMatchBreakdown("RedLinks");
        setupValueForMatchBreakdown("Auto/ConesScoredInAuto");
        setupValueForMatchBreakdown("Auto/CubesScoredInAuto");
    }
```

(Add imports for `Pose2d`, `Rotation2d`, `FieldMirroringUtils` if not already present from Task 4.)

- [ ] **Step 2: Verify build (user)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/Arena2023ChargedUp.java
git commit -m "feat: stage 2023 cones and cubes at field staging marks"
```

---

# Phase 4 — Grid scoring

### Task 7: Add the scoring node (`ChargedUpNode`)

**Files:**
- Create: `.../chargedup2023/ChargedUpNode.java`

**Interfaces:**
- Consumes: `Goal(SimulatedArena arena, Distance xDimension, Distance yDimension, Distance height, String gamePieceType, Translation3d position, boolean isBlue, int max, boolean allowGrounded)`; `Goal.addPoints()` (abstract), `Goal.draw(List<Pose3d>)` (abstract), `Goal.getGamePieceCount()`, `Goal.position`; `arena.addToScore(boolean,int)`, `arena.addValueToMatchBreakdown(boolean,String,int)`; `DriverStation.isAutonomous()`.
- Produces: `ChargedUpNode(Arena2023ChargedUp arena, boolean isBlue, int row, String type, Translation3d position, Distance boxXY, Distance boxHeight, boolean allowGrounded)`; constant int arrays for points; public `final int row`, `final String type`.

Row indices: `0 = hybrid`, `1 = mid`, `2 = high`.

- [ ] **Step 1: Create `ChargedUpNode.java`**

```java
package org.ironmaple.simulation.seasonspecific.chargedup2023;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.List;
import org.ironmaple.simulation.Goal;

/**
 *
 *
 * <h2>A single scoring NODE in a 2023 Charged Up GRID.</h2>
 *
 * <p>Each node is a height-aware {@link Goal}: a game piece scores when its 3D pose falls within the node's box. Hybrid
 * (floor) nodes accept grounded pieces; mid and high nodes require the piece to arrive in the air (as a projectile).
 */
public class ChargedUpNode extends Goal {
    public static final int ROW_HYBRID = 0;
    public static final int ROW_MID = 1;
    public static final int ROW_HIGH = 2;

    // Points per scored piece, indexed by row [hybrid, mid, high].
    private static final int[] AUTO_POINTS_BY_ROW = {3, 4, 6};
    private static final int[] TELEOP_POINTS_BY_ROW = {2, 3, 5};

    public final int row;
    public final String type;

    public ChargedUpNode(
            Arena2023ChargedUp arena,
            boolean isBlue,
            int row,
            String type,
            Translation3d position,
            Distance boxXY,
            Distance boxHeight,
            boolean allowGrounded) {
        super(arena, boxXY, boxXY, boxHeight, type, position, isBlue, 1, allowGrounded);
        this.row = row;
        this.type = type;
    }

    @Override
    protected void addPoints() {
        boolean isAuto = DriverStation.isAutonomous();
        int points = isAuto ? AUTO_POINTS_BY_ROW[row] : TELEOP_POINTS_BY_ROW[row];
        arena.addToScore(isBlue, points);
        if (isAuto) arena.addValueToMatchBreakdown(isBlue, "Auto/" + type + "sScoredInAuto", 1);
    }

    @Override
    public void draw(List<Pose3d> drawList) {
        if (gamePieceCount > 0) drawList.add(new Pose3d(position, new edu.wpi.first.math.geometry.Rotation3d()));
    }
}
```

> Note: `arena` (protected, type `SimulatedArena`) and `isBlue`, `position`, `gamePieceCount` are inherited from `Goal`. `addToScore`/`addValueToMatchBreakdown` are `SimulatedArena` methods, so they resolve on the inherited `arena` field.

- [ ] **Step 2: Verify build (user)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/ChargedUpNode.java
git commit -m "feat: add 2023 grid scoring node (height-aware Goal)"
```

---

### Task 8: Add the grid simulation and wire it into the arena

Builds 27 nodes per alliance (hybrid = co-located cone+cube nodes), ticks/draws them, detects LINKs, publishes breakdown, and is registered in the arena constructor with draw/clear hooks.

**Files:**
- Create: `.../chargedup2023/ChargedUpGridSimulation.java`
- Modify: `.../chargedup2023/Arena2023ChargedUp.java` (constructor, `getGamePiecesPosesByType`, `clearGamePieces`)

**Interfaces:**
- Consumes: `SimulatedArena.Simulatable` (`simulationSubTick(int)`); `ChargedUpNode`; `addCustomSimulation(Simulatable)`; `addValueToMatchBreakdown(boolean,String,int)`; `FieldMirroringUtils.flip(Translation2d)`; node constants table.
- Produces: `ChargedUpGridSimulation(Arena2023ChargedUp arena, boolean isBlue)`; `void draw(List<Pose3d> drawList, String type)`; `void clearGrid()`; `int countLinks()`.

- [ ] **Step 1: Create `ChargedUpGridSimulation.java`**

```java
package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.Meters;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.ArrayList;
import java.util.List;
import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.utils.FieldMirroringUtils;

/**
 *
 *
 * <h2>Simulates one alliance's set of GRIDS in the 2023 Charged Up game.</h2>
 *
 * <p>Owns the 27 scoring {@link ChargedUpNode}s (9 columns × 3 rows; hybrid columns are a co-located cone+cube pair),
 * ticks and draws them, and detects LINKs (3 adjacent scored nodes in a row).
 */
public class ChargedUpGridSimulation implements SimulatedArena.Simulatable {
    // Node X by row (m): hybrid, mid, high
    private static final double[] NODE_X = {1.196975, 0.800100, 0.368300};
    // Node Y by column 0..8 (m)
    private static final double[] NODE_Y = {
        0.512826, 1.071626, 1.630426, 2.189226, 2.748026, 3.306826, 3.865626, 4.424426, 4.983226
    };
    // Z by [row][isCube]; hybrid Z is 0 for both
    private static final double MID_CONE_Z = 0.863600, MID_CUBE_Z = 0.520700;
    private static final double HIGH_CONE_Z = 1.168400, HIGH_CUBE_Z = 0.825500;

    private final boolean isBlue;
    private final List<ChargedUpNode> nodes = new ArrayList<>();
    // [row 0..2][col 0..8] -> the cone-or-shared node used for occupancy/LINK checks
    private final ChargedUpNode[][] coneOrSharedByRowCol = new ChargedUpNode[3][9];

    public ChargedUpGridSimulation(Arena2023ChargedUp arena, boolean isBlue) {
        this.isBlue = isBlue;

        for (int col = 0; col < 9; col++) {
            boolean isCubeColumn = (col == 1 || col == 4 || col == 7);

            // Hybrid (floor): accepts both types -> two co-located nodes; cube node tracks occupancy.
            ChargedUpNode hybridCone = makeNode(arena, ChargedUpNode.ROW_HYBRID, "Cone", col, 0.0, true);
            ChargedUpNode hybridCube = makeNode(arena, ChargedUpNode.ROW_HYBRID, "Cube", col, 0.0, true);
            nodes.add(hybridCone);
            nodes.add(hybridCube);
            coneOrSharedByRowCol[0][col] = hybridCube; // either presence marks the bay; cube chosen arbitrarily

            // Mid
            String midType = isCubeColumn ? "Cube" : "Cone";
            double midZ = isCubeColumn ? MID_CUBE_Z : MID_CONE_Z;
            ChargedUpNode mid = makeNode(arena, ChargedUpNode.ROW_MID, midType, col, midZ, false);
            nodes.add(mid);
            coneOrSharedByRowCol[1][col] = mid;

            // High
            String highType = isCubeColumn ? "Cube" : "Cone";
            double highZ = isCubeColumn ? HIGH_CUBE_Z : HIGH_CONE_Z;
            ChargedUpNode high = makeNode(arena, ChargedUpNode.ROW_HIGH, highType, col, highZ, false);
            nodes.add(high);
            coneOrSharedByRowCol[2][col] = high;
        }
    }

    private ChargedUpNode makeNode(
            Arena2023ChargedUp arena, int row, String type, int col, double z, boolean allowGrounded) {
        Translation2d xy = new Translation2d(NODE_X[row], NODE_Y[col]);
        if (!isBlue) xy = FieldMirroringUtils.flip(xy);
        Translation3d position = new Translation3d(xy.getX(), xy.getY(), z);
        double boxXY = row == ChargedUpNode.ROW_HYBRID ? 0.5 : 0.3;
        return new ChargedUpNode(arena, isBlue, row, type, position, Meters.of(boxXY), Meters.of(0.3), allowGrounded);
    }

    @Override
    public void simulationSubTick(int subTickNum) {
        for (ChargedUpNode node : nodes) node.simulationSubTick(subTickNum);
    }

    public void draw(List<Pose3d> drawList, String type) {
        for (ChargedUpNode node : nodes) {
            if (node.type.equals(type)) node.draw(drawList);
        }
    }

    public void clearGrid() {
        for (ChargedUpNode node : nodes) node.clear();
    }

    /**
     *
     *
     * <h2>Counts LINKs for this alliance.</h2>
     *
     * <p>A LINK is 3 adjacent scored nodes in a single row. Counted greedily left-to-right with non-overlapping groups
     * (up to 3 per row).
     *
     * @return the total number of LINKs across all three rows
     */
    public int countLinks() {
        int links = 0;
        for (int row = 0; row < 3; row++) {
            int run = 0;
            for (int col = 0; col < 9; col++) {
                if (coneOrSharedByRowCol[row][col].getGamePieceCount() > 0) {
                    run++;
                    if (run == 3) {
                        links++;
                        run = 0;
                    }
                } else {
                    run = 0;
                }
            }
        }
        return links;
    }
}
```

> Hybrid occupancy uses only the cube sub-node for LINK purposes; this is an acknowledged approximation (a hybrid bay holding only a cone won't count toward a LINK). If exact hybrid LINKs are needed later, replace the occupancy check with `hybridCone.getGamePieceCount() > 0 || hybridCube.getGamePieceCount() > 0` by storing both. Documented in the spec's limitations.

- [ ] **Step 2: Wire the grids into `Arena2023ChargedUp`**

Add fields and construct in the constructor:

```java
    public final ChargedUpGridSimulation blueGrid;
    public final ChargedUpGridSimulation redGrid;
```

In the `Arena2023ChargedUp(boolean chargeStationSolid)` constructor, after `super(...)`:

```java
        blueGrid = new ChargedUpGridSimulation(this, true);
        addCustomSimulation(blueGrid);
        redGrid = new ChargedUpGridSimulation(this, false);
        addCustomSimulation(redGrid);
```

- [ ] **Step 3: Override `getGamePiecesPosesByType` to draw scored pieces and publish LINKs**

```java
    @Override
    public synchronized List<Pose3d> getGamePiecesPosesByType(String type) {
        List<Pose3d> poses = super.getGamePiecesPosesByType(type);
        blueGrid.draw(poses, type);
        redGrid.draw(poses, type);
        return poses;
    }
```

Add imports `java.util.List`, `edu.wpi.first.math.geometry.Pose3d`.

- [ ] **Step 4: Override `clearGamePieces` and publish LINK counts each sub-tick**

```java
    @Override
    public synchronized void clearGamePieces() {
        super.clearGamePieces();
        blueGrid.clearGrid();
        redGrid.clearGrid();
    }

    @Override
    public void simulationSubTick(int tickNum) {
        super.simulationSubTick(tickNum);
        // LINK counts are current-state, not accumulated -> use the set-style replace method (additive
        // addValueToMatchBreakdown would double-count across sub-ticks).
        replaceValueInMatchBreakDown(true, "BlueLinks", blueGrid.countLinks());
        replaceValueInMatchBreakDown(false, "RedLinks", redGrid.countLinks());
    }
```

> Verified against `SimulatedArena`: `addValueToMatchBreakdown` accumulates (`put(key, old + toAdd)`), while `replaceValueInMatchBreakDown(boolean, String, Integer)` sets the value outright — correct for a live LINK count. `BlueLinks`/`RedLinks` are still pre-registered in `placeGamePiecesOnField` via `setupValueForMatchBreakdown` (Task 6) so they display as 0 before first update.

- [ ] **Step 5: Verify build (user)** — `./gradlew spotlessApply compileJava` → `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/ChargedUpGridSimulation.java \
        project/src/main/java/org/ironmaple/simulation/seasonspecific/chargedup2023/Arena2023ChargedUp.java
git commit -m "feat: add 2023 grid scoring simulation with LINK detection"
```

---

# Phase 5 — Integration verification (user, WPILib env)

### Task 9: End-to-end sim smoke test

**Files:** none (verification only).

- [ ] **Step 1:** In a WPILib robot project, depend on the locally-published maple-sim (`./gradlew publishToMavenLocal` with a test version), and confirm `SimulatedArena.getInstance()` returns `Arena2023ChargedUp`.
- [ ] **Step 2:** Reset the field (`resetFieldForAuto()`) and confirm 4 cones/cubes per alliance appear at the staging marks; visualize `getGamePiecesPosesByType("Cone")` and `"Cube"` in AdvantageScope against the Charged Up field image.
- [ ] **Step 3:** Drive a simulated swerve robot into a grid bay; confirm it can nose in but is blocked sideways by dividers, and is blocked by the charge station and barriers.
- [ ] **Step 4:** Launch a `ChargedUpConeOnFly` / `ChargedUpCubeOnFly` at a node; confirm it scores (piece removed, score increases, node renders the held piece), and that 3 adjacent scored nodes register a LINK.
- [ ] **Step 5:** Report results back; fix any coordinate/scoring discrepancies as follow-up tasks.

---

## Self-Review

**Spec coverage:**
- Packages add/delete/keep → Tasks 2, 3. Core wiring (default, intake, field dims) → Task 3. Field obstacle map (perimeter, grid bays+dividers, charge station toggle, barrier, double substation) → Tasks 2, 4. Cone/Cube OnField → Task 1; OnFly → Task 5. Staging → Task 6. Height-aware Goal nodes → Task 7. Grid sim + LINKs + breakdown + draw/clear → Task 8. Charge-station 3D seam (`addChargeStationCollider`) → Task 4. Limitations (hybrid both-types, cone rotation default) → captured in Tasks 7/8 notes and spec. All spec sections map to a task.
- Skipped colliders (cable bump, single substation) → correctly absent from the obstacle map (Task 4), matching spec.

**Placeholder scan:** No "TBD"/"TODO"/"implement later". Two implementation notes (Task 8 Step 4 breakdown semantics; Task 8 hybrid-LINK approximation) flag real decisions with concrete fallback code, not vague placeholders.

**Type consistency:** `GamePieceInfo` 7-arg constructor consistent (Tasks 1, 5). `Goal` 9-arg constructor matches `ChargedUpNode` super call (Task 7). `ChargedUpNode` constructor signature matches `makeNode` call site (Tasks 7, 8). `*_INFO` constant names (`CHARGED_UP_CONE_INFO`, `CHARGED_UP_CUBE_INFO`) consistent across Tasks 1/5/(staging 6 uses the OnField classes). `FieldMirroringUtils.flip` used consistently. `countLinks`/`clearGrid`/`draw(List,String)` defined in Task 8 and used in the same task's arena wiring.

**Risks flagged for the implementer:** exact line numbers in `IntakeSimulation`/`SimulatedArena` may drift — match by content, not line number. (The match-breakdown additive-vs-set risk is resolved: Task 8 uses `replaceValueInMatchBreakDown` for LINKs, verified against the source.)
