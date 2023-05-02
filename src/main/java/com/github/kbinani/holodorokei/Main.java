package com.github.kbinani.holodorokei;

import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {
    private World world;
    private boolean initialized = false;

    @Override
    public void onEnable() {
        Optional<World> overworld = getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
        if (overworld.isEmpty()) {
            getLogger().log(Level.SEVERE, "server should have at least one overworld dimension");
            setEnabled(false);
            return;
        }
        world = overworld.get();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(this, this);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getEntity().getWorld() != world) {
            return;
        }
        switch (e.getSpawnReason()) {
            case NATURAL:
            case VILLAGE_INVASION:
            case BUILD_WITHER:
            case BUILD_IRONGOLEM:
            case BUILD_SNOWMAN:
            case SPAWNER:
                e.setCancelled(true);
                break;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!initialized) {
            initialized = true;
            getServer().getScheduler().runTaskLater(this, this::setup, 20 * 5);
        }
    }

    private void setup() {
        Editor.WallSign(world, new Point3i(-15, -62, -14), BlockFace.NORTH, DyeColor.YELLOW, "ドロボウでエントリー", "");
        Editor.WallSign(world, new Point3i(-16, -62, -14), BlockFace.NORTH, DyeColor.RED, "ケイサツでエントリー", "（女幹部）");
        Editor.WallSign(world, new Point3i(-17, -62, -14), BlockFace.NORTH, DyeColor.RED, "ケイサツでエントリー", "（研究者）");
        Editor.WallSign(world, new Point3i(-18, -62, -14), BlockFace.NORTH, DyeColor.RED, "ケイサツでエントリー", "（掃除屋）");

        Editor.WallSign(world, new Point3i(7, -61, -14), BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（そらステーション）");
        Editor.WallSign(world, new Point3i(6, -61, -14), BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（しけ村）");
        Editor.WallSign(world, new Point3i(5, -61, -14), BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（ドドドタウン）");
        Editor.WallSign(world, new Point3i(4, -61, -14), BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（不知火建設本社）");

        Editor.WallSign(world, new Point3i(7, -62, -14), BlockFace.NORTH, DyeColor.CYAN, "通常版スタート");
        Editor.WallSign(world, new Point3i(6, -62, -14), BlockFace.NORTH, DyeColor.WHITE, "リセット");
        Editor.WallSign(world, new Point3i(5, -62, -14), BlockFace.NORTH, DyeColor.CYAN, "通常版スタート", "②");
        Editor.WallSign(world, new Point3i(4, -62, -14), BlockFace.NORTH, DyeColor.CYAN, "通常版スタート", "③");

        Editor.WallSign(world, new Point3i(6, -63, -14), BlockFace.NORTH, DyeColor.GREEN, "運営でエントリー");
        Editor.WallSign(world, new Point3i(5, -63, -14), BlockFace.NORTH, DyeColor.WHITE, "エントリー解除");
    }
}