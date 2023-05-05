package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class GameSetting {
  final @Nonnull Set<Player> thieves = new HashSet<>();
  @Nullable
  Player femaleExecutive;
  @Nullable
  Player researcher;
  @Nullable
  Player cleaner;
  final Set<Player> managers = new HashSet<>();
  final Map<AreaType, Integer> areaMissionSchedule = new HashMap<>();
  int duration = 20;
  int resurrectCoolDownSeconds = 10;
  int invulnerableSecondsAfterResurrection = 5;
  boolean enableKatsumokuSeyo = false;
  int copInitialDelaySeconds = 60;

  void reset() {
    var thief = Teams.Instance().thief;
    for (var player : thieves) {
      thief.removePlayer(player);
    }

    var cop = Teams.Instance().cop;
    if (femaleExecutive != null) {
      cop.removePlayer(femaleExecutive);
    }
    if (researcher != null) {
      cop.removePlayer(researcher);
    }
    if (cleaner != null) {
      cop.removePlayer(cleaner);
    }

    var manager = Teams.Instance().manager;
    for (var player : managers) {
      manager.removePlayer(player);
    }
  }

  record JoinResult(@Nullable Component ok, @Nullable String error) {

    static JoinResult Ok(Component message) {
      return new JoinResult(message, null);
    }

    static JoinResult Error(String message) {
      return new JoinResult(null, message);
    }
  }

  @Nullable
  JoinResult join(Player player, Role role) {
    if (femaleExecutive == player) {
      return JoinResult.Error("既にケイサツ（女幹部）として参加済みです");
    }
    if (researcher == player) {
      return JoinResult.Error("既にケイサツ（研究者）として参加済みです");
    }
    if (cleaner == player) {
      return JoinResult.Error("既にケイサツ（掃除屋）として参加済みです");
    }
    if (role == Role.FEMALE_EXECUTIVE && femaleExecutive != null) {
      return JoinResult.Error(femaleExecutive.getName() + "が既にケイサツ（女幹部）として参加済みです");
    }
    if (role == Role.RESEARCHER && researcher != null) {
      return JoinResult.Error(researcher.getName() + "が既にケイサツ（研究者）として参加済みです");
    }
    if (role == Role.CLEANER && cleaner != null) {
      return JoinResult.Error(cleaner.getName() + "が既にケイサツ（掃除屋）として参加済みです");
    }
    if (thieves.contains(player)) {
      return JoinResult.Error("既にドロボウとして参加済みです");
    }
    if (managers.contains(player)) {
      return JoinResult.Error("既に運営として参加済みです");
    }
    switch (role) {
      case THIEF -> {
        Teams.Instance().thief.addPlayer(player);
        thieves.add(player);
        return JoinResult.Ok(player.teamDisplayName().append(Component.text("がエントリーしました（ドロボウ）").color(NamedTextColor.WHITE)));
      }
      case MANAGER -> {
        Teams.Instance().manager.addPlayer(player);
        managers.add(player);
        return JoinResult.Ok(player.teamDisplayName().append(Component.text("がエントリーしました（運営）").color(NamedTextColor.WHITE)));
      }
      case FEMALE_EXECUTIVE -> {
        Teams.Instance().cop.addPlayer(player);
        femaleExecutive = player;
        return JoinResult.Ok(player.teamDisplayName().append(Component.text("がエントリーしました（ケイサツ：女幹部）").color(NamedTextColor.WHITE)));
      }
      case RESEARCHER -> {
        Teams.Instance().cop.addPlayer(player);
        researcher = player;
        return JoinResult.Ok(player.teamDisplayName().append(Component.text("がエントリーしました（ケイサツ：研究者）").color(NamedTextColor.WHITE)));
      }
      case CLEANER -> {
        Teams.Instance().cop.addPlayer(player);
        cleaner = player;
        return JoinResult.Ok(player.teamDisplayName().append(Component.text("がエントリーしました（ケイサツ：掃除屋）").color(NamedTextColor.WHITE)));
      }
    }
    return null;
  }

  boolean leave(Player player) {
    if (femaleExecutive == player) {
      Teams.Instance().cop.removePlayer(player);
      femaleExecutive = null;
      return true;
    }
    if (researcher == player) {
      Teams.Instance().cop.removePlayer(player);
      researcher = null;
      return true;
    }
    if (cleaner == player) {
      Teams.Instance().cop.removePlayer(player);
      cleaner = null;
      return true;
    }
    if (managers.contains(player)) {
      Teams.Instance().manager.removePlayer(player);
      managers.remove(player);
      return true;
    }
    if (thieves.contains(player)) {
      Teams.Instance().thief.removePlayer(player);
      thieves.remove(player);
      return true;
    }
    return false;
  }

  int getNumCops() {
    int count = 0;
    if (femaleExecutive != null) {
      count++;
    }
    if (researcher != null) {
      count++;
    }
    if (cleaner != null) {
      count++;
    }
    return count;
  }

  int getNumThieves() {
    return thieves.size();
  }

  void scheduleShortAreaMission(AreaType type) {
    areaMissionSchedule.clear();
    areaMissionSchedule.put(type, 4);
    duration = 5;
    enableKatsumokuSeyo = false;
    invulnerableSecondsAfterResurrection = 9;
    copInitialDelaySeconds = 30;
  }

  void scheduleRegularAreaMissionRandomly() {
    areaMissionSchedule.clear();
    var types = new ArrayList<>(Arrays.stream(AreaType.values()).toList());
    Collections.shuffle(types);
    for (int i = 0; i < 3; i++) {
      areaMissionSchedule.put(types.get(i), 6 * (3 - i));
    }
    duration = 20;
    enableKatsumokuSeyo = true;
    invulnerableSecondsAfterResurrection = 5;
    copInitialDelaySeconds = 60;
  }

  //NOTE: null ならスタート可, nonnull ならスタートできない理由
  @Nullable
  String canStart() {
    if (thieves.size() == 0) {
      return "ドロボウの参加人数が 0 人です";
    }
    if (femaleExecutive == null && researcher == null && cleaner == null) {
      return "ケイサツの参加人数が 0 人です";
    }
    return null;
  }
}
