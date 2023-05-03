package com.github.kbinani.holodorokei;

public class MissionStatus {
  static MissionStatus Waiting(int v) {
    if (v <= 0) {
      throw new RuntimeException();
    }
    return new MissionStatus(v);
  }

  static MissionStatus InProgress() {
    return new MissionStatus(0);
  }

  static MissionStatus Success() {
    return new MissionStatus(-1);
  }

  static MissionStatus Fail() {
    return new MissionStatus(-2);
  }

  private MissionStatus(int rawValue) {
    this.rawValue = rawValue;
  }

  private final int rawValue;

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MissionStatus s) {
      return this.rawValue == s.rawValue;
    } else {
      return false;
    }
  }

  String description() {
    if (rawValue == 0) {
      return "発生中";
    } else if (rawValue == -1) {
      return "成功！";
    } else if (rawValue == -2) {
      return "失敗...";
    } else {
      return String.format("%d:00〜", rawValue);
    }
  }
}
