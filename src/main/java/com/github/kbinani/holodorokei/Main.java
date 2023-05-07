package com.github.kbinani.holodorokei;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
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
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

public class Main extends JavaPlugin implements Listener, GameDelegate {
  private World world;
  private boolean initialized = false;
  private Game game;
  private GameSetting setting = new GameSetting();
  private final Scheduler scheduler = new Scheduler(this);
  private final Set<Player> managers = new HashSet<>();

  @Override
  public void onEnable() {
    var logger = getLogger();
    var worlds = getServer().getWorlds();
    if (worlds.size() != 1) {
      logger.log(Level.SEVERE, "このプラグインはディメンジョンが複数存在するサーバーをサポートしていません");
      setEnabled(false);
      return;
    }
    World overworld = worlds.get(0);
    if (overworld.getEnvironment() != World.Environment.NORMAL) {
      logger.log(Level.SEVERE, "このプラグインはオーバーワールド以外のディメンジョンをサポートしていません");
      setEnabled(false);
      return;
    }
    world = overworld;

    var reasons = lookupInvalidServerConfigs();
    if (reasons.length > 0) {
      logger.log(Level.SEVERE, "以下の理由でこのプラグインを有効化できませんでした。設定を見直してください:");
      for (int i = 0; i < reasons.length; i++) {
        var reason = reasons[i];
        logger.log(Level.SEVERE, String.format("  %d. %s", i + 1, reason));
      }
      setEnabled(false);
      return;
    }

    PluginManager pluginManager = getServer().getPluginManager();
    pluginManager.registerEvents(this, this);
  }

  @Override
  public void onDisable() {
    if (game != null) {
      game.terminate();
      game = null;
    }
    if (setting != null) {
      setting.reset();
      setting = new GameSetting();
    }
    managers.clear();
    Teams.Reset();
  }

