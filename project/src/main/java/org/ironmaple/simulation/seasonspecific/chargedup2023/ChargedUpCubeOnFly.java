package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static org.wpilib.units.Units.*;

import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.simulation.gamepieces.GamePieceOnFieldSimulation;
import org.ironmaple.simulation.gamepieces.GamePieceProjectile;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.kinematics.ChassisVelocities;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Distance;
import org.wpilib.units.measure.LinearVelocity;

/**
 *
 *
 * <h1>Represents a CUBE launched into the air in the 2023 Charged Up game.</h1>
 *
 * <p>Used as the scoring mechanism: a robot ejects a cube toward a grid node, and the node
 * {@link org.ironmaple.simulation.Goal} detects it by 3D pose.
 */
public class ChargedUpCubeOnFly extends GamePieceProjectile {
    public ChargedUpCubeOnFly(
            Translation2d robotPosition,
            Translation2d shooterPositionOnRobot,
            ChassisVelocities chassisSpeeds,
            Rotation2d shooterFacing,
            Distance initialHeight,
            LinearVelocity launchingSpeed,
            Angle shooterAngle) {
        super(
                ChargedUpCubeOnField.CHARGED_UP_CUBE_INFO,
                robotPosition,
                shooterPositionOnRobot,
                chassisSpeeds,
                shooterFacing,
                initialHeight,
                launchingSpeed,
                shooterAngle);
        super.enableBecomesGamePieceOnFieldAfterTouchGround();
        super.withTouchGroundHeight(0.2);
    }

    @Override
    public void addGamePieceAfterTouchGround(SimulatedArena simulatedArena) {
        if (!super.becomesGamePieceOnGroundAfterTouchGround) return;
        simulatedArena.addGamePiece(new GamePieceOnFieldSimulation(
                ChargedUpCubeOnField.CHARGED_UP_CUBE_INFO,
                () -> Math.max(
                        ChargedUpCubeOnField.CHARGED_UP_CUBE_INFO
                                        .gamePieceHeight()
                                        .in(Meters)
                                / 2,
                        getPositionAtTime(super.launchedTimer.get()).getZ()),
                new Pose2d(
                        getPositionAtTime(launchedTimer.get()).toTranslation2d(),
                        initialLaunchingVelocityMPS.getAngle()),
                super.initialLaunchingVelocityMPS));
    }
}
