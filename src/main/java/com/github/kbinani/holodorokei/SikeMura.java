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
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.util.BoundingBox;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class SikeMura extends Area {
  static class CMission extends Mission {
    final String kEntityTag = "holodorokei_sikemura";
    final Point3i kGoalSign = new Point3i(30, -44, -99);

    @Override
    void start(World world) {
      setGateOpen(true, world);
      var zombies = new Point3i[]{
        new Point3i(31, -60, -27),
        new Point3i(32, -60, -26),
        new Point3i(33, -60, -27),

        new Point3i(38, -60, -38),
        new Point3i(39, -60, -39),
        new Point3i(39, -60, -37),

        new Point3i(45, -60, -29),

        new Point3i(50, -60, -40),

        new Point3i(59, -60, -44),

        new Point3i(38, -60, -61),
        new Point3i(39, -60, -60),

        new Point3i(58, -60, -58),
        new Point3i(57, -60, -57),

        new Point3i(58, -60, -71),
        new Point3i(62, -60, -71),

        new Point3i(33, -60, -73),
        new Point3i(29, -60, -69),

        new Point3i(22, -60, -64),
        new Point3i(14, -60, -67),

        new Point3i(27, -58, -89),

        new Point3i(19, -58, -93),
        new Point3i(20, -58, -94),

        new Point3i(28, -58, -99),

        new Point3i(34, -46, -95),
      };
      for (var pos : zombies) {
        summonZombie(pos.x, pos.y, pos.z, world);
      }

      world.setBlockData(kGoalSign.x, kGoalSign.y, kGoalSign.z, Material.BIRCH_WALL_SIGN.createBlockData("[facing=south]"));
      var block = world.getBlockAt(kGoalSign.x, kGoalSign.y, kGoalSign.z);
      if (block.getState() instanceof Sign sign) {
        sign.line(1, Component.text("[看板を右クリック]").decorate(TextDecoration.BOLD));
        sign.update();
      }
    }

    @Override
    void cleanup(World world) {
      setGateOpen(false, world);
      Kill.EntitiesByScoreboardTag(world, Main.field, kEntityTag);
      world.setBlockData(kGoalSign.x, kGoalSign.y, kGoalSign.z, Material.AIR.createBlockData());
    }

    @Override
    boolean onPlayerInteract(PlayerInteractEvent e) {
      var block = e.getClickedBlock();
      if (block == null) {
        return false;
      }
      var pos = new Point3i(block.getLocation());
      if (!pos.equals(kGoalSign)) {
        return false;
      }
      e.setCancelled(true);
      var boxes = new BoundingBox[]{
        new BoundingBox(16, -46, -101, 55, -26, -92),
        new BoundingBox(20, -46, -94, 34, -44, 90),
        new BoundingBox(17, -58, -99, 19, -44, -98),
      };
      var world = e.getPlayer().getWorld();
      Players.Within(world, boxes, (player -> {
        player.teleport(new Location(world, 25, -60, -77));
      }));
      return true;
    }

    @Override
    boolean onEntityMove(EntityMoveEvent e) {
      return false;
    }

    private void setGateOpen(boolean open, World world) {
      var material = open ? Material.AIR : Material.WHITE_STAINED_GLASS;
      for (int y = -58; y <= -57; y++) {
        world.setBlockData(18, y, -98, material.createBlockData());
      }
    }

    private void summonZombie(int x, int y, int z, World world) {
      final var id = new AtomicReference<UUID>();
      world.spawnEntity(new Location(world, x + 0.5, y, z + 0.5), EntityType.ZOMBIE, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
        Zombie zombie = (Zombie) it;
        zombie.setAdult();
        EntityEquipment equipment = zombie.getEquipment();
        DisableDrop(equipment);
        equipment.clear();
        equipment.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        zombie.addScoreboardTag(kEntityTag);
        zombie.setLootTable(LootTables.EMPTY.getLootTable());
        zombie.setPersistent(true);
        zombie.setCanPickupItems(false);
        id.set(zombie.getUniqueId());
        Teams.Instance().cop.addEntity(zombie);
      });
      var server = Bukkit.getServer();
      CommandSender sender = server.getConsoleSender();
      server.dispatchCommand(sender, String.format("data modify entity %s PersistenceRequired set value 1b", id.get().toString()));
    }

    private static void DisableDrop(EntityEquipment e) {
      e.setHelmetDropChance(0);
      e.setItemInMainHandDropChance(0);
      e.setItemInOffHandDropChance(0);
      e.setChestplateDropChance(0);
      e.setLeggingsDropChance(0);
      e.setBootsDropChance(0);
    }
  }

  SikeMura(World world) {
    super(world);
  }

  @Override
  AreaType type() {
    return AreaType.SIKE_MURA;
  }

  @Override
  ChestPosition[] chestPositionList() {
    return new ChestPosition[]{
      new ChestPosition(13, -60, -41, BlockFace.EAST),
      new ChestPosition(45, -60, -28, BlockFace.EAST),
      new ChestPosition(74, -60, -51, BlockFace.SOUTH),
      new ChestPosition(31, -58, -99, BlockFace.WEST),
      new ChestPosition(14, -60, -98, BlockFace.NORTH),
      new ChestPosition(44, -60, -75, BlockFace.NORTH),
      new ChestPosition(44, -59, -67, BlockFace.SOUTH),
      new ChestPosition(71, -56, -63, BlockFace.WEST),
    };
  }

  @Override
  Point3i[] beaconPositionList() {
    return new Point3i[]{
      new Point3i(45, -60, -44),
      new Point3i(44, -60, -59),
      new Point3i(45, -60, -59),
      new Point3i(33, -60, -74),
      new Point3i(24, -58, -97),
      new Point3i(25, -58, -97),
      new Point3i(50, -60, -89),
      new Point3i(62, -57, -75),
      new Point3i(38, -57, -75),
      new Point3i(63, -60, -40),
      new Point3i(69, -60, -61),
      new Point3i(64, -60, -61),
      new Point3i(62, -56, -63),
    };
  }

  @Override
  Mission initializeMission(World world) {
    return new CMission();
  }

  @Override
  DeliveryItem deliveryItem() {
    return new DeliveryItem(Material.LAVA_BUCKET, "あちゅあちゅマグマ");
  }

  @Override
  Point3i evacuationLocation() {
    //NOTE: 適当. 根拠なし
    return new Point3i(6, -60, -19);
  }

  @Override
  Wall[] shutoutWalls() {
    return new Wall[]{
      new Wall(new Point3i(9, -59, -19), new Point3i(9, -43, -111)),
      new Wall(new Point3i(10, -59, -19), new Point3i(75, -43, -19)),
    };
  }

  @Override
  BoundingBox bounds() {
    return new BoundingBox(9, -63, -112, 77, 384, -18);
  }
}
