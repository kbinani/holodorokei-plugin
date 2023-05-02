package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
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
}
