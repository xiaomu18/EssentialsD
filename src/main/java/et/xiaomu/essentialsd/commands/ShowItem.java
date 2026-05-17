package et.xiaomu.essentialsd.commands;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.Notification;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShowItem implements CommandExecutor {
    private static final TextColor SHOW_COLOR = TextColor.color(0, 148, 213);
    public static final Map<String, ShowItemEntry> cache = new ConcurrentHashMap<>();
    private static final Map<UUID, LocalDateTime> nextTimeAllowShow = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Notification.warnKey(sender, "messages.common.player_only_command");
            return true;
        }
        if (args.length == 1) {
            openView(player, args[0]);
            return true;
        }

        PlayerInventory backpack = player.getInventory();
        ItemStack item = backpack.getItemInMainHand().clone();
        if (item.getType().isAir()) {
            Notification.warnKey(player, "messages.show_item.empty_hand");
            return true;
        }

        if (!coolingDown(player)) {
            return true;
        }

        UUID uuid = UUID.randomUUID();
        TextComponent title = Component.text(EssentialsD.localization.get("ui.show_item.title")).hoverEvent(Component.text(uuid.toString())).color(SHOW_COLOR);
        Inventory inv = EssentialsD.instance.getServer().createInventory((InventoryHolder) null, 54, title);
        inv.setItem(22, sanitizeItemForView(item));
        cache.put(uuid.toString(), new ShowItemEntry(player.getUniqueId(), inv));
        applyCooldown(player);
        broadcastFiltered(player, Component.text(EssentialsD.localization.format("messages.show_item.broadcast", player.getName())).append(buildShowButton(item, uuid)));
        return true;
    }

    private static void openView(Player player, String name) {
        ShowItemEntry entry = cache.get(name);
        if (entry == null) {
            Notification.warnKey(player, "messages.show_item.not_found");
            return;
        }
        if (!EssentialsD.pureManager.canMutuallySee(entry.ownerId(), player.getUniqueId())) {
            Notification.warnKey(player, "messages.show_item.not_visible");
            return;
        }
        player.openInventory(entry.inventory());
    }

    private static void broadcastFiltered(Player sender, Component message) {
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (!EssentialsD.pureManager.canMutuallySee(sender.getUniqueId(), recipient.getUniqueId())) {
                continue;
            }
            recipient.sendMessage(message);
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private static boolean coolingDown(Player player) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextTime = nextTimeAllowShow.get(player.getUniqueId());
        if (nextTime != null && now.isBefore(nextTime)) {
            long secsUntilNext = Math.max(1L, now.until(nextTime, ChronoUnit.SECONDS));
            Notification.warnKey(player, "messages.show_item.cooldown", secsUntilNext);
            return false;
        }
        return true;
    }

    private static void applyCooldown(Player player) {
        int cooldown = EssentialsD.config.getShowItemCoolDown();
        if (cooldown <= 0) {
            nextTimeAllowShow.remove(player.getUniqueId());
            return;
        }
        nextTimeAllowShow.put(player.getUniqueId(), LocalDateTime.now().plusSeconds(cooldown));
    }

    private static Component buildShowButton(ItemStack item, UUID uuid) {
        return Component.text()
                .append(Component.text("[", SHOW_COLOR))
                .append(Component.translatable(item.getType().translationKey()).color(SHOW_COLOR))
                .append(Component.text("]", SHOW_COLOR))
                .clickEvent(ClickEvent.runCommand("/showitem " + uuid))
                .hoverEvent(Component.text(EssentialsD.localization.get("ui.show_item.hover")))
                .build();
    }

    private static ItemStack sanitizeItemForView(ItemStack item) {
        ItemStack displayItem = item.clone();
        if (!displayItem.hasItemMeta()) {
            return displayItem;
        }
        var meta = displayItem.getItemMeta();
        meta.displayName(null);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    public record ShowItemEntry(UUID ownerId, Inventory inventory) {
    }
}
