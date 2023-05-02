package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;

import javax.annotation.Nonnull;

class Editor {
    public static void WallSign(@Nonnull World world, Point3i p, BlockFace facing, DyeColor color, String line2, String line3) {
        int cx = p.x >> 4;
        int cz = p.z >> 4;
        world.loadChunk(cx, cz);
        BlockData blockData = Material.BIRCH_WALL_SIGN.createBlockData("[facing=" + facing.name().toLowerCase() + "]");
        world.setBlockData(p.x, p.y, p.z, blockData);
        Block block = world.getBlockAt(p.x, p.y, p.z);
        BlockState state = block.getState();
        if (!(state instanceof Sign sign)) {
            return;
        }
        sign.line(1, Component.text("[看板を右クリック]").decorate(TextDecoration.BOLD));
        if (!line2.isEmpty()) {
            sign.line(2, Component.text(line2));
        }
        if (!line3.isEmpty()) {
            sign.line(3, Component.text(line3));
        }
        sign.setGlowingText(true);
        sign.setColor(color);
        sign.update();
    }

    public static void WallSign(@Nonnull World world, Point3i p, BlockFace facing, DyeColor color, String line2) {
        WallSign(world, p, facing, color, line2, "");
    }

    public static void Fill(@Nonnull World world, Point3i from, Point3i to, String blockDataString) {
        Server server = Bukkit.getServer();
        BlockData blockData = null;
        try {
            blockData = server.createBlockData(blockDataString);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            return;
        }
        int x0 = Math.min(from.x, to.x);
        int y0 = Math.min(from.y, to.y);
        int z0 = Math.min(from.z, to.z);
        int x1 = Math.max(from.x, to.x);
        int y1 = Math.max(from.y, to.y);
        int z1 = Math.max(from.z, to.z);
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    world.setBlockData(x, y, z, blockData);
                }
            }
        }
    }
}
