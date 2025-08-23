package cn.lunadeer.minecraftpluginutils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ParticleRender {
   private static final int renderMaxRadius = 48;

   public static void showBoxBorder(JavaPlugin plugin, Player player, Location loc1, Location loc2) {
      Scheduler scheduler = new Scheduler(plugin);
      Scheduler.runTask(() -> {
         if (loc1.getWorld().equals(loc2.getWorld())) {
            int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
            int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
            int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
            int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX()) + 1;
            int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY()) + 1;
            int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ()) + 1;
            World world = loc1.getWorld();

            for(int x = minX; x <= maxX; ++x) {
               if (x >= player.getLocation().getBlockX() - 48 && x <= player.getLocation().getBlockX() + 48) {
                  spawnParticle(world, (double)x, (double)minY, (double)minZ);
                  spawnParticle(world, (double)x, (double)minY, (double)maxZ);
                  spawnParticle(world, (double)x, (double)maxY, (double)minZ);
                  spawnParticle(world, (double)x, (double)maxY, (double)maxZ);
               }
            }

            for(int y = minY; y <= maxY; ++y) {
               if (y >= player.getLocation().getBlockY() - 24 && y <= player.getLocation().getBlockY() + 24) {
                  spawnParticle(world, (double)minX, (double)y, (double)minZ);
                  spawnParticle(world, (double)minX, (double)y, (double)maxZ);
                  spawnParticle(world, (double)maxX, (double)y, (double)minZ);
                  spawnParticle(world, (double)maxX, (double)y, (double)maxZ);
               }
            }

            for(int z = minZ; z <= maxZ; ++z) {
               if (z >= player.getLocation().getBlockZ() - 48 && z <= player.getLocation().getBlockZ() + 48) {
                  spawnParticle(world, (double)minX, (double)minY, (double)z);
                  spawnParticle(world, (double)minX, (double)maxY, (double)z);
                  spawnParticle(world, (double)maxX, (double)minY, (double)z);
                  spawnParticle(world, (double)maxX, (double)maxY, (double)z);
               }
            }

         }
      });
   }

   public static void showBoxFace(JavaPlugin plugin, Player player, Location loc1, Location loc2) {
      Scheduler scheduler = new Scheduler(plugin);
      Scheduler.runTask(() -> {
         if (loc1.getWorld().equals(loc2.getWorld())) {
            int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
            int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
            int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
            int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX()) + 1;
            int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY()) + 1;
            int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ()) + 1;
            int player_minx = player.getLocation().getBlockX() - 48;
            int player_maxx = player.getLocation().getBlockX() + 48;
            int player_miny = player.getLocation().getBlockY() - 24;
            int player_maxy = player.getLocation().getBlockY() + 24;
            int player_minz = player.getLocation().getBlockZ() - 48;
            int player_maxz = player.getLocation().getBlockZ() + 48;
            boolean skip_minx = false;
            boolean skip_maxx = false;
            boolean skip_minz = false;
            boolean skip_maxz = false;
            int[] adjustedX = adjustBoundary(player_minx, player_maxx, minX, maxX);
            if (minX != adjustedX[0]) {
               skip_minx = true;
               minX = adjustedX[0];
            }

            if (maxX != adjustedX[1]) {
               skip_maxx = true;
               maxX = adjustedX[1];
            }

            int[] adjustedZ = adjustBoundary(player_minz, player_maxz, minZ, maxZ);
            if (minZ != adjustedZ[0]) {
               skip_minz = true;
               minZ = adjustedZ[0];
            }

            if (maxZ != adjustedZ[1]) {
               skip_maxz = true;
               maxZ = adjustedZ[1];
            }

            int[] adjustedY = adjustBoundary(player_miny, player_maxy, minY, maxY);
            minY = adjustedY[0];
            maxY = adjustedY[1];

            for(int y = minY; y <= maxY; ++y) {
               for(int x = minX; x <= maxX; ++x) {
                  if (!skip_minz) {
                     spawnParticle(player, (double)x, (double)y, (double)minZ);
                  }

                  if (!skip_maxz) {
                     spawnParticle(player, (double)x, (double)y, (double)maxZ);
                  }
               }

               for(int z = minZ; z <= maxZ; ++z) {
                  if (!skip_minx) {
                     spawnParticle(player, (double)minX, (double)y, (double)z);
                  }

                  if (!skip_maxx) {
                     spawnParticle(player, (double)maxX, (double)y, (double)z);
                  }
               }
            }

         }
      });
   }

   private static void spawnParticle(Player player, double x, double y, double z) {
      player.spawnParticle(Particle.FLAME, x, y, z, 2, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
   }

   private static void spawnParticle(World world, double x, double y, double z) {
      world.spawnParticle(Particle.FLAME, x, y, z, 2, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
   }

   private static int[] adjustBoundary(int playerMin, int playerMax, int boundaryMin, int boundaryMax) {
      if (playerMax <= boundaryMin) {
         boundaryMin = boundaryMax;
      } else if (playerMax <= boundaryMax) {
         boundaryMax = playerMax;
         if (playerMin >= boundaryMin) {
            boundaryMin = playerMin;
         }
      } else if (playerMin > boundaryMin) {
         boundaryMin = playerMin;
      } else if (playerMin > boundaryMax) {
         boundaryMin = boundaryMax;
      }

      return new int[]{boundaryMin, boundaryMax};
   }
}
