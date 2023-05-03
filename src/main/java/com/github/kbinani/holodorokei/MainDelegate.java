package com.github.kbinani.holodorokei;

import org.bukkit.plugin.java.JavaPlugin;

public interface MainDelegate {
  JavaPlugin mainDelegateGetOwner();

  void mainDelegateDidFinishGame();
}
