package cn.lunadeer.essentialsd.tuis;

import cn.lunadeer.essentialsd.dtos.HomeInfo;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.stui.ListView;
import cn.lunadeer.minecraftpluginutils.stui.components.Button;
import cn.lunadeer.minecraftpluginutils.stui.components.Line;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeList {
   public static void show(CommandSender sender, String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
      } else {
         Player player = (Player)sender;
         List<HomeInfo> homes = HomeInfo.getHomesOf(((Player)sender).getUniqueId());
         if (homes.isEmpty()) {
            Notification.warn(player, "你还没有设置家");
         } else {
            int page = 1;
            if (args.length == 1) {
               try {
                  page = Integer.parseInt(args[0]);
               } catch (Exception var9) {
               }
            }

            ListView view = ListView.create(5, "/homes");
            view.title("Home 列表");

            for(HomeInfo home : homes) {
               Line line = Line.create().append(home.homeName).append(Button.create("传送").setExecuteCommand("/home " + home.homeName).build()).append(Button.createRed("删除").setExecuteCommand("/delhome " + home.homeName + " " + page).build());
               view.add(line);
            }

            view.showOn(player, page);
         }
      }
   }
}
