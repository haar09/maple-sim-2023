package org.ironmaple.simulation.seasonspecific.chargedup2023;

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
 * <h1>Represents a CONE launched into the air in the 2023 Charged Up game.</h1>
 *
 * <p>Used as the scoring mechanism: a robot ejects a cone toward a grid node, and the node
 * {@link org.ironmaple.simulation.Goal} detects it by 3D pose.
 */
public class ChargedUpConeOnFly extends GamePieceProjectile {
    public ChargedUpConeOnFly(
            Translation2d robotPosition,
            Translation2d shooterPositionOnRobot,
            ChassisVelocities chassisSpeeds,
            Rotation2d shooterFacing,
            Distance initialHeight,
            LinearVelocity launchingSpeed,
            Angle shooterAngle) {
        super(
                ChargedUpConeOnField.CHARGED_UP_CONE_INFO,
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
                ChargedUpConeOnField.CHARGED_UP_CONE_INFO,
                // Cone model is base-anchored: rest its base on the ground (lower bound 0), not at height/2.
                () -> Math.max(0.0, getPositionAtTime(super.launchedTimer.get()).getZ()),
                new Pose2d(
                        getPositionAtTime(launchedTimer.get()).toTranslation2d(),
                        initialLaunchingVelocityMPS.getAngle()),
                super.initialLaunchingVelocityMPS));
    }
}
