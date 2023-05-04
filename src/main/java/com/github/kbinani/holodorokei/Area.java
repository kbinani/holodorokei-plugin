package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Area {
  static class ChestPosition {
    final Point3i position;
    final BlockFace facing;

    ChestPosition(int x, int y, int z, BlockFace facing) {
      this.position = new Point3i(x, y, z);
      this.facing = facing;
    }
  }

  protected final World world;

  protected Mission mission;
  boolean missionStarted = false;
  boolean missionCompleted = false;
  private static @Nullable ItemStack sPlayerHead;

  abstract ChestPosition[] chestPositionList();

  abstract Point3i[] beaconPositionList();

  abstract Mission initializeMission(World world);

  abstract AreaType type();

  Area(World world) {
    this.world = world;
  }

  private static @Nonnull ItemStack CreateHoloXerHead() {
    if (sPlayerHead == null) {
      var item = new ItemStack(Material.PLAYER_HEAD);
      if (item.getItemMeta() instanceof SkullMeta skull) {
        var server = Bukkit.getServer();
        var staff1 = server.getOfflinePlayer("UNEI_Staff1");
        skull.setOwningPlayer(staff1);
        skull.displayName(Component.text("holoXerの頭"));
        item.setItemMeta(skull);
      }
      sPlayerHead = item;
    }
    return sPlayerHead.clone();
  }

  void initialize() {
    for (var p : chestPositionList()) {
      var blockData = Material.CHEST.createBlockData("[facing=" + p.facing.name().toLowerCase() + "]");
      world.setBlockData(p.position.x, p.position.y, p.position.z, blockData);
      var block = world.getBlockAt(p.position.x, p.position.y, p.position.z);
      if (!(block.getState() instanceof Chest chest)) {
        continue;
      }
      var inventory = chest.getInventory();
      inventory.clear();
      var item = CreateHoloXerHead();
      inventory.setItem(13, item);
    }
    //TODO: チェストのうちどれか 1 個だけ納品アイテムを入れる
    for (var p : beaconPositionList()) {
      BlockData blockData = Material.BEACON.createBlockData();
      world.setBlockData(p.x, p.y, p.z, blockData);
    }
    mission = initializeMission(world);
    if (mission != null) {
      mission.cleanup(world);
    }
  }

  void reset() {
    if (mission != null) {
      mission.cleanup(world);
      mission = null;
    }
    missionStarted = false;
    missionCompleted = false;
    for (var p : chestPositionList()) {
      BlockData blockData = Material.AIR.createBlockData();
      world.setBlockData(p.position.x, p.position.y, p.position.z, blockData);
    }
    for (var p : beaconPositionList()) {
      BlockData blockData = Material.AIR.createBlockData();
      world.setBlockData(p.x, p.y, p.z, blockData);
    }
  }

  void startMission() {
    if (missionStarted || missionCompleted) {
      return;
    }
    mission.start(world);
    missionStarted = true;
  }

  boolean onPlayerInteract(PlayerInteractEvent e) {
    if (mission == null) {
      return false;
    }
    if (missionCompleted) {
      return false;
    }
    if (!missionStarted) {
      return false;
    }
    var cleared = mission.onPlayerInteract(e);
    if (cleared) {
      mission.cleanup(world);
      missionCompleted = true;
    }
    return cleared;
  }

  boolean onEntityMove(EntityMoveEvent e) {
    if (mission == null) {
      return false;
    }
    if (missionCompleted) {
      return false;
    }
    if (!missionStarted) {
      return false;
    }
    var cleared = mission.onEntityMove(e);
    if (cleared) {
      mission.cleanup(world);
      missionCompleted = true;
    }
    return cleared;
  }
}
