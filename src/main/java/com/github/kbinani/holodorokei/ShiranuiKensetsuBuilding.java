package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.util.BoundingBox;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class ShiranuiKensetsuBuilding extends Area {
  static class CMission extends Mission {
    final String kEntityTag = "holodorokei_shiranuikensetsu";
    final Point3i kGoalSign = new Point3i(36, -30, 68);

    @Override
    void start(World world) {
      setOpenGate(true, world);
      world.setBlockData(kGoalSign.x, kGoalSign.y, kGoalSign.z, Material.BIRCH_WALL_SIGN.createBlockData("[facing=north]"));
      var block = world.getBlockAt(kGoalSign.x, kGoalSign.y, kGoalSign.z);
      if (block.getState() instanceof Sign sign) {
        sign.line(1, Component.text("[看板を右クリック]").decorate(TextDecoration.BOLD));
        sign.update();
      }

      summonSkeleton(27, -30, 46, world);
      summonSkeleton(44, -31, 62, world);
    }

    @Override
    void cleanup(World world) {
      setOpenGate(false, world);
      world.setBlockData(kGoalSign.x, kGoalSign.y, kGoalSign.z, Material.AIR.createBlockData());
      Kill.EntitiesByScoreboardTag(world, new BoundingBox(25, -31, 29, 47, -25, 70), kEntityTag);
    }

    @Override
    boolean onPlayerInteract(PlayerInteractEvent e) {
      var block = e.getClickedBlock();
      if (block == null) {
        return false;
      }
      var pos = new Point3i(block.getLocation());
      if (pos.equals(kGoalSign)) {
        e.setCancelled(true);
        return true;
      } else {
        return false;
      }
    }

    @Override
    boolean onEntityMove(EntityMoveEvent e) {
      return false;
    }

    private void setOpenGate(boolean open, World world) {
      var material = open ? Material.AIR : Material.WHITE_STAINED_GLASS;
      for (var pos : new Point3i[]{new Point3i(36, -43, 30), new Point3i(36, -44, 30), new Point3i(36, -43, 34), new Point3i(36, -44, 34)}) {
        world.setBlockData(pos.x, pos.y, pos.z, material.createBlockData());
      }
      Editor.Fill(world, new Point3i(38, -25, 69), new Point3i(38, -22, 29), open ? "barrier" : "air");
      Editor.Fill(world, new Point3i(34, -25, 69), new Point3i(34, -22, 29), open ? "barrier" : "air");
    }

    private void summonSkeleton(int x, int y, int z, World world) {
      var id = new AtomicReference<UUID>();
      world.spawnEntity(new Location(world, x + 0.5, y, z + 0.5), EntityType.MINECART, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
        Minecart minecart = (Minecart) it;
        minecart.addScoreboardTag(kEntityTag);

        world.spawnEntity(new Location(world, x + 0.5, y, z + 0.5), EntityType.SKELETON, CreatureSpawnEvent.SpawnReason.COMMAND, e -> {
          Skeleton skeleton = (Skeleton) e;
          EntityEquipment equipment = skeleton.getEquipment();
          equipment.clear();
          equipment.setItemInMainHand(new ItemStack(Material.BOW));
          skeleton.addScoreboardTag(kEntityTag);
          skeleton.setLootTable(LootTables.EMPTY.getLootTable());
          skeleton.setPersistent(true);
          skeleton.setCanPickupItems(false);
          id.set(skeleton.getUniqueId());
          Teams.Instance().cop.addEntity(skeleton);

          minecart.addPassenger(skeleton);
        });
      });
      var server = Bukkit.getServer();
      CommandSender sender = server.getConsoleSender();
      server.dispatchCommand(sender, String.format("data modify entity %s PersistenceRequired set value 1b", id.get().toString()));
    }
  }

  ShiranuiKensetsuBuilding(World world) {
    super(world);
  }

  @Override
  AreaType type() {
    return AreaType.SHIRANUI_KENSETSU;
  }

  @Override
  ChestPosition[] chestPositionList() {
    return new ChestPosition[]{
      new ChestPosition(45, -59, 50, BlockFace.SOUTH),
      new ChestPosition(34, -58, 69, BlockFace.WEST),
      new ChestPosition(49, -58, 66, BlockFace.SOUTH),
      new ChestPosition(64, -58, 45, BlockFace.WEST),
      new ChestPosition(30, -50, 62, BlockFace.EAST),
      new ChestPosition(50, -43, 54, BlockFace.WEST),
      new ChestPosition(51, -44, 49, BlockFace.WEST),
      new ChestPosition(15, -55, 60, BlockFace.EAST),
    };
  }

  @Override
  Point3i[] beaconPositionList() {
    return new Point3i[]{
      new Point3i(49, -59, 31),
      new Point3i(28, -58, 44),
      new Point3i(38, -58, 67),
      new Point3i(53, -58, 53),
      new Point3i(64, -58, 53),
      new Point3i(63, -58, 53),
      new Point3i(64, -58, 39),
      new Point3i(28, -59, 39),
      new Point3i(30, -50, 56),
      new Point3i(30, -50, 67),
      new Point3i(45, -51, 59),
      new Point3i(53, -58, 45),
    };
  }

  @Override
  Mission initializeMission(World world) {
    return new CMission();
  }

  @Override
  DeliveryItem deliveryItem() {
    return new DeliveryItem(Material.NAME_TAG, "不知火建設社員証");
  }
}
