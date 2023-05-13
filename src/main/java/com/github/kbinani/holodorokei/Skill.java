package com.github.kbinani.holodorokei;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record Skill(SkillType type, @Nonnull EffectTarget target, int coolDownSeconds, int effectiveSeconds) {
  @Nullable
  PotionEffect createPotionEffect() {
    var type = type().potionEffectType();
    if (type == null) {
      return null;
    }
    if (type.equals(PotionEffectType.INVISIBILITY)) {
      return new PotionEffect(type, effectiveTicks(), 1, false, false);
    } else {
      return new PotionEffect(type, effectiveTicks(), 1, false);
    }
  }

  int coolDownTicks() {
    return coolDownSeconds * 20;
  }

  int effectiveTicks() {
    return effectiveSeconds * 20;
  }
}
