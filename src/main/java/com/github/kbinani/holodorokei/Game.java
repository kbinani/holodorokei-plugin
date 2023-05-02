package com.github.kbinani.holodorokei;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;

public class Game {
    final GameSetting setting;
    final World world;

    Game(World world, GameSetting setting) {
        this.world = world;
        this.setting = setting;
    }

    void start() {
        for (var pos : new Point3i[]{kContainerChestDeliveryPost, kContainerHopperDeliveryPost}) {
            Block block = world.getBlockAt(pos.x, pos.y, pos.z);
            BlockState state = block.getState();
            if (!(state instanceof Container container)) {
                continue;
            }
            Inventory inventory = container.getInventory();
            inventory.clear();
        }
    }

    int getNumCops() {
        return this.setting.getNumCops();
    }

    int getNumThieves() {
        return this.setting.getNumThieves();
    }

    void terminate() {
        setting.reset();
    }

    private final Point3i kContainerChestDeliveryPost = new Point3i(-5, -60, -25);
    private final Point3i kContainerHopperDeliveryPost = new Point3i(-5, -61, -25);
}