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
    return new PotionEffect(type, effectiveSeconds * 20, 1);
  }

  int cooldownTicks() {
    return coolDownSeconds * 20;
  }
}