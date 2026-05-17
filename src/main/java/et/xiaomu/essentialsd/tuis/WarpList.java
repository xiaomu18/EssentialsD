package et.xiaomu.essentialsd.tuis;

import et.xiaomu.essentialsd.dtos.WarpPoint;
import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.utils.LocUtil;
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
            Notification.warnKey(sender, "messages.common.player_only_command");
        } else {
            Player player = (Player) sender;
            List<WarpPoint> points = WarpPoint.selectAll();
            if (points.isEmpty()) {
                Notification.warnKey(player, "messages.warp_list.empty");
            } else {
                int page = 1;
                if (args.length == 1) {
                    try {
                        page = Integer.parseInt(args[0]);
                    } catch (Exception var9) {
                    }
                }

                ListView view = ListView.create(5, button());
                view.title(EssentialsD.localization.get("ui.warp_list.title"));

                for (WarpPoint point : points) {
                    Line line = Line.create().append((TextComponent) Component.text(point.getName()).hoverEvent(Component.text(LocUtil.toString(point.getLocation())))).append(new CommandButton(EssentialsD.localization.get("ui.common.teleport_button"), "/warp " + point.getName()).build());
                    view.add(line);
                }

                view.showOn(player, page);
            }
        }
    }
}
