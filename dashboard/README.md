# Driver dashboard (Elastic)

We use [Elastic](https://frc-elastic.gitbook.io/docs), which ships with the WPILib installer. The layout is based on [YETI 3506's](https://github.com/yeti-robotics/rebuilt-2026) and has three tabs. The robot code switches tabs automatically when the robot is disabled or enters auto or teleop.

| Tab | Widgets |
|-----|---------|
| **Prematch** | Auto chooser, FMS info, match/station number, alliance, code version, battery, DS attached, vision health, speed mod slider, field, both cameras |
| **Autonomous** | Front + back camera, field, match time, battery, brownout, vision health, FMS info, selected auto, robot speed |
| **Teleoperated** | Front + back camera, field, match time, current hub shift, hub active, battery, brownout, field oriented, robot speed, speed mod slider, FMS info, vision health |

## Setup (once per driver station laptop)
1. Open Elastic.
2. **Settings → Network → Team Number = 3680.** IP Address Mode: "Driver Station" when the DS is running, otherwise "Team Number".
3. **File → Open Layout** and choose `dashboard/elastic-layout.json` from this repo.
4. In the Driver Station, set the dashboard type to **Elastic** so it starts with the DS.

## Cameras
- **Front Camera:** the Limelight stream, `http://limelight.local:5800/stream.mjpg`.
- **Back Camera:** the PhotonVision stream, `http://photonvision.local:1182/stream.mjpg`.

To change a URL, edit the constants at the top of `src/main/java/frc/robot/z2026/DashboardTelemetry.java`. **Verify both on the robot.**

## Changing the layout
Edit it in Elastic, then **File → Save Layout** over `dashboard/elastic-layout.json` and commit it. Tab names must stay `Prematch`, `Autonomous` and `Teleoperated`, or the automatic tab switching stops working.

All topics the layout uses are listed in [.agents/skills/frc3680-dashboard/SKILL.md](../.agents/skills/frc3680-dashboard/SKILL.md).
