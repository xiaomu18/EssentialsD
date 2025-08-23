package cn.lunadeer.minecraftpluginutils;

public class Common {
   public static boolean isPaper() {
      try {
         Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }
}
