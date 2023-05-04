package com.github.kbinani.holodorokei;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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

        world.setBlockData(x, -49, -57, Material.BIRCH_WALL_SIGN.createBlockData("[facing=south]"));
        var block = world.getBlockAt(x, -49, -57);
        if (block.getState() instanceof Sign sign) {
          sign.line(1, Component.text("[看板を右クリック]").decorate(TextDecoration.BOLD));
          sign.update();
        }
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
        world.setBlockData(x, -49, -57, Material.AIR.createBlockData());
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
      e.setCancelled(true);
      clickOrder.add(member);
      if (!clicked.contains(member)) {
        clicked.add(member);
        World world = e.getPlayer().getWorld();
        world.setBlockData(-40 + 2 * index, -48, -59, Material.REDSTONE_BLOCK.createBlockData());
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
      }
      return false;
    }

    @Override
    boolean onEntityMove(EntityMoveEvent e) {
      return false;
    }
  }

  SoraStation(World world) {
    super(world);
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
}
