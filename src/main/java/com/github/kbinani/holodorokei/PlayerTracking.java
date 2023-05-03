package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.SecureRandom;
import java.util.Random;

public class PlayerTracking {
  final Player player;
  final Role role;
  private @Nullable Skill skill;
  private @Nullable SkillType activeSkillType;
  private @Nullable BukkitTask coolDownTimer;
  private final @Nonnull MainDelegate delegate;

  static Random sRandom = null;

  PlayerTracking(Player player, Role role, @Nonnull MainDelegate delegate) {
    this.player = player;
    this.role = role;
    this.delegate = delegate;
  }

  void selectSkill() {
    switch (role) {
      case RESEARCHER -> {
        var candidate = new SkillType[]{SkillType.JUMP_BOOST, SkillType.SPEED, SkillType.INVISIBILITY};
        var type = candidate[getRandomInt(candidate.length)];
        var target = type.target(role);
        if (target != null) {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
        }
      }
      case THIEF -> {
        var candidate = new SkillType[]{SkillType.JUMP_BOOST, SkillType.INVULNERABLE, SkillType.SPEED, SkillType.DARKNESS, SkillType.SLOWNESS, SkillType.COP_FINDER};
        var type = candidate[getRandomInt(candidate.length)];
        var target = type.target(role);
        if (target != null) {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
        }
      }
      case FEMALE_EXECUTIVE -> {
        var type = SkillType.THIEF_FINDER;
        var target = type.target(role);
        if (target != null) {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
        }
      }
      case CLEANER -> {
        var type = SkillType.INVISIBILITY;
        var target = type.target(role);
        if (target != null) {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
        }
      }
      case MANAGER -> this.skill = null;
    }
  }

  record SkillActivationResult(EffectTarget target, PotionEffect effect) {
  }

  @Nullable
  SkillActivationResult tryActivatingSkill() {
    if (role == Role.CLEANER) {
      //NOTE: 掃除屋はホロ杖の効果を起動させない
      return null;
    }
    if (activeSkillType != null) {
      return null;
    }
    if (coolDownTimer != null) {
      return null;
    }
    var skill = this.skill;
    if (skill == null) {
      return null;
    }
    var effect = skill.createPotionEffect();
    if (effect == null) {
      return null;
    }
    activeSkillType = skill.type();
    var message = player.teamDisplayName().append(Component.text("が ").color(NamedTextColor.WHITE));
    if (role == Role.RESEARCHER) {
      message = message.append(Component.text(String.format("特殊能力：研究者（%s）", skill.type().description())).color(NamedTextColor.DARK_PURPLE));
    } else if (role == Role.FEMALE_EXECUTIVE) {
      message = message.append(Component.text("特殊能力：女幹部").color(NamedTextColor.DARK_PURPLE));
    } else if (role == Role.THIEF) {
      message = message.append(Component.text(String.format("特殊能力：%s", skill.type().description())).color(NamedTextColor.BLUE));
    }
    message = message.append(Component.text(" を発動！").color(NamedTextColor.WHITE));
    player.sendMessage(message);
    var scheduler = Bukkit.getScheduler();
    coolDownTimer = scheduler.runTaskLater(delegate.mainDelegateGetOwner(), this::onCoolDown, skill.cooldownTicks());
    if (skill.target() == EffectTarget.SELF) {
      player.addPotionEffect(effect);
      return null;
    } else {
      return new SkillActivationResult(skill.target(), effect);
    }
  }

  void deactivateSkill() {
    if (role != Role.CLEANER) {
      //NOTE: ホロ杖無しにスキルを有効化できるのは掃除屋だけ
      return;
    }
    if (activeSkillType != null) {
      var type = activeSkillType.potionEffectType();
      if (type != null && activeSkillType.target(role) == EffectTarget.SELF) {
        player.removePotionEffect(type);
      }
      activeSkillType = null;
    }
  }

  void activateSkill() {
    if (role != Role.CLEANER) {
      //NOTE: ホロ杖無しにスキルを有効化できるのは掃除屋だけ
      return;
    }
    assert (this.skill != null);
    activeSkillType = this.skill.type();
    var effect = this.skill.createPotionEffect();
    assert (effect != null);
    player.addPotionEffect(effect);
  }

  private void onCoolDown() {
    activeSkillType = null;
    coolDownTimer = null;
    if (role == Role.RESEARCHER) {
      selectSkill();
    }
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
