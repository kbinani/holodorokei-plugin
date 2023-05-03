package com.github.kbinani.holodorokei;

import org.bukkit.scoreboard.DisplaySlot;

import java.util.ArrayList;
import java.util.List;

public class ProgressBoardSet {
  final List<ProgressBoard> thiefBoards = new ArrayList<>();
  final ProgressBoard copBoard;
  final ProgressBoard managerBoard;


  ProgressBoardSet() {
    final var prefix = "holodorokei_";
    var thief = new ProgressBoard(prefix + "thief", DisplaySlot.SIDEBAR_TEAM_YELLOW);
    var prisoner = new ProgressBoard(prefix + "prisoner", DisplaySlot.SIDEBAR_TEAM_DARK_PURPLE);
    thiefBoards.add(thief);
    thiefBoards.add(prisoner);

    managerBoard = new ProgressBoard(prefix + "manager", DisplaySlot.SIDEBAR_TEAM_GREEN);
    copBoard = new ProgressBoard(prefix + "cop", DisplaySlot.SIDEBAR_TEAM_RED);
  }

  void cleanup() {
    for (var board : thiefBoards) {
      board.cleanup();
    }
    copBoard.cleanup();
    managerBoard.cleanup();
  }

  void update(Game game) {
    List<String> common = new ArrayList<>();

    // エリア状況
    common.add("[エリア状況]");
    var closedAreas = game.getClosedAreas();
    if (closedAreas.length == 0) {
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

    for (int i = 0; i < 20 && i < status.length; i++) {
      // ① ~ ⑳
      var prefix = new String(Character.toChars(0x2460 + i));
      var mission = status[i];
      var st = mission.status();
      if (st.equals(MissionStatus.Fail()) || st.equals(MissionStatus.Success())) {
        var line = "└" + prefix + mission.name() + "：" + mission.status().description();
        thief.add(line);
        cop.add(line);
        manager.add(line);
      } else if (st.equals(MissionStatus.InProgress())) {
        var line = "└" + prefix + mission.name() + "：" + mission.status().rawValue;
        thief.add(line);
        cop.add("└" + prefix + "？？？：" + mission.status().rawValue);
        manager.add(line);
      } else {
        var line = "└" + prefix + "？？？：" + mission.status().rawValue;
        thief.add(line);
        cop.add(line);
        //NOTE: 本物は運営はドロボウ・ケイサツと同じのを表示している.
        // けど実際運用する場合は次起こるミッションが見られたほうがいいはず
        manager.add("└" + prefix + mission.name() + "：" + mission.status().rawValue);
      }
    }

    for (var board : thiefBoards) {
      board.update(thief.toArray(new String[]{}));
    }
    managerBoard.update(manager.toArray(new String[]{}));
    copBoard.update(cop.toArray(new String[]{}));
  }
}
