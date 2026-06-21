package org.ironmaple.simulation.seasonspecific.chargedup2023;

import edu.wpi.first.math.geometry.Translation2d;
import org.ironmaple.simulation.SimulatedArena;

/**
 *
 *
 * <h2>The Simulation Arena for the 2023 FRC Game: CHARGED UP.</h2>
 *
 * <p>Models the 2023 field: perimeter, grids (enterable bays with non-passable dividers), charge stations, barriers,
 * and double-substation protrusions. The charge station is a static collider; balance/dock simulation is intentionally
 * out of scope (a dedicated 3D layer is planned).
 */
public class Arena2023ChargedUp extends SimulatedArena {
    public static final double FIELD_LENGTH = 16.54175;
    public static final double FIELD_WIDTH = 8.0137;

    /** The obstacles on the 2023 Charged Up competition field. */
    public static final class ChargedUpFieldObstaclesMap extends FieldMap {
        public ChargedUpFieldObstaclesMap(boolean chargeStationSolid) {
            super();

            // Perimeter
            addBorderLine(new Translation2d(0, 0), new Translation2d(FIELD_LENGTH, 0));
            addBorderLine(new Translation2d(0, FIELD_WIDTH), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));
            addBorderLine(new Translation2d(0, 0), new Translation2d(0, FIELD_WIDTH));
            addBorderLine(new Translation2d(FIELD_LENGTH, 0), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));
        }
    }

    /**
     *
     *
     * <h2>Creates the 2023 Charged Up arena with a solid charge-station collider.</h2>
     */
    public Arena2023ChargedUp() {
        this(true);
    }

    /**
     *
     *
     * <h2>Creates the 2023 Charged Up arena.</h2>
     *
     * @param chargeStationSolid whether the charge-station footprint is an impassable collider
     */
    public Arena2023ChargedUp(boolean chargeStationSolid) {
        super(new ChargedUpFieldObstaclesMap(chargeStationSolid));
    }

    @Override
    public void placeGamePiecesOnField() {}
}
