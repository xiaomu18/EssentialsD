package et.xiaomu.essentialsd.managers;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.XLogger;
import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.VanishState;
import net.kyori.adventure.text.Component;
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
    private final Set<UUID> packetVanishedUuids = ConcurrentHashMap.newKeySet();
    private final Set<Integer> packetVanishedEntityIds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> packetVanishedEntityIdByUuid = new ConcurrentHashMap<>();
    private final Map<Integer, Long> recentPacketVanishedEntityIds = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> pendingPlayerInfoRemoveByViewer = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, Long>> pendingEntityDestroyByViewer = new ConcurrentHashMap<>();
    private final Map<UUID, SilentContainerSession> silentContainerSessions = new ConcurrentHashMap<>();
    private final BossBar bossBar = Bukkit.createBossBar("§7你正处于隐身状态", BarColor.WHITE, BarStyle.SEGMENTED_6);
    private VanishProtocolEnhancer protocolEnhancer;
    private static final long PENDING_REMOVE_TTL_MS = 5000L;

    public VanishManager() {
        bootstrapPersistedManualState();
        refreshEnhancedModeState();
    }

    public boolean isVanished(UUID playerId) {
        return manualVanished.contains(playerId) || forcedVanished.contains(playerId);
    }

    public boolean isVanished(Player player) {
        return isVanished(player.getUniqueId());
    }

    public boolean isForced(UUID playerId) {
        return forcedVanished.contains(playerId);
    }

    public boolean isManualVanished(UUID playerId) {
        return manualVanished.contains(playerId);
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

    public boolean isPacketVanishedEntityId(int entityId) {
        if (packetVanishedEntityIds.contains(entityId)) {
            return true;
        }
        return isRecentPacketVanishedEntityId(entityId);
    }

    public boolean isPacketVanishedUuid(UUID uuid) {
        return packetVanishedUuids.contains(uuid);
    }

    public boolean consumePendingPlayerInfoRemove(UUID viewerId, UUID targetUuid) {
        if (viewerId == null || targetUuid == null) {
            return false;
        }
        return consumePending(pendingPlayerInfoRemoveByViewer, viewerId, targetUuid);
    }

    public boolean consumePendingEntityDestroy(UUID viewerId, int entityId) {
        if (viewerId == null || entityId <= 0) {
            return false;
        }
        return consumePending(pendingEntityDestroyByViewer, viewerId, entityId);
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
        if (!changed) {
            return false;
        }
        if (!VanishState.setManualVanished(playerId, vanished)) {
            if (vanished) {
                manualVanished.remove(playerId);
            } else {
                manualVanished.add(playerId);
            }
            XLogger.warn("玩家 %s 的隐身持久化状态写入失败，本次切换已回滚", player.getName());
            return false;
        }
        boolean isVanished = isVanished(playerId);

        if (wasVanished != isVanished) {
            applyVanishState(player, isVanished);
            refreshVisibilityForTarget(player);
        } else if (isVanished) {
            applyVanishState(player, true);
        }
        return changed;
    }

    public boolean refreshForcedState(Player player, boolean notify) {
        return refreshForcedState(player, player.getGameMode(), notify, true);
    }

    public boolean refreshForcedState(Player player, GameMode mode, boolean notify) {
        return refreshForcedState(player, mode, notify, true);
    }

    private boolean refreshForcedState(Player player, GameMode mode, boolean notify, boolean applyRuntimeState) {
        UUID playerId = player.getUniqueId();
        boolean shouldForce = EssentialsD.config.getForceVanishInDifferentGamemode()
                && mode != Bukkit.getDefaultGameMode();
        boolean wasVanished = isVanished(playerId);
        boolean forcedChanged = shouldForce ? forcedVanished.add(playerId) : forcedVanished.remove(playerId);
        // 强制隐身只负责自动开启，不负责自动关闭。
        boolean autoEnabledByForce = false;
        if (shouldForce && !manualVanished.contains(playerId)) {
            manualVanished.add(playerId);
            if (VanishState.setManualVanished(playerId, true)) {
                autoEnabledByForce = true;
            } else {
                manualVanished.remove(playerId);
                XLogger.warn("玩家 %s 被强制隐身时写入持久化失败，当前会话仍保持强制隐身", player.getName());
            }
        }
        boolean isVanished = isVanished(playerId);

        if (applyRuntimeState) {
            applyVanishState(player, isVanished);
        } else {
            updatePacketTrackedState(player, isVanished);
        }

        if (wasVanished != isVanished) {
            refreshVisibilityForTarget(player);
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
        restoreManualState(player);
        // 登录阶段仅刷新状态与可见性，不触发实体属性读写。
        // 注意：部分服务端在 Login 阶段 GameMode 可能尚未恢复，Join 阶段会再做一次强制隐身判定兜底。
        refreshForcedState(player, player.getGameMode(), false, false);
        // 明确在 PlayerLoginEvent 阶段设置 VisibleByDefault。
        applyVisibleByDefaultState(player, isVanished(player));
        // 在玩家真正加入前完成可见性同步，降低隐身泄露窗口。
        refreshVisibilityFor(player);
    }

    public void handleJoin(Player player) {
        // Join 阶段再次基于最终 GameMode 判定强制隐身，修复“首次加入非默认模式未触发强制隐身”问题。
        refreshForcedState(player, player.getGameMode(), false, true);
        boolean vanished = isVanished(player);
        // Join 阶段兜底刷新一次可见性，避免其他插件在登录链路后覆盖 hide/show 状态。
        refreshVisibilityFor(player);
        if (vanished) {
            Notification.warn(player, isForced(player.getUniqueId()) ? "你当前处于强制隐身状态" : "你仍处于隐身状态");
        }
    }

    public void handleQuit(Player player) {
        UUID playerId = player.getUniqueId();
        if (isVanished(playerId)) {
            markRecentPacketVanishedEntityId(player.getEntityId());
        }
        closeSilentContainerSession(player, null);
        updatePacketTrackedState(player, false);
        clearPendingRemovalForPlayer(playerId, player.getEntityId());
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
        Notification.info(player, Component.text("你悄悄地打开了 ").append(Component.translatable(clickedBlock.getType().translationKey())));
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
        refreshEnhancedModeState();
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            restoreManualState(player);
            updatePacketTrackedState(player, isVanished(player));
            refreshForcedState(player, true);
        }
    }

    public void shutdown() {
        if (protocolEnhancer != null) {
            protocolEnhancer.shutdown();
            protocolEnhancer = null;
        }

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
        packetVanishedUuids.clear();
        packetVanishedEntityIds.clear();
        packetVanishedEntityIdByUuid.clear();
        recentPacketVanishedEntityIds.clear();
        pendingPlayerInfoRemoveByViewer.clear();
        pendingEntityDestroyByViewer.clear();
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

    private void refreshVisibilityFor(Player player) {
        refreshVisibilityForTarget(player);
        refreshVisibilityForViewer(player);
    }

    private void refreshVisibility(Player viewer, Player target) {
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return;
        }
        if (isHiddenFrom(viewer, target)) {
            if (viewer.canSee(target)) {
                markPendingRemoval(viewer, target);
            }
            viewer.hidePlayer(EssentialsD.instance, target);
        } else {
            clearPendingRemoval(viewer.getUniqueId(), target.getUniqueId(), target.getEntityId());
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
            updatePacketTrackedState(player, true);
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
        updatePacketTrackedState(player, false);
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

    private void bootstrapPersistedManualState() {
        List<UUID> persisted = VanishState.getAllManualVanished();
        if (persisted.isEmpty()) {
            return;
        }
        manualVanished.addAll(persisted);
        // 提前把持久化隐身 UUID 放入包过滤缓存，减少加入阶段的泄露风险。
        packetVanishedUuids.addAll(persisted);
    }

    private void restoreManualState(Player player) {
        UUID playerId = player.getUniqueId();
        Boolean persisted = VanishState.isManualVanished(playerId);
        if (persisted == null) {
            XLogger.warn("玩家 %s 的隐身持久化状态读取失败，保留当前内存状态", player.getName());
            return;
        }
        if (persisted) {
            manualVanished.add(playerId);
            return;
        }
        manualVanished.remove(playerId);
    }

    private void updatePacketTrackedState(Player player, boolean vanished) {
        UUID playerId = player.getUniqueId();
        Integer oldEntityId = packetVanishedEntityIdByUuid.remove(playerId);
        if (oldEntityId != null) {
            packetVanishedEntityIds.remove(oldEntityId);
        }
        if (!vanished) {
            if (manualVanished.contains(playerId)) {
                packetVanishedUuids.add(playerId);
            } else {
                packetVanishedUuids.remove(playerId);
            }
            return;
        }

        packetVanishedUuids.add(playerId);
        int entityId = player.getEntityId();
        if (entityId > 0) {
            packetVanishedEntityIdByUuid.put(playerId, entityId);
            packetVanishedEntityIds.add(entityId);
        }
    }

    private void refreshEnhancedModeState() {
        boolean enhancedMode = Boolean.TRUE.equals(EssentialsD.config.getVanishEnhancedMode());
        boolean protocolLibEnabled = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");

        if (!enhancedMode || !protocolLibEnabled) {
            if (protocolEnhancer != null) {
                protocolEnhancer.shutdown();
                protocolEnhancer = null;
            }
            if (enhancedMode) {
                XLogger.warn("配置 vanish.enhanced-mode=true，但未检测到 ProtocolLib，增强模式不会生效");
            }
            return;
        }

        if (protocolEnhancer == null) {
            protocolEnhancer = new VanishProtocolEnhancer(EssentialsD.instance, this);
        }
        protocolEnhancer.enable();
    }

    private void markPendingRemoval(Player viewer, Player target) {
        UUID viewerId = viewer.getUniqueId();
        UUID targetId = target.getUniqueId();
        long expiresAt = System.currentTimeMillis() + PENDING_REMOVE_TTL_MS;
        pendingPlayerInfoRemoveByViewer
                .computeIfAbsent(viewerId, ignored -> new ConcurrentHashMap<>())
                .put(targetId, expiresAt);
        int entityId = target.getEntityId();
        if (entityId > 0) {
            pendingEntityDestroyByViewer
                    .computeIfAbsent(viewerId, ignored -> new ConcurrentHashMap<>())
                    .put(entityId, expiresAt);
        }
    }

    private void clearPendingRemovalForPlayer(UUID playerId, int entityId) {
        pendingPlayerInfoRemoveByViewer.remove(playerId);
        pendingEntityDestroyByViewer.remove(playerId);
        for (Map<UUID, Long> map : pendingPlayerInfoRemoveByViewer.values()) {
            map.remove(playerId);
        }
        if (entityId > 0) {
            for (Map<Integer, Long> map : pendingEntityDestroyByViewer.values()) {
                map.remove(entityId);
            }
        }
    }

    private void clearPendingRemoval(UUID viewerId, UUID targetId, int entityId) {
        Map<UUID, Long> uuidMap = pendingPlayerInfoRemoveByViewer.get(viewerId);
        if (uuidMap != null) {
            uuidMap.remove(targetId);
            if (uuidMap.isEmpty()) {
                pendingPlayerInfoRemoveByViewer.remove(viewerId, uuidMap);
            }
        }
        if (entityId > 0) {
            Map<Integer, Long> entityMap = pendingEntityDestroyByViewer.get(viewerId);
            if (entityMap != null) {
                entityMap.remove(entityId);
                if (entityMap.isEmpty()) {
                    pendingEntityDestroyByViewer.remove(viewerId, entityMap);
                }
            }
        }
    }

    private <K> boolean consumePending(Map<UUID, Map<K, Long>> pendingByViewer, UUID viewerId, K targetKey) {
        Map<K, Long> pending = pendingByViewer.get(viewerId);
        if (pending == null) {
            return false;
        }
        Long expiresAt = pending.get(targetKey);
        if (expiresAt == null) {
            return false;
        }
        pending.remove(targetKey);
        if (pending.isEmpty()) {
            pendingByViewer.remove(viewerId, pending);
        }
        return expiresAt >= System.currentTimeMillis();
    }

    private void markRecentPacketVanishedEntityId(int entityId) {
        if (entityId <= 0) {
            return;
        }
        recentPacketVanishedEntityIds.put(entityId, System.currentTimeMillis() + PENDING_REMOVE_TTL_MS);
    }

    private boolean isRecentPacketVanishedEntityId(int entityId) {
        Long expiresAt = recentPacketVanishedEntityIds.get(entityId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            recentPacketVanishedEntityIds.remove(entityId, expiresAt);
            return false;
        }
        return true;
    }

    private record SilentContainerSession(Inventory sourceInventory, Inventory mirrorInventory) {
    }
}
