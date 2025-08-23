package cn.lunadeer.essentialsd.events;

import cn.lunadeer.essentialsd.EssentialsD;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class Experience implements Listener {
   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onExpOrbSpawn(EntitySpawnEvent event) {
      if (EssentialsD.config.getCombineExpOrbs()) {
         Entity entity = event.getEntity();
         if (entity.getType() == EntityType.EXPERIENCE_ORB) {
            ExperienceOrb orb = (ExperienceOrb)entity;
            Location loc = entity.getLocation();
            double radius = (double)EssentialsD.config.getCombineExpOrbsRadius();

            for(Entity e : loc.getNearbyEntities(radius, radius, radius)) {
               if (e.getType() == EntityType.EXPERIENCE_ORB) {
                  orb.setExperience(orb.getExperience() + ((ExperienceOrb)e).getExperience());
                  e.remove();
               }
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerLogin(PlayerLoginEvent event) {
      Player player = event.getPlayer();
      if (EssentialsD.config.getNoExpCoolDown()) {
         player.setExpCooldown(0);
      }

   }
}
