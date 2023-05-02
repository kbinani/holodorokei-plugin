package com.github.kbinani.holodorokei;

import org.bukkit.World;
import org.bukkit.block.BlockFace;

public class SoraStation extends Area {
    @Override
    String name() {
        return "そらステーション";
    }

    @Override
    ChestPosition[] chestPositionList() {
        return new ChestPosition[]{
                new ChestPosition(-75, -60, -51, BlockFace.SOUTH),
                new ChestPosition(-67, -59, -55, BlockFace.SOUTH),
                new ChestPosition(-78, -50, -45, BlockFace.NORTH),
                new ChestPosition(-86, -50, -78, BlockFace.EAST),
                new ChestPosition(-36, -50, -78, BlockFace.SOUTH),
                new ChestPosition(-28, -50, -67, BlockFace.WEST),
                new ChestPosition(-47, -59, -68, BlockFace.SOUTH),
                new ChestPosition(-49, -56, -89, BlockFace.EAST),
        };
    }

    @Override
    Point3i[] beaconPositionList() {
        return new Point3i[]{
                new Point3i(-89, -56, -32),
                new Point3i(-88, -56, -32),
                new Point3i(-52, -56, -36),
                new Point3i(-30, -57, -40),
                new Point3i(-41, -57, -75),
                new Point3i(-65, -59, -51),
                new Point3i(-70, -59, -51),
                new Point3i(-57, -50, -45),
                new Point3i(-52, -47, -65),
                new Point3i(-52, -47, -69),
                new Point3i(-59, -50, -87),
                new Point3i(-75, -50, -87),
        };
    }

    @Override
    Mission initializeMission(World world) {
        //TODO:
        return null;
    }
}
