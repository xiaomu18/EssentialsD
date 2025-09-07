package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.commands.Mute;
import cn.lunadeer.minecraftpluginutils.Notification;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import fr.xephi.authme.api.v3.AuthMeApi;


public class ChatFunctionEvent implements Listener {
   private final Map<UUID, Long> lastChatTimes = new ConcurrentHashMap<>();

   @EventHandler
   public void onChat(AsyncPlayerChatEvent event) {
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

         String formated = PlaceholderAPI.setPlaceholders(player, EssentialsD.config.chat_format);
         Component parsed = MiniMessage.miniMessage().deserialize(convertLegacyToMiniMessage(formated));

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
      // 先处理十六进制颜色代码 (§#FFFFFF 格式)
      message = message.replaceAll("§(#([0-9a-fA-F]{6}))", "<$1>");

      // 替换颜色代码
      message = message.replace("§0", "<black>");
      message = message.replace("§1", "<dark_blue>");
      message = message.replace("§2", "<dark_green>");
      message = message.replace("§3", "<dark_aqua>");
      message = message.replace("§4", "<dark_red>");
      message = message.replace("§5", "<dark_purple>");
      message = message.replace("§6", "<gold>");
      message = message.replace("§7", "<gray>");
      message = message.replace("§8", "<dark_gray>");
      message = message.replace("§9", "<blue>");
      message = message.replace("§a", "<green>");
      message = message.replace("§b", "<aqua>");
      message = message.replace("§c", "<red>");
      message = message.replace("§d", "<light_purple>");
      message = message.replace("§e", "<yellow>");
      message = message.replace("§f", "<white>");

      // 替换格式代码
      message = message.replace("§k", "<obfuscated>");
      message = message.replace("§l", "<bold>");
      message = message.replace("§m", "<strikethrough>");
      message = message.replace("§n", "<underlined>");
      message = message.replace("§o", "<italic>");
      message = message.replace("§r", "<reset>");

      return message;
   }
}
