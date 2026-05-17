package et.xiaomu.essentialsd.tuis;

import et.xiaomu.essentialsd.dtos.HomeInfo;
import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.stui.ListView;
import cn.lunadeer.utils.stui.components.Line;
import cn.lunadeer.utils.stui.components.buttons.CommandButton;
import cn.lunadeer.utils.stui.components.buttons.ListViewButton;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class HomeList {
    public static ListViewButton button() {
        return new ListViewButton("1") {
            @Override
            public String getCommand(String pageStr) {
                return "/homes " + pageStr;
            }
        };
    }

    public static void show(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
        } else {
            Player player = (Player) sender;
            List<HomeInfo> homes = HomeInfo.getHomesOf(((Player) sender).getUniqueId());
            if (homes.isEmpty()) {
                Notification.warnKey(player, "messages.home.no_home_set");
            } else {
                int page = 1;
                if (args.length == 1) {
                    try {
                        page = Integer.parseInt(args[0]);
                    } catch (Exception ignored) {
                    }
                }

                ListView view = ListView.create(5, button());
                view.title(EssentialsD.localization.get("ui.home_list.title"));

                for (HomeInfo home : homes) {
                    Line line = Line.create().append(home.homeName)
                            .append(new CommandButton(EssentialsD.localization.get("ui.common.teleport_button"), "/home " + home.homeName).build())
                            .append(new CommandButton(EssentialsD.localization.get("ui.common.delete_button"), "/delhome " + home.homeName + " " + page).red().build());
                    view.add(line);
                }

                view.showOn(player, page);
            }
        }
    }
}
