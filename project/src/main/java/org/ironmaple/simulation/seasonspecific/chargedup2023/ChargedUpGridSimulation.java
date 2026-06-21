package org.ironmaple.simulation.seasonspecific.chargedup2023;

import static edu.wpi.first.units.Units.Meters;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.ArrayList;
import java.util.List;
import org.ironmaple.simulation.SimulatedArena;
import org.ironmaple.utils.FieldMirroringUtils;

/**
 *
 *
 * <h2>Simulates one alliance's set of GRIDS in the 2023 Charged Up game.</h2>
 *
 * <p>Owns the 27 scoring {@link ChargedUpNode}s (9 columns x 3 rows; hybrid columns are a co-located cone+cube pair),
 * ticks and draws them, and detects LINKs (3 adjacent scored nodes in a row).
 */
public class ChargedUpGridSimulation implements SimulatedArena.Simulatable {
    // Node X by row (m): hybrid, mid, high
    private static final double[] NODE_X = {1.196975, 0.800100, 0.368300};
    // Node Y by column 0..8 (m)
    private static final double[] NODE_Y = {
        0.512826, 1.071626, 1.630426, 2.189226, 2.748026, 3.306826, 3.865626, 4.424426, 4.983226
    };
    // Z by row/type; hybrid Z is 0 for both
    private static final double MID_CONE_Z = 0.863600, MID_CUBE_Z = 0.520700;
    private static final double HIGH_CONE_Z = 1.168400, HIGH_CUBE_Z = 0.825500;

    private final boolean isBlue;
    private final List<ChargedUpNode> nodes = new ArrayList<>();
    // [row 0..2][col 0..8] -> the node used for occupancy/LINK checks
    private final ChargedUpNode[][] occupancyByRowCol = new ChargedUpNode[3][9];

    public ChargedUpGridSimulation(Arena2023ChargedUp arena, boolean isBlue) {
        this.isBlue = isBlue;

        for (int col = 0; col < 9; col++) {
            boolean isCubeColumn = (col == 1 || col == 4 || col == 7);

            // Hybrid (floor): accepts both types -> two co-located nodes; cube node tracks occupancy.
            ChargedUpNode hybridCone = makeNode(arena, ChargedUpNode.ROW_HYBRID, "Cone", col, 0.0, true);
            ChargedUpNode hybridCube = makeNode(arena, ChargedUpNode.ROW_HYBRID, "Cube", col, 0.0, true);
            nodes.add(hybridCone);
            nodes.add(hybridCube);
            occupancyByRowCol[0][col] = hybridCube;

            // Mid
            String midType = isCubeColumn ? "Cube" : "Cone";
            double midZ = isCubeColumn ? MID_CUBE_Z : MID_CONE_Z;
            ChargedUpNode mid = makeNode(arena, ChargedUpNode.ROW_MID, midType, col, midZ, false);
            nodes.add(mid);
            occupancyByRowCol[1][col] = mid;

            // High
            String highType = isCubeColumn ? "Cube" : "Cone";
            double highZ = isCubeColumn ? HIGH_CUBE_Z : HIGH_CONE_Z;
            ChargedUpNode high = makeNode(arena, ChargedUpNode.ROW_HIGH, highType, col, highZ, false);
            nodes.add(high);
            occupancyByRowCol[2][col] = high;
        }
    }

    private ChargedUpNode makeNode(
            Arena2023ChargedUp arena, int row, String type, int col, double z, boolean allowGrounded) {
        Translation2d xy = new Translation2d(NODE_X[row], NODE_Y[col]);
        if (!isBlue) xy = FieldMirroringUtils.flip(xy);
        Translation3d position = new Translation3d(xy.getX(), xy.getY(), z);
        double boxXY = row == ChargedUpNode.ROW_HYBRID ? 0.5 : 0.3;
        return new ChargedUpNode(arena, isBlue, row, type, position, Meters.of(boxXY), Meters.of(0.3), allowGrounded);
    }

    @Override
    public void simulationSubTick(int subTickNum) {
        for (ChargedUpNode node : nodes) node.simulationSubTick(subTickNum);
    }

    /**
     *
     *
     * <h2>Draws scored pieces of the given type for AdvantageScope.</h2>
     *
     * @param drawList the list of poses to append to
     * @param type the game piece type being drawn ("Cone" or "Cube")
     */
    public void draw(List<Pose3d> drawList, String type) {
        for (ChargedUpNode node : nodes) {
            if (node.type.equals(type)) node.draw(drawList);
        }
    }

    /**
     *
     *
     * <h2>Clears all scored pieces from this grid.</h2>
     */
    public void clearGrid() {
        for (ChargedUpNode node : nodes) node.clear();
    }

    /**
     *
     *
     * <h2>Counts LINKs for this alliance.</h2>
     *
     * <p>A LINK is 3 adjacent scored nodes in a single row. Counted greedily left-to-right with non-overlapping groups
     * (up to 3 per row).
     *
     * @return the total number of LINKs across all three rows
     */
    public int countLinks() {
        int links = 0;
        for (int row = 0; row < 3; row++) {
            int run = 0;
            for (int col = 0; col < 9; col++) {
                if (occupancyByRowCol[row][col].getGamePieceCount() > 0) {
                    run++;
                    if (run == 3) {
                        links++;
                        run = 0;
                    }
                } else {
                    run = 0;
                }
            }
        }
        return links;
    }
}
