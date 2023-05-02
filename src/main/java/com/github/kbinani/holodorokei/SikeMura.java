package com.github.kbinani.holodorokei;

import org.bukkit.World;
import org.bukkit.block.BlockFace;

public class SikeMura extends Area {
    SikeMura(World world) {
        super(world);
    }

    @Override
    String name() {
        return "しけ村";
    }

    @Override
    ChestPosition[] chestPositionList() {
        return new  ChestPosition[]{
                new ChestPosition(13, -60, -41, BlockFace.EAST),
                new ChestPosition(45, -60, -28, BlockFace.EAST),
                new ChestPosition(74, -60, -51, BlockFace.SOUTH),
                new ChestPosition(31, -58, -99, BlockFace.WEST),
                new ChestPosition(14, -60, -98, BlockFace.NORTH),
                new ChestPosition(44, -60, -75, BlockFace.NORTH),
                new ChestPosition(44, -59, -67, BlockFace.SOUTH),
        };
    }

    @Override
    Point3i[] beaconPositionList() {
        return new Point3i[]{
                new Point3i(45, -60, -44),
                new Point3i(44, -60, -59),
                new Point3i(45, -60, -59),
                new Point3i(33, -60, -74),
                new Point3i(24, -58, -97),
                new Point3i(25, -58, -97),
                new Point3i(50, -60, -89),
                new Point3i(62, -57, -75),
                new Point3i(38, -57, -75),
                new Point3i(63, -60, -40),
                new Point3i(69, -60, -61),
                new Point3i(64, -60, -61),
                new Point3i(62, -56, -63),
        };
    }

    @Override
    Mission initializeMission(World world) {
        //TODO:
        return null;
    }
}
