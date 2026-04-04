package et.xiaomu.essentialsd.dtos;

import et.xiaomu.essentialsd.EssentialsD;
import cn.lunadeer.utils.LocationUtils;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.UUID;

public class LoginRecord {
    public static void newLoginRecord(Player player) {
        UUID uuid = player.getUniqueId();
        InetSocketAddress address = player.getAddress();
        String ip;
        if (address == null) {
            ip = "unknown";
        } else {
            ip = address.getAddress().getHostAddress();
        }

        Timestamp login_time = new Timestamp(player.getLastLogin());
        Timestamp logout_time = new Timestamp(System.currentTimeMillis());
        String logout_location = LocationUtils.Serialize(player.getLocation());
        String sql = "INSERT INTO login_record (uuid, ip, login_time, logout_location, logout_time) VALUES (?, ?, ?, ?, ?);";

        try {
            ResultSet rs = EssentialsD.database.query(sql, uuid.toString(), ip, login_time, logout_location, logout_time);

            label56:
            {
                try {
                    if (rs == null) {
                        break label56;
                    }
                } catch (Throwable var12) {
                    if (rs != null) {
                        try {
                            rs.close();
                        } catch (Throwable var11) {
                            var12.addSuppressed(var11);
                        }
                    }

                    throw var12;
                }

                if (rs != null) {
                    rs.close();
                }

                return;
            }

            if (rs != null) {
                rs.close();
            }

        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("创建登录记录失败", e, sql);
        }
    }

    public static String getLatestIp(UUID uuid) {
        String sql = "SELECT ip FROM login_record WHERE uuid = ? ORDER BY logout_time DESC LIMIT 1;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, uuid.toString());
            try {
                if (rs == null || !rs.next()) {
                    return null;
                }
                return rs.getString("ip");
            } finally {
                if (rs != null) {
                    rs.close();
                }
            }
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("获取玩家最近登录 IP 失败", e, sql);
            return null;
        }
    }
}
