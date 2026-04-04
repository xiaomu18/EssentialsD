package et.xiaomu.essentialsd.managers.inspect;

import cn.lunadeer.utils.XLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

final class OfflineInspectDataSource implements InspectDataSource {
    private static volatile NbtBridge nbtBridge;

    private final InspectManager.Mode mode;
    private final String displayName;
    private final ItemStack[] snapshot;

    private OfflineInspectDataSource(InspectManager.Mode mode, String displayName, ItemStack[] snapshot) {
        this.mode = mode;
        this.displayName = displayName;
        this.snapshot = snapshot;
    }

    static OfflineInspectDataSource load(OfflinePlayer player, InspectManager.Mode mode) {
        try {
            Path path = playerDataFile(player.getUniqueId());
            if (path == null || !Files.exists(path)) {
                return null;
            }
            NbtBridge nbt = bridge();
            Object rootTag = nbt.read(path);
            ItemStack[] snapshot = mode == InspectManager.Mode.ENDER_CHEST
                    ? nbt.readEnderChest(rootTag)
                    : nbt.readPlayerInventory(rootTag);
            return new OfflineInspectDataSource(mode, InspectManager.displayName(player), snapshot);
        } catch (Exception e) {
            XLogger.error("读取离线检查数据失败: " + player.getUniqueId());
            XLogger.error(e);
            return null;
        }
    }

    @Override
    public InspectManager.Mode mode() {
        return mode;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public boolean wasOnlineSource() {
        return false;
    }

    @Override
    public boolean readOnly() {
        return true;
    }

    @Override
    public synchronized ItemStack[] snapshot() {
        return InspectManager.cloneItems(snapshot);
    }

    @Override
    public void refreshSnapshot(Runnable callback) {
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public synchronized void applySnapshot(ItemStack[] contents) {
    }

    @Override
    public void close() {
    }

    private static NbtBridge bridge() {
        if (nbtBridge == null) {
            nbtBridge = new NbtBridge();
        }
        return nbtBridge;
    }

    private static Path playerDataFile(UUID uuid) {
        World world = org.bukkit.Bukkit.getWorlds().stream()
                .filter(candidate -> candidate.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(org.bukkit.Bukkit.getWorlds().isEmpty() ? null : org.bukkit.Bukkit.getWorlds().get(0));
        if (world == null) {
            return null;
        }
        return world.getWorldFolder().toPath().resolve("playerdata").resolve(uuid + ".dat");
    }
}
