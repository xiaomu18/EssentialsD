package et.xiaomu.essentialsd.events;

import et.xiaomu.essentialsd.EssentialsD;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InspectInventoryEvent implements Listener {

    @EventHandler
    public void onInspectClick(InventoryClickEvent event) {
        EssentialsD.inspectManager.handleClick(event);
    }

    @EventHandler
    public void onInspectDrag(InventoryDragEvent event) {
        EssentialsD.inspectManager.handleDrag(event);
    }

    @EventHandler
    public void onInspectClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            EssentialsD.inspectManager.handleClose(player, event.getView().getTopInventory());
        }
    }
}
