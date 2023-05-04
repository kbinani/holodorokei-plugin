package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nonnull;

public class Teams {
  final @Nonnull Team thief;
  final @Nonnull Team cop;
  final @Nonnull Team prisoner;
  final @Nonnull Team manager;

  private static Teams instance;

  static @Nonnull Teams Instance() {
    if (instance == null) {
      instance = new Teams();
    }
    return instance;
  }

  Teams() {
    var prefix = "holodorokei_";
    thief = EnsureTeam(prefix + "thief");
    cop = EnsureTeam(prefix + "cop");
    prisoner = EnsureTeam(prefix + "prisoner");
    manager = EnsureTeam(prefix + "manager");

    thief.color(NamedTextColor.YELLOW);
    cop.color(NamedTextColor.RED);
    prisoner.color(NamedTextColor.DARK_PURPLE);
    manager.color(NamedTextColor.GREEN);
  }

  static void Reset() {
    instance.thief.unregister();
    instance.cop.unregister();
    instance.prisoner.unregister();
    instance.manager.unregister();

    instance = new Teams();
  }

  private static @Nonnull Team EnsureTeam(String name) {
    Scoreboard scoreboard = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
    var team = scoreboard.getTeam(name);
    if (team == null) {
      team = scoreboard.registerNewTeam(name);
    }
    return team;
  }
}
