package com.github.kbinani.holodorokei;

import org.bukkit.scoreboard.DisplaySlot;

import java.util.ArrayList;
import java.util.List;

public class ProgressBoardSet {
    final List<ProgressBoard> thiefBoards = new ArrayList<>();
    final ProgressBoard copBoard;
    final ProgressBoard managerBoard;


    ProgressBoardSet() {
        var thief = new ProgressBoard("holodorokei_thief", DisplaySlot.SIDEBAR_TEAM_YELLOW);
        var prisoner = new ProgressBoard("holodorokei_prisoner", DisplaySlot.SIDEBAR_TEAM_DARK_PURPLE);
        thiefBoards.add(thief);
        thiefBoards.add(prisoner);

        managerBoard = new ProgressBoard("holodorokei_manager", DisplaySlot.SIDEBAR_TEAM_GREEN);
        copBoard = new ProgressBoard("holodorokei_cop", DisplaySlot.SIDEBAR_TEAM_RED);
    }

    void cleanup() {
        for (var board : thiefBoards) {
            board.cleanup();
        }
        copBoard.cleanup();
    }

    void update(Game game) {
        List<String> common = new ArrayList<>();

        // エリア状況
        common.add("[エリア状況]");
        var closedAreas = game.getClosedAreas();
        if (closedAreas.length == 0) {
            common.add("封鎖エリア：なし");
            common.add("封鎖エリア：なし");
        } else {
            for (var area : closedAreas) {
                common.add("└" + area + "：封鎖");
            }
        }
        common.add("+" + "-".repeat(16) + "+");

        // 納品ミッション
        common.add("[納品ミッション]");
        if (game.isSoraStationDeliveryFinished() && game.isSikeMuraDeliveryFinished() && game.isShiranuiKensetsuDeliveryFinished() && game.isDododoTownDeliveryFinished()) {
            common.add("└成功！");
        } else {
            common.add("└そらステーション：" + (game.isSoraStationDeliveryFinished() ? "o" : "x"));
            common.add("└しけ村：" + (game.isSikeMuraDeliveryFinished() ? "o" : "x"));
            common.add("└ドドドタウン：" + (game.isDododoTownDeliveryFinished() ? "o" : "x"));
            common.add("└不知火建設本社：" + (game.isShiranuiKensetsuDeliveryFinished() ? "o" : "x"));
        }
        common.add("+" + "-".repeat(14) + "+");

        // エリアミッション
        common.add("[エリアミッション]");
        var thief = new ArrayList<>(common);
        var cop = new ArrayList<>(common);
        var manager = new ArrayList<>(common);
        var status = game.getAreaMissionStatus();
        var prefix = new String[]{"①", "②", "③"};
        for (int i = 0; i < 3 && i < status.length; i++) {
            var mission = status[i];
            switch (mission.status()) {
                case FAIL, SUCCESS -> {
                    var line = "└" + prefix[i] + mission.name() + "：" + mission.status().rawValue;
                    thief.add(line);
                    cop.add(line);
                    manager.add(line);
                }
                case IN_PROGRESS -> {
                    var line = "└" + prefix[i] + mission.name() + "：" + mission.status().rawValue;
                    thief.add(line);
                    cop.add("└" + prefix[i] + "？？？：" + mission.status().rawValue);
                    manager.add(line);
                }
                case WILL_START_6, WILL_START_12, WILL_START_18 -> {
                    var line = "└" + prefix[i] + "？？？：" + mission.status().rawValue;
                    thief.add(line);
                    cop.add(line);
                    manager.add("└" + prefix[i] + mission.name() + "：" + mission.status().rawValue);
                }
            }
        }

        for (var board : thiefBoards) {
            board.update(thief.toArray(new String[]{}));
        }
        managerBoard.update(manager.toArray(new String[]{}));
        copBoard.update(cop.toArray(new String[]{}));
    }
}
