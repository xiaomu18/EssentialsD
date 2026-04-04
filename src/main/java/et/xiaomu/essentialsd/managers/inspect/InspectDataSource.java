package et.xiaomu.essentialsd.managers.inspect;

import org.bukkit.inventory.ItemStack;

interface InspectDataSource {
    InspectManager.Mode mode();

    String displayName();

    boolean isOnline();

    boolean wasOnlineSource();

    boolean readOnly();

    ItemStack[] snapshot();

    void refreshSnapshot(Runnable callback);

    void applySnapshot(ItemStack[] contents);

    void close();
}
