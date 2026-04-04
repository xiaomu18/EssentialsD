package et.xiaomu.essentialsd.managers;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import cn.lunadeer.utils.XLogger;
import cn.lunadeer.utils.stui.components.buttons.CommandButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {
    private static final TextColor MAIN_COLOR = TextColor.color(0, 233, 255);
    private final ConcurrentHashMap<UUID, TpTask> tasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, LocalDateTime> nextTimeAllowTp = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastTpLocation = new ConcurrentHashMap<>();

    private boolean tpReqCheck(Player initiator, Player target) {
        if (initiator == target) {
            Notification.warn(initiator, "传送到自己是不明智的哦。");
            return false;
        }
        if (!target.isOnline()) {
            Notification.error(initiator, "目标玩家 %s 不在线哦。", target.getName());
            return false;
        }
        if (EssentialsD.config.getTpWorldBlackList().contains(target.getWorld().getName())) {
            Notification.error(initiator, "目的地所在世界 %s 不允许传送哦。", target.getWorld().getName());
            return false;
        }

        for (TpTask task : tasks.values()) {
            if (task.initiator == initiator && !task.tpahere) {
                Notification.warn(initiator, "你现在无法发送传送请求，因为当前有一个传送请求还未过期。", target.getName());
                return false;
            }
        }
        return CoolingDown(initiator);
    }

    public void tpaRequest(Player initiator, Player target) {
        if (!tpReqCheck(initiator, target)) {
            return;
        }

        TpTask task = new TpTask();
        task.initiator = initiator;
        task.target = target;
        task.taskId = UUID.randomUUID();
        tasks.put(task.taskId, task);

        Notification.info(initiator, "已向 %s 发送传送请求", target.getName());
        TextComponent acceptBtn = new CommandButton("接受", "/tpa accept " + task.taskId).green().build();
        TextComponent denyBtn = new CommandButton("拒绝", "/tpa deny " + task.taskId).red().build();
        Notification.info(target, Component.text("                            ", Style.style(MAIN_COLOR, TextDecoration.STRIKETHROUGH)));
        Notification.info(target, Component.text("| 玩家 " + initiator.getName() + " 请求传送到你的位置", MAIN_COLOR));
        Notification.info(target, Component.text("| 此请求将在 " + EssentialsD.config.getTpTpaExpire() + " 秒后失效", MAIN_COLOR));
        Notification.info(target, Component.text().append(Component.text("| ", MAIN_COLOR)).append(acceptBtn).append(Component.text("  ", MAIN_COLOR)).append(denyBtn).build());
        Notification.info(target, Component.text("                            ", Style.style(MAIN_COLOR, TextDecoration.STRIKETHROUGH)));
        Scheduler.runTaskLater(() -> tasks.remove(task.taskId), 20L * EssentialsD.config.getTpTpaExpire());
    }

    public void tpahereRequest(Player initiator, Player target) {
        if (!tpReqCheck(initiator, target)) {
            return;
        }

        TpTask task = new TpTask();
        task.initiator = initiator;
        task.target = target;
        task.taskId = UUID.randomUUID();
        task.tpahere = true;
        tasks.put(task.taskId, task);

        Notification.info(initiator, "已向 %s 发送传送请求", target.getName());
        TextComponent acceptBtn = new CommandButton("接受", "/tpa accept " + task.taskId).green().build();
        TextComponent denyBtn = new CommandButton("拒绝", "/tpa deny " + task.taskId).red().build();
        Notification.info(target, Component.text("                            ", Style.style(MAIN_COLOR, TextDecoration.STRIKETHROUGH)));
        Notification.info(target, Component.text("| 玩家 " + initiator.getName() + " 请求传送你到他的位置", MAIN_COLOR));
        Notification.info(target, Component.text("| 此请求将在 " + EssentialsD.config.getTpTpaExpire() + " 秒后失效", MAIN_COLOR));
        Notification.info(target, Component.text().append(Component.text("| ", MAIN_COLOR)).append(acceptBtn).append(Component.text("  ", MAIN_COLOR)).append(denyBtn).build());
        Notification.info(target, Component.text("                            ", Style.style(MAIN_COLOR, TextDecoration.STRIKETHROUGH)));
        Scheduler.runTaskLater(() -> tasks.remove(task.taskId), 20L * EssentialsD.config.getTpTpaExpire());
    }

    public void cancelRequests(Player initiator) {
        int cancelled = 0;
        for (TpTask task : tasks.values()) {
            if (!task.initiator.getUniqueId().equals(initiator.getUniqueId())) {
                continue;
            }

            if (tasks.remove(task.taskId, task)) {
                cancelled++;
                if (task.target.isOnline()) {
                    Notification.warn(task.target, "来自 %s 的传送请求已被取消", initiator.getName());
                }
            }
        }

        if (cancelled == 0) {
            Notification.warn(initiator, "你当前没有可取消的传送请求");
            return;
        }

        Notification.info(initiator, "已取消 %d 个你发起的传送请求", cancelled);
    }

    public void deny(Player player, UUID taskId) {
        TpTask task = tasks.get(taskId);
        if (task == null) {
            Notification.error(player, "传送请求不存在或已过期");
            return;
        }
        if (task.target != player) {
            Notification.error(player, "这不是你的传送请求");
            return;
        }

        tasks.remove(taskId);
        if (task.initiator.isOnline()) {
            Notification.error(task.initiator, "玩家 %s 拒绝了你的传送请求", player.getName());
        }
        if (task.target.isOnline()) {
            Notification.error(player, "已拒绝来自 %s 的传送请求", task.initiator.getName());
        }
    }

    public void accept(Player player, UUID taskId) {
        TpTask task = tasks.get(taskId);
        if (task == null) {
            Notification.error(player, "传送请求不存在或已过期");
            return;
        }
        if (task.target != player) {
            Notification.error(player, "这不是你的传送请求");
            return;
        }

        tasks.remove(taskId);
        if (!task.initiator.isOnline() || !task.target.isOnline()) {
            return;
        }

        Notification.info(task.target, "已接受 %s 的传送请求", task.initiator.getName());
        Notification.info(task.initiator, "玩家 %s 已接受你的传送请求", task.target.getName());

        if (!task.tpahere) {
            try {
                doTeleportDelayed(task.initiator, task.target.getLocation(), EssentialsD.config.getTpDelay(),
                        () -> Notification.info(task.initiator, "正在传送到 %s 的位置", task.target.getName()),
                        () -> {
                            Notification.info(task.initiator, "已传送到 %s 的位置", task.target.getName());
                            Notification.info(task.target, "玩家 %s 已传送到你的位置", task.initiator.getName());
                        });
            } catch (RuntimeException e) {
                Notification.error(player, e.getMessage());
            }
        } else {
            try {
                doTeleportDelayed(task.target, task.initiator.getLocation(), EssentialsD.config.getTpDelay(),
                        () -> Notification.info(task.target, "正在传送到 %s 的位置", task.initiator.getName()),
                        () -> {
                            Notification.info(task.target, "已传送到 %s 的位置", task.initiator.getName());
                            Notification.info(task.initiator, "玩家 %s 已传送到你的位置", task.target.getName());
                        });
            } catch (RuntimeException e) {
                Notification.error(player, e.getMessage());
            }
        }
    }

    public void back(Player player) {
        if (!lastTpLocation.containsKey(player.getUniqueId())) {
            Notification.error(player, "没有找到可返回的位置");
            return;
        }

        Location target = lastTpLocation.get(player.getUniqueId());
        if (EssentialsD.config.getTpWorldBlackList().contains(target.getWorld().getName())) {
            Notification.error(player, "目的地所在世界 %s 不允许传送", target.getWorld().getName());
            return;
        }
        if (!CoolingDown(player)) {
            return;
        }

        if (EssentialsD.config.getTpDelay() > 0) {
            Notification.info(player, "将在 %d 秒后返回上次传送的位置", EssentialsD.config.getTpDelay());
        }

        try {
            doTeleportDelayed(player, target, EssentialsD.config.getTpDelay(),
                    () -> Notification.info(player, "正在返回上次传送的位置"),
                    () -> Notification.info(player, "已返回上次传送的位置"));
        } catch (RuntimeException e) {
            Notification.error(player, e.getMessage());
        }
    }

    public void rtp(Player player) {
        if (EssentialsD.config.getTpWorldBlackList().contains(player.getWorld().getName())) {
            Notification.error(player, "此世界 %s 不允许传送", player.getWorld().getName());
            return;
        }
        if (!CoolingDown(player)) {
            return;
        }

        int radius = EssentialsD.config.getTpRtpRadius();
        World world = null;
        for (World current : EssentialsD.instance.getServer().getWorlds()) {
            if (current.getEnvironment() == World.Environment.NORMAL) {
                world = current;
                break;
            }
        }

        if (world == null) {
            Notification.error(player, "未找到主世界");
            return;
        }

        int x = (int) (Math.random() * radius * 2.0D) - radius + (int) player.getLocation().getX();
        int z = (int) (Math.random() * radius * 2.0D) - radius + (int) player.getLocation().getZ();
        XLogger.debug("RTP: " + x + " " + z);
        Location location = new Location(world, x + 0.5D, player.getY(), z + 0.5D);

        try {
            doTeleportDelayed(player, location, EssentialsD.config.getTpDelay(),
                    () -> Notification.info(player, "正在传送到随机位置"),
                    () -> Notification.info(player, "已传送到随机位置"));
        } catch (RuntimeException e) {
            Notification.error(player, e.getMessage());
        }
    }

    public void doTeleportDelayed(Player player, Location location, Integer delay, Runnable before, Runnable after) {
        doTeleportDelayed(player, location, delay.longValue(), before, after);
    }

    public void doTeleportDelayed(Player player, Location to, Long delay, Runnable before, Runnable after) {
        if (EssentialsD.config.getTpWorldBlackList().contains(to.getWorld().getName())) {
            Notification.error(player, "目的地所在世界 %s 不允许传送", to.getWorld().getName());
            return;
        }
        if (!CoolingDown(player)) {
            return;
        }

        if (delay > 0L) {
            Notification.info(player, "将在 %d 秒后执行传送", delay);
            Scheduler.runTaskAsync(() -> {
                long left = delay;
                while (left > 0L) {
                    if (!player.isOnline()) {
                        return;
                    }
                    Notification.actionBar(player, "传送倒计时：%d 秒", left);
                    left--;
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        XLogger.warn(e.getMessage());
                        return;
                    }
                }
            });
            Scheduler.runTaskLater(() -> {
                before.run();
                doTeleportSafely(player, to);
                after.run();
            }, 20L * delay);
            return;
        }

        before.run();
        doTeleportSafely(player, to);
        after.run();
    }

    private boolean CoolingDown(Player player) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextTime = nextTimeAllowTp.get(player.getUniqueId());
        if (nextTime != null && now.isBefore(nextTime)) {
            long secsUntilNext = now.until(nextTime, ChronoUnit.SECONDS);
            Notification.warn(player, "请等待 %d 秒后再次执行传送请求", secsUntilNext);
            return false;
        }
        return true;
    }

    public void doTeleportSafely(Player player, Location location) {
        if (!CoolingDown(player)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        nextTimeAllowTp.put(player.getUniqueId(), now.plusSeconds(EssentialsD.config.getTpCoolDown()));
        location.getWorld().getChunkAtAsyncUrgently(location).thenAccept(chunk -> {
            int maxAttempts = 512;

            while (location.getBlock().isPassable()) {
                location.setY(location.getY() - 1.0D);
                maxAttempts--;
                if (maxAttempts <= 0) {
                    Notification.error(player, "传送目的地不安全，已取消传送");
                    return;
                }
            }

            Block up1 = location.getBlock().getRelative(BlockFace.UP);
            Block up2 = up1.getRelative(BlockFace.UP);
            maxAttempts = 512;

            while (!up1.isPassable() || up1.isLiquid() || !up2.isPassable() || up2.isLiquid()) {
                location.setY(location.getY() + 1.0D);
                up1 = location.getBlock().getRelative(BlockFace.UP);
                up2 = up1.getRelative(BlockFace.UP);
                maxAttempts--;
                if (maxAttempts <= 0) {
                    Notification.error(player, "传送目的地不安全，已取消传送");
                    return;
                }
            }

            location.setY(location.getY() + 1.0D);
            if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.LAVA) {
                Notification.error(player, "传送目的地不安全，已取消传送");
                return;
            }

            updateLastTpLocation(player);
            player.teleportAsync(location, TeleportCause.PLUGIN);
        });
    }

    public void updateLastTpLocation(Player player) {
        lastTpLocation.put(player.getUniqueId(), player.getLocation());
    }

    public void shutdown() {
        tasks.clear();
        nextTimeAllowTp.clear();
        lastTpLocation.clear();
    }

    private static class TpTask {
        private Player initiator;
        private Player target;
        private UUID taskId;
        private boolean tpahere;
    }
}
