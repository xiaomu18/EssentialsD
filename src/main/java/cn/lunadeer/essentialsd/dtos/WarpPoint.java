package cn.lunadeer.essentialsd.dtos;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.essentialsd.utils.LocUtil;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

public class WarpPoint {
   private Integer id;
   private final String name;
   private final Location location;

   public WarpPoint(Integer id, String name, Location location) {
      this.id = id;
      this.name = name;
      this.location = location;
   }

   public WarpPoint(String name, Location location) {
      this.name = name;
      this.location = location;
   }

   public Integer getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public Location getLocation() {
      return this.location;
   }

   public static void insert(WarpPoint point) {
      String sql = "INSERT INTO warp_point (warp_name, location) VALUES (?, ?);";

      try {
         ResultSet rs = EssentialsD.database.query(sql, point.getName(), LocUtil.toString(point.getLocation()));

         label51: {
            try {
               if (rs == null) {
                  break label51;
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

            return;
         }

         if (rs != null) {
            rs.close();
         }

      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("插入传送点失败", e, sql);
      }
   }

   public static void delete(WarpPoint point) {
      String sql = "DELETE FROM warp_point WHERE warp_name = ?;";

      try {
         ResultSet rs = EssentialsD.database.query(sql, point.getName());

         label51: {
            try {
               if (rs == null) {
                  break label51;
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

            return;
         }

         if (rs != null) {
            rs.close();
         }

      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("删除传送点失败", e, sql);
      }
   }

   public static WarpPoint selectByName(String name) {
      String sql = "SELECT * FROM warp_point WHERE warp_name = ?;";

      try {
         ResultSet rs = EssentialsD.database.query(sql, name);

         WarpPoint var8;
         label60: {
            label61: {
               try {
                  if (rs == null) {
                     var8 = null;
                     break label60;
                  }

                  if (rs.next()) {
                     var8 = new WarpPoint(rs.getInt("id"), rs.getString("warp_name"), LocUtil.fromString(rs.getString("location")));
                     break label61;
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

            return var8;
         }

         if (rs != null) {
            rs.close();
         }

         return var8;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("查询传送点失败", e, sql);
         return null;
      }
   }

   public static List selectAllNames() {
      List<String> names = new ArrayList();
      String sql = "SELECT DISTINCT warp_name FROM warp_point;";

      try {
         ResultSet rs = EssentialsD.database.query(sql);

         Object var3;
         label53: {
            try {
               if (rs == null) {
                  var3 = names;
                  break label53;
               }

               while(rs.next()) {
                  names.add(rs.getString("warp_name"));
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

            return names;
         }

         if (rs != null) {
            rs.close();
         }

         return (List)var3;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("查询传送点名字失败", e, sql);
         return names;
      }
   }

   public static List selectAll() {
      List<WarpPoint> points = new ArrayList();
      String sql = "SELECT * FROM warp_point;";

      try {
         ResultSet rs = EssentialsD.database.query(sql);

         Object var3;
         label53: {
            try {
               if (rs == null) {
                  var3 = points;
                  break label53;
               }

               while(rs.next()) {
                  points.add(new WarpPoint(rs.getInt("id"), rs.getString("warp_name"), LocUtil.fromString(rs.getString("location"))));
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

            return points;
         }

         if (rs != null) {
            rs.close();
         }

         return (List)var3;
      } catch (Exception e) {
         EssentialsD.database.handleDatabaseError("查询传送点失败", e, sql);
         return points;
      }
   }
}
