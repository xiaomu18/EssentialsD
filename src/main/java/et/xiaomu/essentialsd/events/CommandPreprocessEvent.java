package et.xiaomu.essentialsd.events;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CommandPreprocessEvent implements Listener {
    private static final String BLOCKED_COMMAND_ALIAS = "BLOCKED_COMMAND";
    private static final Set<String> PRIVATE_MESSAGE_COMMANDS = new HashSet<>(Arrays.asList(
            "msg", "tell", "w", "whisper"
    ));

    // 存储玩家最后一次使用命令的时间戳
    private final Map<UUID, Long> lastCommandTime = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 跳过没有实际内容的命令
        if (message.length() < 2) return;

        if (shouldBlockPrivateMessage(player, message)) {
            event.setCancelled(true);
            player.sendMessage(Component.translatable("argument.entity.notfound.player").color(NamedTextColor.RED));
            return;
        }

        if (EssentialsD.muteManager.getMute(player) != null && EssentialsD.muteManager.isBlockedCommand(message)) {
            event.setCancelled(true);
            Notification.warn(player, EssentialsD.config.MUTE_BLOCKED_COMMAND_MESSAGE);
            return;
        }

        if (!EssentialsD.config.CMD_ENABLE) {
            return;
        }

        String rootCommand = extractRootCommand(message);
        if (isBlockedCommand(rootCommand)) {
            event.setMessage("/" + BLOCKED_COMMAND_ALIAS);
            return;
        }

        // 检查玩家是否有绕过权限
        if (!player.hasPermission("essd.bypass.CommandCD")) {
            long currentTime = System.currentTimeMillis();

            if (lastCommandTime.containsKey(player.getUniqueId())) {
                long lastTime = lastCommandTime.get(player.getUniqueId());

                // 检查是否仍在冷却中
                if (currentTime - lastTime < EssentialsD.config.CMD_COOLDOWN_MS) {
                    lastCommandTime.put(player.getUniqueId(), currentTime);
                    event.setCancelled(true);
                    player.sendMessage(EssentialsD.config.CMD_CD_MESSAGE);
                    return;
                }
            }

            // 更新最后使用时间
            lastCommandTime.put(player.getUniqueId(), currentTime);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        if (!EssentialsD.config.CMD_ENABLE) {
            return;
        }
        event.getCommands().removeIf(this::isBlockedCommand);
    }

    private boolean isBlockedCommand(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        for (String bannedCommand : EssentialsD.config.CMD_BANNED_LIST) {
            if (command.equals(bannedCommand)) {
                return true;
            }
        }
        return false;
    }

    private String extractRootCommand(String message) {
        String value = message == null ? "" : message.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isBlank()) {
            return "";
        }
        int spaceIndex = value.indexOf(' ');
        if (spaceIndex >= 0) {
            value = value.substring(0, spaceIndex);
        }
        int namespaceIndex = value.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < value.length()) {
            value = value.substring(namespaceIndex + 1);
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private boolean shouldBlockPrivateMessage(Player sender, String message) {
        if (!Boolean.TRUE.equals(EssentialsD.config.getVanishBlockPrivateMessageToVanished())) {
            return false;
        }

        String rootCommand = extractRootCommand(message);
        if (!PRIVATE_MESSAGE_COMMANDS.contains(rootCommand)) {
            return false;
        }

        String targetName = extractFirstArgument(message);
        if (targetName == null || targetName.isBlank()) {
            return false;
        }

        Player target = Bukkit.getPlayer(targetName);
        return target != null && EssentialsD.vanishManager.isHiddenFrom(sender, target);
    }

    private String extractFirstArgument(String message) {
        String value = message == null ? "" : message.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.isBlank()) {
            return null;
        }
        String[] parts = value.split("\\s+", 3);
        if (parts.length < 2) {
            return null;
        }
        return parts[1];
    }
}
