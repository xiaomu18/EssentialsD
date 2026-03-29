package cn.lunadeer.essentialsd.managers.inspect;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
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
        for (int i = 0; i < 27; i++) {
            PLAYER_GUI_TO_SLOT[i] = i + 9;
        }
        for (int i = 0; i < 9; i++) {
            PLAYER_GUI_TO_SLOT[27 + i] = i;
        }
        PLAYER_GUI_TO_SLOT[45] = 39;
        PLAYER_GUI_TO_SLOT[46] = 38;
        PLAYER_GUI_TO_SLOT[47] = 37;
        PLAYER_GUI_TO_SLOT[48] = 36;
        PLAYER_GUI_TO_SLOT[49] = 40;

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

    public void openInspect(Player viewer, OfflinePlayer target, Mode mode) {
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
                openSession(viewer, source);
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
                openSession(viewer, source);
            });
        });
    }

    public void handleClick(InventoryClickEvent event) {
        InspectSession session = getSession(event.getWhoClicked().getUniqueId(), event.getView().getTopInventory());
        if (session == null) {
            return;
        }
        if (session.source().readOnly()) {
            event.setCancelled(true);
            return;
        }
        if ("COLLECT_TO_CURSOR".equals(event.getAction().name())) {
            event.setCancelled(true);
            return;
        }

        if (!session.editable(event.getRawSlot())) {
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                event.setCancelled(true);
                return;
            }
            if (event.getAction().name().contains("MOVE_TO_OTHER_INVENTORY")) {
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

    public void handleDrag(InventoryDragEvent event) {
        InspectSession session = getSession(event.getWhoClicked().getUniqueId(), event.getView().getTopInventory());
        if (session == null) {
            return;
        }
        if (session.source().readOnly()) {
            event.setCancelled(true);
            return;
        }

        for (int rawSlot : event.getRawSlots()) {
            if (!session.editable(rawSlot)) {
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
        if (!session.source().readOnly()) {
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

    static ItemStack createInfoItem(InspectDataSource source) {
        ItemStack item = new ItemStack(source.mode() == Mode.ENDER_CHEST ? Material.ENDER_CHEST : Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("检查目标信息");
        List<String> lore = new ArrayList<>();
        lore.add("目标: " + source.displayName());
        lore.add("模式: " + (source.mode() == Mode.ENDER_CHEST ? "末影箱" : "背包"));
        lore.add("状态: " + (source.isOnline() ? "在线" : "离线"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
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

    private void openSession(Player viewer, InspectDataSource source) {
        InspectSession session = new InspectSession(viewer.getUniqueId(), source);
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
                if (!session.recentlyEdited()) {
                    Scheduler.runEntityTask(viewer, session::renderFromSource);
                }
            }
        }
    }
}
