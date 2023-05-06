package com.github.kbinani.holodorokei;

import net.kyori.adventure.text.Component;

public interface PlayerTrackingDelegate {
  void playerTrackingDidUseSkill(Component message);
}
