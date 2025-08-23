package cn.lunadeer.essentialsd.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocUtil {
   public static String toString(Location loc) {
      return String.format("[%s, %.2f, %.2f, %.2f, %.2f, %.2f]", loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
   }

   public static Location fromString(String str) {
      String[] parts = str.substring(1, str.length() - 1).split(", ");
      return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
   }
}
