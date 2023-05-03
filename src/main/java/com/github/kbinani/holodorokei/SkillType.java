package com.github.kbinani.holodorokei;

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

  long coolDownMillis(Role role) {
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

  long effectiveMillis(Role role) {
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
        return 0;
      default:
        return 0;
    }
  }
}
