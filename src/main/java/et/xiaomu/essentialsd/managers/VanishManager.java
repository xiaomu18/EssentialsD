package et.xiaomu.essentialsd.managers;

import cn.lunadeer.utils.Notification;
import et.xiaomu.essentialsd.EssentialsD;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Lidded;
import org.bukkit.block.Lockable;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VanishManager {
    public static final String PERMISSION_BASE = "essd.vanish";
    public static final String PERMISSION_OTHER = "essd.vanish.other";
    public static final String PERMISSION_SEE = "essd.vanish.see";
    public static final String PERMISSION_CHAT = "essd.vanish.chat";
    public static final String PERMISSION_TPAHERE = "essd.vanish.tpahere";

    private final Set<UUID> manualVanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> forcedVanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> vanishInvulnerable = ConcurrentHashMap.newKeySet();
    private final Set<UUID> vanishSilenced = ConcurrentHashMap.newKeySet();
    private final Set<UUID> vanishNoCollidable = ConcurrentHashMap.newKeySet();
    private final Set<UUID> vanishVisibleByDefaultOff = ConcurrentHashMap.newKeySet();
    private final Map<UUID, SilentContainerSession> silentContainerSessions = new ConcurrentHashMap<>();
    private final BossBar bossBar = Bukkit.createBossBar("§7你正处于隐身状态", BarColor.WHITE, BarStyle.SEGMENTED_6);

    public boolean isVanished(UUID playerId) {
        return manualVanished.contains(playerId) || forcedVanished.contains(playerId);
    }

    public boolean isVanished(Player player) {
        return isVanished(player.getUniqueId());
    }

    public boolean isForced(UUID playerId) {
        return forcedVanished.contains(playerId);
    }

    public boolean canSeeVanished(CommandSender sender) {
        return sender.hasPermission(PERMISSION_SEE);
    }

    public boolean canChatWhileVanished(Player player) {
        return player.hasPermission(PERMISSION_CHAT);
    }

    public boolean canTpahereWhileVanished(Player player) {
        return player.hasPermission(PERMISSION_TPAHERE);
    }

    public boolean isHiddenFrom(Player viewer, Player target) {
        return !viewer.getUniqueId().equals(target.getUniqueId())
                && isVanished(target.getUniqueId())
                && !canSeeVanished(viewer);
    }

    public boolean setManualVanished(Player player, boolean vanished) {
        UUID playerId = player.getUniqueId();
        if (!vanished && isForced(playerId)) {
            return false;
        }

        boolean wasVanished = isVanished(playerId);
        boolean changed = vanished ? manualVanished.add(playerId) : manualVanished.remove(playerId);
        boolean isVanished = isVanished(playerId);

        if (wasVanished != isVanished) {
            applyVanishState(player, isVanished);
            refreshVisibilityForTarget(player);
        } else if (isVanished && changed) {
            applyVanishState(player, true);
        }
        return changed;
    }

    public boolean refreshForcedState(Player player, boolean notify) {
        return refreshForcedState(player, player.getGameMode(), notify);
    }

    public boolean refreshForcedState(Player player, GameMode mode, boolean notify) {
        UUID playerId = player.getUniqueId();
        boolean shouldForce = EssentialsD.config.getForceVanishInDifferentGamemode()
                && mode != Bukkit.getDefaultGameMode();
        boolean wasVanished = isVanished(playerId);
        boolean forcedChanged = shouldForce ? forcedVanished.add(playerId) : forcedVanished.remove(playerId);
        // 强制隐身只负责自动开启，不负责自动关闭。
        boolean autoEnabledByForce = shouldForce && manualVanished.add(playerId);
        boolean isVanished = isVanished(playerId);

        if (wasVanished != isVanished) {
            applyVanishState(player, isVanished);
            refreshVisibilityForTarget(player);
        } else {
            applyVanishState(player, isVanished);
        }

        if (notify && forcedChanged) {
            if (shouldForce) {
                Notification.warn(player, "你当前游戏模式不同于服务器默认游戏模式，已强制开启隐身");
            } else {
                Notification.info(player, isVanished
                        ? "你的游戏模式已恢复为默认模式，强制隐身已解除，隐身状态保持开启"
                        : "你的游戏模式已恢复为默认模式，强制隐身已解除");
            }
        }
        return forcedChanged || autoEnabledByForce;
    }

    public void handleLogin(Player player) {
        refreshForcedState(player, false);
        // 明确在 PlayerLoginEvent 阶段设置 VisibleByDefault。
        applyVisibleByDefaultState(player, isVanished(player));
        // 在玩家真正加入前完成可见性同步，降低隐身泄露窗口。
        refreshVisibilityForTarget(player);
        refreshVisibilityForViewer(player);
    }

    public void handleJoin(Player player) {
        if (isVanished(player)) {
            if (player.isOnline()) {
                bossBar.addPlayer(player);
            }
            Notification.warn(player, isForced(player.getUniqueId()) ? "你当前处于强制隐身状态" : "你仍处于隐身状态");
        }
    }

    public void handleQuit(Player player) {
        closeSilentContainerSession(player, null);
        UUID playerId = player.getUniqueId();
        forcedVanished.remove(playerId);
        vanishInvulnerable.remove(playerId);
        vanishSilenced.remove(playerId);
        vanishNoCollidable.remove(playerId);
        vanishVisibleByDefaultOff.remove(playerId);
        bossBar.removePlayer(player);
    }

    public boolean openContainerWithoutAnimation(Player player, Block clickedBlock) {
        if (clickedBlock == null || !EssentialsD.config.getVanishCancelContainerAnimation() || !isVanished(player)) {
            return false;
        }
        Inventory sourceInventory = resolveAnimationContainerInventory(player, clickedBlock.getState());
        if (sourceInventory == null) {
            return false;
        }

        closeSilentContainerSession(player, null);
        Inventory mirrorInventory = createMirrorInventory(sourceInventory);
        mirrorInventory.setContents(cloneContents(sourceInventory.getContents()));
        silentContainerSessions.put(player.getUniqueId(), new SilentContainerSession(sourceInventory, mirrorInventory));
        player.openInventory(mirrorInventory);
        return true;
    }

    public void closeSilentContainerSession(Player player, Inventory closedTop) {
        UUID playerId = player.getUniqueId();
        SilentContainerSession session = silentContainerSessions.get(playerId);
        if (session == null || (closedTop != null && session.mirrorInventory() != closedTop)) {
            return;
        }
        silentContainerSessions.remove(playerId);
        try {
            if (session.sourceInventory().getSize() == session.mirrorInventory().getSize()) {
                session.sourceInventory().setContents(cloneContents(session.mirrorInventory().getContents()));
            }
        } catch (Throwable ignored) {
        }
    }

    public void reload() {
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            refreshForcedState(player, true);
        }
    }

    public void shutdown() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (UUID playerId : new ArrayList<>(vanishInvulnerable)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && vanishInvulnerable.remove(playerId)) {
                player.setInvulnerable(false);
            } else {
                vanishInvulnerable.remove(playerId);
            }
        }
        for (UUID playerId : new ArrayList<>(vanishSilenced)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && vanishSilenced.remove(playerId)) {
                player.setSilent(false);
            } else {
                vanishSilenced.remove(playerId);
            }
        }
        for (UUID playerId : new ArrayList<>(vanishNoCollidable)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && vanishNoCollidable.remove(playerId)) {
                player.setCollidable(true);
            } else {
                vanishNoCollidable.remove(playerId);
            }
        }
        for (UUID playerId : new ArrayList<>(vanishVisibleByDefaultOff)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline() && vanishVisibleByDefaultOff.remove(playerId)) {
                player.setVisibleByDefault(true);
            } else {
                vanishVisibleByDefaultOff.remove(playerId);
            }
        }
        for (UUID playerId : new ArrayList<>(silentContainerSessions.keySet())) {
            SilentContainerSession session = silentContainerSessions.remove(playerId);
            if (session == null) {
                continue;
            }
            try {
                if (session.sourceInventory().getSize() == session.mirrorInventory().getSize()) {
                    session.sourceInventory().setContents(cloneContents(session.mirrorInventory().getContents()));
                }
            } catch (Throwable ignored) {
            }
        }

        for (Player player : onlinePlayers) {
            bossBar.removePlayer(player);
        }
        for (Player viewer : onlinePlayers) {
            for (Player target : onlinePlayers) {
                if (!viewer.getUniqueId().equals(target.getUniqueId())) {
                    viewer.showPlayer(EssentialsD.instance, target);
                }
            }
        }

        bossBar.removeAll();
        manualVanished.clear();
        forcedVanished.clear();
        vanishInvulnerable.clear();
        vanishSilenced.clear();
        vanishNoCollidable.clear();
        vanishVisibleByDefaultOff.clear();
        silentContainerSessions.clear();
    }

    public List<Player> getVanishedPlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(this::isVanished)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void refreshVisibilityForTarget(Player target) {
        for (Player viewer : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            refreshVisibility(viewer, target);
        }
    }

    private void refreshVisibilityForViewer(Player viewer) {
        for (Player target : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            refreshVisibility(viewer, target);
        }
    }

    private void refreshVisibility(Player viewer, Player target) {
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return;
        }
        if (isHiddenFrom(viewer, target)) {
            viewer.hidePlayer(EssentialsD.instance, target);
        } else {
            viewer.showPlayer(EssentialsD.instance, target);
        }
    }

    private void applyVanishState(Player player, boolean vanished) {
        UUID playerId = player.getUniqueId();
        if (vanished) {
            if (player.isOnline()) {
                bossBar.addPlayer(player);
            }
            if (!player.isInvulnerable()) {
                vanishInvulnerable.add(playerId);
                player.setInvulnerable(true);
            }
            applyVisibleByDefaultState(player, true);

            if (!player.isSilent()) {
                vanishSilenced.add(playerId);
            }
            player.setSilent(true);

            if (EssentialsD.config.getVanishDisableCollidable()) {
                if (player.isCollidable()) {
                    vanishNoCollidable.add(playerId);
                }
                player.setCollidable(false);
            } else if (vanishNoCollidable.remove(playerId)) {
                player.setCollidable(true);
            }
            return;
        }

        bossBar.removePlayer(player);
        if (vanishInvulnerable.remove(playerId)) {
            player.setInvulnerable(false);
        }
        if (vanishSilenced.remove(playerId)) {
            player.setSilent(false);
        }
        if (vanishNoCollidable.remove(playerId)) {
            player.setCollidable(true);
        }
        applyVisibleByDefaultState(player, false);
    }

    private void applyVisibleByDefaultState(Player player, boolean vanished) {
        UUID playerId = player.getUniqueId();
        if (vanished) {
            if (player.isVisibleByDefault()) {
                vanishVisibleByDefaultOff.add(playerId);
            }
            player.setVisibleByDefault(false);
            return;
        }
        if (vanishVisibleByDefaultOff.remove(playerId)) {
            player.setVisibleByDefault(true);
        }
    }

    private Inventory resolveAnimationContainerInventory(Player player, BlockState blockState) {
        if (!(blockState instanceof Lidded)) {
            return null;
        }
        if (blockState instanceof Lockable lockable && lockable.isLocked()) {
            return null;
        }
        if (blockState instanceof Container container) {
            return container.getInventory();
        }
        if (blockState instanceof org.bukkit.block.EnderChest) {
            return player.getEnderChest();
        }
        return null;
    }

    private Inventory createMirrorInventory(Inventory sourceInventory) {
        InventoryType sourceType = sourceInventory.getType();
        if (sourceType == InventoryType.CHEST) {
            return Bukkit.createInventory(null, sourceInventory.getSize());
        }
        try {
            return Bukkit.createInventory(null, sourceType);
        } catch (IllegalArgumentException ignored) {
            return Bukkit.createInventory(null, sourceInventory.getSize());
        }
    }

    private ItemStack[] cloneContents(ItemStack[] contents) {
        ItemStack[] cloned = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            cloned[i] = contents[i] == null ? null : contents[i].clone();
        }
        return cloned;
    }

    private record SilentContainerSession(Inventory sourceInventory, Inventory mirrorInventory) {
    }
}
