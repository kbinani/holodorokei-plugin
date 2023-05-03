package com.github.kbinani.holodorokei;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.HashSet;
import java.util.Set;

public class Bar {
  private final World world;
  private final BoundingBox box;
  private final BossBar instance;
  private final Set<Player> players = new HashSet<>();

  Bar(World world, BoundingBox box) {
    this.world = world;
    this.box = box;
    instance = BossBar.bossBar(Component.empty(), 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
  }

  void cleanup() {
    players.forEach(p -> p.hideBossBar(instance));
    players.clear();
  }

  void update() {
    var dangling = new HashSet<>(players);
    players.clear();
    Players.Within(world, new BoundingBox[]{box}, p -> {
      if (dangling.contains(p)) {
        dangling.remove(p);
        players.add(p);
        return;
      }
      p.showBossBar(instance);
      players.add(p);
    });
    dangling.forEach(p -> p.hideBossBar(instance));
  }

  void name(Component c) {
    instance.name(c);
  }

  void progress(float p) {
    instance.progress(Math.min(Math.max(p, 0), 1));
  }

  void color(BossBar.Color color) {
    instance.color(color);
  }
}
