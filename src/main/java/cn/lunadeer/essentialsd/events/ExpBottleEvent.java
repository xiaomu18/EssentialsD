package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ExpBottleEvent implements Listener {
   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onExpBottleUsage(org.bukkit.event.entity.ExpBottleEvent event) {
      int exp = event.getExperience();
      if (exp < 0) {
         event.setExperience(0);
      }

      event.setExperience((int)((float)exp * EssentialsD.config.getExpBottleRatio()));
   }
}
