// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix6.Utils;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StructArrayPublisher;
import edu.wpi.first.networktables.StructPublisher;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import java.util.Arrays;
import java.util.stream.Stream;
import org.ironmaple.simulation.SimulatedArena;

public class Robot extends TimedRobot {
    private Command m_autonomousCommand;

    private final RobotContainer m_robotContainer;

    private final boolean kUseLimelight = false;

    public Robot() {
        m_robotContainer = new RobotContainer();
    }

    private final StructArrayPublisher<Pose3d> conePosePublisher = NetworkTableInstance.getDefault()
            .getStructArrayTopic("Simulation/ConePoses", Pose3d.struct)
            .publish();
    private final StructArrayPublisher<Pose3d> cubePosePublisher = NetworkTableInstance.getDefault()
            .getStructArrayTopic("Simulation/CubePoses", Pose3d.struct)
            .publish();
    // The maple-sim physics pose — the one that actually collides with field obstacles. Compare this against
    // odometry to see collisions; odometry alone walks through walls.
    private final StructPublisher<Pose2d> drivetrainPosePublisher = NetworkTableInstance.getDefault()
            .getStructTopic("Simulation/DrivetrainPose", Pose2d.struct)
            .publish();

    @Override
    public void robotInit() {}

    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();

        /*
         * This example of adding Limelight is very simple and may not be sufficient for on-field use.
         * Users typically need to provide a standard deviation that scales with the distance to target
         * and changes with number of tags available.
         *
         * This example is sufficient to show that vision integration is possible, though exact implementation
         * of how to use vision should be tuned per-robot and to the team's specification.
         */
        if (kUseLimelight) {
            var llMeasurement = LimelightHelpers.getBotPoseEstimate_wpiBlue("limelight");
            if (llMeasurement != null) {
                m_robotContainer.drivetrain.addVisionMeasurement(
                        llMeasurement.pose, Utils.fpgaToCurrentTime(llMeasurement.timestampSeconds));
            }
        }
    }

    @Override
    public void disabledInit() {}

    @Override
    public void disabledPeriodic() {}

    @Override
    public void disabledExit() {}

    @Override
    public void autonomousInit() {
        m_autonomousCommand = m_robotContainer.getAutonomousCommand();

        if (m_autonomousCommand != null) {
            m_autonomousCommand.schedule();
        }
    }

    @Override
    public void autonomousPeriodic() {}

    @Override
    public void autonomousExit() {}

    @Override
    public void teleopInit() {
        if (m_autonomousCommand != null) {
            m_autonomousCommand.cancel();
        }
    }

    @Override
    public void teleopPeriodic() {}

    @Override
    public void teleopExit() {}

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
    }

    @Override
    public void testPeriodic() {}

    @Override
    public void testExit() {}

    @Override
    public void simulationPeriodic() {
        SimulatedArena arena = SimulatedArena.getInstance();
        // Field pieces plus the pieces currently held on the robot, so the preload is visible.
        conePosePublisher.set(Stream.concat(
                        Arrays.stream(arena.getGamePiecesArrayByType("Cone")),
                        Arrays.stream(m_robotContainer.getHeldConePoses()))
                .toArray(Pose3d[]::new));
        cubePosePublisher.set(Stream.concat(
                        Arrays.stream(arena.getGamePiecesArrayByType("Cube")),
                        Arrays.stream(m_robotContainer.getHeldCubePoses()))
                .toArray(Pose3d[]::new));
        SmartDashboard.putNumber("Simulation/BlueScore", arena.getScore(true));
        SmartDashboard.putNumber("Simulation/RedScore", arena.getScore(false));
        drivetrainPosePublisher.set(m_robotContainer.drivetrain.getMapleSimPose());
    }
}
