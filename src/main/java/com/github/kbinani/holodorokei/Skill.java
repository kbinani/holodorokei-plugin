package com.github.kbinani.holodorokei;

public record Skill(SkillType type, long coolDownMillis, long effectiveMillis) {
}
