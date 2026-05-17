package et.xiaomu.essentialsd.events;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.managers.ChatAntiSpamManager;
import et.xiaomu.essentialsd.managers.MuteManager;
import cn.lunadeer.utils.Notification;
import fr.xephi.authme.api.v3.AuthMeApi;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChatFunctionEvent implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {

        Player player = event.getPlayer();
        String originalMessage = event.getMessage();
        String normalizedMessage = ChatAntiSpamManager.normalizeForDetection(originalMessage);
        boolean self_deception = false;
        String antiSpamIdentityKey = null;
        long currentTime = System.currentTimeMillis();

        if (EssentialsD.vanishManager.isVanished(player) && !EssentialsD.vanishManager.canChatWhileVanished(player)) {
            event.setCancelled(true);
            Notification.warnKey(player, "messages.chat.vanished_blocked");
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("AuthMe")) {
            if (!AuthMeApi.getInstance().isAuthenticated(player)) {
                event.setCancelled(true);
                Notification.warnKey(player, "messages.chat.authme_not_logged_in");
                return;
            }
        }

        MuteManager.Entry muteEntry = EssentialsD.muteManager.getMute(player);
        if (muteEntry != null) {
            if (!EssentialsD.muteManager.isSelfDeceptionMode()) {
                Notification.warnKey(player, "messages.mute.blocked");
                event.setCancelled(true);
                return;
            }
            self_deception = true;
        }

        if (EssentialsD.config.chat_func_enable && !player.isOp()) {
            ChatAntiSpamManager.CheckResult antiSpamResult = EssentialsD.chatAntiSpamManager.checkAttempt(player, normalizedMessage, currentTime);
            antiSpamIdentityKey = antiSpamResult.identityKey();

            if (antiSpamResult.type() == ChatAntiSpamManager.CheckType.RATE_LIMIT) {
                event.setCancelled(true);
                Notification.warnKey(player, "messages.chat.rate_limit");
                return;
            }

            if (antiSpamResult.type() == ChatAntiSpamManager.CheckType.COOLDOWN) {
                event.setCancelled(true);
                EssentialsD.chatAntiSpamManager.recordRateLimitAttempt(antiSpamIdentityKey, currentTime);
                Notification.warnKey(player, "messages.chat.cooldown");
                return;
            }

            if (EssentialsD.config.chat_max_length > 0
                    && ChatAntiSpamManager.getVisibleLength(originalMessage) > EssentialsD.config.chat_max_length) {
                event.setCancelled(true);
                Notification.warnKey(player, "messages.chat.too_long");
                return;
            }

            if (antiSpamResult.type() == ChatAntiSpamManager.CheckType.REPEAT) {
                event.setCancelled(true);
                if (!EssentialsD.config.chat_self_deception_enable) {
                    Notification.warnKey(player, "messages.chat.duplicate");
                    return;
                }
                self_deception = true;
            }

            for (String block_string : EssentialsD.config.forbidWords) {
                if (normalizedMessage.contains(block_string)) {
                    event.setCancelled(true);
                    if (!EssentialsD.config.chat_self_deception_enable) {
                        Notification.warnKey(player, "messages.chat.forbid");
                        return;
                    }
                    self_deception = true;
                    break;
                }
            }

            EssentialsD.config.replaceWords.forEach((key, value) -> {
                if (event.getMessage().contains(key)) {
                    event.setMessage(event.getMessage().replace(key, value));
                }
            });
        }

        if (EssentialsD.pureManager.consumeFirstChatNotice(player.getUniqueId())) {
            player.sendMessage(Component.text(
                    EssentialsD.localization.get("messages.chat.pure_notice"),
                    NamedTextColor.GRAY
            ));
        }

        if (!EssentialsD.config.chat_func_enable) {
            if (self_deception) {
                event.setCancelled(true);
                Component message = Component.text("<" + player.getName() + "> " + event.getMessage());
                sendSelfDeceptionMessage(player, message, event.getRecipients());
                logSelfDeception(player, event.getMessage());
                return;
            }
            filterRecipients(event, player);
            return;
        }

        if (EssentialsD.config.chat_func_enable) {
            event.setCancelled(true);

            if (EssentialsD.config.isDebug()) {
                EssentialsD.instance.getLogger().info("[DEBUG: Chat Raw Message] <" + player.getName() + "> " + event.getMessage());
            }

            StringBuilder chatFormat = new StringBuilder();

            // 获取 chat format 列表
            List<Map<?, ?>> chatFormatList = EssentialsD.config.getChatConfig().getMapList("format");

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

            String replaceMessage;

            if (player.hasPermission(EssentialsD.config.allow_minimessage_perm)) {
                replaceMessage = event.getMessage();
            } else {
                replaceMessage = event.getMessage().replace("<", "\\<");
            }

            TextReplacementConfig replacement = TextReplacementConfig.builder()
                    .matchLiteral("$player_message$")
                    .replacement(MiniMessage.miniMessage().deserialize(convertLegacyToMiniMessage(replaceMessage))) // 纯文本，不会解析 MiniMessage
                    .build();

            // 执行替换
            parsed = parsed.replaceText(replacement);

            if (self_deception) {
                sendSelfDeceptionMessage(player, parsed, event.getRecipients());
                logSelfDeception(player, event.getMessage());
                if (antiSpamIdentityKey != null) {
                    EssentialsD.chatAntiSpamManager.recordRateLimitAttempt(antiSpamIdentityKey, currentTime);
                    EssentialsD.chatAntiSpamManager.recordVisibleMessage(antiSpamIdentityKey, normalizedMessage, currentTime);
                }
                return;
            }

            broadcastFiltered(player, parsed, event.getRecipients());
            if (antiSpamIdentityKey != null) {
                EssentialsD.chatAntiSpamManager.recordRateLimitAttempt(antiSpamIdentityKey, currentTime);
                EssentialsD.chatAntiSpamManager.recordVisibleMessage(antiSpamIdentityKey, normalizedMessage, currentTime);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        EssentialsD.chatAntiSpamManager.clearPlayer(event.getPlayer());
    }

    private void filterRecipients(AsyncPlayerChatEvent event, Player sender) {
        if (!EssentialsD.pureManager.isFeatureEnabled()) {
            return;
        }
        event.getRecipients().removeIf(recipient -> !EssentialsD.pureManager.canMutuallySee(sender.getUniqueId(), recipient.getUniqueId()));
    }

    private void broadcastFiltered(Player sender, Component message, Set<Player> recipients) {
        for (Player recipient : recipients) {
            if (!EssentialsD.pureManager.canMutuallySee(sender.getUniqueId(), recipient.getUniqueId())) {
                continue;
            }
            recipient.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private void sendSelfDeceptionMessage(Player sender, Component message, Set<Player> recipients) {
        sender.sendMessage(message);
        if (!EssentialsD.config.chat_self_deception_show_to_same_ip_players) {
            return;
        }

        String senderIp = MuteManager.normalizeIp(MuteManager.getPlayerIp(sender));
        if (senderIp == null || senderIp.isBlank()) {
            return;
        }

        for (Player recipient : recipients) {
            if (recipient.getUniqueId().equals(sender.getUniqueId())) {
                continue;
            }
            String recipientIp = MuteManager.normalizeIp(MuteManager.getPlayerIp(recipient));
            if (!Objects.equals(senderIp, recipientIp)) {
                continue;
            }
            if (!EssentialsD.pureManager.canMutuallySee(sender.getUniqueId(), recipient.getUniqueId())) {
                continue;
            }
            recipient.sendMessage(message);
        }
    }

    private void logSelfDeception(Player player, String message) {
        EssentialsD.instance.getServer().getLogger().info("[自我欺骗] " + player.getName() + ": " + message);
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
