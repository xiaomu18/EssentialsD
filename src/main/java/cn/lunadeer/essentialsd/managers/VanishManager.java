package cn.lunadeer.essentialsd.managers;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VanishManager {
    public static final String PERMISSION_BASE = "essd.vanish";
    public static final String PERMISSION_OTHER = "essd.vanish.other";
    public static final String PERMISSION_SEE = "essd.vanish.see";
    public static final String PERMISSION_CHAT = "essd.vanish.chat";

    private final Set<UUID> manualVanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> forcedVanished = ConcurrentHashMap.newKeySet();
    private final Set<UUID> vanishInvulnerable = ConcurrentHashMap.newKeySet();
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

    public boolean isManual(UUID playerId) {
        return manualVanished.contains(playerId);
    }

    public boolean canSeeVanished(CommandSender sender) {
        return sender.hasPermission(PERMISSION_SEE);
    }

    public boolean canChatWhileVanished(Player player) {
        return player.hasPermission(PERMISSION_CHAT);
    }

    public boolean isHiddenFrom(Player viewer, Player target) {
        return !viewer.getUniqueId().equals(target.getUniqueId())
                && isVanished(target.getUniqueId())
                && !canSeeVanished(viewer);
    }

    public boolean setManualVanished(Player player, boolean vanished) {
        UUID playerId = player.getUniqueId();
        boolean wasVanished = isVanished(playerId);
        boolean changed = vanished ? manualVanished.add(playerId) : manualVanished.remove(playerId);
        boolean isVanished = isVanished(playerId);
        if (wasVanished != isVanished) {
            onVisibilityStateChanged(player, isVanished);
        } else if (isVanished && changed) {
            ensureBossBar(player);
        }
        return changed;
    }

    public boolean refreshForcedState(Player player, boolean notify) {
        return refreshForcedState(player, player.getGameMode(), notify);
    }

    public boolean refreshForcedState(Player player, GameMode mode, boolean notify) {
        UUID playerId = player.getUniqueId();
        boolean shouldForce = shouldForceVanish(mode);
        boolean wasVanished = isVanished(playerId);
        boolean changed = shouldForce ? forcedVanished.add(playerId) : forcedVanished.remove(playerId);
        boolean isVanished = isVanished(playerId);

        if (wasVanished != isVanished) {
            onVisibilityStateChanged(player, isVanished);
        } else if (isVanished) {
            ensureBossBar(player);
        } else {
            removeBossBar(player);
        }

        if (notify && changed) {
            if (shouldForce) {
                Notification.warn(player, "你当前游戏模式不同于服务器默认游戏模式，已强制开启隐身");
            } else {
                Notification.info(player, "你的游戏模式已恢复为默认模式，强制隐身已解除");
            }
        }
        return changed;
    }

    public void handleJoin(Player player) {
        refreshForcedState(player, false);
        if (isVanished(player)) {
            ensureBossBar(player);
            ensureInvulnerable(player);
            Notification.warn(player, isForced(player.getUniqueId()) ? "你当前处于强制隐身状态" : "你仍处于隐身状态");
        } else {
            removeBossBar(player);
            clearVanishInvulnerable(player);
        }
        refreshTargetForAllViewers(player);
        refreshViewerForAllTargets(player);
    }

    public void handleQuit(Player player) {
        forcedVanished.remove(player.getUniqueId());
        vanishInvulnerable.remove(player.getUniqueId());
        removeBossBar(player);
    }

    public void reload() {
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            refreshForcedState(player, true);
        }
    }

    public List<Player> getVanishedPlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(this::isVanished)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean shouldForceVanish(GameMode gameMode) {
        return EssentialsD.config.getForceVanishInDifferentGamemode()
                && gameMode != Bukkit.getDefaultGameMode();
    }

    private void onVisibilityStateChanged(Player target, boolean vanished) {
        if (vanished) {
            ensureBossBar(target);
            ensureInvulnerable(target);
        } else {
            removeBossBar(target);
            clearVanishInvulnerable(target);
        }
        refreshTargetForAllViewers(target);
    }

    private void refreshTargetForAllViewers(Player target) {
        for (Player viewer : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            refreshViewer(viewer, target);
        }
    }

    private void refreshViewerForAllTargets(Player viewer) {
        for (Player target : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            refreshViewer(viewer, target);
        }
    }

    private void refreshViewer(Player viewer, Player target) {
        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return;
        }
        Scheduler.runEntityTask(viewer, () -> {
            if (!viewer.isOnline() || !target.isOnline()) {
                return;
            }
            if (isHiddenFrom(viewer, target)) {
                viewer.hidePlayer(EssentialsD.instance, target);
            } else {
                viewer.showPlayer(EssentialsD.instance, target);
            }
        });
    }

    private void ensureBossBar(Player player) {
        Scheduler.runEntityTask(player, () -> {
            if (player.isOnline()) {
                bossBar.addPlayer(player);
            }
        });
    }

    private void removeBossBar(Player player) {
        Scheduler.runEntityTask(player, () -> bossBar.removePlayer(player));
    }

    private void ensureInvulnerable(Player player) {
        Scheduler.runEntityTask(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!player.isInvulnerable()) {
                vanishInvulnerable.add(player.getUniqueId());
                player.setInvulnerable(true);
            }
        });
    }

    private void clearVanishInvulnerable(Player player) {
        Scheduler.runEntityTask(player, () -> {
            if (!player.isOnline()) {
                vanishInvulnerable.remove(player.getUniqueId());
                return;
            }
            if (vanishInvulnerable.remove(player.getUniqueId())) {
                player.setInvulnerable(false);
            }
        });
    }
}
