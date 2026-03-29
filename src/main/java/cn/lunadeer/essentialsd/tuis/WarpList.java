package cn.lunadeer.essentialsd.tuis;

import cn.lunadeer.essentialsd.dtos.WarpPoint;
import cn.lunadeer.essentialsd.utils.LocUtil;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.stui.ListView;
import cn.lunadeer.utils.stui.components.Line;
import cn.lunadeer.utils.stui.components.buttons.CommandButton;
import cn.lunadeer.utils.stui.components.buttons.ListViewButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class WarpList {
    public static ListViewButton button() {
        return new ListViewButton("1") {
            @Override
            public String getCommand(String pageStr) {
                return "/warps " + pageStr;
            }
        };
    }

    public static void show(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warn(sender, "只有玩家可以使用此命令");
        } else {
            Player player = (Player) sender;
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

                ListView view = ListView.create(5, button());
                view.title("Warp 列表");

                for (WarpPoint point : points) {
                    Line line = Line.create().append((TextComponent) Component.text(point.getName()).hoverEvent(Component.text(LocUtil.toString(point.getLocation())))).append(new CommandButton("传送", "/warp " + point.getName()).build());
                    view.add(line);
                }

                view.showOn(player, page);
            }
        }
    }
}
