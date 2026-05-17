package et.xiaomu.essentialsd.commands.warp;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.WarpPoint;
import cn.lunadeer.utils.Notification;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SetWarp implements TabExecutor {
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length != 1 && strings.length != 5) {
            Notification.errorKey(commandSender, "messages.setwarp.usage");
            return true;
        } else {
            String name = strings[0];
            Location location;
            if (strings.length == 1) {
                if (!(commandSender instanceof Player)) {
                    Notification.errorKey(commandSender, "messages.setwarp.console_requires_coordinates");
                    return true;
                }

                location = ((Player) commandSender).getLocation();
            } else {
                try {
                    World world = EssentialsD.instance.getServer().getWorld(strings[1]);
                    if (world == null) {
                        Notification.errorKey(commandSender, "messages.setwarp.world_not_found", strings[1]);
                        return true;
                    }

                    double x = Double.parseDouble(strings[2]);
                    double y = Double.parseDouble(strings[3]);
                    double z = Double.parseDouble(strings[4]);
                    location = new Location(world, x, y, z);
                } catch (NumberFormatException var14) {
                    Notification.errorKey(commandSender, "messages.setwarp.coordinates_must_be_numbers");
                    return true;
                }
            }

            WarpPoint existing = WarpPoint.selectByName(name);
            if (existing != null) {
                Notification.errorKey(commandSender, "messages.setwarp.already_exists", name);
                return true;
            } else {
                WarpPoint point = new WarpPoint(name, location);
                WarpPoint.insert(point);
                Notification.infoKey(commandSender, "messages.setwarp.created", name);
                return true;
            }
        }
    }

    public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> res = new ArrayList();
        if (args.length == 1) {
            res.add("warp-name");
        }

        if (args.length == 2) {
            res.add("world-name");
        }

        if (args.length == 3) {
            res.add("x");
        }

        if (args.length == 4) {
            res.add("y");
        }

        if (args.length == 5) {
            res.add("z");
        }

        return res;
    }
}
