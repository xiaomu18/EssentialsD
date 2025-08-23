package cn.lunadeer.minecraftpluginutils;

import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.java.JavaPlugin;

public class Scheduler {
   public static Scheduler instance;
   private final JavaPlugin plugin;
   private boolean isPaper = false;

   public Scheduler(JavaPlugin plugin) {
      instance = this;
      this.plugin = plugin;
      this.isPaper = Common.isPaper();
   }

   public static void cancelAll() {
      if (instance.isPaper) {
         instance.plugin.getServer().getGlobalRegionScheduler().cancelTasks(instance.plugin);
         instance.plugin.getServer().getGlobalRegionScheduler().cancelTasks(instance.plugin);
      } else {
         instance.plugin.getServer().getScheduler().cancelTasks(instance.plugin);
      }

   }

   public static void runTaskLater(Runnable task, long delay) {
      if (delay <= 0L) {
         runTask(task);
      } else {
         if (instance.isPaper) {
            instance.plugin.getServer().getGlobalRegionScheduler().runDelayed(instance.plugin, (plugin) -> task.run(), delay);
         } else {
            instance.plugin.getServer().getScheduler().runTaskLater(instance.plugin, task, delay);
         }

      }
   }

   public static void runTask(Runnable task) {
      if (instance.isPaper) {
         instance.plugin.getServer().getGlobalRegionScheduler().run(instance.plugin, (plugin) -> task.run());
      } else {
         instance.plugin.getServer().getScheduler().runTask(instance.plugin, task);
      }

   }

   public static void runTaskRepeat(Runnable task, long delay, long period) {
      if (instance.isPaper) {
         instance.plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(instance.plugin, (plugin) -> task.run(), delay, period);
      } else {
         instance.plugin.getServer().getScheduler().runTaskTimer(instance.plugin, task, delay, period);
      }

   }

   public static void runTaskLaterAsync(Runnable task, long delay) {
      if (delay <= 0L) {
         runTaskAsync(task);
      } else {
         if (instance.isPaper) {
            instance.plugin.getServer().getAsyncScheduler().runDelayed(instance.plugin, (plugin) -> task.run(), delay * 50L, TimeUnit.MILLISECONDS);
         } else {
            instance.plugin.getServer().getScheduler().runTaskLaterAsynchronously(instance.plugin, task, delay);
         }

      }
   }

   public static void runTaskAsync(Runnable task) {
      if (instance.isPaper) {
         instance.plugin.getServer().getAsyncScheduler().runNow(instance.plugin, (plugin) -> task.run());
      } else {
         instance.plugin.getServer().getScheduler().runTaskAsynchronously(instance.plugin, task);
      }

   }

   public static void runTaskRepeatAsync(Runnable task, long delay, long period) {
      if (instance.isPaper) {
         instance.plugin.getServer().getAsyncScheduler().runAtFixedRate(instance.plugin, (plugin) -> task.run(), delay * 50L, period * 50L, TimeUnit.MILLISECONDS);
      } else {
         instance.plugin.getServer().getScheduler().runTaskTimerAsynchronously(instance.plugin, task, delay, period);
      }

   }
}
