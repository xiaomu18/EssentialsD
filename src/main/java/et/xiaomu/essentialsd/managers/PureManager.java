package et.xiaomu.essentialsd.managers;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.dtos.PureListEntry;
import et.xiaomu.essentialsd.dtos.PureState;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PureManager {
    public static final String PERMISSION_BASE = "essd.pure";
    public static final String PERMISSION_LIST = "essd.purelist";

    private final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> pureLists = new ConcurrentHashMap<>();
    private final Set<UUID> pendingFirstChatNotice = ConcurrentHashMap.newKeySet();

    public PureManager() {
        bootstrap();
    }

    public boolean isFeatureEnabled() {
        return EssentialsD.config.getChatPureEnabled();
    }

    public boolean isEnabled(UUID playerId) {
        return enabledPlayers.contains(playerId);
    }

    public boolean isEnabled(Player player) {
        return isEnabled(player.getUniqueId());
    }

    public boolean setEnabled(UUID playerId, boolean enabled) {
        if (!PureState.setEnabled(playerId, enabled)) {
            return false;
        }
        if (enabled) {
            enabledPlayers.add(playerId);
            return true;
        }
        enabledPlayers.remove(playerId);
        pendingFirstChatNotice.remove(playerId);
        return true;
    }

    public boolean addToList(UUID ownerId, UUID targetId) {
        if (!PureListEntry.add(ownerId, targetId)) {
            return false;
        }
        pureLists.computeIfAbsent(ownerId, ignored -> ConcurrentHashMap.newKeySet()).add(targetId);
        return true;
    }

    public boolean removeFromList(UUID ownerId, UUID targetId) {
        if (!PureListEntry.remove(ownerId, targetId)) {
            return false;
        }
        Set<UUID> list = pureLists.get(ownerId);
        if (list == null) {
            return true;
        }
        list.remove(targetId);
        if (list.isEmpty()) {
            pureLists.remove(ownerId, list);
        }
        return true;
    }

    public boolean clearList(UUID ownerId) {
        if (!PureListEntry.clear(ownerId)) {
            return false;
        }
        pureLists.remove(ownerId);
        return true;
    }

    public boolean isListed(UUID ownerId, UUID targetId) {
        Set<UUID> list = pureLists.get(ownerId);
        return list != null && list.contains(targetId);
    }

    public boolean canMutuallySee(UUID left, UUID right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.equals(right)) {
            return true;
        }
        if (!isFeatureEnabled()) {
            return true;
        }
        return allows(left, right) && allows(right, left);
    }

    public List<UUID> getListedPlayers(UUID ownerId) {
        Set<UUID> list = pureLists.get(ownerId);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    public void handleJoin(Player player) {
        UUID playerId = player.getUniqueId();
        if (isFeatureEnabled() && isEnabled(playerId)) {
            pendingFirstChatNotice.add(playerId);
            return;
        }
        pendingFirstChatNotice.remove(playerId);
    }

    public void handleQuit(Player player) {
        pendingFirstChatNotice.remove(player.getUniqueId());
    }

    public boolean consumeFirstChatNotice(UUID playerId) {
        if (!isFeatureEnabled() || !isEnabled(playerId)) {
            pendingFirstChatNotice.remove(playerId);
            return false;
        }
        return pendingFirstChatNotice.remove(playerId);
    }

    public void shutdown() {
        enabledPlayers.clear();
        pureLists.clear();
        pendingFirstChatNotice.clear();
    }

    private void bootstrap() {
        enabledPlayers.addAll(PureState.getAllEnabled());
        Map<UUID, Set<UUID>> persisted = PureListEntry.getAllEntries();
        for (Map.Entry<UUID, Set<UUID>> entry : persisted.entrySet()) {
            pureLists.computeIfAbsent(entry.getKey(), ignored -> ConcurrentHashMap.newKeySet())
                    .addAll(entry.getValue());
        }
    }

    private boolean allows(UUID ownerId, UUID targetId) {
        if (!enabledPlayers.contains(ownerId)) {
            return true;
        }
        Set<UUID> list = pureLists.get(ownerId);
        return list != null && list.contains(targetId);
    }
}
