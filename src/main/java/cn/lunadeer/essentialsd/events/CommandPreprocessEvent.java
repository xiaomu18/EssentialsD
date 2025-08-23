package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandPreprocessEvent implements Listener {
    // 存储玩家最后一次使用命令的时间戳
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 跳过没有实际内容的命令
        if (message.length() < 2) return;

        if (!EssentialsD.config.CMD_ENABLE) {
            return;
        }

        // 检查玩家是否有绕过权限
        if (!player.hasPermission("essd.bypass.CommandCD")) {
            long currentTime = System.currentTimeMillis();

            if (lastCommandTime.containsKey(player.getUniqueId())) {
                long lastTime = lastCommandTime.get(player.getUniqueId());

                // 检查是否仍在冷却中
                if (currentTime - lastTime < EssentialsD.config.CMD_COOLDOWN_MS) {
                    event.setCancelled(true);
                    player.sendMessage(EssentialsD.config.CMD_CD_MESSAGE);
                    return;
                }
            }

            // 更新最后使用时间
            lastCommandTime.put(player.getUniqueId(), currentTime);
        }

        if (!player.isOp()) {
            for (String banned_cmd : EssentialsD.config.CMD_BANNED_LIST) {
                if (message.startsWith(banned_cmd)) {
                    event.setCancelled(true);
                    Notification.warn(player, "此命令在此服务器上不可用");
                }
            }
        }
    }
}
