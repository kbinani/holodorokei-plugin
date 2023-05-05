package com.github.kbinani.holodorokei;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Scheduler {
  private final JavaPlugin owner;

  Scheduler(JavaPlugin owner) {
    this.owner = owner;
  }

  BukkitTask runTaskLater(Runnable task, long delay) {
    return Bukkit.getScheduler().runTaskLater(owner, task, delay);
  }

  BukkitTask runTaskTimer(Runnable task, long delay, long period) {
    return Bukkit.getScheduler().runTaskTimer(owner, task, delay, period);
  }
}
