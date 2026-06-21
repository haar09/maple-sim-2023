# 2023 Charged Up Port — Sim Test Project

This is the bundled CTRE-Swerve-MapleSim template, repointed at the **local** 2023 Charged Up
maple-sim build (`org.haar09:maplesim2023-java:2023.0.0` from `~/.m2`) and given test controls to
exercise the new arena, game pieces, and grid scoring.

## What was changed from the stock template
- `vendordeps/maple-sim.json` → depends on `org.haar09:maplesim2023-java:2023.0.0` (resolved via `mavenLocal()`).
- Removed PathPlanner + DogLog vendordeps and their usages (not needed; not cached in this env).
- `Robot.java` publishes `Simulation/ConePoses` and `Simulation/CubePoses` (`Pose3d[]` structs),
  `Simulation/DrivetrainPose` (`Pose2d` — the maple-sim **physics** pose that actually collides), plus
  `Simulation/BlueScore` / `Simulation/RedScore` each sim tick.
- Start pose moved to midfield `(8.27, 4.0)`. The stock `(3, 3)` sits **inside** the blue charge-station
  collider, which makes the robot get ejected and the colliders look broken.
- `RobotContainer.java` adds the test controls below.

## Prerequisite (once)
Publish the library to maven-local from the repo root:
```
cd project && ./gradlew publishToMavenLocal
```

## Run the simulation
```
cd templates/CTRE-Swerve-MapleSim
./gradlew simulateJava
```
Then open AdvantageScope (or the SimGUI) and:
- Show the robot using `NT:/Simulation/DrivetrainPose` (the physics pose). Do **not** judge collisions
  by CTRE odometry — odometry integrates wheel motion and walks through walls; the physics body is what
  actually collides.
- Add field poses from `NT:/Simulation/ConePoses` and `/Simulation/CubePoses`.
- Watch `/SmartDashboard/Simulation/BlueScore`, `/RedScore`, and the arena's own
  `/SmartDashboard/MapleSim/MatchData/Breakdown/.../BlueLinks` / `RedLinks`.
- Enable Teleop. Drive with an Xbox controller on port 0.

## Test controls (Xbox, port 0)
| Control | Action |
|---|---|
| Left stick / right stick | Field-centric drive / rotate |
| Left bumper | Re-seed field-centric heading |
| **Right bumper** | Drop a CUBE onto a blue hybrid node (col 4) — deterministic grounded-scoring test; BlueScore should jump +2 (teleop) |
| **Right trigger** | Launch a CUBE projectile from the robot (6 m/s, 60°) — aim at a grid node to score |
| **Left trigger** | Launch a CONE projectile from the robot |
| **POV right** | Reset the field (re-stage pieces, clear scored grids) |

## What to verify (Task 9 checklist)
1. On enable/reset: 4 cones/cubes per alliance appear at the staging marks (cube-cone-cone-cube).
2. Drive into a grid: robot noses into a bay but is blocked sideways by dividers; blocked by the
   charge station and barriers.
3. Right bumper: BlueScore increases (hybrid grounded scoring works).
4. Right/Left trigger near a grid: a projectile that passes through a node box scores and disappears;
   score increases by the row value (low 2 / mid 3 / high 5 teleop).
5. Three adjacent scored nodes in a row → `BlueLinks` becomes 1.

## Notes
- These edits live in the template **submodule** working tree (a separate git repo); they are local
  test scaffolding, not committed to the parent maple-sim repo.
- Known approximations (from the port spec): hybrid LINK counts the cube sub-node only; cone
  upright-orientation scoring is disabled by default.
