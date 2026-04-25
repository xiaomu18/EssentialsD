package et.xiaomu.essentialsd.dtos;

import et.xiaomu.essentialsd.EssentialsD;

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PureState {
    private PureState() {
    }

    public static Set<UUID> getAllEnabled() {
        Set<UUID> ids = new HashSet<>();
        String sql = "SELECT uuid FROM pure_state WHERE enabled = ?;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, true);
            try {
                if (rs == null) {
                    return ids;
                }
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    if (uuid == null || uuid.isBlank()) {
                        continue;
                    }
                    try {
                        ids.add(UUID.fromString(uuid));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                return ids;
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("加载纯净模式状态失败", e, sql);
            return ids;
        }
    }

    public static boolean setEnabled(UUID uuid, boolean enabled) {
        String sql = "INSERT INTO pure_state (uuid, enabled, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) "
                + "ON CONFLICT (uuid) DO UPDATE SET enabled = EXCLUDED.enabled, updated_at = CURRENT_TIMESTAMP;";

        try {
            return EssentialsD.database.execute(sql, uuid.toString(), enabled);
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("保存纯净模式状态失败", e, sql);
            return false;
        }
    }
}
