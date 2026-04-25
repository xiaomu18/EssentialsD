package et.xiaomu.essentialsd.dtos;

import et.xiaomu.essentialsd.EssentialsD;

import java.sql.ResultSet;
import java.util.UUID;

public class PlayerName {
    public static String getName(UUID uuid) {
        String sql = "SELECT last_known_name FROM player_name WHERE uuid = ?;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, uuid.toString());

            String var3;
            label63:
            {
                label69:
                {
                    try {
                        if (rs == null) {
                            var3 = null;
                            break label69;
                        }

                        if (rs.next()) {
                            var3 = rs.getString("last_known_name");
                            break label63;
                        }
                    } catch (Throwable var6) {
                        if (rs != null) {
                            try {
                                rs.close();
                            } catch (Throwable var5) {
                                var6.addSuppressed(var5);
                            }
                        }

                        throw var6;
                    }

                    if (rs != null) {
                        rs.close();
                    }

                    return null;
                }

                if (rs != null) {
                    rs.close();
                }

                return var3;
            }

            if (rs != null) {
                rs.close();
            }

            return var3;
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("获取玩家名字失败", e, sql);
            return null;
        }
    }

    public static UUID getUuid(String name) {
        String sql = "SELECT uuid FROM player_name WHERE LOWER(last_known_name) = LOWER(?) LIMIT 1;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, name);

            UUID result;
            label63:
            {
                label69:
                {
                    try {
                        if (rs == null) {
                            result = null;
                            break label69;
                        }

                        if (rs.next()) {
                            result = UUID.fromString(rs.getString("uuid"));
                            break label63;
                        }
                    } catch (Throwable var6) {
                        if (rs != null) {
                            try {
                                rs.close();
                            } catch (Throwable var5) {
                                var6.addSuppressed(var5);
                            }
                        }

                        throw var6;
                    }

                    if (rs != null) {
                        rs.close();
                    }

                    return null;
                }

                if (rs != null) {
                    rs.close();
                }

                return result;
            }

            if (rs != null) {
                rs.close();
            }

            return result;
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("通过玩家名获取 UUID 失败", e, sql);
            return null;
        }
    }

    public static boolean setName(UUID uuid, String name) {
        String sql = "INSERT INTO player_name (uuid, last_known_name) VALUES (?, ?) ON CONFLICT (uuid) DO NOTHING;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, uuid.toString(), name);

            boolean var4;
            try {
                var4 = true;
            } catch (Throwable var7) {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (rs != null) {
                rs.close();
            }

            return var4;
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("设置玩家名字失败", e, sql);
            return false;
        }
    }

    public static boolean updateName(UUID uuid, String name) {
        String sql = "UPDATE player_name SET last_known_name = ? WHERE uuid = ?;";

        try {
            ResultSet rs = EssentialsD.database.query(sql, name, uuid.toString());

            boolean var4;
            try {
                var4 = true;
            } catch (Throwable var7) {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }

                throw var7;
            }

            if (rs != null) {
                rs.close();
            }

            return var4;
        } catch (Exception e) {
            EssentialsD.database.handleDatabaseError("更新玩家名字失败", e, sql);
            return false;
        }
    }
}
