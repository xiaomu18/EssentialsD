package cn.lunadeer.minecraftpluginutils;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class XLogger {
   public static XLogger instance;
   private final Logger _logger;
   private boolean _debug = false;

   public XLogger(JavaPlugin plugin) {
      instance = this;
      this._logger = plugin.getLogger();
   }

   public static XLogger setDebug(boolean debug) {
      instance._debug = debug;
      return instance;
   }

   public static void info(String message) {
      instance._logger.info(" I | " + message);
   }

   public static void info(String message, Object... args) {
      Logger var10000 = instance._logger;
      String var10001 = String.format(message, args);
      var10000.info(" I | " + var10001);
   }

   public static void warn(String message) {
      instance._logger.warning(" W | " + message);
   }

   public static void warn(String message, Object... args) {
      Logger var10000 = instance._logger;
      String var10001 = String.format(message, args);
      var10000.warning(" W | " + var10001);
   }

   public static void err(String message) {
      instance._logger.severe(" E | " + message);
   }

   public static void err(String message, Object... args) {
      Logger var10000 = instance._logger;
      String var10001 = String.format(message, args);
      var10000.severe(" E | " + var10001);
   }

   public static void debug(String message) {
      if (instance._debug) {
         instance._logger.info(" D | " + message);
      }
   }

   public static void debug(String message, Object... args) {
      if (instance._debug) {
         Logger var10000 = instance._logger;
         String var10001 = String.format(message, args);
         var10000.info(" D | " + var10001);
      }
   }
}
