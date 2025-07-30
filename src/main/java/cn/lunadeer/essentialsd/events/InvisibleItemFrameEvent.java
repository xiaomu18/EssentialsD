package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.recipes.InvisibleGlowItemFrame;
import cn.lunadeer.essentialsd.recipes.InvisibleItemFrame;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InvisibleItemFrameEvent implements Listener {
   @EventHandler
   public void placeItemFrame(HangingPlaceEvent event) {
      Entity entity = event.getEntity();
      if (entity.getType() == EntityType.ITEM_FRAME || entity.getType() == EntityType.GLOW_ITEM_FRAME) {
         if (((ItemStack)Objects.requireNonNull(event.getItemStack())).getItemMeta().getPersistentDataContainer().has(new NamespacedKey(EssentialsD.instance, "invisible"), PersistentDataType.BYTE)) {
            if (entity.getType() == EntityType.ITEM_FRAME) {
               ItemFrame itemFrame = (ItemFrame)entity;
               itemFrame.getPersistentDataContainer().set(new NamespacedKey(EssentialsD.instance, "invisible"), PersistentDataType.BYTE, (byte)1);
            } else if (entity.getType() == EntityType.GLOW_ITEM_FRAME) {
               GlowItemFrame itemFrame = (GlowItemFrame)entity;
               itemFrame.getPersistentDataContainer().set(new NamespacedKey(EssentialsD.instance, "invisible"), PersistentDataType.BYTE, (byte)1);
            }

         }
      }
   }

   @EventHandler
   public void removeItemFrame(HangingBreakByEntityEvent event) {
      Entity entity = event.getEntity();
      if (entity instanceof ItemFrame) {
         if (entity.getPersistentDataContainer().has(new NamespacedKey(EssentialsD.instance, "invisible"), PersistentDataType.BYTE)) {
            ItemFrame itemFrame = (ItemFrame)entity;
            if (entity.getType() == EntityType.ITEM_FRAME) {
               ItemStack item = InvisibleItemFrame.getItemStack();
               itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), item);
            } else if (entity.getType() == EntityType.GLOW_ITEM_FRAME) {
               ItemStack item = InvisibleGlowItemFrame.getItemStack();
               itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), item);
            }

            itemFrame.remove();
            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void putSomeOnItemFrame(PlayerInteractEntityEvent event) {
      Entity entity = event.getRightClicked();
      if (entity instanceof ItemFrame) {
         if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) {
            if (entity.getPersistentDataContainer().has(new NamespacedKey(EssentialsD.instance, "invisible"), PersistentDataType.BYTE)) {
               if (entity.getType() == EntityType.ITEM_FRAME || entity.getType() == EntityType.GLOW_ITEM_FRAME) {
                  ItemFrame itemFrame = (ItemFrame)entity;
                  itemFrame.setVisible(false);
               }

            }
         }
      }
   }

   @EventHandler
   public void removeSomeOnItemFrame(EntityDamageByEntityEvent event) {
      Entity entity = event.getEntity();
      if (entity instanceof ItemFrame) {
         if (event.getDamager() instanceof Player) {
            if (entity.getPersistentDataContainer().has(new NamespacedKey(EssentialsD.instance, "invisible"), PersistentDataType.BYTE)) {
               if (entity.getType() == EntityType.ITEM_FRAME || entity.getType() == EntityType.GLOW_ITEM_FRAME) {
                  ItemFrame itemFrame = (ItemFrame)entity;
                  itemFrame.setVisible(true);
               }

            }
         }
      }
   }
}
