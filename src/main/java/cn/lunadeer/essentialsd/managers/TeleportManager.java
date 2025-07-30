package cn.lunadeer.essentialsd.managers;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.Scheduler;
import cn.lunadeer.minecraftpluginutils.XLogger;
import cn.lunadeer.minecraftpluginutils.stui.components.Button;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class TeleportManager {
   private static final TextColor main_color = TextColor.color(0, 233, 255);
   private final ConcurrentHashMap _tasks = new ConcurrentHashMap();
   private final ConcurrentHashMap _next_time_allow_tp = new ConcurrentHashMap();
   private final ConcurrentHashMap _last_tp_location = new ConcurrentHashMap();

   private boolean tpReqCheckFail(Player initiator, Player target) {
      if (initiator == target) {
         Notification.error(initiator, "不能传送到同一个位置");
         return true;
      } else if (!target.isOnline()) {
         Notification.error(initiator, "玩家 " + target.getName() + " 不在线");
         return true;
      } else if (EssentialsD.config.getTpWorldBlackList().contains(target.getWorld().getName())) {
         Notification.error(initiator, "目的地所在世界 " + initiator.getWorld().getName() + " 不允许传送");
         return true;
      } else {
         return this.CoolingDown(initiator);
      }
   }

   public void tpaRequest(Player initiator, Player target) {
      if (!this.tpReqCheckFail(initiator, target)) {
         TpTask task = new TpTask();
         task.initiator = initiator;
         task.target = target;
         task.taskId = UUID.randomUUID();
         this._tasks.put(task.taskId, task);
         Notification.info(initiator, "已向 " + target.getName() + " 发送传送请求");
         TextComponent acceptBtn = Button.createGreen("接受").setExecuteCommand("/tpa accept " + task.taskId).build();
         TextComponent denyBtn = Button.createRed("拒绝").setExecuteCommand("/tpa deny " + task.taskId).build();
         Notification.info((Player)target, (Component)Component.text("                            ", Style.style(main_color, new TextDecoration[]{TextDecoration.STRIKETHROUGH})));
         Notification.info((Player)target, (Component)Component.text("| 玩家 " + initiator.getName() + " 请求传送到你的位置", main_color));
         Notification.info((Player)target, (Component)Component.text("| 此请求将在  " + EssentialsD.config.getTpTpaExpire() + " 秒后失效", main_color));
         Notification.info(target, ((TextComponent)((TextComponent)Component.text("| ", main_color).append(acceptBtn)).append(Component.text("  ", main_color))).append(denyBtn));
         Notification.info((Player)target, (Component)Component.text("                            ", Style.style(main_color, new TextDecoration[]{TextDecoration.STRIKETHROUGH})));
         Scheduler.runTaskLater(() -> this._tasks.remove(task.taskId), 20L * (long)EssentialsD.config.getTpTpaExpire());
      }
   }

   public void tpahereRequest(Player initiator, Player target) {
      if (!this.tpReqCheckFail(initiator, target)) {
         TpTask task = new TpTask();
         task.initiator = initiator;
         task.target = target;
         task.taskId = UUID.randomUUID();
         task.tpahere = true;
         this._tasks.put(task.taskId, task);
         Notification.info(initiator, "已向 " + target.getName() + " 发送传送请求");
         TextComponent acceptBtn = Button.createGreen("接受").setExecuteCommand("/tpa accept " + task.taskId).build();
         TextComponent denyBtn = Button.createRed("拒绝").setExecuteCommand("/tpa deny " + task.taskId).build();
         Notification.info((Player)target, (Component)Component.text("                            ", Style.style(main_color, new TextDecoration[]{TextDecoration.STRIKETHROUGH})));
         Notification.info((Player)target, (Component)Component.text("| 玩家 " + initiator.getName() + " 请求传送你到他的位置", main_color));
         Notification.info((Player)target, (Component)Component.text("| 此请求将在  " + EssentialsD.config.getTpTpaExpire() + " 秒后失效", main_color));
         Notification.info(target, ((TextComponent)((TextComponent)Component.text("| ", main_color).append(acceptBtn)).append(Component.text("  ", main_color))).append(denyBtn));
         Notification.info((Player)target, (Component)Component.text("                            ", Style.style(main_color, new TextDecoration[]{TextDecoration.STRIKETHROUGH})));
         Scheduler.runTaskLater(() -> this._tasks.remove(task.taskId), 20L * (long)EssentialsD.config.getTpTpaExpire());
      }
   }

   public void deny(Player player, UUID taskId) {
      TpTask task = (TpTask)this._tasks.get(taskId);
      if (task == null) {
         Notification.error(player, "传送请求不存在或已过期");
      } else if (task.target != player) {
         Notification.error(player, "这不是你的传送请求");
      } else {
         this._tasks.remove(taskId);
         if (task.initiator.isOnline()) {
            Notification.error(task.initiator, "玩家 " + player.getName() + " 拒绝了你的传送请求");
         }

         if (task.target.isOnline()) {
            Notification.error(player, "已拒绝 " + task.initiator.getName() + " 的传送请求");
         }

      }
   }

   public void accept(Player player, UUID taskId) {
      TpTask task = (TpTask)this._tasks.get(taskId);
      if (task == null) {
         Notification.error(player, "传送请求不存在或已过期");
      } else if (task.target != player) {
         Notification.error(player, "这不是你的传送请求");
      } else {
         this._tasks.remove(taskId);
         if (task.initiator.isOnline() && task.target.isOnline()) {
            Notification.info(task.target, "已接受 " + task.initiator.getName() + " 的传送请求");
            Notification.info(task.initiator, "玩家 " + task.target.getName() + " 已接受你的传送请求");
            if (!task.tpahere) {
               try {
                  this.doTeleportDelayed(task.initiator, task.target.getLocation(), (Integer)EssentialsD.config.getTpDelay(), () -> Notification.info(task.initiator, "正在传送到 " + task.initiator.getName() + " 的位置"), () -> {
                     Notification.info(task.initiator, "已传送到 " + task.initiator.getName() + " 的位置");
                     Notification.info(task.target, "玩家 " + task.initiator.getName() + " 已传送到你的位置");
                  });
               } catch (RuntimeException e) {
                  Notification.error(player, e.getMessage());
               }
            } else {
               try {
                  this.doTeleportDelayed(task.target, task.initiator.getLocation(), (Integer)EssentialsD.config.getTpDelay(), () -> Notification.info(task.target, "正在传送到 " + task.initiator.getName() + " 的位置"), () -> {
                     Notification.info(task.target, "已传送到 " + task.initiator.getName() + " 的位置");
                     Notification.info(task.initiator, "玩家 " + task.target.getName() + " 已传送到你的位置");
                  });
               } catch (RuntimeException e) {
                  Notification.error(player, e.getMessage());
               }
            }

         }
      }
   }

   public void back(Player player) {
      if (!this._last_tp_location.containsKey(player.getUniqueId())) {
         Notification.error(player, "没有找到可返回的位置");
      } else {
         Location target = (Location)this._last_tp_location.get(player.getUniqueId());
         if (EssentialsD.config.getTpWorldBlackList().contains(target.getWorld().getName())) {
            Notification.error(player, "目的地所在世界 " + target.getWorld().getName() + " 不允许传送");
         } else if (!this.CoolingDown(player)) {
            if (EssentialsD.config.getTpDelay() > 0) {
               Notification.info(player, "将在 " + EssentialsD.config.getTpDelay() + " 秒后返回上次传送的位置");
            }

            try {
               this.doTeleportDelayed(player, target, (Integer)EssentialsD.config.getTpDelay(), () -> Notification.info(player, "正在返回上次传送的位置"), () -> Notification.info(player, "已返回上次传送的位置"));
            } catch (RuntimeException e) {
               Notification.error(player, e.getMessage());
            }

         }
      }
   }

   public void rtp(Player player) {
      if (EssentialsD.config.getTpWorldBlackList().contains(player.getWorld().getName())) {
         Notification.error(player, "此世界 " + player.getWorld().getName() + " 不允许传送");
      } else if (!this.CoolingDown(player)) {
         int radius = EssentialsD.config.getTpRtpRadius();
         World world = null;

         for(World w : EssentialsD.instance.getServer().getWorlds()) {
            if (w.getEnvironment() == Environment.NORMAL) {
               world = w;
               break;
            }
         }

         if (world == null) {
            Notification.error(player, "未找到主世界");
         } else {
            int x = (int)(Math.random() * (double)radius * (double)2.0F) - radius + (int)player.getLocation().getX();
            int z = (int)(Math.random() * (double)radius * (double)2.0F) - radius + (int)player.getLocation().getZ();
            XLogger.debug("RTP: " + x + " " + z);
            Location location = new Location(world, (double)x + (double)0.5F, player.getY(), (double)z + (double)0.5F);

            try {
               this.doTeleportDelayed(player, location, (Integer)EssentialsD.config.getTpDelay(), () -> Notification.info(player, "正在传送到随机位置"), () -> Notification.info(player, "已传送到随机位置"));
            } catch (RuntimeException e) {
               Notification.error(player, e.getMessage());
            }

         }
      }
   }

   public void doTeleportDelayed(Player player, Location location, Integer delay, Runnable before, Runnable after) {
      this.doTeleportDelayed(player, location, delay.longValue(), before, after);
   }

   public void doTeleportDelayed(Player player, Location to, Long delay, Runnable before, Runnable after) {
      if (EssentialsD.config.getTpWorldBlackList().contains(to.getWorld().getName())) {
         Notification.error(player, "目的地所在世界 %s 不允许传送", to.getWorld().getName());
      } else if (!this.CoolingDown(player)) {
         if (delay > 0L) {
            Notification.info(player, "将在 %d 秒后执行传送", delay);
            Scheduler.runTaskAsync(() -> {
               long i = delay;

               while(i > 0L) {
                  if (!player.isOnline()) {
                     return;
                  }

                  Notification.actionBar(player, "传送倒计时 %d 秒", i);
                  --i;

                  try {
                     Thread.sleep(1000L);
                  } catch (InterruptedException e) {
                     XLogger.warn(e.getMessage());
                     return;
                  }
               }

            });
            Scheduler.runTaskLater(() -> {
               before.run();
               this.doTeleportSafely(player, to);
               after.run();
            }, 20L * delay);
         } else {
            before.run();
            this.doTeleportSafely(player, to);
            after.run();
         }

      }
   }

   private boolean CoolingDown(Player player) {
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime next_time = (LocalDateTime)this._next_time_allow_tp.get(player.getUniqueId());
      if (next_time != null && now.isBefore(next_time)) {
         long secs_until_next = now.until(next_time, ChronoUnit.SECONDS);
         Notification.warn(player, "请等待 %d 秒后再次执行传送请求", secs_until_next);
         return true;
      } else {
         return false;
      }
   }

   public void doTeleportSafely(Player player, Location location) {
      if (!this.CoolingDown(player)) {
         LocalDateTime now = LocalDateTime.now();
         this._next_time_allow_tp.put(player.getUniqueId(), now.plusSeconds((long)EssentialsD.config.getTpCoolDown()));
         location.getWorld().getChunkAtAsyncUrgently(location).thenAccept((chunk) -> {
            int max_attempts = 512;

            while(location.getBlock().isPassable()) {
               location.setY(location.getY() - (double)1.0F);
               --max_attempts;
               if (max_attempts <= 0) {
                  Notification.error(player, "传送目的地不安全，已取消传送");
                  return;
               }
            }

            Block up1 = location.getBlock().getRelative(BlockFace.UP);
            Block up2 = up1.getRelative(BlockFace.UP);
            max_attempts = 512;

            while(!up1.isPassable() || up1.isLiquid() || !up2.isPassable() || up2.isLiquid()) {
               location.setY(location.getY() + (double)1.0F);
               up1 = location.getBlock().getRelative(BlockFace.UP);
               up2 = up1.getRelative(BlockFace.UP);
               --max_attempts;
               if (max_attempts <= 0) {
                  Notification.error(player, "传送目的地不安全，已取消传送");
                  return;
               }
            }

            location.setY(location.getY() + (double)1.0F);
            if (location.getBlock().getRelative(BlockFace.DOWN).getType() == Material.LAVA) {
               Notification.error(player, "传送目的地不安全，已取消传送");
            } else {
               this.updateLastTpLocation(player);
               player.teleportAsync(location, TeleportCause.PLUGIN);
            }
         });
      }
   }

   public void updateLastTpLocation(Player player) {
      this._last_tp_location.put(player.getUniqueId(), player.getLocation());
   }

   private static class TpTask {
      public Player initiator;
      public Player target;
      public UUID taskId;
      public Boolean tpahere;

      private TpTask() {
         this.tpahere = false;
      }
   }
}
