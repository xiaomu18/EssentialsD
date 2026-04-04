package et.xiaomu.essentialsd.managers.inspect;

import cn.lunadeer.utils.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class OnlineInspectDataSource implements InspectDataSource {
    private final Player target;
    private final InspectManager.Mode mode;
    private volatile ItemStack[] snapshot;

    OnlineInspectDataSource(Player target, InspectManager.Mode mode) {
        this.target = target;
        this.mode = mode;
        this.snapshot = new ItemStack[mode == InspectManager.Mode.ENDER_CHEST ? InspectManager.ENDER_SLOT_COUNT : InspectManager.PLAYER_SLOT_COUNT];
    }

    @Override
    public InspectManager.Mode mode() {
        return mode;
    }

    @Override
    public String displayName() {
        return target.getName();
    }

    @Override
    public boolean isOnline() {
        return target.isOnline();
    }

    @Override
    public boolean wasOnlineSource() {
        return true;
    }

    @Override
    public boolean readOnly() {
        return false;
    }

    @Override
    public ItemStack[] snapshot() {
        return InspectManager.cloneItems(snapshot);
    }

    @Override
    public void refreshSnapshot(Runnable callback) {
        Scheduler.runEntityTask(target, () -> {
            snapshot = mode == InspectManager.Mode.ENDER_CHEST ? captureEnderChest(target) : capturePlayerInventory(target);
            if (callback != null) {
                callback.run();
            }
        });
    }

    @Override
    public void applySnapshot(ItemStack[] contents) {
        ItemStack[] cloned = InspectManager.cloneItems(contents);
        Scheduler.runEntityTask(target, () -> {
            if (!target.isOnline()) {
                return;
            }
            if (mode == InspectManager.Mode.ENDER_CHEST) {
                applyEnderChest(target, cloned);
            } else {
                applyPlayerInventory(target, cloned);
            }
            snapshot = mode == InspectManager.Mode.ENDER_CHEST ? captureEnderChest(target) : capturePlayerInventory(target);
        });
    }

    @Override
    public void close() {
    }

    private static ItemStack[] capturePlayerInventory(Player player) {
        ItemStack[] result = new ItemStack[InspectManager.PLAYER_SLOT_COUNT];
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        for (int i = 0; i < Math.min(36, storage.length); i++) {
            result[i] = InspectManager.normalize(storage[i]);
        }
        ItemStack[] armor = inventory.getArmorContents();
        result[36] = armor.length > 0 ? InspectManager.normalize(armor[0]) : null;
        result[37] = armor.length > 1 ? InspectManager.normalize(armor[1]) : null;
        result[38] = armor.length > 2 ? InspectManager.normalize(armor[2]) : null;
        result[39] = armor.length > 3 ? InspectManager.normalize(armor[3]) : null;
        result[40] = InspectManager.normalize(inventory.getItemInOffHand());
        return result;
    }

    private static ItemStack[] captureEnderChest(Player player) {
        ItemStack[] result = new ItemStack[InspectManager.ENDER_SLOT_COUNT];
        Inventory inventory = player.getEnderChest();
        for (int i = 0; i < InspectManager.ENDER_SLOT_COUNT; i++) {
            result[i] = InspectManager.normalize(inventory.getItem(i));
        }
        return result;
    }

    private static void applyPlayerInventory(Player player, ItemStack[] contents) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = new ItemStack[36];
        for (int i = 0; i < storage.length; i++) {
            storage[i] = InspectManager.normalize(contents[i]);
        }
        inventory.setStorageContents(storage);
        inventory.setArmorContents(new ItemStack[]{
                InspectManager.normalize(contents[36]),
                InspectManager.normalize(contents[37]),
                InspectManager.normalize(contents[38]),
                InspectManager.normalize(contents[39])
        });
        inventory.setItemInOffHand(InspectManager.normalize(contents[40]));
        player.updateInventory();
    }

    private static void applyEnderChest(Player player, ItemStack[] contents) {
        Inventory inventory = player.getEnderChest();
        for (int i = 0; i < InspectManager.ENDER_SLOT_COUNT; i++) {
            inventory.setItem(i, InspectManager.normalize(contents[i]));
        }
        player.updateInventory();
    }
}
