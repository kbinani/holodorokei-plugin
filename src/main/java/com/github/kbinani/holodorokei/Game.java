package com.github.kbinani.holodorokei;

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
import org.bukkit.entity.Zombie;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {
  private final GameSetting setting;
  private final World world;
  private final SoraStation soraStation;
  private final SikeMura sikeMura;
  private final ShiranuiKensetsuBuilding shiranuiKensetsuBuilding;
  private final DododoTown dododoTown;
  private final Area[] areas;
  private final ProgressBoardSet board;
  private final long duration;
  private final Map<AreaType, BukkitTask> areaMissionStarterTasks = new HashMap<>();
  private @Nullable BukkitTask areaMissionTimeoutTimer;
  private @Nullable Bar missionBossBar;
  private @Nullable BukkitTask gameTimeoutTimer;
  private @Nullable Bar mainBossBar;
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
  private @Nonnull
  final MainDelegate delegate;
  private final UUID sessionId;
  private final Map<AreaType, BukkitTask> areaShutoutTimers = new HashMap<>();

  private AreaMissionStatus[] areaMissions = new AreaMissionStatus[]{};
  private final Map<AreaType, Boolean> deliveryMissions;

  Game(@Nonnull MainDelegate delegate, World world, GameSetting setting) {
    this.world = world;
    this.setting = setting;
    this.soraStation = new SoraStation(world);
    this.sikeMura = new SikeMura(world);
    this.shiranuiKensetsuBuilding = new ShiranuiKensetsuBuilding(world);
    this.dododoTown = new DododoTown(world);
    this.areas = new Area[]{soraStation, sikeMura, shiranuiKensetsuBuilding, dododoTown};
    this.deliveryMissions = new HashMap<>();
    Arrays.stream(AreaType.values()).forEach(type -> {
      deliveryMissions.put(type, false);
    });
    this.board = new ProgressBoardSet();
    this.duration = setting.duration;
    this.delegate = delegate;
    this.sessionId = UUID.randomUUID();

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
      thieves.add(new PlayerTracking(p, Role.THIEF, delegate));
    }
    femaleExecutive = setting.femaleExecutive == null ? null : new PlayerTracking(setting.femaleExecutive, Role.FEMALE_EXECUTIVE, delegate);
    researcher = setting.researcher == null ? null : new PlayerTracking(setting.researcher, Role.RESEARCHER, delegate);
    cleaner = setting.cleaner == null ? null : new PlayerTracking(setting.cleaner, Role.CLEANER, delegate);
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
      area.initialize(sessionId);
    }
    board.update(this);

    var server = Bukkit.getServer();
    var scheduler = server.getScheduler();
    for (var entry : setting.areaMissionSchedule.entrySet()) {
      long waitMinutes = duration - entry.getValue();
      final var type = entry.getKey();
      if (waitMinutes > 0) {
        var task = scheduler.runTaskLater(delegate.mainDelegateGetOwner(), () -> {
          startAreaMission(type);
        }, 20 * 60 * waitMinutes);
        areaMissionStarterTasks.put(type, task);
      }
    }

    startMillis = System.currentTimeMillis();
    gameTimeoutTimer = scheduler.runTaskLater(delegate.mainDelegateGetOwner(), this::timeoutGame, 20 * 60 * duration);

    mainBossBar = new Bar(world, Main.field);
    mainBossBar.progress(1);
    mainBossBar.color(BossBar.Color.GREEN);
    mainBossBar.name(getMainBossBarComponent());
    mainBossBar.update();

    bossBarsUpdateTimer = scheduler.runTaskTimer(delegate.mainDelegateGetOwner(), this::updateBossBars, 20, 20);

    thieves.forEach(p -> {
      giveThieveItems(p);
      p.start((int) duration);
    });
    Arrays.stream(cops).forEach(p -> {
      giveCopItems(p);
      p.selectSkill();
      p.start((int) duration);
    });
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

  record ActiveAreaMission(AreaType type, int remainingSeconds) {
    Component bossBarComponent() {
      int minutes = remainingSeconds / 60;
      int seconds = remainingSeconds - 60 * minutes;
      return Component.text(String.format("エリアミッション（%s）：残り時間：", type.description()))
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

    return Component.text("残り時間：")
      .append(Component.text(String.format("%d:%02d", minutes, seconds)).color(NamedTextColor.GREEN))
      .append(Component.text("  ドロボウ残り："))
      .append(Component.text(thieves.size()).color(NamedTextColor.GOLD))
      .append(Component.text("  復活クールタイム："))
      .append(Component.text(resurrectionTimeoutSeconds).color(NamedTextColor.YELLOW));
  }

  int getNumCops() {
    return this.setting.getNumCops();
  }

  int getNumThieves() {
    return this.setting.getNumThieves();
  }

  void terminate() {
    thieves.forEach(p -> Teams.Instance().thief.removePlayer(p.player));
    prisoners.forEach(p -> Teams.Instance().prisoner.removePlayer(p.player));
    Arrays.stream(cops).forEach(p -> Teams.Instance().cop.removePlayer(p.player));
    setting.managers.forEach(p -> Teams.Instance().manager.removePlayer(p));
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
    if (mainBossBar != null) {
      mainBossBar.cleanup();
      mainBossBar = null;
    }
    if (bossBarsUpdateTimer != null) {
      bossBarsUpdateTimer.cancel();
      bossBarsUpdateTimer = null;
    }
    if (resurrectionCoolDownTimer != null) {
      resurrectionCoolDownTimer.cancel();
      resurrectionCoolDownTimer = null;
    }
    if (missionBossBar != null) {
      missionBossBar.cleanup();
      missionBossBar = null;
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
    for (var timer : areaShutoutTimers.values()) {
      timer.cancel();
    }
    areaShutoutTimers.clear();
  }

  void onPlayerInteract(PlayerInteractEvent e) {
    for (var area : areas) {
      if (area.onPlayerInteract(e)) {
        completeAreaMission(area.type());
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
          applyPotionEffect(result.target(), result.effect());
        }
      }
      case SPLASH_POTION -> {
        var tracking = findPlayer(player, false);
        if (tracking == null) {
          return;
        }
        cancellable.setCancelled(true);
        if (tracking.role == Role.THIEF) {
          arrest(tracking);
        } else {
          tracking.player.teleport(new Location(world, kPrisonCenter.x, kPrisonCenter.y, kPrisonCenter.z));
        }
      }
    }
  }

  private boolean canThiefApplyShortCoolDown() {
    return deliveryMissions.values().stream().allMatch(p -> p);
  }

  void onPlayerToggleSneak(PlayerToggleSneakEvent e) {
    var player = findPlayer(e.getPlayer(), false);
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
    var player = findPlayer(e.getPlayer(), false);
    if (player == null) {
      return;
    }
    if (player.role != Role.THIEF) {
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
    if (!kContainerChestDeliveryPost.equals(new Point3i(source))) {
      return;
    }
    if (!kContainerHopperDeliveryPost.equals(new Point3i(destination))) {
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

  private void thiefDamageByZombie(PlayerTracking thief, Zombie zombie, EntityDamageByEntityEvent e) {
    if (thief.isInvulnerable()) {
      e.setDamage(0);
      return;
    }
    arrest(thief);
  }

  private void thiefDamageByCop(PlayerTracking thief, PlayerTracking cop, EntityDamageByEntityEvent e) {
    if (thief.isInvulnerable()) {
      e.setDamage(0);
      return;
    }
    arrest(thief);
  }

  private void arrest(PlayerTracking tracking) {
    var server = Bukkit.getServer();
    var component = tracking.player.teamDisplayName().append(Component.text("が捕まった！").color(NamedTextColor.WHITE));
    server.sendMessage(component);

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
      server.sendMessage(Component.empty());
      server.sendMessage(Component.text("-".repeat(23)));
      server.sendMessage(Component.text("[結果発表]"));
      server.sendMessage(Component.text("ドロボウが全員捕まった！"));
      server.sendMessage(Component.empty());
      server.sendMessage(Component.text("ケイサツの勝利！"));
      server.sendMessage(Component.text("-".repeat(23)));
      server.sendMessage(Component.empty());

      terminate();
      delegate.mainDelegateDidFinishGame();
    }
  }

  private void zombieDamageByThief(Zombie zombie, PlayerTracking thief, EntityDamageByEntityEvent e) {
    zombie.remove();
  }

  private void prisonerDamageByThief(PlayerTracking prisoner, PlayerTracking thief, EntityDamageByEntityEvent e) {
    if (System.currentTimeMillis() < resurrectionCoolDownMillis) {
      return;
    }
    e.setDamage(0);

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

    var server = Bukkit.getServer();
    var component = prisoner.player.teamDisplayName().append(Component.text("が逃げ出した！").color(NamedTextColor.WHITE));
    server.sendMessage(component);

    var scheduler = server.getScheduler();
    resurrectionCoolDownMillis = System.currentTimeMillis() + (long) setting.resurrectCoolDownSeconds * 1000;
    if (resurrectionCoolDownTimer != null) {
      resurrectionCoolDownTimer.cancel();
    }
    resurrectionCoolDownTimer = scheduler.runTaskLater(delegate.mainDelegateGetOwner(), this::onCoolDownResurrection, (long) setting.resurrectCoolDownSeconds * 20);
    if (resurrectionMainBarUpdateTimer != null) {
      resurrectionMainBarUpdateTimer.cancel();
    }
    resurrectionMainBarUpdateTimer = scheduler.runTaskTimer(delegate.mainDelegateGetOwner(), this::updateBossBars, 0, 20);

    board.update(this);
  }

  private void onCoolDownResurrection() {
    resurrectionCoolDownTimer = null;
    if (resurrectionMainBarUpdateTimer != null) {
      resurrectionMainBarUpdateTimer.cancel();
      resurrectionMainBarUpdateTimer = null;
    }
  }

  private void applyPotionEffect(EffectTarget target, PotionEffect effect) {
    if (target == EffectTarget.THIEF) {
      thieves.forEach(t -> t.player.addPotionEffect(effect));
    } else if (target == EffectTarget.COP) {
      Arrays.stream(cops).forEach(t -> t.player.addPotionEffect(effect));
    }
  }

  private @Nullable PlayerTracking findPlayer(Player player, boolean includePrisoner) {
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
    var prisoner = findPrisonerPlayer(e.getPlayer());
    if (prisoner == null) {
      return;
    }
    var location = prisoner.player.getLocation();
    if (!kPrisonBounds.contains(location.toVector())) {
      world.spawnParticle(Particle.SPELL_WITCH, location, 10);
      location.set(kPrisonCenter.x, kPrisonCenter.y, kPrisonCenter.z);
      prisoner.player.teleport(location);
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
    var server = Bukkit.getServer();
    var scheduler = server.getScheduler();
    areaMissionTimeoutTimer = scheduler.runTaskLater(delegate.mainDelegateGetOwner(), () -> abortAreaMission(type), 20 * 60 * 3);
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

    if (missionBossBar != null) {
      missionBossBar.cleanup();
    }
    missionBossBar = new Bar(world, Main.field);
    missionBossBar.color(BossBar.Color.YELLOW);
    missionBossBar.progress(1);
    var active = getActiveAreaMission();
    if (active != null) {
      missionBossBar.name(active.bossBarComponent());
    }
    missionBossBar.update();
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
    if (missionBossBar != null) {
      missionBossBar.cleanup();
      missionBossBar = null;
    }
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
    if (missionBossBar != null) {
      missionBossBar.cleanup();
      missionBossBar = null;
    }

    areaShutoutTimers.put(type, server.getScheduler().runTaskLater(delegate.mainDelegateGetOwner(), () -> shutoutArea(type), 10 * 20));
  }

  private void shutoutArea(AreaType type) {
    areaShutoutTimers.remove(type);
    for (var area : areas) {
      if (area.type() == type) {
        area.shutout();
        return;
      }
    }
  }

  private void timeoutGame() {
    var server = Bukkit.getServer();
    server.sendMessage(Component.text("-".repeat(23)));
    server.sendMessage(Component.text("[結果発表]"));
    var numEscaped = new AtomicInteger(0);
    thieves.forEach(p -> {
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
      if (ok) {
        server.sendMessage(p.player.teamDisplayName().append(Component.text("が逃げ切った！")));
        numEscaped.incrementAndGet();
      } else {
        //NOTE: 本物はここのメッセージどうなっているのか?
        server.sendMessage(p.player.teamDisplayName().append(Component.text("が逃げ切った。しかし「holoXerの頭」を装備していなかった...")));
      }
    });
    server.sendMessage(Component.empty());
    if (numEscaped.get() > 0) {
      server.sendMessage(Component.text("ドロボウの勝利！"));
    } else {
      server.sendMessage(Component.text("ケイサツの勝利！"));
    }
    server.sendMessage(Component.text("-".repeat(23)));
    server.sendMessage(Component.empty());

    terminate();
    delegate.mainDelegateDidFinishGame();
  }

  private void updateBossBars() {
    if (mainBossBar != null) {
      mainBossBar.name(getMainBossBarComponent());
      mainBossBar.progress(getRemainingGameSeconds() / (float) (duration * 60));
      mainBossBar.update();
    }
    if (missionBossBar != null) {
      var active = getActiveAreaMission();
      if (active != null) {
        missionBossBar.name(active.bossBarComponent());
        missionBossBar.progress(active.remainingSeconds / (float) (3 * 60));
        missionBossBar.update();
      }
    }
  }

  private final Point3i kContainerChestDeliveryPost = new Point3i(-5, -60, -25);
  private final Point3i kContainerHopperDeliveryPost = new Point3i(-5, -61, -25);
  private final String kItemTag = "holodorokei_item";
  private final Point3i kPrisonCenter = new Point3i(-5, -54, 1);
  private final Point3i kEscapeLocation = new Point3i(12, -60, 1);
  private final BoundingBox kPrisonBounds = new BoundingBox(-10.5, -54.5, -4.5, 0.5, -48, 6.5);
}
