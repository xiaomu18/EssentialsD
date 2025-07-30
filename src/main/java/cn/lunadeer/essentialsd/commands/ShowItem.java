package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.stui.components.Button;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public class ShowItem implements CommandExecutor {
   private static final TextColor show_color = TextColor.color(0, 148, 213);
   public static final Map cache = new HashMap();

   public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
         return true;
      } else {
         Player player = (Player)sender;
         if (args.length == 1) {
            openView(player, args[0]);
            return true;
         } else {
            PlayerInventory backpack = player.getInventory();
            ItemStack item = backpack.getItemInMainHand().clone();
            if (item.getType().isAir()) {
               Notification.warn(player, "你的主手为空");
               return true;
            } else {
               String name_str = item.getType().name();
               if (item.getItemMeta().hasDisplayName()) {
                  name_str = ((TextComponent)item.getItemMeta().displayName()).content();
               }

               UUID uuid = UUID.randomUUID();
               Button show = Button.create(name_str).setExecuteCommand("/showitem " + uuid).setHoverText("点击查看物品信息");
               TextComponent title = (TextComponent)((TextComponent)Component.text("物品展示").hoverEvent(Component.text(uuid.toString()))).color(show_color);
               Inventory inv = EssentialsD.instance.getServer().createInventory((InventoryHolder)null, 54, title);
               inv.setItem(22, item);
               cache.put(uuid.toString(), inv);
               player.getServer().sendMessage(Component.text("玩家 " + player.getName() + " 展示了物品 ").append(show.build()));
               return true;
            }
         }
      }
   }

   private static void openView(Player player, String name) {
      if (!cache.containsKey(name)) {
         Notification.warn(player, "物品不存在");
      } else {
         Inventory inv = (Inventory)cache.get(name);
         player.openInventory(inv);
      }
   }
}
