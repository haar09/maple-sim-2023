<p align="center">
  <img src="./docs/media/team_logo.png" width="20%"  alt="team logo"/>
  <img src="./docs/media/icon.png" width="79%"  alt="project logo"/>
</p>

### Elevating FRC Java Robot Simulations to the Next Level with Physics Engines

## Why physics engine?
A simulation engine is a powerful tool that provides realistic approximations of physical systems. With **maple-sim**, we integrate the open-source Java rigid-body dynamics engine, [dyn4j](https://github.com/dyn4j/dyn4j), capable of simulating 2D forces and collisions between rigid shapes. This integration transforms the scope of robot simulations by enabling realistic interactions between robots, field elements, and game pieces.

![physics engine illustration](./docs/media/physics%20engine.png)

## What is maple-sim?

Before **maple-sim**, most FRC robot simulations focused solely on the robot itself—its sensors, movements, and internal operations. 
Now, through the power of physics simulation, **maple-sim** allows your robot to engage directly with its environment. 
Imagine testing robot interactions with obstacles, field elements, and game pieces, all within the simulated world.
A simulation that is realistic enough to **feel like a video game.**

[![Demo Video 1](./docs/media/demo%20video%20cover.png)](https://www.youtube.com/watch?v=CBx1_Dosgec)


With this advanced level of simulation, the possibilities are endless. You can:

- Test autonomous modes with pinpoint accuracy.
- Fine-tune advanced TeleOp enhancement features like pathfinding-auto-alignment.
- Optimize shooters and other subsystems, all while gathering meaningful data from simulated physics.

**And the best part? You can achieve all of this without needing a real robot on hand.**

> **Note:** This is a personal fork of [maple-sim](https://github.com/Shenzhen-Robotics-Alliance/maple-sim) adapted to simulate the **2023 FRC game (Charged Up)** on the **2026 WPILib**. For the original library and its documentation, see the [upstream project](https://github.com/Shenzhen-Robotics-Alliance/maple-sim).

## Getting Started

Install this fork into your WPILib robot project as an online vendor library using the following Vendor URL:

```
https://raw.githubusercontent.com/haar09/maple-sim-2023/main/vendordep/maplesim2023.json
```

In WPILib VSCode: open the command palette → **WPILib: Manage Vendor Libraries** → **Install new libraries (online)** → paste the URL above. (The vendordep must be present on the `main` branch for this URL to resolve.)
<br>

## Releasing a New Version

1. Bump `pubVersion` in `project/publish.gradle` (e.g. `2023.0.1`). That is the only version you edit — `vendordep/maplesim2023.json` regenerates from `project/maplesim2023.json` automatically.
2. From the `project/` folder, run `./gradlew publish`. This writes fresh jars into `vendordep/repos/releases/org/haar09/maplesim2023-java/<version>/` and rewrites the vendordep JSON.
3. Commit and push to `main` (the Vendor URL above is served from `main` via `raw.githubusercontent.com`).
4. In your robot project: **WPILib: Manage Vendor Libraries** → **Check for updates (online)**, then re-run a Gradle sync/build.

> Always bump the version — Gradle caches artifacts by version. Note `raw.githubusercontent.com` has a ~5-minute CDN cache, so a fresh push is not instant.

## Local Testing (no push required)

To test changes against a robot project without publishing to GitHub, use your local Maven cache (`~/.m2`):

1. In `project/publish.gradle`, set a test version, e.g. `def pubVersion = '2023.0.0-test'`.
2. From the `project/` folder, run `./gradlew publishToMavenLocal`. This installs to `~/.m2/repository/org/haar09/maplesim2023-java/2023.0.0-test/`.
3. In the **robot project's** `build.gradle`, add `mavenLocal()` to the `repositories` block so Gradle looks in `~/.m2`.
4. Copy the generated `vendordep/maplesim2023.json` (now stamped `2023.0.0-test`) into the robot project's `vendordeps/` folder. Do **not** use the online URL — the jar only exists locally.
5. Sync/build the robot project. After each library change, re-run `./gradlew publishToMavenLocal` and rebuild the robot project with `./gradlew build --refresh-dependencies`.

> Remember to revert the robot project to a real released version and remove `mavenLocal()` before committing it for teammates.


> 🙏  Big thanks to [@GrahamSH-LLK](https://www.chiefdelphi.com/u/nstrike/summary) for all the help in setting up the online documentation.
> 🙏  Big thanks to [@nstrike](https://www.chiefdelphi.com/u/nstrike/summary) for all the help in setting up the Java Docs and VendorDep publishing.

## Reporting Bugs, Developing and Contributing

- If you've encountered a bug while using maple-sim in your robot code, please [submit an issue](https://github.com/Shenzhen-Robotics-Alliance/maple-sim/issues/new/choose) and select the "Bug Report" option.  We review issues regularly and will respond as quickly as possible.

- If you have an idea for a new feature, please [submit an issue](https://github.com/Shenzhen-Robotics-Alliance/maple-sim/issues/new/choose) and select the "Feature Request" option.

- If you think the API for an existing feature could be improved for better readability or usability, please [submit an issue](https://github.com/Shenzhen-Robotics-Alliance/maple-sim/issues/new/choose) and select the "API Enhancement" option.

- For detailed guidelines on contributing to the project, please refer to the [contribution guide](https://shenzhen-robotics-alliance.github.io/maple-sim/CONTRIBUTION/index.html).
