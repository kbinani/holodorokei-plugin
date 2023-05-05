package com.github.kbinani.holodorokei;

import org.bukkit.potion.PotionEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record Skill(SkillType type, @Nonnull EffectTarget target, int coolDownSeconds, int effectiveSeconds) {
  @Nullable
  PotionEffect createPotionEffect() {
    var type = type().potionEffectType();
    if (type == null) {
      return null;
    }
    return new PotionEffect(type, effectiveTicks(), 1);
  }

  int coolDownTicks() {
    return coolDownSeconds * 20;
  }

  int effectiveTicks() {
    return effectiveSeconds * 20;
  }
}
