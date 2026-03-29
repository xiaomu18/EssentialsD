package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.commands.Mute;
import cn.lunadeer.utils.Notification;
import fr.xephi.authme.api.v3.AuthMeApi;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatFunctionEvent implements Listener {
    private final Map<UUID, Long> lastChatTimes = new ConcurrentHashMap<>();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();

        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
            if (!AuthMeApi.getInstance().isAuthenticated(player)) {
                event.setCancelled(true);
                Notification.warn(player, "请先完成登录验证再尝试发送消息");
                return;
            }
        }

        if (((Mute) EssentialsD.commands.get("mute")).mutedList.contains(player.getUniqueId())) {
            Notification.warn(player, "你已被禁言");
            event.setCancelled(true);
            return;
        }
        if (((Mute) EssentialsD.commands.get("mute")).mutedIpList.contains(player.getAddress().getHostString())) {
            Notification.warn(player, "你已被禁言");
            event.setCancelled(true);
            return;
        }

        boolean self_deception = false;

        if (EssentialsD.config.chat_func_enable && !player.isOp()) {
            UUID uuid = player.getUniqueId();

            // 检查冷却
            if (lastChatTimes.containsKey(uuid)) {
                long currentTime = System.currentTimeMillis();
                long lastChatTime = lastChatTimes.get(uuid);

                if (currentTime - lastChatTime < EssentialsD.config.COOLDOWN_MS) {
                    // 取消聊天并发送提示
                    event.setCancelled(true);
                    Notification.warn(player, "你发言太快, 请稍等片刻后重新发送");
                    return;
                }
            }

            // 更新最后发言时间
            lastChatTimes.put(uuid, System.currentTimeMillis());

            for (String block_string : EssentialsD.config.forbidWords) {
                if (event.getMessage().contains(block_string)) {
                    event.setCancelled(true);
                    if (!EssentialsD.config.self_deception_mode) {
                        Notification.warn(player, EssentialsD.config.forbidMessage);
                        return;
                    } else {
                        self_deception = true;
                    }
                }
            }

            EssentialsD.config.replaceWords.forEach((key, value) -> {
                if (event.getMessage().contains(key)) {
                    event.setMessage(event.getMessage().replace(key, value));
                }
            });
        }

        if (EssentialsD.config.chat_func_enable) {
            event.setCancelled(true);

            if (EssentialsD.config.isDebug()) {
                EssentialsD.instance.getLogger().info("[DEBUG: Chat Raw Message] <" + player.getName() + "> " + event.getMessage());
            }

            StringBuilder chatFormat = new StringBuilder();

            // 获取 chat format 列表
            List<Map<?, ?>> chatFormatList = EssentialsD.config.getConfig().getMapList("chat.format");

            for (Map<?, ?> entry : chatFormatList) {
                // 获取权限（可能为空）
                String permission = (String) entry.get("permission");

                if (permission != null && !permission.isEmpty()) {
                    if (permission.startsWith("!")) {
                        if (player.hasPermission(permission.replace("!", ""))) {
                            continue;
                        }
                    } else if (!player.hasPermission(permission)) {
                        continue;
                    }
                }

                // 获取条件列表
                List<?> conditions = (List<?>) entry.get("conditions");

                if (conditions != null) {
                    boolean pass = true;
                    for (Object obj : conditions) {
                        String condition = (String) obj;
                        if (!condition.isEmpty() && !checkCondition(player, condition)) {
                            pass = false;
                            break;
                        }
                    }
                    if (!pass) {
                        continue;
                    }
                }

                // 获取文本内容
                String text = (String) entry.get("text");

                if (text == null || text.isEmpty()) {
                    continue;
                }

                chatFormat.append(PlaceholderAPI.setPlaceholders(player, text));
            }

            Component parsed = MiniMessage.miniMessage().deserialize(convertLegacyToMiniMessage(chatFormat.toString()));

            TextReplacementConfig replacement;
            String replaceMessage;

            if (player.hasPermission(EssentialsD.config.allow_minimessage_perm)) {
                replaceMessage = event.getMessage();
            } else {
                replaceMessage = event.getMessage().replace("<", "\\<");
            }

            replacement = TextReplacementConfig.builder()
                    .matchLiteral("$player_message$")
                    .replacement(MiniMessage.miniMessage().deserialize(convertLegacyToMiniMessage(replaceMessage))) // 纯文本，不会解析 MiniMessage
                    .build();

            // 执行替换
            parsed = parsed.replaceText(replacement);

            if (self_deception) {
                player.sendMessage(parsed);
                EssentialsD.instance.getServer().getLogger().info("[仅自己可见] " + player.getName() + ": " + event.getMessage());
                return;
            }
            // 向全服广播（包括控制台）
            EssentialsD.instance.getServer().sendMessage(parsed);
        }
    }

    private static String convertLegacyToMiniMessage(String message) {
        // 先处理 §x§F§F§F§F§F§F 格式的十六进制颜色代码
        Pattern pattern = Pattern.compile("§x(§[0-9a-fA-F]){6}");
        Matcher matcher = pattern.matcher(message);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String match = matcher.group();
            // 提取十六进制值并转换为 MiniMessage 格式
            String hex = match.replace("§", "").substring(1); // 去掉所有 § 并去掉开头的 'x'
            matcher.appendReplacement(sb, "<reset><#" + hex + ">");
        }
        matcher.appendTail(sb);
        message = sb.toString();

        // 处理十六进制颜色代码 (§#FFFFFF 格式)
        message = message.replaceAll("§(#([0-9a-fA-F]{6}))", "<reset><$1>");

        // 替换颜色代码
        message = message.replace("§0", "<reset><black>");
        message = message.replace("§1", "<reset><dark_blue>");
        message = message.replace("§2", "<reset><dark_green>");
        message = message.replace("§3", "<reset><dark_aqua>");
        message = message.replace("§4", "<reset><dark_red>");
        message = message.replace("§5", "<reset><dark_purple>");
        message = message.replace("§6", "<reset><gold>");
        message = message.replace("§7", "<reset><gray>");
        message = message.replace("§8", "<reset><dark_gray>");
        message = message.replace("§9", "<reset><blue>");
        message = message.replace("§a", "<reset><green>");
        message = message.replace("§b", "<reset><aqua>");
        message = message.replace("§c", "<reset><red>");
        message = message.replace("§d", "<reset><light_purple>");
        message = message.replace("§e", "<reset><yellow>");
        message = message.replace("§f", "<reset><white>");

        // 替换格式代码
        message = message.replace("§k", "<obfuscated>");
        message = message.replace("§l", "<bold>");
        message = message.replace("§m", "<strikethrough>");
        message = message.replace("§n", "<underlined>");
        message = message.replace("§o", "<italic>");
        message = message.replace("§r", "<reset>");

        return message;
    }

    private static Boolean checkCondition(Player player, String condition) {
        if (condition.contains(" == ")) {
            String[] parts = condition.split(" == ", 2);
            return PlaceholderAPI.setPlaceholders(player, parts[0]).equals(PlaceholderAPI.setPlaceholders(player, parts[1]));
        } else if (condition.contains(" != ")) {
            String[] parts = condition.split(" != ", 2);
            return !PlaceholderAPI.setPlaceholders(player, parts[0]).equals(PlaceholderAPI.setPlaceholders(player, parts[1]));
        } else if (condition.contains(" > ")) {
            String[] parts = condition.split(" > ", 2);
            int part1 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[0]));
            int part2 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[1]));
            return part1 > part2;
        } else if (condition.contains(" < ")) {
            String[] parts = condition.split(" < ", 2);
            int part1 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[0]));
            int part2 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[1]));
            return part1 < part2;
        } else if (condition.contains(" >= ")) {
            String[] parts = condition.split(" >= ", 2);
            int part1 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[0]));
            int part2 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[1]));
            return part1 >= part2;
        } else if (condition.contains(" <= ")) {
            String[] parts = condition.split(" <= ", 2);
            int part1 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[0]));
            int part2 = Integer.parseInt(PlaceholderAPI.setPlaceholders(player, parts[1]));
            return part1 <= part2;
        }

        return false;
    }
}
