package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.recipes.Crowbar;
import cn.lunadeer.minecraftpluginutils.Notification;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CrowEvent implements Listener {
   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onCrowbarUse(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      ItemStack mainHand = player.getInventory().getItemInMainHand();
      if (event.getHand() != EquipmentSlot.OFF_HAND) {
         if (mainHand.getType() == Crowbar.getItemStack().getType()) {
            if (((ItemMeta)Objects.requireNonNull(mainHand.getItemMeta())).getPersistentDataContainer().has(new NamespacedKey(EssentialsD.instance, "this_is_crowbar"), PersistentDataType.BYTE)) {
               if (event.getClickedBlock() != null) {
                  Block block = event.getClickedBlock();
                  if (block.getType() != Material.POWERED_RAIL && block.getType() != Material.DETECTOR_RAIL && block.getType() != Material.ACTIVATOR_RAIL) {
                     BlockData blockData = block.getBlockData();
                     if (blockData instanceof Rail) {
                        Rail rail = (Rail)blockData;
                        if (!Crowbar.changeable(rail.getShape())) {
                           Notification.warn(player, "无法使用撬棍修改此铁轨的方向");
                        } else {
                           rail.setShape(Crowbar.changeToNext(rail.getShape()));
                           block.setBlockData(rail);
                           event.setCancelled(true);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
