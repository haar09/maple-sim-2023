package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.math.geometry.Pose2d;
import org.dyn4j.geometry.Rectangle;
import org.ironmaple.simulation.gamepieces.GamePieceOnFieldSimulation;

/**
 *
 *
 * <h1>Represents a CUBE in the 2023 Charged Up game.</h1>
 *
 * <p>A CUBE is a purple inflated PVC-fabric cube ~9.5 in (~24 cm) per side, weighing ~71 g. Soft and light; modeled
 * with a square footprint and high damping to approximate its low-energy behavior.
 */
public class ChargedUpCubeOnField extends GamePieceOnFieldSimulation {
    public static final GamePieceInfo CHARGED_UP_CUBE_INFO = new GamePieceInfo(
            "Cube", new Rectangle(0.241, 0.241), Meters.of(0.241), Kilograms.of(0.071), 3.0, 5.0, 0.1);

    public ChargedUpCubeOnField(Pose2d initialPose) {
        super(CHARGED_UP_CUBE_INFO, initialPose);
    }
}
