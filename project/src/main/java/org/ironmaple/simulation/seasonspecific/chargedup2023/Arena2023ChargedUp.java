package org.ironmaple.simulation.seasonspecific.chargedup2023;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.utils.FieldMirroringUtils;

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
        private static final double GRID_DEPTH_X = 1.377950;
        private static final double[] DIVIDER_Y = {
            0.792226, 1.351026, 1.909826, 2.468626, 3.027426, 3.586226, 4.145026, 4.703826
        };
        private static final double DIVIDER_THICKNESS_Y = 0.127000;

        private static final double CHARGE_STATION_CENTER_X = 3.888625;
        private static final double CHARGE_STATION_CENTER_Y = 2.748030;
        private static final double CHARGE_STATION_SIZE_X = 1.938275;
        private static final double CHARGE_STATION_SIZE_Y = 2.479044;

        private static final double BARRIER_CENTER_X = 1.117600;
        private static final double BARRIER_CENTER_Y = 5.499100;
        private static final double BARRIER_SIZE_X = 2.235200;
        private static final double BARRIER_SIZE_Y = 0.406400;

        private static final double DOUBLE_SUBSTATION_CENTER_X = 16.363950;
        private static final double DOUBLE_SUBSTATION_CENTER_Y = 6.749796;
        private static final double DOUBLE_SUBSTATION_SIZE_X = 0.355600;
        private static final double DOUBLE_SUBSTATION_SIZE_Y = 2.438400;

        private final boolean chargeStationSolid;

        public ChargedUpFieldObstaclesMap(boolean chargeStationSolid) {
            super();
            this.chargeStationSolid = chargeStationSolid;

            // Perimeter
            addBorderLine(new Translation2d(0, 0), new Translation2d(FIELD_LENGTH, 0));
            addBorderLine(new Translation2d(0, FIELD_WIDTH), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));
            addBorderLine(new Translation2d(0, 0), new Translation2d(0, FIELD_WIDTH));
            addBorderLine(new Translation2d(FIELD_LENGTH, 0), new Translation2d(FIELD_LENGTH, FIELD_WIDTH));

            addBlueObstacles();
            addRedObstacles();
        }

        private void addBlueObstacles() {
            addGridDividers(false);
            addChargeStationCollider(false);
            addRectangularObstacle(
                    BARRIER_SIZE_X, BARRIER_SIZE_Y, new Pose2d(BARRIER_CENTER_X, BARRIER_CENTER_Y, new Rotation2d()));
            // Red's double substation physically sits at the blue end (flip of blue's definition).
            Translation2d redDsCenter =
                    FieldMirroringUtils.flip(new Translation2d(DOUBLE_SUBSTATION_CENTER_X, DOUBLE_SUBSTATION_CENTER_Y));
            addRectangularObstacle(
                    DOUBLE_SUBSTATION_SIZE_X, DOUBLE_SUBSTATION_SIZE_Y, new Pose2d(redDsCenter, new Rotation2d()));
        }

        private void addRedObstacles() {
            addGridDividers(true);
            addChargeStationCollider(true);
            Translation2d barrierCenter =
                    FieldMirroringUtils.flip(new Translation2d(BARRIER_CENTER_X, BARRIER_CENTER_Y));
            addRectangularObstacle(BARRIER_SIZE_X, BARRIER_SIZE_Y, new Pose2d(barrierCenter, new Rotation2d()));
            // Blue's double substation physically sits at the red end (un-flipped blue definition).
            addRectangularObstacle(
                    DOUBLE_SUBSTATION_SIZE_X,
                    DOUBLE_SUBSTATION_SIZE_Y,
                    new Pose2d(DOUBLE_SUBSTATION_CENTER_X, DOUBLE_SUBSTATION_CENTER_Y, new Rotation2d()));
        }

        private void addGridDividers(boolean isRed) {
            for (double dividerY : DIVIDER_Y) {
                Translation2d center = new Translation2d(GRID_DEPTH_X / 2.0, dividerY);
                if (isRed) center = FieldMirroringUtils.flip(center);
                addRectangularObstacle(GRID_DEPTH_X, DIVIDER_THICKNESS_Y, new Pose2d(center, new Rotation2d()));
            }
        }

        /**
         * Adds the charge-station collider. Isolated so the future 3D charge-station layer can override or replace it
         * without touching the rest of the obstacle map.
         */
        private void addChargeStationCollider(boolean isRed) {
            if (!chargeStationSolid) return;
            Translation2d center = new Translation2d(CHARGE_STATION_CENTER_X, CHARGE_STATION_CENTER_Y);
            if (isRed) center = FieldMirroringUtils.flip(center);
            addRectangularObstacle(CHARGE_STATION_SIZE_X, CHARGE_STATION_SIZE_Y, new Pose2d(center, new Rotation2d()));
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
