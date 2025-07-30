package cn.lunadeer.essentialsd.events;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class ArmorStandHandsEvent implements Listener {
   @EventHandler
   public void onAddArmsForArmStand(EntityDamageByEntityEvent event) {
      Entity entity = event.getEntity();
      if (entity instanceof ArmorStand) {
         if (event.getDamager() instanceof Player) {
            Player player = (Player)event.getDamager();
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.STICK) {
               event.setCancelled(true);
               ArmorStand armorStand = (ArmorStand)entity;
               if (!armorStand.hasArms()) {
                  armorStand.setArms(true);
                  mainHand.setAmount(mainHand.getAmount() - 1);
               }
            }
         }
      }
   }
}
