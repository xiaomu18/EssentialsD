package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.commands.Mute;
import cn.lunadeer.minecraftpluginutils.Notification;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
               Notification.warn(player, EssentialsD.config.forbidMessage);
               return;
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
         String formated = PlaceholderAPI.setPlaceholders(player, EssentialsD.config.chat_format);
         formated = formated.replace("$player_message$", event.getMessage());
         EssentialsD.instance.getServer().broadcastMessage(formated);
      }
   }
}
