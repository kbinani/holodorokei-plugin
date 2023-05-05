package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.Optional;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener, GameDelegate {
  private World world;
  private boolean initialized = false;
  private Game game;
  private GameSetting setting = new GameSetting();
  private final Scheduler scheduler = new Scheduler(this);

  @Override
  public void onEnable() {
    Optional<World> overworld = getServer().getWorlds().stream().filter(it -> it.getEnvironment() == World.Environment.NORMAL).findFirst();
    if (overworld.isEmpty()) {
      getLogger().log(Level.SEVERE, "server should have at least one overworld dimension");
      setEnabled(false);
      return;
    }
    world = overworld.get();

    PluginManager pluginManager = getServer().getPluginManager();
    pluginManager.registerEvents(this, this);
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onCreatureSpawn(CreatureSpawnEvent e) {
    if (e.isCancelled()) {
      return;
    }
    if (e.getEntity().getWorld() != world) {
      return;
    }
    switch (e.getSpawnReason()) {
      case NATURAL:
      case VILLAGE_INVASION:
      case BUILD_WITHER:
      case BUILD_IRONGOLEM:
      case BUILD_SNOWMAN:
      case SPAWNER:
        e.setCancelled(true);
        break;
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    if (!initialized) {
      initialized = true;
      getServer().getScheduler().runTaskLater(this, this::setup, 20 * 5);
    }
  }

  @EventHandler
  @SuppressWarnings("unused")
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
    if (!player.getWorld().getUID().equals(world.getUID())) {
      return;
    }
    if (game != null) {
      game.onPlayerInteract(e);
    }
    Block block = e.getClickedBlock();
    if (block == null) {
      return;
    }
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    Point3i location = new Point3i(block.getLocation());
    if (location.equals(kButtonEntryThief)) {
      if (game == null) {
        onClickJoin(player, Role.THIEF);
      }
    } else if (location.equals(kButtonEntryCopFemaleExecutive)) {
      if (game == null) {
        onClickJoin(player, Role.FEMALE_EXECUTIVE);
      }
    } else if (location.equals(kButtonEntryCopResearcher)) {
      if (game == null) {
        onClickJoin(player, Role.RESEARCHER);
      }
    } else if (location.equals(kButtonEntryCopCleaner)) {
      if (game == null) {
        onClickJoin(player, Role.CLEANER);
      }
    } else if (location.equals(kButtonEntryManager)) {
      if (game == null) {
        onClickJoin(player, Role.MANAGER);
      }
    } else if (location.equals(kButtonLeave)) {
      if (game == null) {
        onClickLeave(player);
      }
    } else if (location.equals(kButtonStartShortSoraStation)) {
      if (game == null) {
        var s = this.setting;
        s.scheduleShortAreaMission(AreaType.SORA_STATION);
        scheduleNewGame(s);
      }
    } else if (location.equals(kButtonStartShortSikeVillage)) {
      if (game == null) {
        var s = this.setting;
        s.scheduleShortAreaMission(AreaType.SIKE_MURA);
        scheduleNewGame(s);
      }
    } else if (location.equals(kButtonStartShortDododoTown)) {
      if (game == null) {
        var s = this.setting;
        s.scheduleShortAreaMission(AreaType.DODODO_TOWN);
        scheduleNewGame(s);
      }
    } else if (location.equals(kButtonStartShortShiranuiConstructionBuilding)) {
      if (game == null) {
        var s = this.setting;
        s.scheduleShortAreaMission(AreaType.SHIRANUI_KENSETSU);
        scheduleNewGame(s);
      }
    } else if (location.equals(kButtonStartNormal1) || location.equals(kButtonStartNormal2) || location.equals(kButtonStartNormal3)) {
      if (game == null) {
        //TODO: 通常, 通常②, 通常③の違いは?
        var s = this.setting;
        s.scheduleRegularAreaMissionRandomly();
        scheduleNewGame(s);
      }
    } else if (location.equals(kButtonReset)) {
      onClickReset();
    }
  }

  @EventHandler
  public void onEntityMove(EntityMoveEvent e) {
    if (game == null) {
      return;
    }
    var entity = e.getEntity();
    if (!entity.getWorld().getUID().equals(world.getUID())) {
      return;
    }
    game.onEntityMove(e);
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent e) {
    if (e.getCause() != EntityDamageEvent.DamageCause.FALL) {
      return;
    }
    if (!(e.getEntity() instanceof Player player)) {
      return;
    }
    e.setCancelled(true);
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
    if (game == null) {
      return;
    }
    game.onPlayerToggleSneak(e);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    if (game == null) {
      return;
    }
    game.onBlockBreak(e);
  }

  @EventHandler
  public void onBlockDropItem(BlockDropItemEvent e) {
    if (game == null) {
      return;
    }
    game.onBlockDropItem(e);
  }

  @EventHandler
  public void onPlayerItemDamage(PlayerItemDamageEvent e) {
    if (game == null) {
      return;
    }
    game.onPlayerItemDamage(e);
  }

  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    if (game == null) {
      return;
    }
    game.onEntityDamageByEntity(e);
  }

  @EventHandler
  public void onInventoryMoveItem(InventoryMoveItemEvent e) {
    if (game == null) {
      return;
    }
    game.onInventoryMoveItem(e);
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent e) {
    if (game == null) {
      return;
    }
    game.onPlayerMove(e);
  }

  private void setup() {
    Editor.WallSign(world, kButtonEntryThief, BlockFace.NORTH, DyeColor.YELLOW, "ドロボウでエントリー", "");
    Editor.WallSign(world, kButtonEntryCopFemaleExecutive, BlockFace.NORTH, DyeColor.RED, "ケイサツでエントリー", "（女幹部）");
    Editor.WallSign(world, kButtonEntryCopResearcher, BlockFace.NORTH, DyeColor.RED, "ケイサツでエントリー", "（研究者）");
    Editor.WallSign(world, kButtonEntryCopCleaner, BlockFace.NORTH, DyeColor.RED, "ケイサツでエントリー", "（掃除屋）");

    Editor.WallSign(world, kButtonStartShortSoraStation, BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（そらステーション）");
    Editor.WallSign(world, kButtonStartShortSikeVillage, BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（しけ村）");
    Editor.WallSign(world, kButtonStartShortDododoTown, BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（ドドドタウン）");
    Editor.WallSign(world, kButtonStartShortShiranuiConstructionBuilding, BlockFace.NORTH, DyeColor.YELLOW, "ショート版スタート", "（不知火建設本社）");

    Editor.WallSign(world, kButtonStartNormal1, BlockFace.NORTH, DyeColor.CYAN, "通常版スタート");
    Editor.WallSign(world, kButtonReset, BlockFace.NORTH, DyeColor.WHITE, "リセット");
    Editor.WallSign(world, kButtonStartNormal2, BlockFace.NORTH, DyeColor.CYAN, "通常版スタート", "②");
    Editor.WallSign(world, kButtonStartNormal3, BlockFace.NORTH, DyeColor.CYAN, "通常版スタート", "③");

    Editor.WallSign(world, kButtonEntryManager, BlockFace.NORTH, DyeColor.GREEN, "運営でエントリー");
    Editor.WallSign(world, kButtonLeave, BlockFace.NORTH, DyeColor.WHITE, "エントリー解除");

    var p = new Point3i(-5, -60, -24);
    BlockData blockData = Material.BIRCH_WALL_SIGN.createBlockData("[facing=south]");
    world.setBlockData(p.x, p.y, p.z, blockData);
    Block block = world.getBlockAt(p.x, p.y, p.z);
    BlockState state = block.getState();
    if (state instanceof Sign sign) {
      sign.line(1, Component.text("納品ミッション"));
      sign.line(2, Component.text("納品先はこちら！"));
      sign.setColor(DyeColor.YELLOW);
      sign.setGlowingText(true);
      sign.update();
    }
  }

  private void reset() {
    setup();
    if (game != null) {
      game.terminate();
      game = null;
    }
    if (setting != null) {
      setting.reset();
    }
    setting = new GameSetting();
    Teams.Reset();
    getServer().sendMessage(Component.text("全ての進行状況をリセットしました"));
  }

  private void onClickJoin(Player player, Role role) {
    var result = setting.join(player, role);
    if (result == null) {
      return;
    }
    if (result.error != null) {
      player.sendMessage(Component.text(result.error).color(NamedTextColor.RED));
    }
    if (result.ok != null) {
      getServer().sendMessage(result.ok);
    }
  }

  private void onClickLeave(Player player) {
    var name = player.teamDisplayName();
    if (setting.leave(player)) {
      getServer().sendMessage(name.append(Component.text("がエントリー解除しました").color(NamedTextColor.WHITE)));
    }
  }

  private void onClickReset() {
    reset();
  }

  private void scheduleNewGame(GameSetting setting) {
    var server = getServer();
    var reason = setting.canStart();
    if (reason != null) {
      server.sendMessage(Component.text(String.format("ゲームを開始できません。理由: %s", reason)).color(NamedTextColor.RED));
      return;
    }
    game = new Game(this, scheduler, world, setting);
    this.setting = new GameSetting();
    server.sendMessage(Component.empty());
    server.sendMessage(Component.text(String.format("ゲームを開始します！（ドロボウ：%d人、ケイサツ：%d人）", game.getNumThieves(), game.getNumCops())));
    server.sendMessage(Component.empty());
    Countdown.Then(world, new BoundingBox[]{field}, this, (count) -> this.game != null, () -> {
      if (this.game == null) {
        return false;
      }
      this.game.start();
      return true;
    }, 20);
  }

  private final Point3i kButtonEntryThief = new Point3i(-15, -62, -14);
  private final Point3i kButtonEntryCopFemaleExecutive = new Point3i(-16, -62, -14);
  private final Point3i kButtonEntryCopResearcher = new Point3i(-17, -62, -14);
  private final Point3i kButtonEntryCopCleaner = new Point3i(-18, -62, -14);

  private final Point3i kButtonStartShortSoraStation = new Point3i(7, -61, -14);
  private final Point3i kButtonStartShortSikeVillage = new Point3i(6, -61, -14);
  private final Point3i kButtonStartShortDododoTown = new Point3i(5, -61, -14);
  private final Point3i kButtonStartShortShiranuiConstructionBuilding = new Point3i(4, -61, -14);

  private final Point3i kButtonStartNormal1 = new Point3i(7, -62, -14);
  private final Point3i kButtonReset = new Point3i(6, -62, -14);
  private final Point3i kButtonStartNormal2 = new Point3i(5, -62, -14);
  private final Point3i kButtonStartNormal3 = new Point3i(4, -62, -14);

  private final Point3i kButtonEntryManager = new Point3i(6, -63, -14);
  private final Point3i kButtonLeave = new Point3i(5, -63, -14);

  public static final BoundingBox field = new BoundingBox(-141, -64, -112, 77, 384, 140);
  public static final String kAreaItemSessionIdKey = "holodorokei_session_id";

  @Override
  public void gameDidFinish() {
    this.game = null;
  }
}
