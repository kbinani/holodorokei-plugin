package com.github.kbinani.holodorokei;

import org.bukkit.Location;

import java.util.Objects;

public class Point3i {
  public int x;
  public int y;
  public int z;

  public Point3i(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Point3i(Location l) {
    this.x = l.getBlockX();
    this.y = l.getBlockY();
    this.z = l.getBlockZ();
  }

  public Point3i(Point3i other) {
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof Point3i v)) {
      return false;
    }
    return v.x == x && v.y == y && v.z == z;
  }

  @Override
  public String toString() {
    return String.format("[%d,%d,%d]", x, y, z);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x, y, z);
  }
}
