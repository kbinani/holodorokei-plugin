package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;

abstract class Mission {
    abstract boolean onPlayerInteract(PlayerInteractEvent e);
    abstract boolean onEntityMove(EntityMoveEvent e);
    abstract void reset();
}
