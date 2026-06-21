package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.*;

import edu.wpi.first.math.geometry.Pose2d;
import org.dyn4j.geometry.Rectangle;
import org.ironmaple.simulation.gamepieces.GamePieceOnFieldSimulation;

/**
 *
 *
 * <h1>Represents a CONE in the 2023 Charged Up game.</h1>
 *
 * <p>A CONE is a yellow rubber marker cone ~13 in (~33 cm) tall with an 8 in (~20 cm) square base, weighing ~653 g.
 * Modeled with a square footprint matching the base.
 */
public class ChargedUpConeOnField extends GamePieceOnFieldSimulation {
    public static final GamePieceInfo CHARGED_UP_CONE_INFO =
            new GamePieceInfo("Cone", new Rectangle(0.203, 0.203), Meters.of(0.33), Kilograms.of(0.653), 2.0, 4.0, 0.2);

    public ChargedUpConeOnField(Pose2d initialPose) {
        super(CHARGED_UP_CONE_INFO, initialPose);
    }
}
