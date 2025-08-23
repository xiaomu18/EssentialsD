package cn.lunadeer.essentialsd.tuis;

import cn.lunadeer.essentialsd.dtos.WarpPoint;
import cn.lunadeer.essentialsd.utils.LocUtil;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.stui.ListView;
import cn.lunadeer.minecraftpluginutils.stui.components.Button;
import cn.lunadeer.minecraftpluginutils.stui.components.Line;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpList {
   public static void show(CommandSender sender, String[] args) {
      if (!(sender instanceof Player)) {
         Notification.warn(sender, "只有玩家可以使用此命令");
      } else {
         Player player = (Player)sender;
         List<WarpPoint> points = WarpPoint.selectAll();
         if (points.isEmpty()) {
            Notification.warn(player, "没有传送点");
         } else {
            int page = 1;
            if (args.length == 1) {
               try {
                  page = Integer.parseInt(args[0]);
               } catch (Exception var9) {
               }
            }

            ListView view = ListView.create(5, "/warps");
            view.title("Warp 列表");

            for(WarpPoint point : points) {
               Line line = Line.create().append((TextComponent)Component.text(point.getName()).hoverEvent(Component.text(LocUtil.toString(point.getLocation())))).append(Button.create("传送").setExecuteCommand("/warp " + point.getName()).build());
               view.add(line);
            }

            view.showOn(player, page);
         }
      }
   }
}
