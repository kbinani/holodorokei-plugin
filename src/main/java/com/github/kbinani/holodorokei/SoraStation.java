package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.BoundingBox;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.*;

public class SoraStation extends Area {
  static class CMission extends Mission {
    enum Member {
      IROHA(0),
      KOYO(1),
      LAPLUS(2),
      LUI(3),
      CHLOE(4);

      final int index;

      private Member(int index) {
        this.index = index;
      }

      static @Nullable Member fromIndex(int index) {
        return switch (index) {
          case 0 -> IROHA;
          case 1 -> KOYO;
          case 2 -> LAPLUS;
          case 3 -> LUI;
          case 4 -> CHLOE;
          default -> null;
        };
      }

      String color() {
        return switch (this) {
          case IROHA -> "青緑色";
          case KOYO -> "桃色";
          case LAPLUS -> "紫色";
          case LUI -> "赤紫色";
          case CHLOE -> "赤色";
        };
      }
    }

    final ArrayList<Member> clickOrder = new ArrayList<>();
    final Set<Member> clicked = new HashSet<>();
    final Question question;

    static Question[] questions = new Question[]{
      new Question(new String[]{"", "背が高い者ものから", "ボタンを押せ", ""}, new Member[]{
        Member.LUI, // 161
        Member.IROHA, // 156
        Member.KOYO, // 153
        Member.CHLOE, // 148
        Member.LAPLUS, // 139
      }),
      new Question(new String[]{"", "世界に知られた順に", "ボタンを押せ", ""}, new Member[]{
        Member.LAPLUS, // 2021/11/26
        Member.LUI, // 2021/11/27
        Member.KOYO, // 2021/11/28
        Member.CHLOE, // 2021/11/29
        Member.IROHA, // 2021/11/30
      }),
      new Question(new String[]{"フルネームの", "五十音順に", "ボタンを押せ", ""}, new Member[]{
        Member.IROHA,
        Member.CHLOE,
        Member.LUI,
        Member.KOYO,
        Member.LAPLUS,
      }),
      new Question(new String[]{"3Dお披露目配信の", "早かった順に", "ボタンを押せ", ""}, new Member[]{
        Member.IROHA, // 2022/6/9
        Member.CHLOE, // 2022/6/13
        Member.KOYO, // 2022/6/16
        Member.LUI, // 2022/6/21
        Member.LAPLUS, // 2022/6/27
      }),
      new Question(new String[]{"", "誕生日の順に", "ボタンを押せ", ""}, new Member[]{
        Member.KOYO, // 3/15
        Member.CHLOE, // 5/18
        Member.LAPLUS, // 5/25
        Member.LUI, // 6/11
        Member.IROHA, // 6/18
      }),
    };

    static class Question {
      String[] text;
      Member[] correctClickOrder;

      Question(String[] text, Member[] correctClickOrder) {
        this.text = text;
        this.correctClickOrder = correctClickOrder;
      }
    }

    CMission() {
      Random random;
      try {
        random = SecureRandom.getInstance("SHA1PRNG");
      } catch (Throwable e) {
        random = new Random();
      }
      int index = random.nextInt(questions.length);
      question = questions[index];
    }

    @Override
    void start(World world) {
      for (int x : new int[]{-32, -34, -36, -38, -40}) {
        world.setBlockData(x, -48, -59, Material.AIR.createBlockData());
      }

      world.setBlockData(-36, -48, -61, Material.BIRCH_WALL_SIGN.createBlockData("[facing=north]"));
      var block = world.getBlockAt(-36, -48, -61);
      if (block.getState() instanceof Sign sign) {
        for (int i = 0; i < question.text.length && i < 4; i++) {
          sign.line(i, Component.text(question.text[i]));
        }
        sign.update();
      }
    }

    @Override
    void cleanup(World world) {
      for (int x : new int[]{-32, -34, -36, -38, -40}) {
        world.setBlockData(x, -48, -59, Material.AIR.createBlockData());
      }

      world.setBlockData(-36, -48, -61, Material.AIR.createBlockData());
    }

