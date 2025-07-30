package cn.lunadeer.minecraftpluginutils;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class Teleport {
   public static CompletableFuture doTeleportSafely(Player player, Location location) {
      CompletableFuture<Boolean> future = new CompletableFuture();
      if (!Common.isPaper()) {
         Location loc = getSafeTeleportLocation(location);
         if (loc == null) {
            Notification.error(player, "传送目的地不安全，已取消传送");
            future.complete(false);
            return future;
         }

         player.teleport(loc, TeleportCause.PLUGIN);
      } else {
         location.getWorld().getChunkAtAsyncUrgently(location).thenAccept((chunk) -> {
            Location loc = getSafeTeleportLocation(location);
            if (loc == null) {
               Notification.error(player, "传送目的地不安全，已取消传送");
               future.complete(false);
            } else {
               player.teleportAsync(loc, TeleportCause.PLUGIN);
               future.complete(true);
            }
         });
      }

      return future;
   }

   public static Location getSafeTeleportLocation(Location location) {
      int max_attempts = 512;

      while(location.getBlock().isPassable()) {
         location.setY(location.getY() - (double)1.0F);
         --max_attempts;
         if (max_attempts <= 0) {
            return null;
         }
      }

      Block up1 = location.getBlock().getRelative(BlockFace.UP);
      Block up2 = up1.getRelative(BlockFace.UP);
      max_attempts = 512;

      while(!up1.isPassable() || up1.isLiquid() || !up2.isPassable() || up2.isLiquid()) {
         location.setY(location.getY() + (double)1.0F);
         up1 = location.getBlock().getRelative(BlockFace.UP);
         up2 = up1.getRelative(BlockFace.UP);
         --max_attempts;
         if (max_attempts <= 0) {
            return null;
         }
      }

      location.setY(location.getY() + (double)1.0F);
      if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.LAVA) {
         return null;
      } else {
         return location;
      }
   }
}
