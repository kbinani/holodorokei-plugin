package com.github.kbinani.holodorokei;

public class Game {
    final GameSetting setting;

    Game(GameSetting setting) {
        this.setting = setting;
    }

    void terminate() {
        setting.reset();
    }
}