    @Override
    boolean onPlayerInteract(PlayerInteractEvent e) {
      var block = e.getClickedBlock();
      if (block == null) {
        return false;
      }
      var location = block.getLocation();
      int x = location.getBlockX();
      if (location.getBlockY() != -49 || location.getBlockZ() != -57) {
        return false;
      }
      int index = -1;
      if (x == -40) {
        index = 0;
      } else if (x == -38) {
        index = 1;
      } else if (x == -36) {
        index = 2;
      } else if (x == -34) {
        index = 3;
      } else if (x == -32) {
        index = 4;
      } else {
        return false;
      }
      var member = Member.fromIndex(index);
      if (member == null) {
        return false;
      }
      var player = e.getPlayer();
      if (shutdownScheduled) {
        player.sendMessage(Component.text(String.format("%sのボタンがある。今は何も起こらないようだ...", member.color())));
        return false;
      }
      clickOrder.add(member);
      if (!clicked.contains(member)) {
        clicked.add(member);
        World world = e.getPlayer().getWorld();
        world.setBlockData(-40 + 2 * index, -48, -59, Material.REDSTONE_BLOCK.createBlockData());

        // {6, 8, 10, 11, 13} = 5 + A000210
        int note = 5 + (int) Math.floor(clicked.size() * (Math.E - 1));
        player.playNote(player.getLocation(), Instrument.BIT, new Note(note));
      }
      for (int i = 0; i < clickOrder.size() - 4; i++) {
        boolean ok = true;
        for (int j = 0; j < 5; j++) {
          if (clickOrder.get(i + j) != question.correctClickOrder[j]) {
            ok = false;
            break;
          }
        }
        if (ok) {
          player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
          return true;
        }
      }
      if (clicked.size() == Member.values().length) {
        // 全てクリックして不正解だった場合は全部消灯する
        clicked.clear();
        var world = e.getPlayer().getWorld();
        for (int xx : new int[]{-32, -34, -36, -38, -40}) {
          world.setBlockData(xx, -48, -59, Material.AIR.createBlockData());
        }
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
      }
      return false;
    }

    @Override
    boolean onEntityMove(EntityMoveEvent e) {
      return false;
    }
  }

  SoraStation(World world, Scheduler scheduler) {
    super(world, scheduler);
  }

  @Override
  AreaType type() {
    return AreaType.SORA_STATION;
  }

  @Override
  ChestPosition[] chestPositionList() {
    return new ChestPosition[]{
      new ChestPosition(-75, -60, -51, BlockFace.SOUTH),
      new ChestPosition(-67, -59, -55, BlockFace.SOUTH),
      new ChestPosition(-78, -50, -45, BlockFace.NORTH),
      new ChestPosition(-86, -50, -78, BlockFace.EAST),
      new ChestPosition(-36, -50, -78, BlockFace.SOUTH),
      new ChestPosition(-28, -50, -67, BlockFace.WEST),
      new ChestPosition(-47, -59, -68, BlockFace.SOUTH),
      new ChestPosition(-49, -56, -89, BlockFace.EAST),
    };
  }

  @Override
  Point3i[] beaconPositionList() {
    return new Point3i[]{
      new Point3i(-89, -56, -32),
      new Point3i(-88, -56, -32),
      new Point3i(-52, -56, -36),
      new Point3i(-30, -57, -40),
      new Point3i(-41, -57, -75),
      new Point3i(-65, -59, -51),
      new Point3i(-70, -59, -51),
      new Point3i(-57, -50, -45),
      new Point3i(-52, -47, -65),
      new Point3i(-52, -47, -69),
      new Point3i(-59, -50, -87),
      new Point3i(-75, -50, -87),
    };
  }

  @Override
  Mission initializeMission(World world) {
    return new CMission();
  }

  @Override
  DeliveryItem deliveryItem() {
    return new DeliveryItem(Material.MINECART, "そらトレイン");
  }

  @Override
  Wall[] shutoutWalls() {
    return new Wall[]{
      new Wall(new Point3i(-23, -59, -17), new Point3i(-91, -43, -17)),
      new Wall(new Point3i(-23, -59, -18), new Point3i(-23, -43, -90)),
    };
  }

  @Override
  Point3i evacuationLocation() {
    return new Point3i(-20, -60, -18);
  }

  @Override
  BoundingBox bounds() {
    return new BoundingBox(-92, -63, -91, -22, 384, -16);
  }

  @Override
  void start(UUID sessionId) {
    super.start(sessionId);

    for (int x : new int[]{-32, -34, -36, -38, -40}) {
      world.setBlockData(x, -49, -57, Material.BIRCH_WALL_SIGN.createBlockData("[facing=south]"));
      var block = world.getBlockAt(x, -49, -57);
      if (block.getState() instanceof Sign sign) {
        sign.line(1, Component.text("[看板を右クリック]").decorate(TextDecoration.BOLD));
        sign.update();
      }
    }
  }

  @Override
  void cleanup() {
    super.cleanup();
    for (int x : new int[]{-32, -34, -36, -38, -40}) {
      world.setBlockData(x, -49, -57, Material.AIR.createBlockData());
    }
  }
}
