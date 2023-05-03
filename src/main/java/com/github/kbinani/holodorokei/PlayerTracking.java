package com.github.kbinani.holodorokei;

import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.Random;

public class PlayerTracking {
  final Player player;
  final Role role;
  private @Nullable Skill skill;

  static Random sRandom = null;

  PlayerTracking(Player player, Role role) {
    this.player = player;
    this.role = role;
  }

  @Nullable
  Skill choooseSkill() {
    switch (role) {
      case RESEARCHER -> {
        var candidate = new SkillType[]{SkillType.JUMP_BOOST, SkillType.SPEED, SkillType.INVISIBILITY};
        var type = candidate[getRandomInt(candidate.length)];
        this.skill = new Skill(type, type.coolDownMillis(role), type.effectiveMillis(role));
      }
      case THIEF -> {
        var candidate = new SkillType[]{SkillType.JUMP_BOOST, SkillType.INVULNERABLE, SkillType.SPEED, SkillType.DARKNESS, SkillType.SLOWNESS, SkillType.COP_FINDER};
        var type = candidate[getRandomInt(candidate.length)];
        this.skill = new Skill(type, type.coolDownMillis(role), type.effectiveMillis(role));
      }
      case FEMALE_EXECUTIVE -> {
        var type = SkillType.THIEF_FINDER;
        this.skill = new Skill(type, type.coolDownMillis(role), type.effectiveMillis(role));
      }
      case CLEANER -> {
        var type = SkillType.INVISIBILITY;
        this.skill = new Skill(type, type.coolDownMillis(role), type.effectiveMillis(role));
      }
      case MANAGER -> this.skill = null;
    }
    return this.skill;
  }

  @Nullable
  Skill skill() {
    return this.skill;
  }

  private int getRandomInt(int bound) {
    if (sRandom == null) {
      try {
        sRandom = SecureRandom.getInstance("SHA1PRNG");
      } catch (Throwable e) {
        sRandom = new Random();
      }
    }
    return sRandom.nextInt(bound);
  }
}
