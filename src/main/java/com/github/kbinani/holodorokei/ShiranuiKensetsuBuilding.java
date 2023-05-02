package com.github.kbinani.holodorokei;

import org.bukkit.World;
import org.bukkit.block.BlockFace;

public class ShiranuiKensetsuBuilding extends Area {
    ShiranuiKensetsuBuilding(World world) {
        super(world);
    }

    @Override
    String name() {
        return "不知火建設本社";
    }

    @Override
    ChestPosition[] chestPositionList() {
        return new ChestPosition[]{
                new ChestPosition(45, -59, 50, BlockFace.SOUTH),
                new ChestPosition(34, -58, 69, BlockFace.WEST),
                new ChestPosition(49, -58, 66, BlockFace.SOUTH),
                new ChestPosition(64, -58, 45, BlockFace.WEST),
                new ChestPosition(30, -50, 62, BlockFace.EAST),
                new ChestPosition(50, -43, 54, BlockFace.WEST),
                new ChestPosition(51, -44, 49, BlockFace.WEST),
                new ChestPosition(15, -55, 60, BlockFace.EAST),
        };
    }

    @Override
    Point3i[] beaconPositionList() {
        return new Point3i[]{
            new Point3i(49, -59, 31),
                new Point3i(28, -58, 44),
                new Point3i(38, -58, 67),
                new Point3i(53, -58, 53),
                new Point3i(64, -58, 53),
                new Point3i(63, -58, 53),
                new Point3i(43, -58, 45),
                new Point3i(64, -58, 39),
                new Point3i(28, -59, 39),
                new Point3i(30, -50, 56),
                new Point3i(30, -50, 67),
                new Point3i(45, -51, 59),
                new Point3i(53, -58, 45),
        };
    }

    @Override
    Mission initializeMission(World world) {
        //TODO:
        return null;
    }
}
