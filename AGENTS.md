# AGENTS.md: FRC Team 3680 Elemental Dragons robot code

This file is the starting point for any AI agent (Claude Code, Codex, Copilot, Cursor, and others) working in this repo. Read it first, then open the file under **Further reading** that matches your task.

## What this repo is
- Java robot code for **FRC team 3680**, built with **WPILib / GradleRIO 2026.2.1** on **Java 17**.
- It is **not** command-based and does **not** use AdvantageKit. The team has its own framework of **modules** (mechanisms), **action poses** (named targets) and **auto sequences** (event chains). Don't add `SubsystemBase`, `Command`s or `@AutoLog` unless the team asks.
- Code for each season lives in its own package. `src/main/java/frc/robot/Main.java` chooses which one runs. **The active season is `frc.robot.z2026`** (REBUILT).

## Build, simulate, deploy
| Task | Command |
|------|---------|
| Compile + unit tests | `./gradlew build` |
| Desktop simulation (opens sim GUI) | `./gradlew simulateJava` |
| Deploy to the roboRIO (team 3680) | `./gradlew deploy` |

- The team number lives in `.wpilib/wpilib_preferences.json`.
- Dependencies come from `frcmaven.wpi.edu` and the vendor mavens. With no network to those sites the build fails before compiling, which is not a code error.
- Vendordeps are in `vendordeps/`: REVLib, Studica (NavX), photonlib, WPILibNewCommands. Add or update them only through the WPILib "Manage Vendor Libraries" tool; never hand-edit the JSON.

## Repo map (`src/main/java/frc/robot/`)
| Package | What it holds |
|---------|---------------|
| `Main.java` | Entry point. Change only the `Robot` import to switch seasons. |
| `z2026/` | **Active season.** `Robot.java` holds the hardware objects and mode hooks. `Constants.java` has dimensions, speeds and field geometry. `ActionPoses.java` defines named targets per module. `AutoModes.java` lists the autos. `Joystick.java` maps controller buttons. `Dashboard.java` holds the debug choosers. `DashboardTelemetry.java` feeds the Elastic dashboard. `Sequences/` holds the auto sequences. `action/` holds the `Location`/`Position` enums. |
| `z2025/`, `z2024/`, `zMechinarium/` | Earlier seasons and robots, kept as reference. Don't edit them for 2026 work. |
| `modules/` | Mechanism framework: `RobotModule`, `ModuleController`, `SwerveDriveModule`, `SwerveMotorModule`, `SingleMotorModule`, `SingleActuatorModule`, `DualMotorModule`, `MecanumDriveModule`, `DifferentialDriveModule`. |
| `auto/` | Auto framework: `AutoController`, `AutoSequence`, `AutoEvent*`, `AutoTarget`. See its [README](src/main/java/frc/robot/auto/README.md). |
| `action/` | `ActionPose`, `Group`, `Action`: the address of any target (group + location + index + position + action). |
| `positioner/` | Vision pose sources: `LimeLightPositioner`, `PhotonVisionPositioner`, `LimelightHelpers`. |
| `gyro/`, `encoder/`, `switches/` | Hardware wrappers (NavX `AHRSGyro`, `REVEncoder`, limit switches). |
| `misc/` | `GameController` (Xbox / PS4 / FlightStick / Switch2Pro mapping), `Utility` (units, alliance flipping, geometry), `Elastic` (dashboard tab switching and notifications). |

## How the robot loop works (z2026)
1. `robotInit()` builds a `ModuleController` around `SwerveDriveModule` and calls `AddModule(...)` for `shooter`, `intake` and `feeder`. It then calls `modules.Initialize()`, publishes choosers and autos, and finally calls `DashboardTelemetry.Initialize(...)`.
2. Every mode init calls `commonInit()`. That registers button-mapped autos, builds the `GameController`, then calls `ActionPoses.Initialize` and `Joystick.InitializeButtonMappings`. **This order matters.** The start pose is set once per match.
3. Teleop each tick runs `m_controller.ProcessButtons()`, then button-mapped autos `Update()`, then `modules.ProcessState(false)`.
4. Auto each tick runs `currentAutoMode.Update()`, then `modules.ProcessState(true)`. The auto is picked by name from `SmartDashboard/Auto Selector`.
5. `robotPeriodic()` runs `DashboardTelemetry.Periodic()` in every mode.

