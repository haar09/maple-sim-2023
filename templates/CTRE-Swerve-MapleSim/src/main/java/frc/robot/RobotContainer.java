// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.swerve.SwerveRequest;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.simulation.seasonspecific.chargedup2023.ChargedUpConeOnFly;
import org.ironmaple.simulation.seasonspecific.chargedup2023.ChargedUpCubeOnFly;

public class RobotContainer {
    private double MaxSpeed = TunerConstants.kSpeedAt12Volts.in(MetersPerSecond); // kSpeedAt12Volts desired top speed
    private double MaxAngularRate =
            RotationsPerSecond.of(0.75).in(RadiansPerSecond); // 3/4 of a rotation per second max angular velocity

    /* Setting up bindings for necessary control of the swerve drive platform */
    private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
            .withDeadband(MaxSpeed * 0.1)
            .withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
            .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // Use open-loop control for drive motors
    private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
    private final SwerveRequest.PointWheelsAt point = new SwerveRequest.PointWheelsAt();
    private final SwerveRequest.RobotCentric forwardStraight =
            new SwerveRequest.RobotCentric().withDriveRequestType(DriveRequestType.OpenLoopVoltage);

    private final Telemetry logger = new Telemetry(MaxSpeed);

    private final CommandXboxController joystick = new CommandXboxController(0);

    public final CommandSwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();

    // Game pieces preloaded on the robot. Charged Up robots carry one piece; we preload one of each so both
    // can be test-fired. Decremented on launch, refilled by reload (RB).
    private int heldCone = 1;
    private int heldCube = 1;
    private static final double HELD_PIECE_HEIGHT_M = 0.5;

    public RobotContainer() {
        configureBindings();

        // Start in open field (midfield). Note: (3, 3) is INSIDE the blue charge-station collider.
        drivetrain.resetPose(new Pose2d(8.27, 4.0, new Rotation2d()));

        // Stage the field game pieces (cones/cubes at the staging marks) at startup.
        if (RobotBase.isSimulation()) SimulatedArena.getInstance().resetFieldForAuto();
    }

    private void configureBindings() {
        // Note that X is defined as forward according to WPILib convention,
        // and Y is defined as to the left according to WPILib convention.
        drivetrain.setDefaultCommand(
                // Drivetrain will execute this command periodically
                drivetrain.applyRequest(
                        () -> drive.withVelocityX(
                                        -joystick.getLeftY() * MaxSpeed) // Drive forward with negative Y (forward)
                                .withVelocityY(-joystick.getLeftX() * MaxSpeed) // Drive left with negative X (left)
                                .withRotationalRate(-joystick.getRightX()
                                        * MaxAngularRate) // Drive counterclockwise with negative X (left)
                        ));

        joystick.a().whileTrue(drivetrain.applyRequest(() -> brake));
        joystick.b()
                .whileTrue(drivetrain.applyRequest(
                        () -> point.withModuleDirection(new Rotation2d(-joystick.getLeftY(), -joystick.getLeftX()))));

        joystick.pov(0)
                .whileTrue(drivetrain.applyRequest(
                        () -> forwardStraight.withVelocityX(0.5).withVelocityY(0)));
        joystick.pov(180)
                .whileTrue(drivetrain.applyRequest(
                        () -> forwardStraight.withVelocityX(-0.5).withVelocityY(0)));

        // Run SysId routines when holding back/start and X/Y.
        // Note that each routine should be run exactly once in a single log.
        joystick.back().and(joystick.y()).whileTrue(drivetrain.sysIdDynamic(Direction.kForward));
        joystick.back().and(joystick.x()).whileTrue(drivetrain.sysIdDynamic(Direction.kReverse));
        joystick.start().and(joystick.y()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kForward));
        joystick.start().and(joystick.x()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kReverse));

        // reset the field-centric heading on left bumper press
        joystick.leftBumper().onTrue(drivetrain.runOnce(() -> drivetrain.seedFieldCentric()));

        // ---- 2023 Charged Up port test controls (simulation only) ----
        // Right trigger: shoot the held CUBE from the robot's actual pose.
        joystick.rightTrigger()
                .onTrue(Commands.runOnce(() -> launchPiece(false)).ignoringDisable(true));
        // Left trigger: shoot the held CONE from the robot's actual pose.
        joystick.leftTrigger().onTrue(Commands.runOnce(() -> launchPiece(true)).ignoringDisable(true));
        // Right bumper: reload one cone + one cube onto the robot.
        joystick.rightBumper().onTrue(Commands.runOnce(this::reload).ignoringDisable(true));
        // POV right: reset the field (re-stage all game pieces, clear scored grids).
        joystick.povRight().onTrue(Commands.runOnce(this::resetField).ignoringDisable(true));

        drivetrain.registerTelemetry(logger::telemeterize);
    }

    /**
     * Shoots a held cone or cube as a projectile from the robot's actual (maple-sim physics) pose. Does nothing if no
     * piece of that type is held.
     */
    private void launchPiece(boolean cone) {
        if (!RobotBase.isSimulation()) return;
        if (cone && heldCone <= 0) return;
        if (!cone && heldCube <= 0) return;

        Pose2d robotPose = drivetrain.getMapleSimPose();
        Translation2d robotPosition = robotPose.getTranslation();
        Rotation2d facing = robotPose.getRotation();
        if (cone) {
            heldCone--;
            SimulatedArena.getInstance()
                    .addGamePieceProjectile(new ChargedUpConeOnFly(
                            robotPosition,
                            new Translation2d(),
                            drivetrain.getState().Speeds,
                            facing,
                            Meters.of(HELD_PIECE_HEIGHT_M),
                            MetersPerSecond.of(6.0),
                            Degrees.of(55)));
        } else {
            heldCube--;
            SimulatedArena.getInstance()
                    .addGamePieceProjectile(new ChargedUpCubeOnFly(
                            robotPosition,
                            new Translation2d(),
                            drivetrain.getState().Speeds,
                            facing,
                            Meters.of(HELD_PIECE_HEIGHT_M),
                            MetersPerSecond.of(6.0),
                            Degrees.of(55)));
        }
    }

    /** Reloads one cone and one cube onto the robot. */
    private void reload() {
        heldCone = 1;
        heldCube = 1;
    }

    /** Pose of the held cone for visualization (empty array if none held). */
    public Pose3d[] getHeldConePoses() {
        return heldPiecePoses(heldCone);
    }

    /** Pose of the held cube for visualization (empty array if none held). */
    public Pose3d[] getHeldCubePoses() {
        return heldPiecePoses(heldCube);
    }

    private Pose3d[] heldPiecePoses(int count) {
        if (count <= 0 || !RobotBase.isSimulation()) return new Pose3d[0];
        Pose2d robotPose = drivetrain.getMapleSimPose();
        return new Pose3d[] {
            new Pose3d(
                    new Translation3d(robotPose.getX(), robotPose.getY(), HELD_PIECE_HEIGHT_M),
                    new Rotation3d(0, 0, robotPose.getRotation().getRadians()))
        };
    }

    /** Resets the field to its auto starting configuration (re-stages pieces, clears scored grids). */
    private void resetField() {
        if (!RobotBase.isSimulation()) return;
        SimulatedArena.getInstance().resetFieldForAuto();
        reload();
    }

    public Command getAutonomousCommand() {
        return Commands.none();
    }
}
