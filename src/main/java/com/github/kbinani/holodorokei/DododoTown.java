package com.github.kbinani.holodorokei;

import org.bukkit.World;
import org.bukkit.block.BlockFace;

public class DododoTown extends Area {
    DododoTown(World world) {
        super(world);
    }

    @Override
    String name() {
        return "ドドドタウン";
    }

    @Override
    ChestPosition[] chestPositionList() {
        return new ChestPosition[]{
                new ChestPosition(-91, -59, 65, BlockFace.SOUTH),
                new ChestPosition(-54, -59, 116, BlockFace.NORTH),
                new ChestPosition(-106, -59, 117, BlockFace.NORTH),
                new ChestPosition(-125, -59, 121, BlockFace.SOUTH),
                new ChestPosition(-133, -56, 100, BlockFace.NORTH),
                new ChestPosition(-114, -60, 101, BlockFace.NORTH),
                new ChestPosition(-110, -58, 71, BlockFace.EAST),
                new ChestPosition(-103, -61, 25, BlockFace.WEST),
        };
    }

    @Override
    Point3i[] beaconPositionList() {
        return new Point3i[]{
                new Point3i(-34, -60, 75),
                new Point3i(-32, -60, 77),
                new Point3i(-73, -60, 64),
                new Point3i(-85, -60, 64),
                new Point3i(-90, -60, 98),
                new Point3i(-88, -60, 100),
                new Point3i(-50, -59, 116),
                new Point3i(-73, -59, 117),
                new Point3i(-85, -59, 117),
                new Point3i(-131, -60, 55),
                new Point3i(-129, -60, 57),
        };
    }

    @Override
    Mission initializeMission(World world) {
        //TODO:
        return null;
    }
}
