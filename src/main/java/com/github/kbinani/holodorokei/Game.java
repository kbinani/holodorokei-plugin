package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Game {
  private final JavaPlugin owner;
  private final GameSetting setting;
  private final World world;
  private final SoraStation soraStation;
  private final SikeMura sikeMura;
  private final ShiranuiKensetsuBuilding shiranuiKensetsuBuilding;
  private final DododoTown dododoTown;
  private final Area[] areas;
  private final ProgressBoardSet board;
  private final int duration;
  private final Map<AreaType, BukkitTask> areaMissionStarterTasks = new HashMap<>();
  private @Nullable BukkitTask areaMissionTimeoutTimer;

  private AreaMissionStatus[] areaMissions = new AreaMissionStatus[]{};
  private final DeliveryMissionStatus[] deliveryMissions = new DeliveryMissionStatus[]{
    new DeliveryMissionStatus(AreaType.SORA_STATION, false),
    new DeliveryMissionStatus(AreaType.SIKE_MURA, false),
    new DeliveryMissionStatus(AreaType.SHIRANUI_KENSETSU, false),
    new DeliveryMissionStatus(AreaType.DODODO_TOWN, false),
  };

  Game(JavaPlugin owner, World world, GameSetting setting) {
    this.owner = owner;
    this.world = world;
    this.setting = setting;
    this.soraStation = new SoraStation(world);
    this.sikeMura = new SikeMura(world);
    this.shiranuiKensetsuBuilding = new ShiranuiKensetsuBuilding(world);
    this.dododoTown = new DododoTown(world);
    this.areas = new Area[]{soraStation, sikeMura, shiranuiKensetsuBuilding, dododoTown};
    this.board = new ProgressBoardSet();
    this.duration = setting.duration;

    record Entry(AreaType type, int minutes) {
      AreaMissionStatus toStatus() {
        return new AreaMissionStatus(type, MissionStatus.Waiting(Entry.this.minutes));
      }
    }

    var areaMissions = new ArrayList<Entry>();
    for (var sched : setting.areaMissionSchedule.entrySet()) {
      var area = sched.getKey();
      var minutes = sched.getValue();
      areaMissions.add(new Entry(area, minutes));
    }
    areaMissions.sort((a, b) -> b.minutes - a.minutes);
    this.areaMissions = areaMissions.stream().map(Entry::toStatus).toList().toArray(new AreaMissionStatus[]{});
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

    var scheduler = Bukkit.getServer().getScheduler();
    for (var entry : setting.areaMissionSchedule.entrySet()) {
      long waitMinutes = duration - entry.getValue();
      final var type = entry.getKey();
      if (waitMinutes > 0) {
        var task = scheduler.runTaskLater(owner, () -> {
          startAreaMission(type);
        }, 20 * 60 * waitMinutes);
        areaMissionStarterTasks.put(type, task);
      }
    }
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
    areaMissionStarterTasks.forEach((type, task) -> task.cancel());
    areaMissionStarterTasks.clear();
    if (areaMissionTimeoutTimer != null) {
      areaMissionTimeoutTimer.cancel();
      areaMissionTimeoutTimer = null;
    }
  }

  void onPlayerInteract(PlayerInteractEvent e) {
    for (var area : areas) {
      if (area.onPlayerInteract(e)) {
        completeAreaMission(area.type());
      }
    }
  }

  void onEntityMove(EntityMoveEvent e) {
    for (var area : areas) {
      if (area.onEntityMove(e)) {
        completeAreaMission(area.type());
      }
    }
  }

  AreaMissionStatus[] getAreaMissions() {
    return this.areaMissions;
  }

  DeliveryMissionStatus[] getDeliveryMissions() {
    return this.deliveryMissions;
  }

  private void startAreaMission(AreaType type) {
    areaMissionStarterTasks.remove(type);
    for (int i = 0; i < areaMissions.length; i++) {
      var mission = areaMissions[i];
      if (mission.type() == type) {
        areaMissions[i] = new AreaMissionStatus(type, MissionStatus.InProgress());
        break;
      }
    }
    var server = Bukkit.getServer();
    var scheduler = server.getScheduler();
    areaMissionTimeoutTimer = scheduler.runTaskLater(owner, () -> abortAreaMission(type), 20 * 60 * 3);
    Players.Within(world, new BoundingBox[]{Main.field}, (p) -> {
      p.showTitle(Title.title(Component.text("MISSION START!"), Component.empty()));
    });
    server.sendMessage(Component.empty());
    server.sendMessage(Component.text("-".repeat(23)));
    server.sendMessage(Component.text(String.format("[エリアミッション（%s）スタート！]", type.description())));
    server.sendMessage(Component.text("-".repeat(23)));
    for (var area : areas) {
      if (area.type() == type) {
        area.startMission();
        break;
      }
    }
    board.update(this);
  }

  private void completeAreaMission(AreaType type) {
    for (int i = 0; i < areaMissions.length; i++) {
      var mission = areaMissions[i];
      if (mission.type() == type && mission.status().equals(MissionStatus.InProgress())) {
        areaMissions[i] = new AreaMissionStatus(type, MissionStatus.Success());
        if (areaMissionTimeoutTimer != null) {
          areaMissionTimeoutTimer.cancel();
        }
        var server = Bukkit.getServer();
        Players.Within(world, new BoundingBox[]{Main.field}, (p) -> {
          p.showTitle(Title.title(Component.text("MISSION CLEAR!!"), Component.empty()));
        });
        server.sendMessage(Component.empty());
        server.sendMessage(Component.text("-".repeat(23)));
        server.sendMessage(Component.text(String.format("[エリアミッション（%s）成功！]", type.description())));
        server.sendMessage(Component.text("-".repeat(23)));
        board.update(this);
        return;
      }
    }
  }

  private void abortAreaMission(AreaType type) {
    for (int i = 0; i < areaMissions.length; i++) {
      var mission = areaMissions[i];
      if (mission.type() == type && mission.status().equals(MissionStatus.InProgress())) {
        areaMissions[i] = new AreaMissionStatus(type, MissionStatus.Fail());
        if (areaMissionTimeoutTimer != null) {
          areaMissionTimeoutTimer.cancel();
        }
        var server = Bukkit.getServer();
        Players.Within(world, new BoundingBox[]{Main.field}, (p) -> {
          p.showTitle(Title.title(Component.text("MISSION FAILED"), Component.empty()));
        });
        server.sendMessage(Component.empty());
        server.sendMessage(Component.text("-".repeat(23)));
        server.sendMessage(Component.text(String.format("[エリアミッション（%s）失敗！]", type.description())));
        server.sendMessage(Component.text("-".repeat(23)));
        server.sendMessage(Component.empty());
        server.sendMessage(Component.text("-".repeat(23)));
        server.sendMessage(Component.text("※警告※").color(NamedTextColor.RED));
        server.sendMessage(Component.text(String.format("【%s】エリアが10秒後に封鎖されます！", type.description())));
        server.sendMessage(Component.text("-".repeat(23)));
        board.update(this);
        //TODO: エリア封鎖処理
        return;
      }
    }
  }

  private final Point3i kContainerChestDeliveryPost = new Point3i(-5, -60, -25);
  private final Point3i kContainerHopperDeliveryPost = new Point3i(-5, -61, -25);
}
