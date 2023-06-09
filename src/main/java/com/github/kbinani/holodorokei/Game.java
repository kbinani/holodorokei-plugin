package com.github.kbinani.holodorokei;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Game implements PlayerTrackingDelegate {
  public @Nullable WeakReference<GameDelegate> delegate;
  private final GameSetting setting;
  private final World world;
  private final Area[] areas;
  private final ProgressBoardSet board;
  private final long duration;
  private final Map<AreaType, BukkitTask> areaMissionStarterTasks = new HashMap<>();
  private @Nullable BukkitTask areaMissionTimeoutTimer;
  private @Nullable BossBar missionCopBossBar;
  private @Nullable BossBar missionOtherBossBar;
  private @Nullable BukkitTask gameTimeoutTimer;
  private @Nullable BossBar mainBossBar;
  private @Nullable BukkitTask bossBarsUpdateTimer;
  private long startMillis;
  private final Set<PlayerTracking> thieves = new HashSet<>();
  private final Set<PlayerTracking> prisoners = new HashSet<>();
  private final @Nullable PlayerTracking femaleExecutive;
  private final @Nullable PlayerTracking researcher;
  private final @Nullable PlayerTracking cleaner;
  private final PlayerTracking[] cops;
  private long resurrectionCoolDownMillis;
  private @Nullable BukkitTask resurrectionCoolDownTimer;
  private @Nullable BukkitTask resurrectionMainBarUpdateTimer;
  private final Scheduler scheduler;
  private final UUID sessionId;
  private @Nullable BukkitTask katsumokuSeyoNoticeTimer;
  private @Nullable BukkitTask katsumokuSeyoStartTimer;
  private boolean katsumokuActivated = false;
  private final Map<NamespacedKey, Long> potionsActiveForCopsUntilMillis = new HashMap<>();
  private final Map<NamespacedKey, Long> potionsActiveForThievesUntilMillis = new HashMap<>();
  private final Map<Point3i, BukkitTask> witchParticleTimers = new HashMap<>();
  private final Logger logger;

  private final AreaMissionStatus[] areaMissions;
  private final Map<AreaType, Boolean> deliveryMissions;

  private static Random sRandom = null;

  Game(Scheduler scheduler, World world, GameSetting setting, Logger parent) {
    this.world = world;
    this.setting = setting;
    var soraStation = new SoraStation(world, scheduler);
    var sikeMura = new SikeMura(world, scheduler);
    var shiranuiKensetsuBuilding = new ShiranuiKensetsuBuilding(world, scheduler);
    var dododoTown = new DododoTown(world, scheduler);
    this.areas = new Area[]{soraStation, sikeMura, shiranuiKensetsuBuilding, dododoTown};
    this.deliveryMissions = new HashMap<>();
    Arrays.stream(AreaType.values()).forEach(type -> {
      deliveryMissions.put(type, false);
    });
    this.board = new ProgressBoardSet();
    this.duration = setting.duration;
    this.scheduler = scheduler;
    this.sessionId = UUID.randomUUID();
    this.logger = Logger.getLogger("holodorokei." + this.sessionId);
    this.logger.setParent(parent);
    this.logger.info("constructor");

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

    for (var p : setting.thieves) {
      thieves.add(new PlayerTracking(p, Role.THIEF, scheduler, this, logger));
    }
    femaleExecutive = setting.femaleExecutive == null ? null : new PlayerTracking(setting.femaleExecutive, Role.FEMALE_EXECUTIVE, scheduler, this, logger);
    researcher = setting.researcher == null ? null : new PlayerTracking(setting.researcher, Role.RESEARCHER, scheduler, this, logger);
    cleaner = setting.cleaner == null ? null : new PlayerTracking(setting.cleaner, Role.CLEANER, scheduler, this, logger);
    var cops = new ArrayList<PlayerTracking>();
    if (femaleExecutive != null) {
      cops.add(femaleExecutive);
    }
    if (researcher != null) {
      cops.add(researcher);
    }
    if (cleaner != null) {
      cops.add(cleaner);
    }
    this.cops = cops.toArray(new PlayerTracking[]{});
  }

  void start() {
    logger.info("start");
    logger.info("participants:");
    logger.info("  female executive: " + (femaleExecutive == null ? "N/A" : femaleExecutive.player.getName()));
    logger.info("  researcher: " + (researcher == null ? "N/A" : researcher.player.getName()));
    logger.info("  cleaner: " + (cleaner == null ? "N/A" : cleaner.player.getName()));
    logger.info("  thieves:");
    for (var p : thieves) {
      logger.info("    " + p.player.getName());
    }
    for (var pos : new Point3i[]{kDeliveryPostChestUpper, kDeliveryPostHopper, kDeliveryPostChestLower}) {
      Block block = world.getBlockAt(pos.x, pos.y, pos.z);
      BlockState state = block.getState();
      if (!(state instanceof Container container)) {
        continue;
      }
      Inventory inventory = container.getInventory();
      inventory.clear();
    }
    for (var area : areas) {
      area.start(sessionId);
    }
    board.update(this);

    for (var entry : setting.areaMissionSchedule.entrySet()) {
      long waitMinutes = duration - entry.getValue();
      final var type = entry.getKey();
      if (waitMinutes > 0) {
        var task = scheduler.runTaskLater(() -> {
          startAreaMission(type);
        }, 20 * 60 * waitMinutes);
        areaMissionStarterTasks.put(type, task);
      }
    }

    startMillis = System.currentTimeMillis();
    gameTimeoutTimer = scheduler.runTaskLater(this::timeoutGame, 20 * 60 * duration);

    var server = Bukkit.getServer();
    mainBossBar = BossBar.bossBar(getMainBossBarComponent(), 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    server.getOnlinePlayers().forEach(p -> {
      p.showBossBar(mainBossBar);
    });

    bossBarsUpdateTimer = scheduler.runTaskTimer(this::updateBossBars, 20, 20);

    if (setting.enableKatsumokuSeyo) {
      katsumokuSeyoNoticeTimer = scheduler.runTaskLater(this::noticeKatsumokuSeyo, (duration - 4) * 60 * 20);
      katsumokuSeyoStartTimer = scheduler.runTaskLater(this::startKatsumokuSeyo, (duration - 3) * 60 * 20);
    }

    thieves.forEach(p -> {
      giveThieveItems(p);
      p.start();
    });
    Arrays.stream(cops).forEach(p -> {
      giveCopItems(p);
      p.selectSkill();
      p.start();
      teleportToPrisonCenter(p.player);
      var darknessSeconds = setting.copInitialDelaySeconds;
      var effect = new PotionEffect(PotionEffectType.DARKNESS, darknessSeconds * 20, 1, false);
      p.player.addPotionEffect(effect);
      potionsActiveForCopsUntilMillis.put(PotionEffectType.DARKNESS.getKey(), System.currentTimeMillis() + (long) darknessSeconds * 1000);
    });

    var y = -54;
    for (int x = -9; x <= -2; x++) {
      spawnParticleAndNext(new Point3i(x, y, -4), false);
      spawnParticleAndNext(new Point3i(x, y, 5), false);
    }
    for (int z = -4; z <= 5; z++) {
      spawnParticleAndNext(new Point3i(-10, y, z), false);
      spawnParticleAndNext(new Point3i(-1, y, z), false);
    }
  }

  private void spawnParticleAndNext(Point3i p, boolean spawn) {
    if (spawn) {
      world.spawnParticle(Particle.SPELL_WITCH, p.x + 0.5, p.y, p.z + 0.5, 1);
    }
    var delta = getRandomInt(5 * 20, 10 * 20);
    var point = new Point3i(p);
    witchParticleTimers.put(point, scheduler.runTaskLater(() -> spawnParticleAndNext(point, true), delta));
  }

  private int getRandomInt(int min, int max) {
    if (sRandom == null) {
      try {
        sRandom = SecureRandom.getInstance("SHA1PRNG");
      } catch (Throwable e) {
        sRandom = new Random();
      }
    }
    return sRandom.nextInt(min, max);
  }

  private void noticeKatsumokuSeyo() {
    var title = Title.title(Component.text("力が解放されそうだ..."), Component.empty());
    var messages = new Component[]{
      Component.empty(),
      Component.text("-".repeat(23)),
      Component.text("？？？？：「力が解放されそうだ...」").color(NamedTextColor.DARK_PURPLE),
      Component.text("-".repeat(23)),
    };
    Players.Within(world, Main.field, p -> {
      p.showTitle(title);
      Arrays.stream(messages).forEach(p::sendMessage);
    });
  }

  private void startKatsumokuSeyo() {
    katsumokuActivated = true;
    var title = Title.title(Component.text("刮目せよ！！"), Component.text("最終スキル発動！"));
    var messages = new Component[]{
      Component.empty(),
      Component.text("-".repeat(23)),
      Component.text("最終スキル発動！").color(NamedTextColor.DARK_PURPLE),
      Component.empty(),
      Component.text("「刮目せよ！」").color(NamedTextColor.DARK_PURPLE),
      Component.empty(),
      Component.text("※以降ドロボウ").append(Component.text("復活不可！").color(NamedTextColor.RED)).append(Component.text("！").color(NamedTextColor.WHITE)),
      Component.text("-".repeat(23)),
    };
    Players.Within(world, Main.field, p -> {
      p.showTitle(title);
      Arrays.stream(messages).forEach(p::sendMessage);
    });
  }

  private void teleportToPrisonCenter(Player player) {
    var location = player.getLocation();
    location.set(kPrisonCenter.x, kPrisonCenter.y, kPrisonCenter.z);
    player.teleport(location);
  }

  private void giveThieveItems(PlayerTracking tracking) {
    var inventory = tracking.player.getInventory();
    inventory.clear();

    var pickaxe = new ItemStack(Material.IRON_PICKAXE);
    addItemTag(pickaxe);
    var pickaxeMeta = pickaxe.getItemMeta();
    if (pickaxeMeta != null) {
      pickaxeMeta.displayName(Component.text("スキル発掘ピッケル"));
      pickaxeMeta.setDestroyableKeys(List.of(NamespacedKey.minecraft("beacon")));
      pickaxe.setItemMeta(pickaxeMeta);
    }
    inventory.setItem(0, pickaxe);

    var stick = new ItemStack(Material.CARROT_ON_A_STICK);
    addItemTag(stick);
    var stickMeta = stick.getItemMeta();
    if (stickMeta != null) {
      stickMeta.displayName(Component.text("ホロ杖"));
      stick.setItemMeta(stickMeta);
    }
    inventory.setItem(1, stick);

    var potion = new ItemStack(Material.SPLASH_POTION);
    addItemTag(potion);
    var potionMeta = potion.getItemMeta();
    if (potionMeta != null) {
      potionMeta.displayName(Component.text("自首用ポーション"));
      potionMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
      potion.setItemMeta(potionMeta);
    }
    inventory.setItem(2, potion);
  }

  private void giveCopItems(PlayerTracking tracking) {
    var inventory = tracking.player.getInventory();
    inventory.clear();

    var stick = new ItemStack(Material.CARROT_ON_A_STICK);
    addItemTag(stick);
    var stickMeta = stick.getItemMeta();
    if (stickMeta != null) {
      stickMeta.displayName(Component.text("ホロ杖"));
      stick.setItemMeta(stickMeta);
    }
    inventory.setItem(0, stick);

    var potion = new ItemStack(Material.SPLASH_POTION);
    addItemTag(potion);
    var potionMeta = potion.getItemMeta();
    if (potionMeta != null) {
      potionMeta.displayName(Component.text("自首用ポーション"));
      potionMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
      potion.setItemMeta(potionMeta);
    }
    inventory.setItem(1, potion);
  }

  private void addItemTag(ItemStack item) {
    var meta = item.getItemMeta();
    if (meta == null) {
      return;
    }
    PersistentDataContainer container = meta.getPersistentDataContainer();
    container.set(NamespacedKey.minecraft(kItemTag), PersistentDataType.BYTE, (byte) 1);
    item.setItemMeta(meta);
  }

  private int getRemainingGameSeconds() {
    long remaining = Math.min(startMillis + duration * 60 * 1000 - System.currentTimeMillis(), duration * 60 * 1000);
    return (int) (remaining / 1000);
  }

  @Override
  public void playerTrackingDidUseSkill(Component message) {
    Bukkit.getServer().getOnlinePlayers().forEach(p -> {
      var tracking = findPlayer(p, true);
      if (tracking == null) {
        p.sendMessage(message);
      }
    });
  }

  record ActiveAreaMission(AreaType type, int remainingSeconds) {
    Component bossBarComponent(boolean areaVisible) {
      int minutes = remainingSeconds / 60;
      int seconds = remainingSeconds - 60 * minutes;
      String areaDisplay = areaVisible ? type.description() : "？？？";
      return Component.text(String.format("エリアミッション（%s）：残り時間：", areaDisplay))
        .append(Component.text(String.format("%d:%02d", minutes, seconds)).color(NamedTextColor.GOLD));
    }
  }

  private @Nullable ActiveAreaMission getActiveAreaMission() {
    AreaType type = null;
    for (var m : areaMissions) {
      if (m.status().equals(MissionStatus.InProgress())) {
        type = m.type();
        break;
      }
    }
    if (type == null) {
      return null;
    }
    var schedule = setting.areaMissionSchedule.get(type);
    if (schedule == null) {
      return null;
    }
    long missionStartMillis = startMillis + duration * 60 * 1000 - (long) schedule * 60 * 1000;
    long missionEndMillis = missionStartMillis + 3 * 60 * 1000;
    int seconds = (int) Math.max(0, missionEndMillis - System.currentTimeMillis()) / 1000;
    return new ActiveAreaMission(type, seconds);
  }

  private Component getMainBossBarComponent() {
    int remaining = getRemainingGameSeconds();
    int minutes = remaining / 60;
    int seconds = remaining - minutes * 60;

    long resurrectionTimeoutSeconds = Math.max(0, (resurrectionCoolDownMillis - System.currentTimeMillis()) / 1000);

    var name = Component.text("残り時間：")
      .append(Component.text(String.format("%d:%02d", minutes, seconds)).color(NamedTextColor.GREEN))
      .append(Component.text("  ドロボウ残り："))
      .append(Component.text(thieves.size()).color(NamedTextColor.GOLD));
    if (katsumokuActivated) {
      return name.append(Component.text("  復活不可").color(NamedTextColor.RED));
    } else {
      return name
        .append(Component.text("  復活クールタイム："))
        .append(Component.text(resurrectionTimeoutSeconds).color(NamedTextColor.YELLOW));
    }
  }

  int getNumCops() {
    return this.setting.getNumCops();
  }

  int getNumThieves() {
    return this.setting.getNumThieves();
  }

  void terminate() {
    var server = Bukkit.getServer();
    thieves.forEach(p -> Teams.Instance().thief.removePlayer(p.player));
    prisoners.forEach(p -> Teams.Instance().prisoner.removePlayer(p.player));
    Arrays.stream(cops).forEach(p -> Teams.Instance().cop.removePlayer(p.player));
    server.getOnlinePlayers().forEach(p -> {
      Teams.Instance().manager.removePlayer(p);
    });
    for (var area : areas) {
      area.cleanup();
    }
    board.cleanup();
    areaMissionStarterTasks.forEach((type, task) -> task.cancel());
    areaMissionStarterTasks.clear();
    if (areaMissionTimeoutTimer != null) {
      areaMissionTimeoutTimer.cancel();
      areaMissionTimeoutTimer = null;
    }
    if (gameTimeoutTimer != null) {
      gameTimeoutTimer.cancel();
      gameTimeoutTimer = null;
    }
    server.getOnlinePlayers().forEach(p -> {
      if (mainBossBar != null) {
        p.hideBossBar(mainBossBar);
      }
      if (missionCopBossBar != null) {
        p.hideBossBar(missionCopBossBar);
      }
      if (missionOtherBossBar != null) {
        p.hideBossBar(missionOtherBossBar);
      }
    });
    mainBossBar = null;
    missionCopBossBar = null;
    missionOtherBossBar = null;
    if (bossBarsUpdateTimer != null) {
      bossBarsUpdateTimer.cancel();
      bossBarsUpdateTimer = null;
    }
    if (resurrectionCoolDownTimer != null) {
      resurrectionCoolDownTimer.cancel();
      resurrectionCoolDownTimer = null;
    }
    for (var t : thieves) {
      t.player.getInventory().clear();
      t.cleanup();
    }
    for (var t : prisoners) {
      t.player.getInventory().clear();
      t.cleanup();
    }
    for (var t : cops) {
      t.player.getInventory().clear();
      t.cleanup();
    }
    if (katsumokuSeyoNoticeTimer != null) {
      katsumokuSeyoNoticeTimer.cancel();
      katsumokuSeyoNoticeTimer = null;
    }
    if (katsumokuSeyoStartTimer != null) {
      katsumokuSeyoStartTimer.cancel();
      katsumokuSeyoStartTimer = null;
    }
    for (var timer : witchParticleTimers.values()) {
      timer.cancel();
    }
    witchParticleTimers.clear();
  }

  void onPlayerInteract(PlayerInteractEvent e) {
    for (var area : areas) {
      if (area.onPlayerInteract(e)) {
        completeAreaMission(area.type());
      }
    }
    var cop = findCopPlayer(e.getPlayer());
    if (e.getHand() == EquipmentSlot.HAND && e.getAction() == Action.RIGHT_CLICK_BLOCK && cop != null) {
      //NOTE: この仕様が実装されているのか不明: ケイサツが holoXer の頭や納品アイテムをガメることで妨害になる. この妨害は防御できないのでそもそもチェスト開けることを禁止にする.
      var block = e.getClickedBlock();
      if (block != null && (block.getType() == Material.CHEST || block.getType() == Material.HOPPER)) {
        e.setCancelled(true);
        return;
      }
    }
    if (e.getHand() == EquipmentSlot.HAND && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
      var item = e.getItem();
      if (item != null) {
        useItem(e.getPlayer(), item, e);
      }
    }
  }

  private void useItem(Player player, ItemStack item, Cancellable cancellable) {
    var meta = item.getItemMeta();
    if (meta == null) {
      return;
    }
    var container = meta.getPersistentDataContainer();
    var value = container.get(NamespacedKey.minecraft(kItemTag), PersistentDataType.BYTE);
    if (value == null) {
      return;
    }
    switch (item.getType()) {
      case CARROT_ON_A_STICK -> {
        var tracking = findPlayer(player, false);
        if (tracking == null) {
          return;
        }
        var result = tracking.tryActivatingSkill(tracking.role == Role.THIEF && canThiefApplyShortCoolDown());
        if (result != null) {
          applyPotionEffect(result);
        }
      }
      case SPLASH_POTION -> {
        var tracking = findPlayer(player, true);
        if (tracking == null) {
          return;
        }
        cancellable.setCancelled(true);
        if (tracking.isArrested()) {
          return;
        }
        if (tracking.role == Role.THIEF) {
          arrest(tracking);
        } else {
          teleportToPrisonCenter(tracking.player);
        }
      }
    }
  }

  private boolean canThiefApplyShortCoolDown() {
    return deliveryMissions.values().stream().allMatch(p -> p);
  }

  void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
    var player = findCopPlayer(e.getPlayer());
    if (player == null) {
      return;
    }
    if (player.role != Role.CLEANER) {
      return;
    }
    if (e.isSneaking()) {
      player.activateSkill();
    } else {
      player.deactivateSkill();
    }
  }

  void onBlockBreak(BlockBreakEvent e) {
    var player = findThiefPlayer(e.getPlayer());
    if (player == null) {
      return;
    }
    var block = e.getBlock();
    if (block.getType() != Material.BEACON) {
      return;
    }
    player.selectSkill();
  }

  void onBlockDropItem(BlockDropItemEvent e) {
    e.setCancelled(true);
  }

  void onPlayerItemDamage(PlayerItemDamageEvent e) {
    e.setCancelled(true);
  }

  void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
    // ゾンビ => thief: 即死, 牢屋に転送して prisoner に変換
    // cop => thief: 即死, 牢屋に転送して prisoner に変換
    // thief => ゾンビ: 即死
    // thief => prisoner: 生還

    //NOTE: この仕様は実装されているのか不明だけどあったほうがいいはず.
    // cop => sheep: 攻撃を無効にする. cop 側が予め羊を殺害しておけばエリアミッションの達成は不可能になるため.

    var attacker = e.getDamager();
    var defender = e.getEntity();
    if (attacker instanceof Player playerAttacker) {
      var copAttacker = findCopPlayer(playerAttacker);
      if (copAttacker == null) {
        var thiefAttacker = findThiefPlayer(playerAttacker);
        if (thiefAttacker == null) {
          return;
        }
        if (defender instanceof Zombie zombieDefender) {
          zombieDamageByThief(zombieDefender, thiefAttacker, e);
        } else if (defender instanceof Player defenderPlayer) {
          var prisonerDefender = findPrisonerPlayer(defenderPlayer);
          if (prisonerDefender == null) {
            return;
          }
          prisonerDamageByThief(prisonerDefender, thiefAttacker, e);
        }
      } else if (defender instanceof Player playerDefender) {
        var thiefDefender = findThiefPlayer(playerDefender);
        if (thiefDefender == null) {
          return;
        }
        thiefDamageByCop(thiefDefender, copAttacker, e);
      } else if (defender instanceof Sheep) {
        e.setCancelled(true);
      }
    } else if (attacker instanceof Zombie zombieAttacker && defender instanceof Player playerDefender) {
      var thiefDefender = findThiefPlayer(playerDefender);
      if (thiefDefender == null) {
        return;
      }
      thiefDamageByZombie(thiefDefender, zombieAttacker, e);
    }
  }

  void onInventoryMoveItem(InventoryMoveItemEvent e) {
    var source = e.getSource().getLocation();
    var destination = e.getDestination().getLocation();
    if (source == null || destination == null) {
      return;
    }
    if (!kDeliveryPostHopper.equals(new Point3i(source))) {
      return;
    }
    if (!kDeliveryPostChestLower.equals(new Point3i(destination))) {
      return;
    }
    var actual = e.getItem();
    for (var area : areas) {
      if (deliveryMissions.getOrDefault(area.type(), false)) {
        continue;
      }
      var expected = area.deliveryItem();
      if (actual.getType() != expected.material()) {
        continue;
      }
      var meta = actual.getItemMeta();
      if (meta == null) {
        continue;
      }
      var container = meta.getPersistentDataContainer();
      var sessionId = container.get(NamespacedKey.minecraft(Main.kAreaItemSessionIdKey), PersistentDataType.STRING);
      if (sessionId == null) {
        continue;
      }
      if (sessionId.equals(this.sessionId.toString())) {
        var server = Bukkit.getServer();
        server.sendMessage(Component.text(String.format("[納品ミッション] %sのアイテムを納品した！", area.type().description())));
        deliveryMissions.put(area.type(), true);
        if (deliveryMissions.values().stream().allMatch(p -> p)) {
          server.sendMessage(Component.empty());
          server.sendMessage(Component.text("-".repeat(23)));
          server.sendMessage(Component.text("[納品ミッション成功！ドロボウ側のクールタイムが短縮された！]"));
          server.sendMessage(Component.text("-".repeat(23)));
        }
        board.update(this);
      }
    }
  }

  boolean onPlayerJoin(PlayerJoinEvent e) {
    var player = e.getPlayer();
    var id = player.getUniqueId();
    for (var tracking : cops) {
      if (tracking.player.getUniqueId().equals(id)) {
        tracking.player = player;
        ensurePotionEffects(tracking);
        giveCopItems(tracking);
        if (System.currentTimeMillis() < startMillis + (long) setting.copInitialDelaySeconds * 1000) {
          var location = player.getLocation();
          if (!kPrisonBounds.contains(location.toVector())) {
            teleportToPrisonCenter(player);
          }
        }
        return true;
      }
    }
    return false;
  }

  void onPlayerQuit(PlayerQuitEvent e) {
    var player = e.getPlayer();
    var tracking = findPlayer(player, true);
    if (tracking == null) {
      for (var effect : player.getActivePotionEffects()) {
        player.removePotionEffect(effect.getType());
      }
      return;
    }
    if (tracking.role != Role.THIEF) {
      return;
    }
    var server = Bukkit.getServer();
    server.sendMessage(tracking.player.teamDisplayName().append(Component.text("がログアウトしたため棄権扱いとします").color(NamedTextColor.WHITE)));
    Teams.Instance().thief.removePlayer(tracking.player);
    Teams.Instance().prisoner.removePlayer(tracking.player);
    if (!thieves.remove(tracking)) {
      prisoners.remove(tracking);
    }
    for (var effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
    if (thieves.isEmpty()) {
      finishWithCopWin();
    }
  }

  void onEntityDropItem(EntityDropItemEvent e) {
    if (!(e.getEntity() instanceof Sheep)) {
      return;
    }
    var item = e.getItemDrop();
    var stack = item.getItemStack();
    if (stack.getType() != Material.LEAD) {
      return;
    }
    for (var area : areas) {
      if (area.type() == AreaType.DODODO_TOWN) {
        if (area.missionStarted && area.missionFinished) {
          item.remove();
          break;
        }
      }
    }
  }

  private void thiefDamageByZombie(PlayerTracking thief, Zombie zombie, EntityDamageByEntityEvent e) {
    if (thief.isInvulnerable()) {
      e.setDamage(0);
      return;
    }
    logger.log(Level.INFO, "thief " + thief.player.getName() + " damage by zombie");
    arrest(thief);
  }

  private void thiefDamageByCop(PlayerTracking thief, PlayerTracking cop, EntityDamageByEntityEvent e) {
    if (thief.isInvulnerable()) {
      e.setDamage(0);
      return;
    }
    logger.log(Level.INFO, "thief " + thief.player.getName() + " damage by cop " + cop.player.getName());
    arrest(thief);
  }


  private void arrest(PlayerTracking tracking) {
    var message = tracking.player.teamDisplayName().append(Component.text("が捕まった！").color(NamedTextColor.WHITE));
    arrestWithMessage(tracking, message);
  }

  private void arrestWithMessage(PlayerTracking tracking, @Nonnull Component message) {
    var server = Bukkit.getServer();
    server.sendMessage(message);

    tracking.player.setBedSpawnLocation(new Location(world, kPrisonCenter.x, kPrisonCenter.y, kPrisonCenter.z), true);
    tracking.player.setHealth(0);
    prisoners.add(tracking);
    thieves.remove(tracking);
    Teams.Instance().thief.removePlayer(tracking.player);
    Teams.Instance().prisoner.addPlayer(tracking.player);
    tracking.depriveSkill();
    tracking.setArrested(true);

    board.update(this);

    if (thieves.isEmpty()) {
      finishWithCopWin();
    }
  }

  private void finishWithCopWin() {
    var server = Bukkit.getServer();
    server.sendMessage(Component.empty());
    server.sendMessage(Component.text("-".repeat(23)));
    server.sendMessage(Component.text("[結果発表]"));
    server.sendMessage(Component.text("ドロボウが全員捕まった！"));
    server.sendMessage(Component.empty());
    server.sendMessage(Component.text("ケイサツの勝利！"));
    server.sendMessage(Component.text("-".repeat(23)));
    server.sendMessage(Component.empty());

    terminate();
    var delegate = this.delegate == null ? null : this.delegate.get();
    if (delegate != null) {
      delegate.gameDidFinish();
    }
  }

  private void zombieDamageByThief(Zombie zombie, PlayerTracking thief, EntityDamageByEntityEvent e) {
    zombie.remove();
    logger.log(Level.INFO, "zombie damage by thief " + thief.player.getName());
  }

  private void prisonerDamageByThief(PlayerTracking prisoner, PlayerTracking thief, EntityDamageByEntityEvent e) {
    final String title = "prisoner " + prisoner.player.getName() + " damage by thief " + thief.player.getName();
    if (System.currentTimeMillis() < resurrectionCoolDownMillis) {
      logger.log(Level.INFO, title + " cancel, during resurrection cooldown");
      return;
    }
    if (katsumokuActivated) {
      logger.log(Level.INFO, title + " cancel, during final skill");
      return;
    }
    e.setDamage(0);
    logger.log(Level.INFO, title + " resurrection success");

    var server = Bukkit.getServer();
    var component = prisoner.player.teamDisplayName().append(Component.text("が逃げ出した！").color(NamedTextColor.WHITE));
    server.sendMessage(component);

    prisoners.remove(prisoner);
    thieves.add(prisoner);
    var p = kEscapeLocation;
    prisoner.player.teleport(new Location(world, p.x, p.y, p.z));
    thief.player.teleport(new Location(world, p.x, p.y, p.z));
    Teams.Instance().prisoner.removePlayer(prisoner.player);
    Teams.Instance().thief.addPlayer(prisoner.player);
    prisoner.addInvulnerableByResurrection(setting.invulnerableSecondsAfterResurrection);
    thief.addInvulnerableByResurrection(setting.invulnerableSecondsAfterResurrection);
    prisoner.setArrested(false);

    resurrectionCoolDownMillis = System.currentTimeMillis() + (long) setting.resurrectCoolDownSeconds * 1000;
    if (resurrectionCoolDownTimer != null) {
      resurrectionCoolDownTimer.cancel();
    }
    resurrectionCoolDownTimer = scheduler.runTaskLater(this::onCoolDownResurrection, (long) setting.resurrectCoolDownSeconds * 20);
    if (resurrectionMainBarUpdateTimer != null) {
      resurrectionMainBarUpdateTimer.cancel();
    }
    resurrectionMainBarUpdateTimer = scheduler.runTaskTimer(this::updateBossBars, 0, 20);

    board.update(this);
  }

  private void onCoolDownResurrection() {
    resurrectionCoolDownTimer = null;
    if (resurrectionMainBarUpdateTimer != null) {
      resurrectionMainBarUpdateTimer.cancel();
      resurrectionMainBarUpdateTimer = null;
    }
    logger.log(Level.INFO, "on cooldown resurrection");
  }

  private void applyPotionEffect(PlayerTracking.SkillActivationResult result) {
    if (result.target() == EffectTarget.THIEF) {
      long until = result.activeUntilMillis();
      var effectType = result.effectType();
      var current = potionsActiveForThievesUntilMillis.get(effectType.getKey());
      if (current != null) {
        until = Math.max(until, current);
      }
      potionsActiveForThievesUntilMillis.put(effectType.getKey(), until);

      var ticks = (int) Math.ceil((until - System.currentTimeMillis()) / 1000.0 * 20);
      if (ticks > 0) {
        var effect = new PotionEffect(result.effectType(), ticks, 1, false);
        thieves.forEach(t -> t.player.addPotionEffect(effect));
      }
      logger.log(Level.INFO, "apply potion effect " + result.effectType().getName() + " to thief");
    } else if (result.target() == EffectTarget.COP) {
      var effectType = result.effectType();
      long until = result.activeUntilMillis();
      var current = potionsActiveForCopsUntilMillis.get(effectType.getKey());
      if (current != null) {
        until = Math.max(until, current);
      }
      potionsActiveForCopsUntilMillis.put(effectType.getKey(), until);

      var ticks = (int) Math.ceil((until - System.currentTimeMillis()) / 1000.0 * 20);
      if (ticks > 0) {
        var effect = new PotionEffect(result.effectType(), ticks, 1, false);
        Arrays.stream(cops).forEach(t -> t.player.addPotionEffect(effect));
      }
      logger.log(Level.INFO, "apply potion effect " + result.effectType().getName() + " to cop");
    }
  }

  @Nullable
  PlayerTracking findPlayer(Player player, boolean includePrisoner) {
    if (femaleExecutive != null && femaleExecutive.player.equals(player)) {
      return femaleExecutive;
    }
    if (researcher != null && researcher.player.equals(player)) {
      return researcher;
    }
    if (cleaner != null && cleaner.player.equals(player)) {
      return cleaner;
    }
    var thief = thieves.stream().filter(p -> p.player.equals(player)).findFirst();
    if (thief.isPresent()) {
      return thief.get();
    }
    if (includePrisoner) {
      var prisoner = prisoners.stream().filter(p -> p.player.equals(player)).findFirst();
      if (prisoner.isPresent()) {
        return prisoner.get();
      }
    }
    return null;
  }

  private @Nullable PlayerTracking findCopPlayer(Player player) {
    if (femaleExecutive != null && femaleExecutive.player.equals(player)) {
      return femaleExecutive;
    }
    if (researcher != null && researcher.player.equals(player)) {
      return researcher;
    }
    if (cleaner != null && cleaner.player.equals(player)) {
      return cleaner;
    }
    return null;
  }

  private @Nullable PlayerTracking findThiefPlayer(Player player) {
    var thief = thieves.stream().filter(p -> p.player.equals(player)).findFirst();
    return thief.orElse(null);
  }

  private @Nullable PlayerTracking findPrisonerPlayer(Player player) {
    var prisoner = prisoners.stream().filter(p -> p.player.equals(player)).findFirst();
    return prisoner.orElse(null);
  }

  void onEntityMove(EntityMoveEvent e) {
    for (var area : areas) {
      if (area.onEntityMove(e)) {
        completeAreaMission(area.type());
      }
    }
  }

  void onPlayerMove(PlayerMoveEvent e) {
    Player player = e.getPlayer();
    var mode = player.getGameMode();
    if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
      return;
    }
    var location = player.getLocation();
    if (kPrisonBounds.contains(location.toVector())) {
      return;
    }
    if (System.currentTimeMillis() < startMillis + (long) setting.copInitialDelaySeconds * 1000) {
      if (findCopPlayer(player) == null && findPrisonerPlayer(player) == null) {
        return;
      }
    } else {
      if (findPrisonerPlayer(player) == null) {
        return;
      }
    }
    teleportToPrisonCenter(player);
  }

  void onPlayerPostRespawn(PlayerPostRespawnEvent e) {
    var tracking = findPlayer(e.getPlayer(), true);
    if (tracking == null) {
      return;
    }
    ensurePotionEffects(tracking);
  }

  private void ensurePotionEffects(PlayerTracking tracking) {
    var now = System.currentTimeMillis();
    switch (tracking.role) {
      case THIEF -> {
        for (var entry : potionsActiveForThievesUntilMillis.entrySet()) {
          var ticks = (int) Math.ceil((entry.getValue() - now) / 1000.0 * 20);
          var effectType = PotionEffectType.getByKey(entry.getKey());
          if (ticks > 0 && effectType != null) {
            var effect = new PotionEffect(effectType, ticks, 1, false);
            tracking.player.addPotionEffect(effect);
          }
        }
      }
      case FEMALE_EXECUTIVE, RESEARCHER, CLEANER -> {
        for (var entry : potionsActiveForCopsUntilMillis.entrySet()) {
          var ticks = (int) Math.ceil((entry.getValue() - now) / 1000.0 * 20);
          var effectType = PotionEffectType.getByKey(entry.getKey());
          if (ticks > 0 && effectType != null) {
            var effect = new PotionEffect(effectType, ticks, 1, false);
            tracking.player.addPotionEffect(effect);
          }
        }
      }
    }
  }

  AreaMissionStatus[] getAreaMissions() {
    return this.areaMissions;
  }

  DeliveryMissionStatus[] getDeliveryMissions() {
    var missions = new ArrayList<DeliveryMissionStatus>();
    var orderedTypes = new AreaType[]{AreaType.SORA_STATION, AreaType.SIKE_MURA, AreaType.SHIRANUI_KENSETSU, AreaType.DODODO_TOWN};
    for (var type : orderedTypes) {
      var s = deliveryMissions.getOrDefault(type, false);
      missions.add(new DeliveryMissionStatus(type, s));
    }
    return missions.toArray(new DeliveryMissionStatus[]{});
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
    areaMissionTimeoutTimer = scheduler.runTaskLater(() -> abortAreaMission(type), 20 * 60 * 3);
    var notified = new HashSet<Player>();
    thieves.forEach(p -> {
      sendMissionStartMessage(p.player, type.description());
      notified.add(p.player);
    });
    Arrays.stream(cops).forEach(p -> {
      sendMissionStartMessage(p.player, "？？？");
      notified.add(p.player);
    });
    Players.Within(world, new BoundingBox[]{Main.field}, (p) -> {
      p.showTitle(Title.title(Component.text("MISSION START!"), Component.empty()));
      if (!notified.contains(p)) {
        sendMissionStartMessage(p, type.description());
      }
    });
    for (var area : areas) {
      if (area.type() == type) {
        area.startMission();
        break;
      }
    }
    board.update(this);

    if (missionCopBossBar != null) {
      hideBossBar(missionCopBossBar);
    }
    if (missionOtherBossBar != null) {
      hideBossBar(missionOtherBossBar);
    }
    missionCopBossBar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    missionOtherBossBar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    updateBossBars();
  }

  private void hideBossBar(BossBar bar) {
    Bukkit.getServer().getOnlinePlayers().forEach(p -> p.hideBossBar(bar));
  }

  private void sendMissionStartMessage(Player p, String missionName) {
    p.sendMessage(Component.empty());
    p.sendMessage(Component.text("-".repeat(23)));
    p.sendMessage(Component.text(String.format("[エリアミッション（%s）スタート！]", missionName)));
    p.sendMessage(Component.text("-".repeat(23)));
  }

  private void completeAreaMission(AreaType type) {
    int index = -1;
    for (int i = 0; i < areaMissions.length; i++) {
      var m = areaMissions[i];
      if (m.type() == type && m.status().equals(MissionStatus.InProgress())) {
        index = i;
        break;
      }
    }
    if (index < 0) {
      return;
    }
    areaMissions[index] = new AreaMissionStatus(type, MissionStatus.Success());
    if (areaMissionTimeoutTimer != null) {
      areaMissionTimeoutTimer.cancel();
      areaMissionTimeoutTimer = null;
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
    server.getOnlinePlayers().forEach(p -> {
      if (missionCopBossBar != null) {
        p.hideBossBar(missionCopBossBar);
      }
      if (missionOtherBossBar != null) {
        p.hideBossBar(missionOtherBossBar);
      }
    });
    missionCopBossBar = null;
    missionOtherBossBar = null;
  }

  private void abortAreaMission(AreaType type) {
    int index = -1;
    for (int i = 0; i < areaMissions.length; i++) {
      var m = areaMissions[i];
      if (m.type() == type && m.status().equals(MissionStatus.InProgress())) {
        index = i;
        break;
      }
    }
    if (index < 0) {
      return;
    }
    areaMissions[index] = new AreaMissionStatus(type, MissionStatus.Fail());
    if (areaMissionTimeoutTimer != null) {
      areaMissionTimeoutTimer.cancel();
      areaMissionTimeoutTimer = null;
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
    server.getOnlinePlayers().forEach(p -> {
      if (missionCopBossBar != null) {
        p.hideBossBar(missionCopBossBar);
      }
      if (missionOtherBossBar != null) {
        p.hideBossBar(missionOtherBossBar);
      }
    });
    missionCopBossBar = null;
    missionOtherBossBar = null;
    for (var area : areas) {
      if (area.type() == type) {
        area.scheduleShutout();
        break;
      }
    }
  }

  private void timeoutGame() {
    (new HashSet<>(thieves)).forEach(p -> {
      var ok = false;
      var inventory = p.player.getInventory();
      var helmet = inventory.getHelmet();
      if (helmet != null && helmet.getType() == Material.PLAYER_HEAD) {
        var meta = helmet.getItemMeta();
        if (meta != null) {
          var container = meta.getPersistentDataContainer();
          var sessionId = container.get(NamespacedKey.minecraft(Main.kAreaItemSessionIdKey), PersistentDataType.STRING);
          if (sessionId != null && sessionId.equals(this.sessionId.toString())) {
            ok = true;
          }
        }
      }
      if (!ok) {
        var message = Component.text("「holoXerの頭」を装備していなかったため ")
          .append(p.player.teamDisplayName())
          .append(Component.text(" が捕まった！").color(NamedTextColor.WHITE));
        arrestWithMessage(p, message);
      }
    });

    if (thieves.size() > 0) {
      var server = Bukkit.getServer();
      server.sendMessage(Component.text("-".repeat(23)));
      server.sendMessage(Component.text("[結果発表]"));
      thieves.forEach(p -> {
        server.sendMessage(p.player.teamDisplayName().append(Component.text("が逃げ切った！")));
      });
      server.sendMessage(Component.empty());
      server.sendMessage(Component.text("ドロボウの勝利！"));
      server.sendMessage(Component.text("-".repeat(23)));
      server.sendMessage(Component.empty());

      terminate();
      var delegate = this.delegate == null ? null : this.delegate.get();
      if (delegate != null) {
        delegate.gameDidFinish();
      }
    }
  }

  private void updateBossBars() {
    var server = Bukkit.getServer();
    if (mainBossBar != null) {
      mainBossBar.name(getMainBossBarComponent());
      mainBossBar.progress(getRemainingGameSeconds() / (float) (duration * 60));
      server.getOnlinePlayers().forEach(p -> p.showBossBar(mainBossBar));
    }
    var active = getActiveAreaMission();
    if (active != null) {
      server.getOnlinePlayers().forEach(p -> {
        if ((femaleExecutive != null && femaleExecutive.player == p) || (researcher != null && researcher.player == p) || (cleaner != null && cleaner.player == p)) {
          if (missionCopBossBar != null) {
            p.showBossBar(missionCopBossBar);
          }
        } else {
          if (missionOtherBossBar != null) {
            p.showBossBar(missionOtherBossBar);
          }
        }
      });
      if (missionCopBossBar != null) {
        missionCopBossBar.name(active.bossBarComponent(false));
        missionCopBossBar.progress(active.remainingSeconds / (float) (3 * 60));
      }
      if (missionOtherBossBar != null) {
        missionOtherBossBar.name(active.bossBarComponent(true));
        missionOtherBossBar.progress(active.remainingSeconds / (float) (3 * 60));
      }
    }
  }

  private final Point3i kDeliveryPostChestUpper = new Point3i(-5, -60, -25);
  private final Point3i kDeliveryPostHopper = new Point3i(-5, -61, -25);
  private final Point3i kDeliveryPostChestLower = new Point3i(-5, -62, -25);
  private final String kItemTag = "holodorokei_item";
  private final Point3i kPrisonCenter = new Point3i(-5, -54, 1);
  private final Point3i kEscapeLocation = new Point3i(12, -60, 1);
  private final BoundingBox kPrisonBounds = new BoundingBox(-10.5, -54.5, -4.5, 0.5, -48, 6.5);
}
