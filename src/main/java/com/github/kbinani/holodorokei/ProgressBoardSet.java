package com.github.kbinani.holodorokei;

import org.bukkit.scoreboard.DisplaySlot;

import java.util.ArrayList;
import java.util.Arrays;
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
    var closedAreas = new ArrayList<AreaType>();
    for (var mission : game.getAreaMissions()) {
      if (mission.status().equals(MissionStatus.Fail())) {
        closedAreas.add(mission.type());
      }
    }
    if (closedAreas.size() == 0) {
      common.add("封鎖エリア：なし");
    } else {
      for (var area : closedAreas) {
        common.add("└" + area.description() + "：封鎖");
      }
    }
    common.add("+" + "-".repeat(16) + "+");

    // 納品ミッション
    common.add("[納品ミッション]");
    var deliveryMissions = game.getDeliveryMissions();
    if (Arrays.stream(deliveryMissions).allMatch(DeliveryMissionStatus::completed)) {
      common.add("└成功！");
    } else {
      for (var mission : deliveryMissions) {
        common.add("└" + mission.type().description() + "：" + (mission.completed() ? "o" : "x"));
      }
    }
    common.add("+" + "-".repeat(14) + "+");

    // エリアミッション
    common.add("[エリアミッション]");
    var thief = new ArrayList<>(common);
    var cop = new ArrayList<>(common);
    var manager = new ArrayList<>(common);
    var status = game.getAreaMissions();

    for (int i = 0; i < 20 && i < status.length; i++) {
      // ① ~ ⑳
      var prefix = new String(Character.toChars(0x2460 + i));
      var mission = status[i];
      var st = mission.status();
      if (st.equals(MissionStatus.Fail()) || st.equals(MissionStatus.Success())) {
        var line = "└" + prefix + mission.type().description() + "：" + mission.status().description();
        thief.add(line);
        cop.add(line);
        manager.add(line);
      } else if (st.equals(MissionStatus.InProgress())) {
        var line = "└" + prefix + mission.type().description() + "：" + mission.status().description();
        thief.add(line);
        cop.add("└" + prefix + "？？？：" + mission.status().description());
        manager.add(line);
      } else {
        var line = "└" + prefix + "？？？：" + mission.status().description();
        thief.add(line);
        cop.add(line);
        //NOTE: 本物は運営はドロボウ・ケイサツと同じのを表示している.
        // けど実際運用する場合は次起こるミッションが見られたほうがいいはず
        manager.add("└" + prefix + mission.type().description() + "：" + mission.status().description());
      }
    }

    for (var board : thiefBoards) {
      board.update(thief.toArray(new String[]{}));
    }
    managerBoard.update(manager.toArray(new String[]{}));
    copBoard.update(cop.toArray(new String[]{}));
  }
}
