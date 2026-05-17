package et.xiaomu.essentialsd.events;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.inventory.ItemStack;

public class CreativeTakeListener implements Listener {
    @EventHandler
    public void onCreativeItemTake(InventoryCreativeEvent event) {
        if (event.getWhoClicked().isOp()) {
            return;
        }

        // 获取被点击的物品
        ItemStack clickedItem = event.getCursor(); // 或使用 getCurrentItem()

        if (clickedItem.getType() == Material.AIR) {
            return;
        }

        if (EssentialsD.config.forbidTakeItems.contains(clickedItem.getType())) {
            event.setCancelled(true);
            Notification.warnKey(event.getWhoClicked(), "messages.creative_take.blocked");
            return;
        }

        if (EssentialsD.config.forbidNBTItem && clickedItem.hasItemMeta()) {
            event.setCancelled(true);
            Notification.warnKey(event.getWhoClicked(), "messages.creative_take.blocked");
            return;
        }
    }
}
