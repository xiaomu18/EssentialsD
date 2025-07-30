package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.commands.ShowItem;
import cn.lunadeer.minecraftpluginutils.XLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public class ShowItemEvent implements Listener {
   @EventHandler
   public void onShowItemClick(InventoryClickEvent event) {
      Inventory inv = event.getClickedInventory();
      if (inv != null) {
         InventoryView view = event.getView();
         Component title = view.title();
         HoverEvent<?> hoverEvent = title.hoverEvent();
         if (hoverEvent != null) {
            if (hoverEvent.value() instanceof TextComponent) {
               TextComponent hoverEventValue = (TextComponent)hoverEvent.value();
               XLogger.debug("ShowItemEvent: " + hoverEventValue.content());
               if (ShowItem.cache.containsKey(hoverEventValue.content())) {
                  event.setCancelled(true);
               }
            }
         }
      }
   }
}
