package org.ironmaple.simulation.seasonspecific.chargedup2023;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.wpilibj.DriverStation;
import java.util.List;
import org.ironmaple.simulation.Goal;

/**
 *
 *
 * <h2>A single scoring NODE in a 2023 Charged Up GRID.</h2>
 *
 * <p>Each node is a height-aware {@link Goal}: a game piece scores when its 3D pose falls within the node's box. Hybrid
 * (floor) nodes accept grounded pieces; mid and high nodes require the piece to arrive in the air (as a projectile).
 */
public class ChargedUpNode extends Goal {
    public static final int ROW_HYBRID = 0;
    public static final int ROW_MID = 1;
    public static final int ROW_HIGH = 2;

    // Points per scored piece, indexed by row [hybrid, mid, high].
    private static final int[] AUTO_POINTS_BY_ROW = {3, 4, 6};
    private static final int[] TELEOP_POINTS_BY_ROW = {2, 3, 5};

    private static final double CUBE_HEIGHT_M = 0.241;

    public final int row;
    public final String type;

    public ChargedUpNode(
            Arena2023ChargedUp arena,
            boolean isBlue,
            int row,
            String type,
            Translation3d position,
            Distance boxXY,
            Distance boxHeight,
            boolean allowGrounded) {
        super(arena, boxXY, boxXY, boxHeight, type, position, isBlue, 1, allowGrounded);
        this.row = row;
        this.type = type;
    }

    @Override
    protected void addPoints() {
        boolean isAuto = DriverStation.isAutonomous();
        int points = isAuto ? AUTO_POINTS_BY_ROW[row] : TELEOP_POINTS_BY_ROW[row];
        arena.addToScore(isBlue, points);
        if (isAuto) arena.addValueToMatchBreakdown(isBlue, "Auto/" + type + "sScoredInAuto", 1);
    }

    @Override
    public void draw(List<Pose3d> drawList) {
        if (gamePieceCount <= 0) return;
        // Cube model is center-anchored (raise by half its height); cone model is base-anchored (rest on surface).
        double z = position.getZ() + (type.equals("Cube") ? CUBE_HEIGHT_M / 2.0 : 0.0);
        drawList.add(new Pose3d(new Translation3d(position.getX(), position.getY(), z), new Rotation3d()));
    }
}
