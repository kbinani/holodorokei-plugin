package com.github.kbinani.holodorokei;

public enum MissionStatus {
    WILL_START_18("18:00〜"),
    WILL_START_12("12:00〜"),
    WILL_START_6("6:00〜"),
    IN_PROGRESS("発生中"),
    SUCCESS("成功！"),
    FAIL("失敗...");

    private MissionStatus(String rawValue) {
        this.rawValue = rawValue;
    }

    final String rawValue;
}
