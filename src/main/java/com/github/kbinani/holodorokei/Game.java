package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class Game {
  final GameSetting setting;
  final World world;
  final SoraStation soraStation;
  final SikeMura sikeMura;
  final ShiranuiKensetsuBuilding shiranuiKensetsuBuilding;
  final DododoTown dododoTown;
  final Area[] areas;
  final ProgressBoardSet board;

  private AreaMissionStatus[] areaMissions = new AreaMissionStatus[]{};
  private final DeliveryMissionStatus[] deliveryMissions = new DeliveryMissionStatus[]{
    new DeliveryMissionStatus(AreaType.SORA_STATION, false),
    new DeliveryMissionStatus(AreaType.SIKE_MURA, false),
    new DeliveryMissionStatus(AreaType.SHIRANUI_KENSETSU, false),
    new DeliveryMissionStatus(AreaType.DODODO_TOWN, false),
  };

  Game(World world, GameSetting setting) {
    this.world = world;
    this.setting = setting;
    this.soraStation = new SoraStation(world);
    this.sikeMura = new SikeMura(world);
    this.shiranuiKensetsuBuilding = new ShiranuiKensetsuBuilding(world);
    this.dododoTown = new DododoTown(world);
    this.areas = new Area[]{soraStation, sikeMura, shiranuiKensetsuBuilding, dododoTown};
    this.board = new ProgressBoardSet();
    //TODO: どのミッションを発生させるか抽選する
    this.areaMissions = new AreaMissionStatus[]{
      new AreaMissionStatus(AreaType.SORA_STATION, MissionStatus.Waiting(18)),
      new AreaMissionStatus(AreaType.SHIRANUI_KENSETSU, MissionStatus.Waiting(12)),
      new AreaMissionStatus(AreaType.SIKE_MURA, MissionStatus.Waiting(6)),
    };
  }

  void start() {
    for (var pos : new Point3i[]{kContainerChestDeliveryPost, kContainerHopperDeliveryPost}) {
      Block block = world.getBlockAt(pos.x, pos.y, pos.z);
      BlockState state = block.getState();
      if (!(state instanceof Container container)) {
        continue;
      }
      Inventory inventory = container.getInventory();
      inventory.clear();
    }
    for (var area : areas) {
      area.initialize();
    }
    board.update(this);
  }

  int getNumCops() {
    return this.setting.getNumCops();
  }

  int getNumThieves() {
    return this.setting.getNumThieves();
  }

  void terminate() {
    setting.reset();
    for (var area : areas) {
      area.reset();
    }
    board.cleanup();
  }

  void onPlayerInteract(PlayerInteractEvent e) {
    for (var area : areas) {
      area.onPlayerInteract(e);
    }
  }

  void onEntityMove(EntityMoveEvent e) {
    for (var area : areas) {
      area.onEntityMove(e);
    }
  }

  AreaMissionStatus[] getAreaMissions() {
    return this.areaMissions;
  }

  DeliveryMissionStatus[] getDeliveryMissions() {
    return this.deliveryMissions;
  }

  private final Point3i kContainerChestDeliveryPost = new Point3i(-5, -60, -25);
  private final Point3i kContainerHopperDeliveryPost = new Point3i(-5, -61, -25);
}
