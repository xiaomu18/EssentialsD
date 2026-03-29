package cn.lunadeer.essentialsd.managers.inspect;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

final class InspectSession {
    private final UUID viewerId;
    private final InspectDataSource source;
    private final InspectInventoryHolder holder;
    private final Inventory inventory;
    private volatile long lastLocalEditAt = 0L;

    InspectSession(UUID viewerId, InspectDataSource source) {
        this.viewerId = viewerId;
        this.source = source;
        this.holder = new InspectInventoryHolder();
        int size = source.mode() == InspectManager.Mode.ENDER_CHEST ? InspectManager.ENDER_SLOT_COUNT : InspectManager.PLAYER_GUI_SIZE;
        String titlePrefix = source.mode() == InspectManager.Mode.ENDER_CHEST ? "检查末影箱: " : "检查背包: ";
        this.inventory = Bukkit.createInventory(holder, size, titlePrefix + source.displayName());
        this.holder.setInventory(this.inventory);
    }

    UUID viewerId() {
        return viewerId;
    }

    InspectDataSource source() {
        return source;
    }

    Inventory inventory() {
        return inventory;
    }

    boolean editable(int rawSlot) {
        if (source.readOnly()) {
            return false;
        }
        if (rawSlot < 0 || rawSlot >= inventory.getSize()) {
            return true;
        }
        if (source.mode() == InspectManager.Mode.ENDER_CHEST) {
            return rawSlot < InspectManager.ENDER_SLOT_COUNT;
        }
        return InspectManager.PLAYER_GUI_TO_SLOT[rawSlot] >= 0;
    }

    void markLocalEdit() {
        this.lastLocalEditAt = System.currentTimeMillis();
    }

    boolean recentlyEdited() {
        return System.currentTimeMillis() - lastLocalEditAt < 250L;
    }

    void renderFromSource() {
        ItemStack[] snapshot = source.snapshot();
        if (source.mode() == InspectManager.Mode.ENDER_CHEST) {
            for (int i = 0; i < InspectManager.ENDER_SLOT_COUNT; i++) {
                inventory.setItem(i, InspectManager.normalize(snapshot[i]));
            }
            return;
        }

        for (int i = 0; i < InspectManager.PLAYER_GUI_SIZE; i++) {
            if (InspectManager.PLAYER_GUI_TO_SLOT[i] >= 0) {
                inventory.setItem(i, InspectManager.normalize(snapshot[InspectManager.PLAYER_GUI_TO_SLOT[i]]));
            } else {
                inventory.setItem(i, InspectManager.FILLER.clone());
            }
        }
        inventory.setItem(53, InspectManager.createInfoItem(source));
    }

    void applyTopToSource() {
        if (source.mode() == InspectManager.Mode.ENDER_CHEST) {
            ItemStack[] contents = new ItemStack[InspectManager.ENDER_SLOT_COUNT];
            for (int i = 0; i < InspectManager.ENDER_SLOT_COUNT; i++) {
                contents[i] = InspectManager.normalize(inventory.getItem(i));
            }
            source.applySnapshot(contents);
            return;
        }

        ItemStack[] current = source.snapshot();
        ItemStack[] updated = new ItemStack[InspectManager.PLAYER_SLOT_COUNT];
        for (int i = 0; i < InspectManager.PLAYER_SLOT_COUNT; i++) {
            updated[i] = i < current.length ? InspectManager.normalize(current[i]) : null;
        }
        for (int guiSlot : InspectManager.PLAYER_EDITABLE_SLOTS) {
            updated[InspectManager.PLAYER_GUI_TO_SLOT[guiSlot]] = InspectManager.normalize(inventory.getItem(guiSlot));
        }
        source.applySnapshot(updated);
    }

    void close() {
        source.close();
    }
}
