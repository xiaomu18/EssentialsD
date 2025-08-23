package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.XLogger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class ChairEvent implements Listener {
   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (EssentialsD.config.getChairEnable()) {
         if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null) {
               Player player = event.getPlayer();
               if (block.getState().getBlockData() instanceof Stairs) {
                  Stairs stairs = (Stairs)block.getState().getBlockData();
                  int chair_width = 1;
                  if (!block.getRelative(BlockFace.DOWN).isEmpty()) {
                     if (!player.isSneaking() && player.getVehicle() != null) {
                        player.getVehicle().remove();
                     } else if (!(player.getLocation().distance(block.getLocation().add((double)0.5F, (double)0.0F, (double)0.5F)) > (double)2.0F)) {
                        if (EssentialsD.config.getChairSignCheck()) {
                           boolean sign1 = false;
                           boolean sign2 = false;
                           if (stairs.getFacing() != BlockFace.NORTH && stairs.getFacing() != BlockFace.SOUTH) {
                              if (stairs.getFacing() == BlockFace.EAST || stairs.getFacing() == BlockFace.WEST) {
                                 sign1 = this.checkSign(block, BlockFace.NORTH);
                                 sign2 = this.checkSign(block, BlockFace.SOUTH);
                              }
                           } else {
                              sign1 = this.checkSign(block, BlockFace.EAST);
                              sign2 = this.checkSign(block, BlockFace.WEST);
                           }

                           if (!sign1 || !sign2) {
                              return;
                           }
                        }

                        if (EssentialsD.config.getChairMaxWidth() > 0) {
                           if (stairs.getFacing() != BlockFace.NORTH && stairs.getFacing() != BlockFace.SOUTH) {
                              if (stairs.getFacing() == BlockFace.EAST || stairs.getFacing() == BlockFace.WEST) {
                                 chair_width += this.getChairWidth(block, BlockFace.NORTH);
                                 chair_width += this.getChairWidth(block, BlockFace.SOUTH);
                              }
                           } else {
                              chair_width += this.getChairWidth(block, BlockFace.EAST);
                              chair_width += this.getChairWidth(block, BlockFace.WEST);
                           }

                           if (chair_width > EssentialsD.config.getChairMaxWidth()) {
                              return;
                           }
                        }

                        if (player.getVehicle() != null) {
                           player.getVehicle().remove();
                        }

                        ArmorStand drop = this.dropSeat(block);
                        if (!drop.addPassenger(player)) {
                           XLogger.debug("Failed to make player " + player.getName() + " sit on a chair.");
                        } else {
                           XLogger.debug("Player " + player.getName() + " is sitting on a chair.");
                        }

                        event.setUseInteractedBlock(Result.DENY);
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onPassengerLeave(EntityDismountEvent event) {
      if (EssentialsD.config.getChairEnable()) {
         Entity vehicle = event.getDismounted();
         Entity passenger = event.getEntity();
         if (vehicle instanceof ArmorStand) {
            if (passenger instanceof Player) {
               vehicle.remove();
               passenger.teleportAsync(passenger.getLocation().add((double)0.0F, (double)(EssentialsD.config.getChairSitHeight() * -1.0F), (double)0.0F));
            }
         }
      }
   }

   @EventHandler
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      if (EssentialsD.config.getChairEnable()) {
         Entity vehicle = event.getPlayer().getVehicle();
         if (vehicle != null) {
            if (vehicle instanceof ArmorStand) {
               event.setTo(event.getTo().add((double)0.0F, (double)(EssentialsD.config.getChairSitHeight() * -1.0F), (double)0.0F));
            }
         }
      }
   }

   @EventHandler
   public void onBlockBreak(BlockBreakEvent event) {
      if (EssentialsD.config.getChairEnable()) {
         if (event.getBlock().getState().getBlockData() instanceof Stairs) {
            this.clearSeat(event.getBlock());
         }
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      if (EssentialsD.config.getChairEnable()) {
         Entity vehicle = event.getPlayer().getVehicle();
         if (vehicle instanceof ArmorStand) {
            vehicle.remove();
            event.getPlayer().teleportAsync(event.getPlayer().getLocation().add((double)0.0F, (double)(EssentialsD.config.getChairSitHeight() * -1.0F), (double)0.0F));
         }

      }
   }

   private ArmorStand dropSeat(Block chair) {
      this.clearSeat(chair);
      Location location = chair.getLocation().add((double)0.5F, (double)EssentialsD.config.getChairSitHeight() + (double)0.5F, (double)0.5F);
      switch (((Stairs)chair.getState().getBlockData()).getFacing()) {
         case SOUTH:
            location.setDirection(new Vector(0, 0, -1));
            break;
         case WEST:
            location.setDirection(new Vector(1, 0, 0));
            break;
         case NORTH:
            location.setDirection(new Vector(0, 0, 1));
            break;
         case EAST:
            location.setDirection(new Vector(-1, 0, 0));
      }

      ArmorStand armorStand = (ArmorStand)chair.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
      if (!EssentialsD.config.isDebug()) {
         armorStand.setVisible(false);
      }

      armorStand.setGravity(false);
      armorStand.setInvulnerable(true);
      armorStand.setSmall(true);
      XLogger.debug("Chair dropped at " + location.toString());
      return armorStand;
   }

   private void clearSeat(Block chair) {
      Location location = chair.getLocation().add((double)0.5F, (double)EssentialsD.config.getChairSitHeight() + (double)0.5F, (double)0.5F);

      for(Entity e : location.getWorld().getNearbyEntities(location, 0.4, 0.4, 0.4)) {
         if (e instanceof ArmorStand) {
            e.remove();
         }
      }

   }

   private int getChairWidth(Block block, BlockFace face) {
      int width = 0;

      for(int i = 1; i <= EssentialsD.config.getChairMaxWidth(); ++i) {
         Block relative = block.getRelative(face, i);
         if (!(relative.getState().getBlockData() instanceof Stairs) || ((Stairs)relative.getState().getBlockData()).getFacing() != ((Stairs)block.getState().getBlockData()).getFacing()) {
            break;
         }

         ++width;
      }

      return width;
   }

   private boolean checkSign(Block block, BlockFace face) {
      for(int i = 1; i <= EssentialsD.config.getChairMaxWidth(); ++i) {
         Block relative = block.getRelative(face, i);
         if (!(relative.getState().getBlockData() instanceof Stairs)) {
            return relative.getState().getBlockData() instanceof WallSign;
         }
      }

      return false;
   }
}
