package com.github.kbinani.holodorokei;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener {
    private World world;

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
}