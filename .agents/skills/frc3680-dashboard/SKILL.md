---
name: frc3680-dashboard
description: >
  Team 3680's driver dashboard (Elastic), cameras and NetworkTables keys. Use when adding a value to
  the dashboard, changing the Elastic layout, fixing a camera stream, or switching dashboard tabs.
  Triggers on: dashboard, Elastic, SmartDashboard, Shuffleboard, NetworkTables, camera, stream,
  Field2d, auto chooser, match time, shift, DashboardTelemetry.
---

# 3680 dashboard

| File | Role |
|------|------|
| `src/main/java/frc/robot/z2026/DashboardTelemetry.java` | Registers the 2 cameras, builds the `Auto Choices` chooser, publishes `SmartDashboard/Dashboard/*` every tick, and switches Elastic tabs. **Read-only with respect to the robot**: it never commands a module. |
| `src/main/java/frc/robot/misc/Elastic.java` | Elastic's official helper (`selectTab`, `sendNotification`). Copied unchanged except for the package. |
| `dashboard/elastic-layout.json` | The Elastic layout: tabs Prematch / Autonomous / Teleoperated. |
| `dashboard/README.md` | How drivers load the layout. |
| `src/main/java/frc/robot/z2026/Dashboard.java` | Older debug choosers (Group / Location / Index / Position / Action). |

## Hook points in `z2026/Robot.java`
- `robotInit()` (end): `DashboardTelemetry.Initialize(modules, m_positioner)`. It must come after `AutoModes.Initialize`, because it reads `Auto List`.
- `robotPeriodic()`: `DashboardTelemetry.Periodic()`.
- `disabledInit` / `autonomousInit` / `teleopInit`: `DashboardTelemetry.SelectTab("Prematch" | "Autonomous" | "Teleoperated")`. The tab names must match the layout exactly.

## Keys
| Topic | Type | Source |
|-------|------|--------|
| `/SmartDashboard/Field` | Field2d | `SwerveDriveModule` (robot pose) |
| `/CameraPublisher/Front Camera` | camera | Limelight, `http://limelight.local:5800/stream.mjpg` |
| `/CameraPublisher/Back Camera` | camera | PhotonVision, `http://photonvision.local:1182/stream.mjpg` |
| `/SmartDashboard/Auto Choices` | chooser | `DashboardTelemetry`; copies the selection into `Auto Selector` |
| `/SmartDashboard/Auto Selector` | string | read by `Robot.autonomousInit` |
| `/SmartDashboard/Auto List` | string[] | `AutoModes` |
| `/SmartDashboard/DB/Slider 0` | double | speed mod, read each teleop tick |
| `/SmartDashboard/DB/String 5` | string | code build version |
| `/SmartDashboard/Dashboard/Match Time`, `Battery Voltage`, `Browned Out`, `DS Attached`, `FMS Attached`, `Alliance` | | `DashboardTelemetry` |
| `/SmartDashboard/Dashboard/Current Shift`, `Hub Active` | string, bool | `DashboardTelemetry` (2026 hub shifts, ported from YETI 3506's `ShiftHandler`) |
| `/SmartDashboard/Dashboard/Robot X`, `Robot Y`, `Robot Heading`, `Robot Speed`, `Field Oriented`, `Vision Health` | | `DashboardTelemetry` |
| `/FMSInfo/*` | | WPILib built-in |
| `/<module id>/*`, `/SmartDashboard/<module id> mech` | | each `SingleMotorModule` |

## Recipes
- **Add a value:** add a `SmartDashboard.put...(prefix + "Name", value)` in `DashboardTelemetry.Periodic()`. Then add a widget for `/SmartDashboard/Dashboard/Name` to `dashboard/elastic-layout.json`, or add it in Elastic and re-export (File → Save Layout).
- **Change a camera:** edit the `*CameraUrl` constants. Limelight streams on port 5800. PhotonVision streams camera N on 1181 + 2N (raw) and 1182 + 2N (processed). Confirm in each camera's web UI.
- **Send a driver popup:** `Elastic.sendNotification(new Elastic.Notification(Elastic.NotificationLevel.WARNING, "Title", "Body"));`
- **Layout grid:** Elastic uses 128 px cells and x/y/width/height in pixels. Keep widgets from overlapping; the current layout is 15 × 7 cells.
- Don't call `positioner.IsHealthy()` or other stateful methods from telemetry. Use getters only (e.g. `GetHealthReason()`).