## Conventions
- **Method names are PascalCase** (`Initialize`, `ProcessState`, `AddModule`, `GetModule`). Fields are camelCase; hardware fields use `can_`, `enc_`, `m_` prefixes. Keep this style.
- **Units:** meters, and radians inside drive and PID math. `Positioner` and Limelight work in **degrees**. The gyro is **CCW-positive** (the wrapper negates NavX yaw). Convert with `Utility.degreesToRadians` / `inchesToMeters`.
- **Coordinates:** WPILib blue-origin field, X forward, Y left. Build red-side targets with `Utility.rotateToAllianceStart` / `transformToAllianceStart`; don't duplicate them.
- **Auto sequences** build everything in `Initialize()`, never in the constructor, and guard against a second init.
- **Modules are looked up by string ID** (`modules.GetModule("shooter")`). The ID is the first constructor argument; keep it in sync.
- Tuning values belong in the season's `Constants.java`, not inline.

## Hardware map (2026 robot, from `z2026/Robot.java`)
| Device | CAN ID | Notes |
|--------|--------|-------|
| Steer LF / RF / LR / RR | 2 / 3 / 4 / 5 | SparkMax with absolute encoders. Offsets are 4.71 / 0 / 3.14 / 1.57 rad. |
| Drive LF / RF / LR / RR | 6 / 7 / 8 / 9 | SparkFlex. **LF drive is currently `null` (commented out).** |
| Intake | 10 | SparkMax, brushless |
| Shooter | 11 | SparkMax, brushless, velocity mode with encoder |
| Feeder | 12 | SparkMax, **brushed** |

- **Gyro:** NavX (`AHRSGyro`).
- **Vision:** one Limelight (`limelight.local`, name `""`) used by `LimeLightPositioner`, plus PhotonVision (`photonvision.local`, port 5800 forwarded).
- **Swerve:** setup steps and troubleshooting are in [.agents/skills/frc3680-modules/references/swerve-setup.md](.agents/skills/frc3680-modules/references/swerve-setup.md).

## Dashboard / NetworkTables keys
- The driver dashboard is **Elastic**, using [dashboard/elastic-layout.json](dashboard/elastic-layout.json). Setup is in [dashboard/README.md](dashboard/README.md).
- Keys the code publishes:
  - `SmartDashboard/Field`: Field2d with the robot pose.
  - `SmartDashboard/Auto List`: all autos.
  - `SmartDashboard/Auto Choices`: the chooser.
  - `SmartDashboard/Auto Selector`: the auto that will run.
  - `SmartDashboard/DB/String 0-6`: build version, start location.
  - `SmartDashboard/DB/Slider 0`: speed mod.
  - `SmartDashboard/Dashboard/*`: match time, battery, shift, hub active, robot pose and speed, vision health.
  - `CameraPublisher/Front Camera` and `CameraPublisher/Back Camera`.
- When you add or rename a key, update the layout JSON and the [dashboard skill](.agents/skills/frc3680-dashboard/SKILL.md).

## Rules for agents
1. **Prefer adding over rewriting.** Don't reformat, reorder or "clean up" code you weren't asked to touch. Commented-out code here is often deliberate: it records alternate wiring or experiments.
2. Don't touch older season packages, `Main.java` (except the season switch), `LimelightHelpers.java` (vendored) or `vendordeps/*.json`.
3. Never change CAN IDs, motor inversions or swerve offsets without telling the human. A wrong value can damage the robot.
4. Check your work with `./gradlew build`. If you can, also run `./gradlew simulateJava` and look at NetworkTables.
5. Git: branch off `main`, make small commits with clear messages, and open a PR to `main` (see [README.md](README.md)).

## Further reading
| Task | Read |
|------|------|
| Add or modify a mechanism, swerve, motors | [.agents/skills/frc3680-modules/SKILL.md](.agents/skills/frc3680-modules/SKILL.md) |
| Write or debug an autonomous routine | [.agents/skills/frc3680-auto/SKILL.md](.agents/skills/frc3680-auto/SKILL.md) → [auto/README.md](src/main/java/frc/robot/auto/README.md) |
| Dashboard, cameras, NetworkTables | [.agents/skills/frc3680-dashboard/SKILL.md](.agents/skills/frc3680-dashboard/SKILL.md) |
| Start a new season / game | [.agents/skills/frc3680-new-season/SKILL.md](.agents/skills/frc3680-new-season/SKILL.md) |
| WPILib APIs | https://docs.wpilib.org |
| REV Spark / REVLib | https://docs.revrobotics.com/revlib |
| Limelight | https://docs.limelightvision.io |
| Elastic dashboard | https://frc-elastic.gitbook.io/docs |
