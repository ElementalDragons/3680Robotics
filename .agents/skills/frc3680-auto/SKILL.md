---
name: frc3680-auto
description: >
  Writing and debugging autonomous routines in team 3680's custom auto framework (AutoController,
  AutoSequence, AutoEventTarget, AutoEventTime, ActionPose). Use when creating a new auto, adding
  a button-mapped sequence, registering action poses, or when an auto hangs on a target.
  Triggers on: auto, autonomous, sequence, AutoModes, ActionPoses, AwaitTarget, BeginWith, Then,
  waypoint, Auto Selector.
---

# 3680 auto framework

The full reference is **[src/main/java/frc/robot/auto/README.md](../../../src/main/java/frc/robot/auto/README.md)**; read it before you change auto code. This file is a quick map.

## Moving parts
- **`ActionPose(group, location, locationIndex, position, action, AutoTarget)`** is the address of a target.
  - `Group` and `Action` are in `frc.robot.action`; `Location` and `Position` are per-season (`z2026/action/`).
  - `-1` / `Any` are wildcards.
- **`AutoTarget`** is what to reach. Forms: `Measurement` (rotations or RPM for single motors), `Position` + `Orientation`, or `Position` + `LookAt` for drive.
- **`z2026/ActionPoses.java`** registers every target on each module (`AddActionPose`). It runs in `commonInit()` on every mode init.
- **`AutoSequence`** is a chain of events, built in `Initialize()` with `BeginWith(e1).Then(e2)...`.
- **`AutoEventTarget(name, isAsync, pose, EventType.AwaitTarget, ac)`** sets the pose on all modules and waits until they settle.
- **`AutoEventTime(name, isAsync, ms, type, ac)`** is a timed wait or timed value. Use `EventType.None` for a pure wait.
- **`AutoController`** owns sequences. **`z2026/AutoModes.java`** registers controllers by label and publishes `SmartDashboard/Auto List`.

## Add a new auto
1. Create `z2026/Sequences/MyAuto.java` extending `AutoSequence`. Copy the pattern from `SpinupAndShoot.java`:
   - The constructor only calls `super(label, ac)` and stores `ac`.
   - `Initialize()` calls `super.Initialize()`, guards with an `initialized` boolean, builds the poses and events, then chains them.
2. Make sure every pose you await is registered in `ActionPoses.java`, with **matching** group, location, index, position and action. A typo means the target never matches and the auto hangs.
3. Register it in `AutoModes.Initialize`:
   ```java
   AutoController myAuto = new AutoController("MyAuto", modules);
   myAuto.AddSequence(new MyAuto(myAuto.GetLabel(), myAuto));
   AutoModes.put(myAuto.GetLabel(), myAuto);
   ```
   Do this **before** the `putStringArray("Auto List", ...)` line.
4. Pick it on the Elastic dashboard (Prematch → Auto Choices), which writes `SmartDashboard/Auto Selector`.

## Button-mapped sequences in teleop
`Robot.commonInit()` puts sequences into `buttonMappedAutos` with a teleop `AutoController`. Teleop calls `Update()` on each of them every tick. Bind one in `Joystick.java` with `sequence::InitializeViaButton`.

## Debugging
- The auto hangs on a step: the pose didn't match, or the module never settles (check `floatTolerance` and `settleCyclesRequired`). Look at the module's NetworkTables table (`targetPose`, `settleCount`, `deltaMeasurement`).
- The robot drives to the wrong side: red-alliance positions come from `Utility.transformToAllianceStart`. Check the DS alliance and `DB/String 6` (start location).
- Field-oriented is forced on in `autonomousInit`.
