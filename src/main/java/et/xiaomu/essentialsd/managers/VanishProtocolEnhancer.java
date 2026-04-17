package et.xiaomu.essentialsd.managers;

import cn.lunadeer.utils.XLogger;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import et.xiaomu.essentialsd.EssentialsD;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class VanishProtocolEnhancer {
    private static final PacketType[] OUTGOING_PACKET_TYPES = new PacketType[]{
            PacketType.Play.Server.PLAYER_INFO,
            PacketType.Play.Server.PLAYER_INFO_REMOVE,
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.ENTITY_DESTROY,
            PacketType.Play.Server.ENTITY_METADATA,
            PacketType.Play.Server.ENTITY_EQUIPMENT,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
            PacketType.Play.Server.ENTITY_LOOK,
            PacketType.Play.Server.ENTITY_HEAD_ROTATION,
            PacketType.Play.Server.ANIMATION,
            PacketType.Play.Server.ATTACH_ENTITY,
            PacketType.Play.Server.MOUNT,
            PacketType.Play.Server.ENTITY_SOUND,
            PacketType.Play.Server.COLLECT
    };
    private static final PacketType[] INCOMING_PACKET_TYPES = new PacketType[]{
            PacketType.Play.Client.USE_ENTITY
    };

    private final EssentialsD plugin;
    private final VanishManager vanishManager;
    private final ProtocolManager protocolManager;
    private PacketAdapter outgoingListener;
    private PacketAdapter incomingListener;
    private boolean enabled;

    public VanishProtocolEnhancer(EssentialsD plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public synchronized void enable() {
        if (enabled) {
            return;
        }
        outgoingListener = createOutgoingListener();
        incomingListener = createIncomingListener();
        protocolManager.addPacketListener(outgoingListener);
        protocolManager.addPacketListener(incomingListener);
        enabled = true;
        XLogger.info("Vanish 增强模式已启用 (ProtocolLib)");
    }

    public synchronized void shutdown() {
        if (!enabled) {
            return;
        }
        if (outgoingListener != null) {
            protocolManager.removePacketListener(outgoingListener);
            outgoingListener = null;
        }
        if (incomingListener != null) {
            protocolManager.removePacketListener(incomingListener);
            incomingListener = null;
        }
        enabled = false;
        XLogger.info("Vanish 增强模式已关闭");
    }

    private PacketAdapter createOutgoingListener() {
        return new PacketAdapter(plugin, ListenerPriority.HIGHEST, OUTGOING_PACKET_TYPES) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }

                Player receiver = event.getPlayer();
                if (receiver == null || vanishManager.canSeeVanished(receiver)) {
                    return;
                }

                PacketType type = event.getPacketType();
                PacketContainer packet = event.getPacket();

                if (PacketType.Play.Server.PLAYER_INFO.equals(type)) {
                    filterPlayerInfoPacket(event, packet);
                    return;
                }
                if (PacketType.Play.Server.PLAYER_INFO_REMOVE.equals(type)) {
                    filterPlayerInfoRemovePacket(event, packet, receiver);
                    return;
                }
                if (PacketType.Play.Server.SPAWN_ENTITY.equals(type)) {
                    if (containsVanishedUuid(packet) || containsVanishedEntityId(readInteger(packet, 0))) {
                        event.setCancelled(true);
                    }
                    return;
                }
                if (PacketType.Play.Server.ENTITY_DESTROY.equals(type)) {
                    filterEntityDestroyPacket(event, packet, receiver);
                    return;
                }
                if (PacketType.Play.Server.MOUNT.equals(type)) {
                    if (containsVanishedEntityId(readMountEntityIds(packet))) {
                        event.setCancelled(true);
                    }
                    return;
                }
                if (PacketType.Play.Server.ATTACH_ENTITY.equals(type)
                        || PacketType.Play.Server.COLLECT.equals(type)
                        || PacketType.Play.Server.ENTITY_SOUND.equals(type)) {
                    if (containsVanishedEntityId(readAllIntegers(packet))) {
                        event.setCancelled(true);
                    }
                    return;
                }
                if (PacketType.Play.Server.ENTITY_METADATA.equals(type)
                        || PacketType.Play.Server.ENTITY_EQUIPMENT.equals(type)
                        || PacketType.Play.Server.ENTITY_TELEPORT.equals(type)
                        || PacketType.Play.Server.REL_ENTITY_MOVE.equals(type)
                        || PacketType.Play.Server.REL_ENTITY_MOVE_LOOK.equals(type)
                        || PacketType.Play.Server.ENTITY_LOOK.equals(type)
                        || PacketType.Play.Server.ENTITY_HEAD_ROTATION.equals(type)
                        || PacketType.Play.Server.ANIMATION.equals(type)) {
                    if (containsVanishedEntityId(readInteger(packet, 0))) {
                        event.setCancelled(true);
                    }
                }
            }
        };
    }

    private PacketAdapter createIncomingListener() {
        return new PacketAdapter(plugin, ListenerPriority.HIGHEST, INCOMING_PACKET_TYPES) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) {
                    return;
                }

                Player sender = event.getPlayer();
                if (sender == null || vanishManager.canSeeVanished(sender)) {
                    return;
                }

                Integer targetEntityId = readInteger(event.getPacket(), 0);
                if (containsVanishedEntityId(targetEntityId)) {
                    event.setCancelled(true);
                }
            }
        };
    }

    private void filterPlayerInfoPacket(PacketEvent event, PacketContainer packet) {
        StructureModifier<List<PlayerInfoData>> playerInfoDataLists = packet.getPlayerInfoDataLists();
        if (playerInfoDataLists.size() == 0) {
            return;
        }
        List<PlayerInfoData> original = playerInfoDataLists.read(0);
        if (original == null || original.isEmpty()) {
            return;
        }

        List<PlayerInfoData> filtered = new ArrayList<>(original.size());
        for (PlayerInfoData data : original) {
            UUID uuid = extractProfileUuid(data);
            if (uuid == null || !vanishManager.isPacketVanishedUuid(uuid)) {
                filtered.add(data);
            }
        }

        if (filtered.size() == original.size()) {
            return;
        }
        if (filtered.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        playerInfoDataLists.write(0, filtered);
    }

    private void filterPlayerInfoRemovePacket(PacketEvent event, PacketContainer packet, Player receiver) {
        StructureModifier<List<UUID>> uuidLists = packet.getUUIDLists();
        if (uuidLists.size() == 0) {
            return;
        }
        List<UUID> original = uuidLists.read(0);
        if (original == null || original.isEmpty()) {
            return;
        }

        UUID receiverId = receiver.getUniqueId();
        List<UUID> filtered = new ArrayList<>(original.size());
        for (UUID uuid : original) {
            if (!shouldFilterPlayerInfoRemoveEntry(receiverId, uuid)) {
                filtered.add(uuid);
            }
        }

        if (filtered.size() == original.size()) {
            return;
        }
        if (filtered.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        uuidLists.write(0, filtered);
    }

    private boolean shouldFilterPlayerInfoRemoveEntry(UUID receiverId, UUID targetUuid) {
        if (targetUuid == null || !vanishManager.isPacketVanishedUuid(targetUuid)) {
            return false;
        }
        return !vanishManager.consumePendingPlayerInfoRemove(receiverId, targetUuid);
    }

    private void filterEntityDestroyPacket(PacketEvent event, PacketContainer packet, Player receiver) {
        UUID receiverId = receiver.getUniqueId();

        StructureModifier<List<Integer>> intLists = packet.getIntLists();
        if (intLists.size() > 0) {
            List<Integer> ids = intLists.read(0);
            if (ids != null && !ids.isEmpty()) {
                List<Integer> filtered = filterDestroyEntityIds(receiverId, ids);
                applyFilteredEntityDestroyList(event, intLists, ids, filtered);
                return;
            }
        }

        StructureModifier<int[]> integerArrays = packet.getIntegerArrays();
        if (integerArrays.size() > 0) {
            int[] ids = integerArrays.read(0);
            if (ids != null && ids.length > 0) {
                List<Integer> original = new ArrayList<>(ids.length);
                for (int id : ids) {
                    original.add(id);
                }
                List<Integer> filtered = filterDestroyEntityIds(receiverId, original);
                if (filtered.size() == original.size()) {
                    return;
                }
                if (filtered.isEmpty()) {
                    event.setCancelled(true);
                    return;
                }
                int[] rewritten = new int[filtered.size()];
                for (int i = 0; i < filtered.size(); i++) {
                    rewritten[i] = filtered.get(i);
                }
                integerArrays.write(0, rewritten);
            }
        }
    }

    private List<Integer> filterDestroyEntityIds(UUID receiverId, Collection<Integer> entityIds) {
        List<Integer> filtered = new ArrayList<>();
        for (Integer entityId : entityIds) {
            if (!shouldFilterEntityDestroyEntry(receiverId, entityId)) {
                filtered.add(entityId);
            }
        }
        return filtered;
    }

    private boolean shouldFilterEntityDestroyEntry(UUID receiverId, Integer entityId) {
        if (entityId == null || !vanishManager.isPacketVanishedEntityId(entityId)) {
            return false;
        }
        return !vanishManager.consumePendingEntityDestroy(receiverId, entityId);
    }

    private void applyFilteredEntityDestroyList(PacketEvent event,
                                                StructureModifier<List<Integer>> intLists,
                                                List<Integer> original,
                                                List<Integer> filtered) {
        if (filtered.size() == original.size()) {
            return;
        }
        if (filtered.isEmpty()) {
            event.setCancelled(true);
            return;
        }
        intLists.write(0, filtered);
    }

    private UUID extractProfileUuid(PlayerInfoData data) {
        if (data == null) {
            return null;
        }
        WrappedGameProfile profile = data.getProfile();
        return profile == null ? null : profile.getUUID();
    }

    private boolean containsVanishedUuid(PacketContainer packet) {
        StructureModifier<UUID> uuids = packet.getUUIDs();
        for (int i = 0; i < uuids.size(); i++) {
            UUID uuid = uuids.read(i);
            if (uuid != null && vanishManager.isPacketVanishedUuid(uuid)) {
                return true;
            }
        }
        return false;
    }

    private Integer readInteger(PacketContainer packet, int index) {
        StructureModifier<Integer> integers = packet.getIntegers();
        if (integers.size() <= index) {
            return null;
        }
        return integers.read(index);
    }

    private Collection<Integer> readMountEntityIds(PacketContainer packet) {
        List<Integer> ids = new ArrayList<>(readAllIntegers(packet));
        StructureModifier<int[]> integerArrays = packet.getIntegerArrays();
        if (integerArrays.size() > 0) {
            int[] passengers = integerArrays.read(0);
            if (passengers != null) {
                for (int passengerId : passengers) {
                    ids.add(passengerId);
                }
            }
        }
        StructureModifier<List<Integer>> intLists = packet.getIntLists();
        if (intLists.size() > 0) {
            List<Integer> passengerList = intLists.read(0);
            if (passengerList != null) {
                ids.addAll(passengerList);
            }
        }
        return ids;
    }

    private Collection<Integer> readAllIntegers(PacketContainer packet) {
        StructureModifier<Integer> integers = packet.getIntegers();
        List<Integer> result = new ArrayList<>(integers.size());
        for (int i = 0; i < integers.size(); i++) {
            result.add(integers.read(i));
        }
        return result;
    }

    private boolean containsVanishedEntityId(Integer entityId) {
        return entityId != null && vanishManager.isPacketVanishedEntityId(entityId);
    }

    private boolean containsVanishedEntityId(Collection<Integer> entityIds) {
        for (Integer entityId : entityIds) {
            if (containsVanishedEntityId(entityId)) {
                return true;
            }
        }
        return false;
    }
}
