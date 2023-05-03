package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.World;
import org.bukkit.event.player.PlayerInteractEvent;

abstract class Mission {
  abstract boolean onPlayerInteract(PlayerInteractEvent e);

  abstract boolean onEntityMove(EntityMoveEvent e);

  abstract void start(World world);

  abstract void cleanup(World world);
}
