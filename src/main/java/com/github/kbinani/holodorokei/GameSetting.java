package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class GameSetting {
    final @Nonnull Set<Player> thieves = new HashSet<>();
    @Nullable
    Player femaleExecutive;
    @Nullable
    Player researcher;
    @Nullable
    Player cleaner;
    final Set<Player> managers = new HashSet<>();

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

    static class JoinResult {
        final Component ok;
        final String error;

        JoinResult(Component ok, String error) {
            this.ok = ok;
            this.error = error;
        }

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
        if (femaleExecutive != null) {
            return JoinResult.Error(femaleExecutive.getName() + "が既にケイサツ（女幹部）として参加済みです");
        }
        if (researcher == player) {
            return JoinResult.Error("既にケイサツ（研究者）として参加済みです");
        }
        if (researcher != null) {
            return JoinResult.Error(researcher.getName() + "が既にケイサツ（研究者）として参加済みです");
        }
        if (cleaner == player) {
            return JoinResult.Error("既にケイサツ（掃除屋）として参加済みです");
        }
        if (cleaner != null) {
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

    @Nullable String leave(Player player) {
        if (femaleExecutive == player) {
            Teams.Instance().cop.removePlayer(player);
            femaleExecutive = null;
            return player.getName() + "が ケイサツ（女幹部）のエントリーを解除しました";
        }
        if (researcher == player) {
            Teams.Instance().cop.removePlayer(player);
            researcher = null;
            return player.getName() + "が ケイサツ（研究者）のエントリーを解除しました";
        }
        if (cleaner == player) {
            Teams.Instance().cop.removePlayer(player);
            cleaner = null;
            return player.getName() + "が ケイサツ（掃除屋）のエントリーを解除しました";
        }
        if (managers.contains(player)) {
            Teams.Instance().manager.removePlayer(player);
            managers.remove(player);
            return player.getName() + "が 運営 のエントリーを解除しました";
        }
        if (thieves.contains(player)) {
            Teams.Instance().thief.removePlayer(player);
            thieves.remove(player);
            return player.getName() + "が ドロボウ のエントリーを解除しました";
        }
        return null;
    }
}
