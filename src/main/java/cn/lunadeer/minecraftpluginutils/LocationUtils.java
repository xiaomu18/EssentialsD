package cn.lunadeer.minecraftpluginutils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtils {
   public static String Serialize(Location loc) {
      String var10000 = loc.getWorld().getName();
      return var10000 + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
   }

   public static Location Deserialize(String loc) {
      String[] parts = loc.split(",");
      World world = Bukkit.getWorld(parts[0]);
      double x = Double.parseDouble(parts[1]);
      double y = Double.parseDouble(parts[2]);
      double z = Double.parseDouble(parts[3]);
      float yaw = Float.parseFloat(parts[4]);
      float pitch = Float.parseFloat(parts[5]);
      return new Location(world, x, y, z, yaw, pitch);
   }
}
