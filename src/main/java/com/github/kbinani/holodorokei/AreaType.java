package com.github.kbinani.holodorokei;

public enum AreaType {
  SORA_STATION,
  SIKE_MURA,
  SHIRANUI_KENSETSU,
  DODODO_TOWN;

  String description() {
    return switch (this) {
      case SIKE_MURA -> "しけ村";
      case DODODO_TOWN -> "ドドドタウン";
      case SORA_STATION -> "そらステーション";
      case SHIRANUI_KENSETSU -> "不知火建設本社";
    };
  }
}
