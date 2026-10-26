---
name: frc3680-modules
description: >
  How team 3680's custom module framework works (RobotModule, ModuleController, SwerveDriveModule,
  SingleMotorModule, SingleActuatorModule). Use when adding or changing a mechanism, wiring a motor,
  mapping a button to a module, tuning swerve, or debugging why a module doesn't move.
  Triggers on: module, mechanism, motor, SparkMax, SparkFlex, CAN ID, swerve, shooter, intake, feeder,
  ModuleController, ApplyValue, ProcessState, AddActionPose.
---

# 3680 module framework

The repo does **not** use WPILib command-based. Every mechanism is a `RobotModule`, and one `ModuleController` owns them all.

## Pieces
| Class | Role |
|-------|------|
| `modules/RobotModule.java` | Interface for every mechanism: `Initialize`, `ProcessState(isAuto)`, `ApplyValue`/`ApplyInverse(pressed)`, action-pose methods, `AbandonTarget`. |
| `modules/DriveModule.java` | Interface for drivetrains: forward/lateral/rotation input, field-oriented, poses. |
| `modules/ModuleController.java` | Holds the drive module plus a `Hashtable<String, RobotModule>`. Fans `ProcessState` and `SetTargetActionPose` out to every module, and applies speed modifiers (`divider`, `speedMod`, speed dilation). |
| `modules/SwerveDriveModule.java` | Four `SwerveMotorModule`s, pose estimator (odometry + `Positioner`s), PID to target poses, publishes `SmartDashboard/Field`. |
| `modules/SingleMotorModule.java` | One motor with an optional encoder and limit switches. Runs open loop, position PID, or velocity mode (`UseVelocity`). Publishes a NetworkTables table named after its ID and a `<id> mech` Mechanism2d. |
| `modules/SingleActuatorModule.java` | Relay or pneumatic on/off. |
| `DualMotorModule`, `MecanumDriveModule`, `DifferentialDriveModule` | Alternatives used in older seasons. |

## Per-tick flow
`Robot.teleopPeriodic` → `GameController.ProcessButtons()` (calls the registered consumers) → `modules.ProcessState(isAuto)` → each module reads its encoder, calls `EvaluateTargetPose()` (moves toward its `ActionPose` target, then settles and calls `AbandonTarget()`), and finally sets the motor output.

## Adding a mechanism (z2026 example)
1. **Hardware**: in `z2026/Robot.java`, declare the motor (and encoder) next to the others, e.g.
   `final SparkMax can_climber = new SparkMax(13, MotorType.kBrushless);`
   Pick an unused CAN ID; the list is in `AGENTS.md`.
2. **Module**: in the same file, construct it:
   `SingleMotorModule climber_module = new SingleMotorModule("climber", can_climber, speed, invert, upperLimit, lowerLimit, enc, encMultiplier, reverseMultiplier, distancePerRotation, endDistance, useVelocity);`
3. **Register**: in `robotInit()`, add `modules.AddModule(climber_module);` **before** `modules.Initialize()`.
4. **Buttons**: in `z2026/Joystick.java`, fetch it with `(SingleMotorModule) modules.GetModule("climber")` and bind it, e.g. `m_controller.RegisterBinaryButtonConsumer(ButtonName.LeftShoulderButton, climber::ApplyValue);`. Button names are in `GameController.ButtonName`.
5. **Targets for autos**: in `z2026/ActionPoses.java`, add `climber.AddActionPose(new ActionPose(Group.Any, Location.Any.getValue(), -1, Position.Any.getValue(), Action.Raise, new AutoTarget(rotations)));`
6. Put tuning numbers in `z2026/Constants.java`.

## Gotchas
- `SingleMotorModule.Toggle(pressed)` flips the motor on/off on each press (rising edge) and keeps it running via `sustainedDriveSpeed`. Bind it like `ApplyValue`. Note: the agitator in `z2026/Joystick.java` is commented as a toggle but is bound to `agitator::ApplyValue` (hold-to-run).
- `ApplyValue` / `ApplyInverse` add drive speed **for the tick the button is held**, then `currentDriveSpeed` resets to `sustainedDriveSpeed`. Holding the button keeps the motor running.
- Velocity mode (`UseVelocity = true`) uses `AutoTarget(measurement)` as RPM. Example: shooter `Action.Feed` = -3000 RPM, which is still a TODO to tune.
- Limit switches stop the motor **and** abandon the target, so an `AwaitTarget` never deadlocks.
- In sim (`!RobotBase.isReal()`) encoders are faked from the commanded speed.
- The module ID string is used for lookup **and** as the NetworkTables table name. Renaming it breaks `GetModule(...)` calls and dashboards.

## Swerve
Before you touch any swerve corner, offset or inversion, read [references/swerve-setup.md](references/swerve-setup.md).
