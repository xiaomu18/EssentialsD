package cn.lunadeer.essentialsd.dtos;

import cn.lunadeer.essentialsd.EssentialsD;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;

public class NameRecord {
   public UUID uuid;
   public String name;
   public Long time;
   public String timeString;

   public static boolean newNameRecord(Player player) {
      UUID uuid = player.getUniqueId();
      String name = player.getName();
      String sql = "INSERT INTO name_record (uuid, name, time) VALUES (?, ?, CURRENT_TIMESTAMP);";

      try {
         ResultSet rs = EssentialsD.database.query(sql, uuid.toString(), name);

         boolean var5;
         try {
            var5 = true;
         } catch (Throwable var8) {
            if (rs != null) {
               try {
                  rs.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (rs != null) {
            rs.close();
         }

         return var5;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("创建名字记录失败", e, sql);
         return false;
      }
   }

   public static List get5NameHistoryOf(UUID uuid) {
      return getNameHistoryOf(uuid, 5);
   }

   public static List getNameHistoryOf(UUID uuid, Integer limit) {
      List<NameRecord> records = new ArrayList();
      String sql = "SELECT * FROM name_record WHERE uuid = ? ORDER BY time DESC LIMIT " + limit + ";";

      try {
         ResultSet rs = EssentialsD.database.query(sql, uuid.toString());

         Object var10;
         label60: {
            try {
               if (rs == null) {
                  var10 = records;
                  break label60;
               }

               while(rs.next()) {
                  NameRecord record = new NameRecord();
                  record.uuid = UUID.fromString(rs.getString("uuid"));
                  record.name = rs.getString("name");
                  record.time = rs.getTimestamp("time").getTime();
                  record.timeString = rs.getTimestamp("time").toString();
                  records.add(record);
               }
            } catch (Throwable var8) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (rs != null) {
               rs.close();
            }

            return records;
         }

         if (rs != null) {
            rs.close();
         }

         return (List)var10;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("获取名字记录失败", e, sql);
         return records;
      }
   }
}
