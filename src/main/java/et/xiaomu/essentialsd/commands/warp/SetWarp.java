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
            Notification.error(commandSender, "用法: /setwarp <name> 或 /setwarp <name> <world> <x> <y> <z>");
            return true;
        } else {
            String name = strings[0];
            Location location;
            if (strings.length == 1) {
                if (!(commandSender instanceof Player)) {
                    Notification.error(commandSender, "请指定坐标 /setwarp <name> <world> <x> <y> <z>");
                    return true;
                }

                location = ((Player) commandSender).getLocation();
            } else {
                try {
                    World world = EssentialsD.instance.getServer().getWorld(strings[1]);
                    if (world == null) {
                        Notification.error(commandSender, "世界 %s 不存在", strings[1]);
                        return true;
                    }

                    double x = Double.parseDouble(strings[2]);
                    double y = Double.parseDouble(strings[3]);
                    double z = Double.parseDouble(strings[4]);
                    location = new Location(world, x, y, z);
                } catch (NumberFormatException var14) {
                    Notification.error(commandSender, "坐标必须是数字");
                    return true;
                }
            }

            WarpPoint existing = WarpPoint.selectByName(name);
            if (existing != null) {
                Notification.error(commandSender, "传送点 %s 已存在", name);
                return true;
            } else {
                WarpPoint point = new WarpPoint(name, location);
                WarpPoint.insert(point);
                Notification.info(commandSender, "传送点 %s 已设置", name);
                return true;
            }
        }
    }

    public @Nullable List onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> res = new ArrayList();
        if (args.length == 1) {
            res.add("传送点名称");
        }

        if (args.length == 2) {
            res.add("世界名称");
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
