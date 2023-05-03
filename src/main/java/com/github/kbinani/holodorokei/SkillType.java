package com.github.kbinani.holodorokei;

import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;

public enum SkillType {
  // 共通
  NONE,
  JUMP_BOOST,
  SPEED,

  // ケイサツ
  THIEF_FINDER,
  INVISIBILITY,

  // ドロボウ
  COP_FINDER,
  INVULNERABLE,
  DARKNESS,
  SLOWNESS;

  int coolDownSeconds(Role role) {
    switch (role) {
      case THIEF:
        return switch (this) {
          case JUMP_BOOST, INVULNERABLE, SPEED, DARKNESS, SLOWNESS, COP_FINDER -> 60;
          default -> 0;
        };
      case FEMALE_EXECUTIVE:
        if (this == THIEF_FINDER) {
          return 40;
        } else {
          return 0;
        }
      case RESEARCHER:
        return switch (this) {
          case JUMP_BOOST, SPEED, INVISIBILITY -> 15;
          default -> 0;
        };
      case CLEANER:
        return 0;
      default:
        return 0;
    }
  }

  int effectiveSeconds(Role role) {
    switch (role) {
      case THIEF:
        return switch (this) {
          case JUMP_BOOST -> 20;
          case INVULNERABLE -> 15;
          case SPEED -> 10;
          case DARKNESS -> 8;
          case SLOWNESS -> 10;
          case COP_FINDER -> 3;
          default -> 0;
        };
      case FEMALE_EXECUTIVE:
        if (this == THIEF_FINDER) {
          return 10;
        } else {
          return 0;
        }
      case RESEARCHER:
        return switch (this) {
          case JUMP_BOOST -> 15;
          case SPEED -> 10;
          case INVISIBILITY -> 9;
          default -> 0;
        };
      case CLEANER:
        if (this == INVISIBILITY) {
          return 20 * 60 * 1000;
        } else {
          return 0;
        }
      default:
        return 0;
    }
  }

  @Nullable
  PotionEffectType potionEffectType() {
    return switch (this) {
      case NONE -> null;
      case JUMP_BOOST -> PotionEffectType.JUMP;
      case SPEED -> PotionEffectType.SPEED;
      case THIEF_FINDER -> PotionEffectType.GLOWING;
      case INVISIBILITY -> PotionEffectType.INVISIBILITY;
      case COP_FINDER -> PotionEffectType.GLOWING;
      case INVULNERABLE -> null;
      case DARKNESS -> PotionEffectType.DARKNESS;
      case SLOWNESS -> PotionEffectType.SLOW;
    };
  }

  @Nullable
  EffectTarget target(Role role) {
    switch (role) {
      case THIEF:
        return switch (this) {
          case JUMP_BOOST -> EffectTarget.SELF;
          case INVULNERABLE -> EffectTarget.SELF;
          case SPEED -> EffectTarget.SELF;
          case DARKNESS -> EffectTarget.COP;
          case SLOWNESS -> EffectTarget.COP;
          case COP_FINDER -> EffectTarget.COP;
          default -> null;
        };
      case FEMALE_EXECUTIVE:
        if (this == THIEF_FINDER) {
          return EffectTarget.THIEF;
        } else {
          return null;
        }
      case RESEARCHER:
        return switch (this) {
          case JUMP_BOOST -> EffectTarget.SELF;
          case SPEED -> EffectTarget.SELF;
          case INVISIBILITY -> EffectTarget.SELF;
          default -> null;
        };
      case CLEANER:
        if (this == INVISIBILITY) {
          return EffectTarget.SELF;
        } else {
          return null;
        }
      default:
        return null;
    }
  }

  String description() {
    return switch (this) {
      case NONE -> "なし";
      case JUMP_BOOST -> "ジャンプ力上昇";
      case SPEED -> "移動速度上昇";
      case INVULNERABLE -> "無敵化";
      case DARKNESS -> "暗闇";
      case INVISIBILITY -> "透明化";
      case SLOWNESS -> "移動速度低下";
      case COP_FINDER -> "ケイサツサーチ";
      case THIEF_FINDER -> "女幹部";
    };
  }
}
