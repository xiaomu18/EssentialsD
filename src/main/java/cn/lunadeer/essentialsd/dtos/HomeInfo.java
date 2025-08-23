package cn.lunadeer.essentialsd.dtos;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.LocationUtils;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;

public class HomeInfo {
   public UUID uuid;
   public String homeName;
   public Location location;

   public static boolean newHome(HomeInfo info) {
      String sql = "INSERT INTO home_info (uuid, home_name, location) VALUES (?, ?, ?);";

      try {
         ResultSet rs = EssentialsD.database.query(sql, info.uuid, info.homeName, LocationUtils.Serialize(info.location));

         boolean var3;
         try {
            var3 = true;
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

         return var3;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("创建Home失败", e, sql);
         return false;
      }
   }

   public static boolean deleteHome(UUID uuid, String homeName) {
      String sql = "DELETE FROM home_info WHERE uuid = ? AND home_name = ?;";

      try {
         ResultSet rs = EssentialsD.database.query(sql, uuid.toString(), homeName);

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
         EssentialsD.database.handleDatabaseError("删除Home失败", e, sql);
         return false;
      }
   }

   public static List<HomeInfo> getHomesOf(UUID uuid) {
      List<HomeInfo> homes = new ArrayList<>();
      String sql = "SELECT * FROM home_info WHERE uuid = ?;";

      try {
         ResultSet rs = EssentialsD.database.query(sql, uuid.toString());

         Object var9;
         label60: {
            try {
               if (rs == null) {
                  var9 = homes;
                  break label60;
               }

               while(rs.next()) {
                  HomeInfo home = new HomeInfo();
                  home.uuid = UUID.fromString(rs.getString("uuid"));
                  home.homeName = rs.getString("home_name");
                  home.location = LocationUtils.Deserialize(rs.getString("location"));
                  homes.add(home);
               }
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

            return homes;
         }

         if (rs != null) {
            rs.close();
         }

         return (List)var9;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("获取Home列表失败", e, sql);
         return homes;
      }
   }

   public static HomeInfo getHome(UUID uuid, String homeName) {
      String sql = "SELECT * FROM home_info WHERE uuid = ? AND home_name = ?;";

      try {
         ResultSet rs = EssentialsD.database.query(sql, uuid.toString(), homeName);

         HomeInfo var5;
         label63: {
            HomeInfo home;
            label69: {
               try {
                  if (rs == null) {
                     home = null;
                     break label69;
                  }

                  if (rs.next()) {
                     home = new HomeInfo();
                     home.uuid = UUID.fromString(rs.getString("uuid"));
                     home.homeName = rs.getString("home_name");
                     home.location = LocationUtils.Deserialize(rs.getString("location"));
                     var5 = home;
                     break label63;
                  }
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

               return null;
            }

            if (rs != null) {
               rs.close();
            }

            return home;
         }

         if (rs != null) {
            rs.close();
         }

         return var5;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("获取Home失败", e, sql);
         return null;
      }
   }
}
