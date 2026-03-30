package cn.lunadeer.essentialsd.managers.inspect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

final class InspectSession {
    private record CursorOrigin(boolean topInventory, int slot) {
    }

    private final UUID viewerId;
    private final InspectDataSource source;
    private final boolean readOnly;
    private final InspectInventoryHolder holder;
    private final Inventory inventory;
    private volatile long lastLocalEditAt = 0L;
    private volatile CursorOrigin cursorOrigin = null;

    InspectSession(UUID viewerId, InspectDataSource source, boolean readOnly) {
        this.viewerId = viewerId;
        this.source = source;
        this.readOnly = readOnly;
        this.holder = new InspectInventoryHolder();
        int size = source.mode() == InspectManager.Mode.ENDER_CHEST ? InspectManager.ENDER_SLOT_COUNT : InspectManager.PLAYER_GUI_SIZE;
        this.inventory = Bukkit.createInventory(holder, size, InspectManager.createTitle(source, readOnly));
        this.holder.setInventory(this.inventory);
    }

    UUID viewerId() {
        return viewerId;
    }

    InspectDataSource source() {
        return source;
    }

    boolean readOnly() {
        return readOnly;
    }

    Inventory inventory() {
        return inventory;
    }

    boolean editable(int rawSlot) {
        if (readOnly) {
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

    void setCursorOrigin(boolean topInventory, int slot) {
        this.cursorOrigin = new CursorOrigin(topInventory, slot);
    }

    void clearCursorOriginSlot() {
        this.cursorOrigin = null;
    }

    boolean recentlyEdited() {
        return System.currentTimeMillis() - lastLocalEditAt < 1000L;
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
        inventory.setItem(8, InspectManager.createInfoItem(source, readOnly));
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

    void restoreCursorItem(Player viewer) {
        CursorOrigin origin = this.cursorOrigin;
        ItemStack cursor = InspectManager.normalize(viewer.getItemOnCursor());
        if (cursor == null || origin == null) {
            return;
        }
        ItemStack remaining = cursor.clone();
        if (origin.topInventory()) {
            if (editable(origin.slot())) {
                remaining = restoreToSlot(inventory, origin.slot(), remaining);
            }
        } else {
            remaining = restoreToSlot(viewer.getInventory(), origin.slot(), remaining);
        }
        if (remaining != null) {
            remaining = returnToViewer(viewer, remaining);
        }
        viewer.setItemOnCursor(null);
        clearCursorOriginSlot();
        if (remaining != null) {
            viewer.getWorld().dropItemNaturally(viewer.getLocation(), remaining);
        }
    }

    void close() {
        clearCursorOriginSlot();
        source.close();
    }

    private static ItemStack restoreToSlot(Inventory targetInventory, int slot, ItemStack item) {
        ItemStack target = InspectManager.normalize(targetInventory.getItem(slot));
        if (target == null) {
            targetInventory.setItem(slot, item);
            return null;
        }
        if (!target.isSimilar(item)) {
            return item;
        }
        int maxStack = Math.min(target.getMaxStackSize(), targetInventory.getMaxStackSize());
        int space = Math.max(0, maxStack - target.getAmount());
        if (space == 0) {
            return item;
        }
        int merged = Math.min(space, item.getAmount());
        target.setAmount(target.getAmount() + merged);
        targetInventory.setItem(slot, target);
        if (merged >= item.getAmount()) {
            return null;
        }
        ItemStack remaining = item.clone();
        remaining.setAmount(item.getAmount() - merged);
        return remaining;
    }

    private static ItemStack returnToViewer(Player viewer, ItemStack item) {
        for (ItemStack overflow : viewer.getInventory().addItem(item).values()) {
            return InspectManager.normalize(overflow);
        }
        return null;
    }
}
