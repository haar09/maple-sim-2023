package org.ironmaple.utils;

import java.util.Optional;
import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Translation2d;
import org.wpilib.math.geometry.Translation3d;

public class FieldMirroringUtils {
    public static final double FIELD_WIDTH = 16.54175;
    public static final double FIELD_HEIGHT = 8.0137;

    public static Rotation2d toCurrentAllianceRotation(Rotation2d rotationAtBlueSide) {
        return isSidePresentedAsRed() ? flip(rotationAtBlueSide) : rotationAtBlueSide;
    }

    // 2023 Charged Up is a mirror-symmetric field (mirror across the vertical centerline), NOT 180-degree
    // rotational like 2024+. Red alliance = (fieldLength - x, y) with rotation reflected about the Y axis.
    public static Rotation2d flip(Rotation2d rotation) {
        return new Rotation2d(-rotation.getCos(), rotation.getSin());
    }

    public static Translation2d toCurrentAllianceTranslation(Translation2d translationAtBlueSide) {
        return isSidePresentedAsRed() ? flip(translationAtBlueSide) : translationAtBlueSide;
    }

    public static Translation2d flip(Translation2d translation) {
        return new Translation2d(FIELD_WIDTH - translation.getX(), translation.getY());
    }

    // Ported from upstream 3bf85bd ("Fixed red hub recycling of fuel"): flip(Pose3d) must also flip the
    // rotation. Adapted from 180-degree rotational symmetry to 2023's mirror symmetry: yaw is reflected
    // about the Y axis (theta -> pi - theta), roll is negated, pitch is unchanged.
    public static Pose3d flip(Pose3d toFlip) {
        return new Pose3d(
                new Translation3d(FIELD_WIDTH - toFlip.getX(), toFlip.getY(), toFlip.getZ()),
                new Rotation3d(
                        -toFlip.getRotation().getX(),
                        toFlip.getRotation().getY(),
                        Math.PI - toFlip.getRotation().getZ()));
    }

    public static Translation3d toCurrentAllianceTranslation(Translation3d translation3dAtBlueSide) {
        final Translation2d translation3dAtCurrentAlliance =
                toCurrentAllianceTranslation(translation3dAtBlueSide.toTranslation2d());
        if (isSidePresentedAsRed())
            return new Translation3d(
                    translation3dAtCurrentAlliance.getX(),
                    translation3dAtCurrentAlliance.getY(),
                    translation3dAtBlueSide.getZ());
        return translation3dAtBlueSide;
    }

    public static Pose2d toCurrentAlliancePose(Pose2d poseAtBlueSide) {
        return new Pose2d(
                toCurrentAllianceTranslation(poseAtBlueSide.getTranslation()),
                toCurrentAllianceRotation(poseAtBlueSide.getRotation()));
    }

    public static boolean isSidePresentedAsRed() {
        final Optional<Alliance> alliance = MatchState.getAlliance();
        return alliance.isPresent() && alliance.get().equals(Alliance.RED);
    }

    public static Rotation2d getCurrentAllianceDriverStationFacing() {
        return toCurrentAllianceRotation(Rotation2d.kZero);
    }
}
