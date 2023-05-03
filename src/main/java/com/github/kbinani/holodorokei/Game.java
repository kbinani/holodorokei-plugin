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

    private String[] closedAreas = new String[0];
    private boolean soraStationDelivery = false;
    private boolean sikeMuraDelivery = false;
    private boolean dododoTownDelivery = false;
    private boolean shiranuiKensetsuDelivery = false;
    private AreaMission[] missions;

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
        this.missions = new AreaMission[]{
                new AreaMission(soraStation.name(), MissionStatus.WILL_START_18),
                new AreaMission(shiranuiKensetsuBuilding.name(), MissionStatus.WILL_START_12),
                new AreaMission(sikeMura.name(), MissionStatus.WILL_START_6),
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

    String[] getClosedAreas() {
        return this.closedAreas;
    }

    boolean isSoraStationDeliveryFinished() {
        return this.soraStationDelivery;
    }

    boolean isSikeMuraDeliveryFinished() {
        return this.sikeMuraDelivery;
    }

    boolean isDododoTownDeliveryFinished() {
        return this.dododoTownDelivery;
    }

    boolean isShiranuiKensetsuDeliveryFinished() {
        return this.shiranuiKensetsuDelivery;
    }

    record AreaMission(String name, MissionStatus status) {
    }

    AreaMission[] getAreaMissionStatus() {
        return this.missions;
    }

    private final Point3i kContainerChestDeliveryPost = new Point3i(-5, -60, -25);
    private final Point3i kContainerHopperDeliveryPost = new Point3i(-5, -61, -25);
}
