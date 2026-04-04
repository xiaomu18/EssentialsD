package et.xiaomu.essentialsd.managers.inspect;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InspectManager {

    public enum Mode {
        PLAYER_INVENTORY,
        ENDER_CHEST
    }

    static final int PLAYER_GUI_SIZE = 54;
    static final int PLAYER_SLOT_COUNT = 41;
    static final int ENDER_SLOT_COUNT = 27;
    static final int[] PLAYER_GUI_TO_SLOT = new int[PLAYER_GUI_SIZE];
    static final ItemStack FILLER = createFiller();
    static final int[] PLAYER_EDITABLE_SLOTS;

    static {
        Arrays.fill(PLAYER_GUI_TO_SLOT, -1);
        PLAYER_GUI_TO_SLOT[1] = 39;
        PLAYER_GUI_TO_SLOT[2] = 38;
        PLAYER_GUI_TO_SLOT[3] = 37;
        PLAYER_GUI_TO_SLOT[4] = 36;
        PLAYER_GUI_TO_SLOT[6] = 40;
        for (int i = 0; i < 27; i++) {
            PLAYER_GUI_TO_SLOT[18 + i] = i + 9;
        }
        for (int i = 0; i < 9; i++) {
            PLAYER_GUI_TO_SLOT[45 + i] = i;
        }

        List<Integer> editable = new ArrayList<>();
        for (int i = 0; i < PLAYER_GUI_TO_SLOT.length; i++) {
            if (PLAYER_GUI_TO_SLOT[i] >= 0) {
                editable.add(i);
            }
        }
        PLAYER_EDITABLE_SLOTS = editable.stream().mapToInt(Integer::intValue).toArray();
    }

    private final Map<UUID, InspectSession> sessions = new ConcurrentHashMap<>();

    public InspectManager() {
        Scheduler.runTaskRepeat(this::tickSessions, 20L, 10L);
    }

    public void shutdown() {
        for (InspectSession session : new ArrayList<>(sessions.values())) {
            sessions.remove(session.viewerId());
            Player viewer = Bukkit.getPlayer(session.viewerId());
            if (viewer != null && viewer.isOnline()) {
                if (!session.readOnly()) {
                    session.restoreCursorItem(viewer);
                    session.applyTopToSource();
                }
                if (viewer.getOpenInventory().getTopInventory() == session.inventory()) {
                    viewer.closeInventory();
                }
            }
            session.close();
        }
        sessions.clear();
    }

    public void openInspect(Player viewer, OfflinePlayer target, Mode mode, boolean writable) {
        closeSession(viewer.getUniqueId());
        if (target.isOnline()) {
            Player onlineTarget = target.getPlayer();
            if (onlineTarget == null) {
                Notification.error(viewer, "无法读取玩家 %s 的数据", displayName(target));
                return;
            }
            OnlineInspectDataSource source = new OnlineInspectDataSource(onlineTarget, mode);
            source.refreshSnapshot(() -> Scheduler.runEntityTask(viewer, () -> {
                if (!viewer.isOnline()) {
                    return;
                }
                if (!source.isOnline()) {
                    Notification.warn(viewer, "玩家 %s 已下线，无法打开检查界面", source.displayName());
                    return;
                }
                openSession(viewer, source, writable);
            }));
            return;
        }

        Scheduler.runTaskAsync(() -> {
            OfflineInspectDataSource source = OfflineInspectDataSource.load(target, mode);
            Scheduler.runEntityTask(viewer, () -> {
                if (!viewer.isOnline()) {
                    return;
                }
                if (source == null) {
                    Notification.error(viewer, "无法读取玩家 %s 的离线数据", displayName(target));
                    return;
                }
                openSession(viewer, source, false);
            });
        });
    }

    public void handleClick(InventoryClickEvent event) {
        InspectSession session = getSession(event.getWhoClicked().getUniqueId(), event.getView().getTopInventory());
        if (session == null) {
            return;
        }
        if (session.readOnly()) {
            event.setCancelled(true);
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            if (isSupportedBottomAction(event.getAction().name())) {
                trackCursorOrigin(session, event);
                return;
            }
            event.setCancelled(true);
            return;
        }
        if (!isSupportedTopAction(event.getAction().name())) {
            event.setCancelled(true);
            return;
        }
        if (!session.editable(event.getRawSlot())) {
            event.setCancelled(true);
            return;
        }
        trackCursorOrigin(session, event);

        session.markLocalEdit();
        if (event.getWhoClicked() instanceof Player viewer) {
            Scheduler.runTaskLater(() -> {
                if (viewer.isOnline()) {
                    Scheduler.runEntityTask(viewer, session::applyTopToSource);
                }
            }, 1L);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        InspectSession session = getSession(event.getWhoClicked().getUniqueId(), event.getView().getTopInventory());
        if (session == null) {
            return;
        }
        if (session.readOnly()) {
            event.setCancelled(true);
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= event.getView().getTopInventory().getSize() || !session.editable(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        session.markLocalEdit();
        if (event.getWhoClicked() instanceof Player viewer) {
            Scheduler.runTaskLater(() -> {
                if (viewer.isOnline()) {
                    Scheduler.runEntityTask(viewer, session::applyTopToSource);
                }
            }, 1L);
        }
    }

    public void handleClose(Player viewer, Inventory topInventory) {
        InspectSession session = getSession(viewer.getUniqueId(), topInventory);
        if (session == null) {
            return;
        }
        if (!session.readOnly()) {
            session.restoreCursorItem(viewer);
            session.applyTopToSource();
        }
        session.close();
        sessions.remove(viewer.getUniqueId());
    }

    static ItemStack normalize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return item.clone();
    }

    static ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            clone[i] = normalize(source[i]);
        }
        return clone;
    }

    static ItemStack createInfoItem(InspectDataSource source, boolean readOnly) {
        ItemStack item = new ItemStack(source.mode() == Mode.ENDER_CHEST ? Material.ENDER_CHEST : Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("检查目标信息", NamedTextColor.GOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("目标: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(source.displayName(), NamedTextColor.YELLOW)));
        lore.add(Component.text("容器: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(source.mode() == Mode.ENDER_CHEST ? "末影箱" : "背包", NamedTextColor.AQUA)));
        lore.add(Component.text("状态: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(source.isOnline() ? "在线" : "离线",
                        source.isOnline() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.text("权限: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(readOnly ? "只读模式" : "读写模式",
                        readOnly ? NamedTextColor.RED : NamedTextColor.GREEN)));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    static Component createTitle(InspectDataSource source, boolean readOnly) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text(source.displayName(), NamedTextColor.YELLOW));
        builder.append(Component.text("[", NamedTextColor.WHITE));
        builder.append(Component.text(source.isOnline() ? "在线" : "离线",
                source.isOnline() ? NamedTextColor.GREEN : NamedTextColor.RED));
        builder.append(Component.text("] 的" + (source.mode() == Mode.ENDER_CHEST ? "末影箱 " : "背包 "), NamedTextColor.WHITE));
        builder.append(Component.text(readOnly ? "[只读]" : "[读写模式]", NamedTextColor.DARK_GRAY));
        return builder.build();
    }

    static String displayName(OfflinePlayer target) {
        return target.getName() != null ? target.getName() : target.getUniqueId().toString();
    }

    private static ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private void openSession(Player viewer, InspectDataSource source, boolean writable) {
        InspectSession session = new InspectSession(viewer.getUniqueId(), source, !writable);
        sessions.put(viewer.getUniqueId(), session);
        session.renderFromSource();
        viewer.openInventory(session.inventory());
        Notification.info(viewer, "已打开 %s 的%s", source.displayName(), source.mode() == Mode.ENDER_CHEST ? "末影箱" : "背包");
    }

    private void closeSession(UUID viewerId) {
        InspectSession session = sessions.remove(viewerId);
        if (session != null) {
            session.close();
        }
    }

    private InspectSession getSession(UUID viewerId, Inventory topInventory) {
        InspectSession session = sessions.get(viewerId);
        if (session == null) {
            return null;
        }
        return session.inventory() == topInventory ? session : null;
    }

    private void tickSessions() {
        for (InspectSession session : new ArrayList<>(sessions.values())) {
            Player viewer = Bukkit.getPlayer(session.viewerId());
            if (viewer == null || !viewer.isOnline()) {
                sessions.remove(session.viewerId());
                session.close();
                continue;
            }
            if (viewer.getOpenInventory().getTopInventory() != session.inventory()) {
                sessions.remove(session.viewerId());
                session.close();
                continue;
            }
            if (session.source().wasOnlineSource() && !session.source().isOnline()) {
                sessions.remove(session.viewerId());
                Scheduler.runEntityTask(viewer, () -> {
                    if (viewer.getOpenInventory().getTopInventory() == session.inventory()) {
                        viewer.closeInventory();
                        Notification.warn(viewer, "%s 已下线，检查界面已关闭", session.source().displayName());
                    }
                });
                session.close();
                continue;
            }
            if (session.source().isOnline()) {
                session.source().refreshSnapshot(null);
                if (!session.recentlyEdited() && isCursorClear(viewer)) {
                    Scheduler.runEntityTask(viewer, session::renderFromSource);
                }
            }
        }
    }

    private static boolean isSupportedTopAction(String actionName) {
        return switch (actionName) {
            case "PICKUP_ALL", "PICKUP_HALF", "PICKUP_SOME", "PICKUP_ONE",
                 "PLACE_ALL", "PLACE_SOME", "PLACE_ONE",
                 "SWAP_WITH_CURSOR", "NOTHING" -> true;
            default -> false;
        };
    }

    private static boolean isSupportedBottomAction(String actionName) {
        return switch (actionName) {
            case "PICKUP_ALL", "PICKUP_HALF", "PICKUP_SOME", "PICKUP_ONE",
                 "PLACE_ALL", "PLACE_SOME", "PLACE_ONE",
                 "SWAP_WITH_CURSOR", "NOTHING" -> true;
            default -> false;
        };
    }

    private static boolean isCursorClear(Player viewer) {
        ItemStack cursor = viewer.getItemOnCursor();
        return cursor == null || cursor.getType().isAir();
    }

    private static void trackCursorOrigin(InspectSession session, InventoryClickEvent event) {
        boolean topInventory = event.getClickedInventory() == event.getView().getTopInventory();
        if (!topInventory) {
            if (isPlacementAction(event.getAction().name())) {
                session.clearCursorOriginSlot();
                return;
            }
            if (isPickupLikeAction(event.getAction().name()) && normalize(event.getCurrentItem()) != null) {
                session.setCursorOrigin(false, event.getSlot());
            }
            return;
        }
        if (!session.editable(event.getRawSlot())) {
            return;
        }
        if (isPickupLikeAction(event.getAction().name()) && normalize(event.getCurrentItem()) != null) {
            session.setCursorOrigin(true, event.getRawSlot());
            return;
        }
        if (isPlacementAction(event.getAction().name())) {
            session.clearCursorOriginSlot();
        }
    }

    private static boolean isPickupLikeAction(String actionName) {
        return switch (actionName) {
            case "PICKUP_ALL", "PICKUP_HALF", "PICKUP_SOME", "PICKUP_ONE", "SWAP_WITH_CURSOR" -> true;
            default -> false;
        };
    }

    private static boolean isPlacementAction(String actionName) {
        return switch (actionName) {
            case "PLACE_ALL", "PLACE_SOME", "PLACE_ONE", "SWAP_WITH_CURSOR" -> true;
            default -> false;
        };
    }
}
