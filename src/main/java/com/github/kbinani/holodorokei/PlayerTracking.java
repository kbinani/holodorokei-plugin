package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerTracking {
  Player player;
  final Role role;
  private @Nullable Skill skill;
  private @Nullable SkillType activeSkillType;
  private long skillCoolDownMillis;
  private @Nullable BukkitTask coolDownTimer;
  private final @Nonnull Scheduler scheduler;
  private @Nullable BukkitTask actionBarUpdateTimer;
  private @Nullable BukkitTask invulnerableTimeoutTimer;
  private long resurrectionTimeoutMillis;
  private boolean arrested = false;
  private final WeakReference<PlayerTrackingDelegate> delegate;
  private final Logger logger;

  static Random sRandom = null;

  PlayerTracking(Player player, Role role, @Nonnull Scheduler scheduler, @Nonnull PlayerTrackingDelegate delegate, Logger parent) {
    this.player = player;
    this.role = role;
    this.scheduler = scheduler;
    this.delegate = new WeakReference<>(delegate);
    this.logger = Logger.getLogger(parent.getName() + "(" + role.name() + "," + player.getName() + ")");
    this.logger.setParent(parent);
  }

  void selectSkill() {
    switch (role) {
      case RESEARCHER -> {
        var candidate = new SkillType[]{SkillType.JUMP_BOOST, SkillType.SPEED, SkillType.INVISIBILITY};
        var type = candidate[getRandomInt(candidate.length)];
        var target = type.target(role);
        if (target == null) {
          logger.log(Level.WARNING, "select skill: " + type.description() + "; target cannot be identified");
        } else {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
          logger.log(Level.INFO, "select skill: " + type.description());
        }
      }
      case THIEF -> {
        var candidate = new SkillType[]{SkillType.JUMP_BOOST, SkillType.INVULNERABLE, SkillType.SPEED, SkillType.DARKNESS, SkillType.SLOWNESS, SkillType.COP_FINDER};
        var type = candidate[getRandomInt(candidate.length)];
        var target = type.target(role);
        if (target == null) {
          logger.log(Level.WARNING, "select skill: " + type.description() + "; target cannot be identified");
        } else {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
          logger.log(Level.INFO, "select skill: " + type.description());
        }
      }
      case FEMALE_EXECUTIVE -> {
        var type = SkillType.THIEF_FINDER;
        var target = type.target(role);
        if (target == null) {
          logger.log(Level.WARNING, "select skill: " + type.description() + "; target cannot be identified");
        } else {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
          logger.log(Level.INFO, "select skill: " + type.description());
        }
      }
      case CLEANER -> {
        var type = SkillType.INVISIBILITY;
        var target = type.target(role);
        if (target == null) {
          logger.log(Level.WARNING, "select skill: " + type.description() + "; target cannot be identified");
        } else {
          this.skill = new Skill(type, target, type.coolDownSeconds(role), type.effectiveSeconds(role));
          logger.log(Level.INFO, "select skill: " + type.description());
        }
      }
    }
    updateActionBar();
  }

  record SkillActivationResult(EffectTarget target, PotionEffectType effectType, long activeUntilMillis) {
  }

  @Nullable
  SkillActivationResult tryActivatingSkill(boolean shortened) {
    final String logTitle = "try activating skill: ";
    if (role == Role.CLEANER) {
      //NOTE: 掃除屋はホロ杖の効果を起動させない
      return null;
    }
    if (activeSkillType != null) {
      logger.log(Level.WARNING, logTitle + "skip, activeSkillType not null");
      return null;
    }
    if (coolDownTimer != null) {
      logger.log(Level.WARNING, logTitle + "skip, coolDownTimer not null");
      return null;
    }
    var skill = this.skill;
    if (skill == null) {
      logger.log(Level.WARNING, logTitle + "skip, skill is null");
      return null;
    }
    activeSkillType = skill.type();
    int coolDownTicks = skill.coolDownTicks();
    if (role == Role.THIEF && shortened) {
      // 60 => 40
      coolDownTicks = coolDownTicks * 2 / 3;
    }
    long coolDownMillis = (long) coolDownTicks * 1000 / 20;
    skillCoolDownMillis = System.currentTimeMillis() + coolDownMillis;
    var message = player.teamDisplayName().append(Component.text("が ").color(NamedTextColor.WHITE));
    if (role == Role.RESEARCHER) {
      message = message.append(Component.text(String.format("特殊能力：研究者（%s）", skill.type().description())).color(NamedTextColor.DARK_PURPLE));
    } else if (role == Role.FEMALE_EXECUTIVE) {
      message = message.append(Component.text("特殊能力：女幹部").color(NamedTextColor.DARK_PURPLE));
    } else if (role == Role.THIEF) {
      TextColor color;
      if (skill.target() == EffectTarget.SELF) {
        color = NamedTextColor.RED;
      } else {
        color = NamedTextColor.BLUE;
      }
      message = message.append(Component.text(String.format("特殊能力：%s", skill.type().description())).color(color));
    }
    message = message.append(Component.text(" を発動！").color(NamedTextColor.WHITE));
    player.sendMessage(message);
    logger.log(Level.INFO, logTitle + "activating skill: " + skill.type().description() + ", cooldown " + coolDownMillis + "ms");
    var delegate = this.delegate.get();
    if (delegate != null) {
      delegate.playerTrackingDidUseSkill(message);
    }
    coolDownTimer = scheduler.runTaskLater(this::onCoolDown, coolDownTicks);
    restartActionBarUpdateTimer();
    if (skill.target() == EffectTarget.SELF) {
      if (skill.type() == SkillType.INVULNERABLE) {
        addInvulnerablePotionEffect(skill.effectiveTicks());

        if (invulnerableTimeoutTimer != null) {
          invulnerableTimeoutTimer.cancel();
        }
        invulnerableTimeoutTimer = scheduler.runTaskLater(() -> {
          activeSkillType = null;
          updateActionBar();
        }, skill.effectiveTicks());
      } else {
        var effect = skill.createPotionEffect();
        if (effect != null) {
          player.addPotionEffect(effect);
        }
      }
      return null;
    } else {
      var effectType = skill.type().potionEffectType();
      if (effectType == null) {
        return null;
      }
      return new SkillActivationResult(skill.target(), effectType, System.currentTimeMillis() + (long) skill.effectiveSeconds() * 1000);
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
      logger.log(Level.INFO, "deactivate skill: " + activeSkillType.description());
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
    logger.log(Level.INFO, "activate skill: " + this.skill.type().description());
  }

  boolean isInvulnerable() {
    if (activeSkillType != null && activeSkillType == SkillType.INVULNERABLE) {
      return true;
    }
    return System.currentTimeMillis() < resurrectionTimeoutMillis;
  }

  void depriveSkill() {
    logger.log(Level.INFO, "deprive skill");
    if (coolDownTimer != null) {
      coolDownTimer.cancel();
      coolDownTimer = null;
    }
    if (invulnerableTimeoutTimer != null) {
      invulnerableTimeoutTimer.cancel();
      invulnerableTimeoutTimer = null;
    }
    if (activeSkillType != null) {
      var effectType = activeSkillType.potionEffectType();
      if (effectType != null && activeSkillType.target(role) == EffectTarget.SELF) {
        player.removePotionEffect(effectType);
      }
      activeSkillType = null;
    }
    skillCoolDownMillis = 0;
    skill = null;
    resurrectionTimeoutMillis = 0;
    updateActionBar();
  }

  private void onCoolDown() {
    var before = activeSkillType;
    activeSkillType = null;
    coolDownTimer = null;
    logger.log(Level.INFO, "on cooldown skill: " + (before == null ? "N/A" : before.description()));
    if (role == Role.RESEARCHER) {
      selectSkill();
    }
    updateActionBar();
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

  private void updateActionBar() {
    var component = Component.empty();
    if (role == Role.THIEF) {
      if (arrested) {
        component = Component.text("捕まっています！");
      } else {
        if (skill == null) {
          component = Component.text("特殊能力：なし");
        } else {
          component = Component.text(String.format("特殊能力：%s", skill.type().description()));
        }
      }
    } else {
      if (role == Role.CLEANER) {
        component = Component.text("特殊能力：掃除屋");
      } else if (role == Role.FEMALE_EXECUTIVE) {
        component = Component.text("特殊能力：女幹部");
      } else if (role == Role.RESEARCHER) {
        component = Component.text("特殊能力：研究者");
      } else {
        return;
      }
    }
    var remaining = skillCoolDownMillis - System.currentTimeMillis();
    if (remaining > 0) {
      component = component.append(Component.text(String.format("（クールタイム中：あと%d秒）", (int) Math.round(remaining / 1000.0))).color(NamedTextColor.GOLD));
    }
    player.sendActionBar(component);
  }

  private void restartActionBarUpdateTimer() {
    if (actionBarUpdateTimer != null) {
      actionBarUpdateTimer.cancel();
    }
    updateActionBar();
    actionBarUpdateTimer = scheduler.runTaskTimer(this::updateActionBar, 20, 20);
  }

  void start() {
    updateActionBar();
    actionBarUpdateTimer = scheduler.runTaskTimer(this::updateActionBar, 20, 20);
    player.setFoodLevel(20);
    player.setHealth(20);
    logger.log(Level.INFO, "start");
  }

  void cleanup() {
    if (actionBarUpdateTimer != null) {
      actionBarUpdateTimer.cancel();
      actionBarUpdateTimer = null;
    }
    if (invulnerableTimeoutTimer != null) {
      invulnerableTimeoutTimer.cancel();
      invulnerableTimeoutTimer = null;
    }
    if (coolDownTimer != null) {
      coolDownTimer.cancel();
      coolDownTimer = null;
    }
    for (var effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
    logger.log(Level.INFO, "cleanup");
  }

  private void addInvulnerablePotionEffect(int ticks) {
    var effect = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, ticks, 10, false);
    player.addPotionEffect(effect);
    logger.log(Level.INFO, "add invulnerable potion effect");
  }

  void addInvulnerableByResurrection(int seconds) {
    resurrectionTimeoutMillis = System.currentTimeMillis() + (long) seconds * 1000;
    addInvulnerablePotionEffect(seconds * 20);
    logger.log(Level.INFO, "add invulnerable by resurrection");
  }

  void setArrested(boolean arrested) {
    if (role != Role.THIEF) {
      return;
    }
    this.arrested = arrested;
    logger.log(Level.INFO, "arrested: " + arrested);
  }

  boolean isArrested() {
    return arrested;
  }
}
