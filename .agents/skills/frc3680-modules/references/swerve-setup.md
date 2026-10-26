# Swerve setup and troubleshooting (2026 robot)

This expands the setup comment above `swerveDriveModule` in `src/main/java/frc/robot/z2026/Robot.java`.

## Geometry
- Frame: 27" × 27" (`Constants.frameSize`), plus 4" of bumper.
- Wheel axis is 1.81" in from each frame edge (`Constants.motorOffset`). The result, `Constants.motorPosition`, is each module's X/Y from robot center.
- Wheels are 3" diameter. Drive gear ratio 5.08 (`driveGearRatio`), steer gear ratio 46.42 (`steerGearRatio`).
- Module order in the constructor: LeftFront (+X, +Y), RightFront (+X, −Y), LeftRear (−X, +Y), RightRear (−X, −Y). X is forward, Y is left.
- The motor labels are rotated so that the **Limelight side is "forward"**. The software name of a corner may not match the physical corner; see the comments next to each `SwerveMotorDefinition`.

## Current per-corner values
| Corner | Drive CAN | Steer CAN | Rotation offset (rad) |
|--------|-----------|-----------|------------------------|
| LeftFront | 6 (**disabled / null**) | 2 | 4.71 |
| RightFront | 7 | 3 | 0 |
| LeftRear | 8 | 4 | 3.14 |
| RightRear | 9 | 5 | 1.57 |

All steer encoders are REV absolute encoders mounted `EncoderMountLocation.AfterGearbox`.

## Checklist when the drive misbehaves
1. **Corners labeled and assigned correctly.** If not, wheels turn in opposite directions from each other.
2. **Rotation direction (inverted or not).**
   - Turning the wrong way shows up as wheels pointing inward during rotation, and left/right swapped.
   - If the encoder and motor disagree on direction, you get oscillation that never settles, or constant spinning.
   - Any sporadic behavior usually means encoder and motor direction disagree.
3. **Coordinate system:** X forward, Y left.
4. **Rotation offsets** are correct for each corner and given in **radians**. If they're wrong, the wheels won't point the right way in any situation. With REV encoders, right-front should be 0 and the other corners rotate from it, with no inversion on rotation.

## Math notes
- The rotation PID uses continuous input over [−π, π]. LookAt rotations get a 1.5× boost.
- `SwerveModuleState.optimize` picks the shortest steering path and flips the drive direction past 90°.
- Pose estimation fuses odometry with `Positioner.GetPoseEstimate()` on real hardware only.
- The gyro is NavX `AHRSGyro`: CCW-positive degrees, unbounded. Internal math converts to radians.

Never change these values without telling a human mentor; a wrong offset can slam modules into hard stops.
