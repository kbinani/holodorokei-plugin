package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;

import java.util.HashSet;

public class ProgressBoard {
    final String name;
    final Objective objective;

    ProgressBoard(String name, DisplaySlot slot) {
        this.name = name;
        var server = Bukkit.getServer();
        var manager = server.getScoreboardManager();
        var board = manager.getMainScoreboard();
        var objective = board.getObjective(name);
        if (objective == null) {
            objective = board.registerNewObjective(name, Criteria.DUMMY, Component.text("ホロドロケイ"), RenderType.INTEGER);
        }
        objective.setDisplaySlot(slot);
        this.objective = objective;
    }

    void update(String[] lines) {
        var board = objective.getScoreboard();
        if (board == null) {
            return;
        }
        var dangling = new HashSet<String>(board.getEntries());
        for (int i = 0; i < lines.length; i++) {
            var line = lines[i];
            var score = lines.length - i;
            objective.getScore(line).setScore(score);
            dangling.remove(line);
        }
        for (String d : dangling) {
            objective.getScore(d).resetScore();
        }
    }

    void cleanup() {
        var board = objective.getScoreboard();
        if (board == null) {
            return;
        }
        for (var entry : board.getEntries()) {
            objective.getScore(entry).resetScore();
        }
    }
}
