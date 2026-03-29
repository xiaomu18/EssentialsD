package cn.lunadeer.essentialsd.commands;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.stui.components.buttons.Button;
import cn.lunadeer.utils.stui.components.buttons.CommandButton;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShowItem implements CommandExecutor {
    private static final TextColor SHOW_COLOR = TextColor.color(0, 148, 213);
    public static final Map<String, Inventory> cache = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.warn(sender, "只有玩家可以使用此命令");
            return true;
        }
        if (args.length == 1) {
            openView(player, args[0]);
            return true;
        }

        PlayerInventory backpack = player.getInventory();
        ItemStack item = backpack.getItemInMainHand().clone();
        if (item.getType().isAir()) {
            Notification.warn(player, "你的主手为空");
            return true;
        }

        String name = item.getType().name();
        if (item.getItemMeta().hasDisplayName()) {
            name = ((TextComponent) item.getItemMeta().displayName()).content();
        }

        UUID uuid = UUID.randomUUID();
        Button show = new CommandButton(name, "/showitem " + uuid).setHoverText("点击查看物品信息");
        TextComponent title = Component.text("物品展示").hoverEvent(Component.text(uuid.toString())).color(SHOW_COLOR);
        Inventory inv = EssentialsD.instance.getServer().createInventory((InventoryHolder) null, 54, title);
        inv.setItem(22, item);
        cache.put(uuid.toString(), inv);
        player.getServer().sendMessage(Component.text("玩家 " + player.getName() + " 展示了物品 ").append(show.build()));
        return true;
    }

    private static void openView(Player player, String name) {
        if (!cache.containsKey(name)) {
            Notification.warn(player, "物品不存在");
            return;
        }
        player.openInventory(cache.get(name));
    }
}