  @EventHandler
  public void onCreatureSpawn(CreatureSpawnEvent e) {
    if (e.isCancelled()) {
      return;
    }
    switch (e.getSpawnReason()) {
      case NATURAL, VILLAGE_INVASION, BUILD_WITHER, BUILD_IRONGOLEM, BUILD_SNOWMAN, SPAWNER -> e.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent e) {
    if (!initialized) {
      initialized = true;
      getServer().getScheduler().runTaskLater(this, this::setup, 20 * 5);
    }
    var player = e.getPlayer();
    if (game != null) {
      if (!game.onPlayerJoin(e)) {
        player.sendMessage(Component.text("途中参加のため運営扱いでの参加となります"));
        player.setGameMode(GameMode.SPECTATOR);
        joinAsManager(player);
      }
    }
    for (var effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
    var effect = new PotionEffect(PotionEffectType.SATURATION, 30 * 24 * 60 * 60 * 20, 1, false, false);
    player.addPotionEffect(effect);
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent e) {
    if (game == null) {
      return;
    }
    game.onPlayerQuit(e);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e) {
    Player player = e.getPlayer();
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
        if (managers.contains(player)) {
          getServer().sendMessage(Component.text("既に運営として参加済みです").color(NamedTextColor.RED));
        } else {
          joinAsManager(player);
        }
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
    game.onEntityMove(e);
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

  @EventHandler
  public void onPlayerPostRespawn(PlayerPostRespawnEvent e) {
    var player = e.getPlayer();
    var effect = new PotionEffect(PotionEffectType.SATURATION, 30 * 24 * 60 * 60 * 20, 1, false, false);
    player.addPotionEffect(effect);
    if (game == null) {
      return;
    }
    game.onPlayerPostRespawn(e);
  }

  @EventHandler
  public void onEntityDropItem(EntityDropItemEvent e) {
    if (game == null) {
      return;
    }
    game.onEntityDropItem(e);
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
    if (result.error() != null) {
      player.sendMessage(Component.text(result.error()).color(NamedTextColor.RED));
    }
    if (result.ok() != null) {
      getServer().sendMessage(result.ok());
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
    game = new Game(scheduler, world, setting);
    game.delegate = new WeakReference<>(this);
    this.setting = new GameSetting();
    server.sendMessage(Component.empty());
    server.sendMessage(Component.text(String.format("ゲームを開始します！（ドロボウ：%d人、ケイサツ：%d人）", game.getNumThieves(), game.getNumCops())));
    server.sendMessage(Component.empty());
    Countdown.Then(world, new BoundingBox[]{field}, this, (count) -> this.game != null, () -> {
      if (this.game == null) {
        return false;
      }
      this.game.start();
      server.getOnlinePlayers().forEach(p -> {
        if (game.findPlayer(p, true) == null) {
          joinAsManager(p);
        }
      });
      managers.forEach(p -> {
        p.setGameMode(GameMode.SPECTATOR);
      });
      return true;
    }, 20);
  }

  private void joinAsManager(Player player) {
    managers.add(player);
    Teams.Instance().manager.addPlayer(player);
    if (game == null) {
      getServer().sendMessage(player.teamDisplayName().append(Component.text("がエントリーしました（運営）").color(NamedTextColor.WHITE)));
    }
  }

  @Override
  public void gameDidFinish() {
    this.game = null;
    managers.forEach(p -> {
      Teams.Instance().manager.removePlayer(p);
      var location = p.getLocation();
      teleport(p, location.x(), world.getHighestBlockYAt(location.getBlockX(), location.getBlockY()) + 1, location.z());
      p.setGameMode(GameMode.ADVENTURE);
    });
  }

  private void teleport(Player player, double x, double y, double z) {
    var location = player.getLocation();
    location.set(x, y, z);
    player.teleport(location);
  }

  private String[] lookupInvalidServerConfigs() {
    var reasons = new ArrayList<String>();
    if (!Boolean.TRUE.equals(world.getGameRuleValue(GameRule.KEEP_INVENTORY))) {
      reasons.add("gamerule keepInventory が false になっています。true に設定して下さい");
    }
    if (!Boolean.FALSE.equals(world.getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES))) {
      reasons.add("gamerule showDeathMessages が true になっています。false に設定して下さい");
    }
    if (!Boolean.FALSE.equals(world.getGameRuleValue(GameRule.FALL_DAMAGE))) {
      reasons.add("gamerule fallDamage が true になっています。false に設定して下さい");
    }
    if (!Boolean.FALSE.equals(world.getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS))) {
      reasons.add("gamerule annouceAdvancements が true になっています。false に設定して下さい");
    }
    if (!world.getPVP()) {
      reasons.add("pvp が無効化されています。有効にして下さい");
    }
    var spigot = getServer().spigot();
    var paper = spigot.getPaperConfig();
    if (!Boolean.FALSE.equals(paper.get("__________WORLDS__________.__defaults__.hopper.disable-move-event"))) {
      reasons.add("paper の設定項目 hopper.disable-move-event が true になっているため納品ミッションが動作しません。false に設定して下さい");
    }
    if (!Boolean.TRUE.equals(paper.get("__________WORLDS__________.__defaults__.scoreboards.allow-non-player-entities-on-scoreboards"))) {
      reasons.add("paper の設定項目 scoreboards.allow-non-player-entities-on-scoreboards が false になっているためしけ村エリアミッションが動作しません。true に設定して下さい");
    }
    try {
      var props = new Properties();
      props.load(new FileInputStream("server.properties"));
      var spawnProtection = props.getProperty("spawn-protection");
      if (spawnProtection == null) {
        reasons.add("server.properties の spawn-protection が未設定です。スポーン地点付近のアドベンチャーモードのプレイヤーがホロ杖を使えるようにするために spawn-protection=-1 に設定して下さい");
      } else {
        int distance = Integer.parseInt(spawnProtection);
        if (distance > 0) {
          reasons.add("server.properties の spawn-protection が不正です。スポーン地点付近のアドベンチャーモードのプレイヤーがホロ杖を使えるようにするために spawn-protection=-1 に設定して下さい");
        }
      }
    } catch (IOException e) {
      reasons.add("server.properties が読み込めません");
    } catch (NumberFormatException e) {
      reasons.add("server.properties の spawn-protection が不正です。スポーン地点付近のアドベンチャーモードのプレイヤーがホロ杖を使えるようにするために spawn-protection=-1 に設定して下さい");
    }
    return reasons.toArray(new String[0]);
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
}
