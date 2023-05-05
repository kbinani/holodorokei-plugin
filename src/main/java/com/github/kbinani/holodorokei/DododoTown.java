package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.util.BoundingBox;

public class DododoTown extends Area {
  static class CMission extends Mission {
    final String kEntityTag = "holodorokei_dododotown";
    final BoundingBox kGoalBox = new BoundingBox(-31, -60, 100, -24, -55, 107);

    @Override
    void start(World world) {
      Editor.Fill(world, new Point3i(-128, -60, 21), new Point3i(-124, -60, 25), "birch_fence");
      Editor.Fill(world, new Point3i(-127, -60, 22), new Point3i(-125, -60, 24), "air");
      Editor.Fill(world, new Point3i(-128, -60, 23), new Point3i(-128, -60, 23), "birch_fence_gate[facing=east]");
      Editor.Fill(world, new Point3i(-124, -60, 23), new Point3i(-124, -60, 23), "birch_fence_gate[facing=east]");

      world.setBlockData(-126, -60, 26, Material.CHEST.createBlockData("[facing=south]"));
      var block = world.getBlockAt(-126, -60, 26);
      if (block.getState() instanceof Chest chest) {
        var inventory = chest.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
          var item = new ItemStack(Material.LEAD, 1);
          inventory.setItem(i, item);
        }
      }

      summonSheep(-126, -60, 23, world);
    }

    @Override
    void cleanup(World world) {
      Editor.Fill(world, new Point3i(-128, -60, 21), new Point3i(-124, -60, 25), "air");
      world.setBlockData(-126, -60, 26, Material.AIR.createBlockData());
      Kill.EntitiesByScoreboardTag(world, Main.field, kEntityTag);
    }

    @Override
    boolean onPlayerInteract(PlayerInteractEvent e) {
      return false;
    }

    @Override
    boolean onEntityMove(EntityMoveEvent e) {
      if (e.getEntity() instanceof Sheep sheep) {
        return sheep.getScoreboardTags().contains(kEntityTag) && kGoalBox.contains(sheep.getLocation().toVector());
      }
      return false;
    }

    private void summonSheep(int x, int y, int z, World world) {
      world.spawnEntity(new Location(world, x + 0.5, y, z + 0.5), EntityType.SHEEP, CreatureSpawnEvent.SpawnReason.COMMAND, it -> {
        Sheep sheep = (Sheep) it;
        sheep.setAdult();
        sheep.addScoreboardTag(kEntityTag);
        sheep.setLootTable(LootTables.EMPTY.getLootTable());
        sheep.setPersistent(true);
        sheep.setCanPickupItems(false);
      });
    }
  }

  DododoTown(World world, Scheduler scheduler) {
    super(world, scheduler);
  }

  @Override
  AreaType type() {
    return AreaType.DODODO_TOWN;
  }

  @Override
  ChestPosition[] chestPositionList() {
    return new ChestPosition[]{
      new ChestPosition(-91, -59, 65, BlockFace.SOUTH),
      new ChestPosition(-54, -59, 116, BlockFace.NORTH),
      new ChestPosition(-106, -59, 117, BlockFace.NORTH),
      new ChestPosition(-125, -59, 121, BlockFace.SOUTH),
      new ChestPosition(-133, -56, 100, BlockFace.NORTH),
      new ChestPosition(-114, -60, 101, BlockFace.NORTH),
      new ChestPosition(-110, -58, 71, BlockFace.EAST),
      new ChestPosition(-103, -61, 25, BlockFace.WEST),
    };
  }

  @Override
  Point3i[] beaconPositionList() {
    return new Point3i[]{
      new Point3i(-34, -60, 75),
      new Point3i(-32, -60, 77),
      new Point3i(-73, -60, 64),
      new Point3i(-85, -60, 64),
      new Point3i(-90, -60, 98),
      new Point3i(-88, -60, 100),
      new Point3i(-50, -59, 116),
      new Point3i(-73, -59, 117),
      new Point3i(-85, -59, 117),
      new Point3i(-131, -60, 55),
      new Point3i(-129, -60, 57),
    };
  }

  @Override
  Mission initializeMission(World world) {
    return new CMission();
  }

  @Override
  DeliveryItem deliveryItem() {
    return new DeliveryItem(Material.YELLOW_WOOL, "わためぇの毛");
  }

  @Override
  Point3i evacuationLocation() {
    //NOTE: 適当. 根拠なし
    return new Point3i(-23, -60, 15);
  }

  @Override
  Wall[] shutoutWalls() {
    return new Wall[]{
      new Wall(new Point3i(-22, -59, 17), new Point3i(-22, -43, 137)),
      new Wall(new Point3i(-23, -59, 17), new Point3i(-140, -43, 17)),
    };
  }

  @Override
  BoundingBox bounds() {
    return new BoundingBox(-141, -63, 17, -21, 384, 139);
  }
}
