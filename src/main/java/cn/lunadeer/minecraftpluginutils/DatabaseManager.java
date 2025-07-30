package cn.lunadeer.minecraftpluginutils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
   private final TYPE type;
   private Connection conn;

   public DatabaseManager(JavaPlugin plugin, TYPE type, String host, String port, String name, String user, String pass) {
      this.type = type;

      try {
         if (type.equals(DatabaseManager.TYPE.PGSQL)) {
            XLogger.info("正在连接到 PostgreSQL 数据库");
            Class.forName("org.postgresql.Driver");
            String connectionUrl = "jdbc:postgresql://" + host + ":" + port;
            connectionUrl = connectionUrl + "/" + name;
            this.conn = DriverManager.getConnection(connectionUrl, user, pass);
         } else if (type.equals(DatabaseManager.TYPE.SQLITE)) {
            XLogger.info("正在连接到 SQLite 数据库");
            Class.forName("org.sqlite.JDBC");
            File var10000 = plugin.getDataFolder();
            String connectionUrl = "jdbc:sqlite:" + var10000 + "/" + name + ".db";
            this.conn = DriverManager.getConnection(connectionUrl);
            this.query("PRAGMA foreign_keys = ON;");
         } else {
            this.handleDatabaseError("不支持的数据库类型: ", new Exception("只能为 postgresql 或 sqlite"), "");
            this.conn = null;
         }

         if (this.conn != null) {
            XLogger.info("数据库连接成功");
         }
      } catch (SQLException | ClassNotFoundException e) {
         this.handleDatabaseError("数据库连接失败: ", e, (String)null);
         this.conn = null;
      }

   }

   public void handleDatabaseError(String errorMessage, Exception e, String sql) {
      XLogger.err("=== 严重错误 ===");
      XLogger.err(errorMessage + e.getMessage());
      XLogger.err("SQL: " + sql);
      XLogger.err("===============");
   }

   private String sqlReg(String sql) {
      if (sql.contains("SERIAL PRIMARY KEY") && this.type.equals(DatabaseManager.TYPE.SQLITE)) {
         sql = sql.replace("SERIAL PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT");
      }

      return sql;
   }

   /** @deprecated */
   @Deprecated
   public ResultSet query(String sql) {
      if (this.conn == null) {
         this.handleDatabaseError("数据库连接失败: ", new Exception("Connection is null"), sql);
         return null;
      } else {
         try {
            Statement stmt = this.conn.createStatement();
            sql = this.sqlReg(sql);
            if (stmt.execute(sql)) {
               return stmt.getResultSet();
            }
         } catch (SQLException e) {
            this.handleDatabaseError("数据库操作失败: ", e, sql);
         }

         return null;
      }
   }

   public ResultSet query(String sql, Object... args) {
      if (this.conn == null) {
         this.handleDatabaseError("数据库连接失败: ", new Exception("Connection is null"), sql);
         return null;
      } else {
         try {
            sql = this.sqlReg(sql);
            PreparedStatement stmt = this.conn.prepareStatement(sql);

            for(int i = 0; i < args.length; ++i) {
               stmt.setObject(i + 1, args[i]);
            }

            if (stmt.execute()) {
               return stmt.getResultSet();
            }
         } catch (SQLException e) {
            this.handleDatabaseError("数据库操作失败: ", e, sql);
         }

         return null;
      }
   }

   public void addColumnIfNotExists(String tableName, String columnName, String columnDefinition) {
      if (!this.isColumnExist(tableName, columnName)) {
         if (this.type.equals(DatabaseManager.TYPE.PGSQL)) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " " + columnDefinition + ";";
            this.query(sql);
         } else if (this.type.equals(DatabaseManager.TYPE.SQLITE)) {
            this.query("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition + ";");
         }

      }
   }

   public void deleteColumnIfExists(String tableName, String columnName) {
      if (this.isColumnExist(tableName, columnName)) {
         if (this.type.equals(DatabaseManager.TYPE.PGSQL)) {
            String sql = "ALTER TABLE " + tableName + " DROP COLUMN IF EXISTS " + columnName + ";";
            this.query(sql);
         } else if (this.type.equals(DatabaseManager.TYPE.SQLITE)) {
            this.query("ALTER TABLE " + tableName + " DROP COLUMN " + columnName + ";");
         }

      }
   }

   public boolean isColumnExist(String tableName, String columnName) {
      if (this.type.equals(DatabaseManager.TYPE.PGSQL)) {
         String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = '" + tableName + "' AND column_name = '" + columnName + "';";

         try {
            ResultSet rs = this.query(sql);

            boolean var5;
            label84: {
               try {
                  if (rs != null && rs.next()) {
                     var5 = true;
                     break label84;
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

               return false;
            }

            if (rs != null) {
               rs.close();
            }

            return var5;
         } catch (Exception e) {
            this.handleDatabaseError("查询列是否存在失败", e, sql);
            return false;
         }
      } else if (!this.type.equals(DatabaseManager.TYPE.SQLITE)) {
         return false;
      } else {
         try {
            ResultSet rs = this.query("PRAGMA table_info(" + tableName + ");");
            if (rs != null) {
               while(rs.next()) {
                  if (columnName.equals(rs.getString("name"))) {
                     return true;
                  }
               }
            }
         } catch (SQLException e) {
            this.handleDatabaseError("查询列是否存在失败", e, (String)null);
         }

         return false;
      }
   }

   public void close() {
      try {
         XLogger.info("正在关闭数据库连接");
         this.conn.close();
         XLogger.info("数据库连接已关闭");
      } catch (SQLException e) {
         this.handleDatabaseError("关闭数据库连接失败: ", e, (String)null);
      }

   }

   public static enum TYPE {
      PGSQL,
      SQLITE;

      // $FF: synthetic method
      private static TYPE[] $values() {
         return new TYPE[]{PGSQL, SQLITE};
      }
   }
}
