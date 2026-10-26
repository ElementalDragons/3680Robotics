---
name: frc3680-new-season
description: >
  How team 3680 starts code for a new FRC game or robot: copying a season package (zYYYY), updating
  field geometry and enums, and switching Main.java. Use at kickoff, when building a practice/off-season
  robot, or when updating WPILib for a new year.
  Triggers on: new season, kickoff, new game, new robot, z2027, copy season, Main.java, field size,
  WPILib update, GradleRIO version.
---

# Starting a new season

Each season or robot is a self-contained package under `src/main/java/frc/robot/`: `z2024`, `z2025`, `z2026`, or a named robot like `zMechinarium`. The shared framework (`modules/`, `auto/`, `action/`, `positioner/`, `gyro/`, `encoder/`, `misc/`) is reused across years.

## Steps
1. **Copy** the latest season package, e.g. `z2026` → `z2027`, and change every `package frc.robot.z2026...` and import to `z2027`.
   - The files to copy are `Robot`, `Constants`, `ActionPoses`, `AutoModes`, `Joystick`, `Dashboard`, `DashboardTelemetry`, `action/Location`, `action/Position` and `Sequences/`.
2. **Game geometry** in the new `Constants.java`:
   - `fieldSize`, `startArea` and start positions.
   - AprilTag index maps (`HubIndexToTag`, etc.).
   - `getBlueStartFieldPosition` / `getBlueStartKnownRotation`.
3. **Enums:** rename the `Location` and `Position` values to the new game's field elements. Keep `Any = -1`.
4. **Hardware** in `Robot.java`: CAN IDs, modules, swerve offsets (see [frc3680-modules](../frc3680-modules/SKILL.md)).
5. **Autos and buttons:** rewrite `ActionPoses`, `AutoModes`, `Sequences/` and `Joystick` for the new game.
6. **Dashboard:** in `DashboardTelemetry`, replace the game-specific shift logic. Update `field_game` and the widgets in `dashboard/elastic-layout.json`.
7. **Switch** `Main.java` to `import frc.robot.z2027.Robot;`. This is the only edit allowed in `Main.java`.
8. **WPILib year:** run the WPILib VS Code "Import a WPILib 20XX Gradle project" or update the GradleRIO version in `build.gradle`. Update vendordeps with the vendor library manager.
9. Update `AGENTS.md`: the active season and the hardware map.

Leave the old season package in place. The team keeps old seasons as reference.
