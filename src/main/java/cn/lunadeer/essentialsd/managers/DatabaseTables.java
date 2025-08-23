package cn.lunadeer.essentialsd.managers;

import cn.lunadeer.essentialsd.EssentialsD;

public class DatabaseTables {
   public static void migrate() {
      String sql = "";
      sql = "CREATE TABLE IF NOT EXISTS player_name ( uuid              VARCHAR(36) NOT NULL UNIQUE PRIMARY KEY, last_known_name   TEXT NOT NULL);";
      EssentialsD.database.query(sql);
      sql = "CREATE TABLE IF NOT EXISTS login_record ( id                SERIAL PRIMARY KEY, uuid              VARCHAR(36) NOT NULL, ip                VARCHAR(15) NOT NULL, login_time        TIMESTAMP NOT NULL, logout_location   TEXT, logout_time       TIMESTAMP, FOREIGN KEY (uuid) REFERENCES player_name(uuid) ON DELETE CASCADE);";
      EssentialsD.database.query(sql);
      sql = "CREATE TABLE IF NOT EXISTS name_record ( id                SERIAL PRIMARY KEY, uuid              VARCHAR(36) NOT NULL, name              TEXT NOT NULL, time              TIMESTAMP NOT NULL, FOREIGN KEY (uuid) REFERENCES player_name(uuid) ON DELETE CASCADE);";
      EssentialsD.database.query(sql);
      sql = "CREATE TABLE IF NOT EXISTS tp_record ( id                SERIAL PRIMARY KEY, initiator_uuid    VARCHAR(36) NOT NULL, from_location     TEXT NOT NULL, to_location       TEXT NOT NULL, type              TEXT NOT NULL, success           BOOLEAN NOT NULL, time              TIMESTAMP NOT NULL, FOREIGN KEY (initiator_uuid) REFERENCES player_name(uuid) ON DELETE CASCADE);";
      EssentialsD.database.query(sql);
      sql = "CREATE TABLE IF NOT EXISTS message_record ( id                SERIAL PRIMARY KEY, sender_uuid       VARCHAR(36) NOT NULL, message           TEXT NOT NULL, time              TIMESTAMP NOT NULL, FOREIGN KEY (sender_uuid) REFERENCES player_name(uuid) ON DELETE CASCADE);";
      EssentialsD.database.query(sql);
      sql = "CREATE TABLE IF NOT EXISTS command_record ( id                SERIAL PRIMARY KEY, executor_uuid     VARCHAR(36) NOT NULL, command           TEXT NOT NULL, time              TIMESTAMP NOT NULL, FOREIGN KEY (executor_uuid) REFERENCES player_name(uuid) ON DELETE CASCADE);";
      EssentialsD.database.query(sql);
      sql = "CREATE TABLE IF NOT EXISTS home_info ( id                SERIAL PRIMARY KEY, uuid              VARCHAR(36) NOT NULL, home_name         TEXT NOT NULL, location          TEXT NOT NULL, FOREIGN KEY (uuid) REFERENCES player_name(uuid) ON DELETE CASCADE);";
      EssentialsD.database.query(sql);
      sql = "CREATE TABLE IF NOT EXISTS warp_point ( id                SERIAL PRIMARY KEY, warp_name         TEXT NOT NULL, location          TEXT NOT NULL);";
      EssentialsD.database.query(sql);
   }
}
