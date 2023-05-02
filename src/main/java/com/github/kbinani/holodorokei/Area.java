package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public abstract class Area {
    static class ChestPosition {
        final Point3i position;
        final BlockFace facing;
        ChestPosition(int x, int y, int z, BlockFace facing) {
            this.position = new Point3i(x, y, z);
            this.facing = facing;
        }
    }

    protected Mission mission;

    abstract String name();
    abstract ChestPosition[] chestPositionList();
    abstract Point3i[] beaconPositionList();
    abstract Mission initializeMission(World world);

    void initialize(World world) {
        for (var p : chestPositionList()) {
            BlockData blockData = Material.CHEST.createBlockData("[facing=" + p.facing.name().toLowerCase() + "]");
            world.setBlockData(p.position.x, p.position.y, p.position.z, blockData);
            Block block = world.getBlockAt(p.position.x, p.position.y, p.position.z);
            BlockState state = block.getState();
            if (!(state instanceof Chest chest)) {
                continue;
            }
            Inventory inventory = chest.getInventory();
            inventory.clear();
        }
        for (var p : beaconPositionList()) {
            BlockData blockData = Material.BEACON.createBlockData();
            world.setBlockData(p.x, p.y, p.z, blockData);
        }
    }

    void reset(World world) {
        if (mission != null) {
            mission.reset();
            mission = null;
        }
        for (var p : chestPositionList()) {
            BlockData blockData = Material.AIR.createBlockData();
            world.setBlockData(p.position.x, p.position.y, p.position.z, blockData);
        }
        for (var p : beaconPositionList()) {
            BlockData blockData = Material.AIR.createBlockData();
            world.setBlockData(p.x, p.y, p.z, blockData);
        }
    }

    boolean onPlayerInteract(PlayerInteractEvent e) {
        //TODO:
        return false;
    }

    boolean onEntityMove(EntityMoveEvent e) {
        //TODO:
        return false;
    }
}
