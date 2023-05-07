package com.github.kbinani.holodorokei;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

public class Players {
  private Players() {
  }

  public static void Within(World world, BoundingBox[] boxes, Consumer<Player> callback) {
    Server server = Bukkit.getServer();
    server.getOnlinePlayers().forEach(player -> {
      Vector location = player.getLocation().toVector();
      for (BoundingBox box : boxes) {
        if (box.contains(location)) {
          callback.accept(player);
          break;
        }
      }
    });
  }

  public static void Within(World world, BoundingBox box, Consumer<Player> callback) {
    Within(world, new BoundingBox[]{box}, callback);
  }
}
