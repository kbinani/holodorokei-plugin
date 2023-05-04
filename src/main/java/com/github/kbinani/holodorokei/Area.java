package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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

  record DeliveryItem(Material material, String text) {
  }

  abstract DeliveryItem deliveryItem();

  record Wall(Point3i from, Point3i to) {
  }

  abstract Wall[] shutoutWalls();

  abstract Point3i evacuationLocation();

  abstract BoundingBox bounds();

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

  void initialize(UUID sessionId) {
    var positions = new ArrayList<>(List.of(chestPositionList()));
    Collections.shuffle(positions);
    for (int i = 0; i < positions.size(); i++) {
      var p = positions.get(i);
      var blockData = Material.CHEST.createBlockData("[facing=" + p.facing.name().toLowerCase() + "]");
      world.setBlockData(p.position.x, p.position.y, p.position.z, blockData);
      var block = world.getBlockAt(p.position.x, p.position.y, p.position.z);
      if (!(block.getState() instanceof Chest chest)) {
        continue;
      }
      var inventory = chest.getInventory();
      inventory.clear();
      if (i == 0) {
        var deliveryItem = this.deliveryItem();
        var item = new ItemStack(deliveryItem.material, 1);
        var meta = item.getItemMeta();
        if (meta != null) {
          meta.displayName(Component.text(deliveryItem.text));
          meta.lore(List.of(Component.text("全4エリアのアイテムを全て納品すると...？")));
          var container = meta.getPersistentDataContainer();
          container.set(NamespacedKey.minecraft(Main.kAreaItemSessionIdKey), PersistentDataType.STRING, sessionId.toString());
          item.setItemMeta(meta);
        }
        inventory.setItem(13, item);
      } else {
        var holoXerHead = CreateHoloXerHead();
        var meta = holoXerHead.getItemMeta();
        if (meta != null) {
          var container = meta.getPersistentDataContainer();
          container.set(NamespacedKey.minecraft(Main.kAreaItemSessionIdKey), PersistentDataType.STRING, sessionId.toString());
          holoXerHead.setItemMeta(meta);
        }
        inventory.setItem(13, holoXerHead);
      }
    }
    for (var p : beaconPositionList()) {
      BlockData blockData = Material.BEACON.createBlockData();
      world.setBlockData(p.x, p.y, p.z, blockData);
    }
    mission = initializeMission(world);
    if (mission != null) {
      mission.cleanup(world);
    }
  }

  void cleanup() {
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
    for (var wall : shutoutWalls()) {
      Editor.Fill(world, wall.from, wall.to, "air");
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

  void shutout() {
    for (var wall : shutoutWalls()) {
      Editor.Fill(world, wall.from, wall.to, "quartz_bricks");
    }
    var pos = evacuationLocation();
    Players.Within(world, new BoundingBox[]{bounds()}, p -> {
      var mode = p.getGameMode();
      if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
        return;
      }
      p.teleport(new Location(world, pos.x, pos.y, pos.z));
    });
  }
}
