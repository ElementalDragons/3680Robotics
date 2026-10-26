package frc.robot.z2026;

import java.util.Arrays;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.HttpCamera;
import edu.wpi.first.cscore.HttpCamera.HttpCameraKind;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.misc.Elastic;
import frc.robot.modules.ModuleController;
import frc.robot.positioner.Positioner;

// Publishes everything the Elastic layout in /dashboard/elastic-layout.json needs.
// Only reads robot state - it never commands a module, so it is safe to call from any mode.
public class DashboardTelemetry {
  // TODO: verify both stream URLs on the real robot.
  // Limelight 4 serves its MJPEG stream on port 5800; PhotonVision serves camera 0 processed on 1182 (raw on 1181).
  public static final String frontCameraName = "Front Camera";
  public static final String frontCameraUrl = "http://limelight.local:5800/stream.mjpg";
  public static final String backCameraName = "Back Camera";
  public static final String backCameraUrl = "http://photonvision.local:1182/stream.mjpg";

  // the 2026 hub alternates which alliance is active every 25 seconds between the transition and endgame periods
  public enum Shift {
    Auto,
    Transition,
    Red,
    Blue,
    Endgame,
    Unknown,
  }

  static final String prefix = "Dashboard/";

  static ModuleController modules;
  static Positioner positioner;

  static SendableChooser<String> autoChooser;
  static String lastAutoSelection = null;

  static Shift firstShift = Shift.Unknown;
  static Shift currentShift = Shift.Unknown;

  static Pose3d lastPose = null;
  static double lastPoseTime = 0;

  // Call at the end of robotInit, after AutoModes.Initialize has published "Auto List".
  public static void Initialize(ModuleController Modules, Positioner Positioner) {
    modules = Modules;
    positioner = Positioner;

    CameraServer.addCamera(new HttpCamera(frontCameraName, frontCameraUrl, HttpCameraKind.kMJPGStreamer));
    CameraServer.addCamera(new HttpCamera(backCameraName, backCameraUrl, HttpCameraKind.kMJPGStreamer));

    InitializeAutoChooser();
  }

  // Elastic ComboBox Chooser for the autos in "Auto List".  The selection is copied into "Auto Selector",
  // which is what Robot.autonomousInit already reads, so the old SmartDashboard text box keeps working too.
  static void InitializeAutoChooser() {
    autoChooser = new SendableChooser<>();

    var autoList = SmartDashboard.getStringArray("Auto List", new String[] {});
    Arrays.sort(autoList);
    var currentSelection = SmartDashboard.getString("Auto Selector", "");

    for (var auto : autoList) {
      if (auto.equals(currentSelection) || (currentSelection.isEmpty() && auto.equals(autoList[0])))
        autoChooser.setDefaultOption(auto, auto);
      else
        autoChooser.addOption(auto, auto);
    }

    SmartDashboard.putData("Auto Choices", autoChooser);
  }

  public static void Periodic() {
    var selectedAuto = autoChooser != null ? autoChooser.getSelected() : null;
    if (selectedAuto != null && !selectedAuto.equals(lastAutoSelection)) {
      SmartDashboard.putString("Auto Selector", selectedAuto);
      lastAutoSelection = selectedAuto;
    }

    SmartDashboard.putNumber(prefix + "Match Time", DriverStation.getMatchTime());
    SmartDashboard.putNumber(prefix + "Battery Voltage", RobotController.getBatteryVoltage());
    SmartDashboard.putBoolean(prefix + "Browned Out", RobotController.isBrownedOut());
    SmartDashboard.putBoolean(prefix + "DS Attached", DriverStation.isDSAttached());
    SmartDashboard.putBoolean(prefix + "FMS Attached", DriverStation.isFMSAttached());

    var alliance = DriverStation.getAlliance();
    SmartDashboard.putString(prefix + "Alliance", alliance.isPresent() ? alliance.get().toString() : "Unknown");

    UpdateShift();
    SmartDashboard.putString(prefix + "Current Shift", currentShift.toString());
    SmartDashboard.putBoolean(prefix + "Hub Active", IsOurHubActive(alliance.orElse(null)));

    if (modules != null) {
      var pose = modules.GetPosition();
      var now = Timer.getFPGATimestamp();
      if (pose != null) {
        SmartDashboard.putNumber(prefix + "Robot X", pose.getX());
        SmartDashboard.putNumber(prefix + "Robot Y", pose.getY());
        SmartDashboard.putNumber(prefix + "Robot Heading", Math.toDegrees(pose.getRotation().getZ()));

        if (lastPose != null && now > lastPoseTime) {
          var speed = pose.getTranslation().getDistance(lastPose.getTranslation()) / (now - lastPoseTime);
          SmartDashboard.putNumber(prefix + "Robot Speed", speed);
        }
        lastPose = pose;
        lastPoseTime = now;
      }

      SmartDashboard.putBoolean(prefix + "Field Oriented", modules.GetDriveModule().IsFieldOriented());
    }

    if (positioner != null)
      SmartDashboard.putString(prefix + "Vision Health", positioner.GetHealthReason());
  }

  // Port of YETI's ShiftHandler (yeti-robotics/rebuilt-2026) without AdvantageKit.
  // The game data letter names the alliance whose hub goes inactive first, so the other alliance is active first.
  static void UpdateShift() {
    if (DriverStation.isAutonomousEnabled()) {
      currentShift = Shift.Auto;
      return;
    }
    if (!DriverStation.isTeleopEnabled()) {
      currentShift = Shift.Unknown;
      return;
    }

    if (firstShift == Shift.Unknown) {
      var gameData = DriverStation.getGameSpecificMessage();
      if (!gameData.isEmpty()) {
        switch (gameData.charAt(0)) {
          case 'B':
            firstShift = Shift.Red;
            break;
          case 'R':
            firstShift = Shift.Blue;
            break;
          default:
            break;
        }
      }
    }

    var matchTime = DriverStation.getMatchTime();
    if (matchTime >= 130) {
      currentShift = Shift.Transition;
    } else if (matchTime <= 30) {
      currentShift = Shift.Endgame;
    } else if (firstShift == Shift.Unknown) {
      currentShift = Shift.Unknown;
    } else {
      int blockTime = (int) (130 - matchTime) / 25;
      currentShift = blockTime % 2 == 0 ? firstShift : (firstShift == Shift.Red ? Shift.Blue : Shift.Red);
    }
  }

  static boolean IsOurHubActive(Alliance alliance) {
    switch (currentShift) {
      case Auto:
      case Transition:
      case Endgame:
        return true;
      case Red:
        return alliance == Alliance.Red;
      case Blue:
        return alliance == Alliance.Blue;
      default:
        return false;
    }
  }

  public static void SelectTab(String tabName) {
    Elastic.selectTab(tabName);
  }
}
