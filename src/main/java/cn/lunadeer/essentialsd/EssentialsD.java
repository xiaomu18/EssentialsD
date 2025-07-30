package cn.lunadeer.essentialsd;

import cn.lunadeer.essentialsd.commands.*;
import cn.lunadeer.essentialsd.commands.home.DelHome;
import cn.lunadeer.essentialsd.commands.home.Home;
import cn.lunadeer.essentialsd.commands.home.Homes;
import cn.lunadeer.essentialsd.commands.home.SetHome;
import cn.lunadeer.essentialsd.commands.tp.Back;
import cn.lunadeer.essentialsd.commands.tp.Rtp;
import cn.lunadeer.essentialsd.commands.tp.Tpa;
import cn.lunadeer.essentialsd.commands.tp.TpaHere;
import cn.lunadeer.essentialsd.commands.warp.DelWarp;
import cn.lunadeer.essentialsd.commands.warp.SetWarp;
import cn.lunadeer.essentialsd.commands.warp.Warp;
import cn.lunadeer.essentialsd.commands.warp.Warps;
import cn.lunadeer.essentialsd.events.*;
import cn.lunadeer.essentialsd.managers.ConfigManager;
import cn.lunadeer.essentialsd.managers.DatabaseTables;
import cn.lunadeer.essentialsd.managers.TeleportManager;
import cn.lunadeer.essentialsd.recipes.Crowbar;
import cn.lunadeer.essentialsd.recipes.InvisibleGlowItemFrame;
import cn.lunadeer.essentialsd.recipes.InvisibleItemFrame;
import cn.lunadeer.essentialsd.recipes.LightBlock;
import cn.lunadeer.essentialsd.recipes.StackedEnchantBook;
import cn.lunadeer.minecraftpluginutils.DatabaseManager;
import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.Scheduler;
import cn.lunadeer.minecraftpluginutils.XLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EssentialsD extends JavaPlugin {
   public static EssentialsD instance;
   public static ConfigManager config;
   public static TeleportManager tpManager;
   public static DatabaseManager database;
   public static Map<String, CommandExecutor> commands = new HashMap<>();

   public void onEnable() {
      instance = this;
      new XLogger(instance);
      config = new ConfigManager(instance);
      new Notification(instance);
      database = new DatabaseManager(this, DatabaseManager.TYPE.valueOf(config.getDbType().toUpperCase()), config.getDbHost(), config.getDbPort(), config.getDbName(), config.getDbUser(), config.getDbPass());
      DatabaseTables.migrate();
      new Scheduler(this);
      tpManager = new TeleportManager();

      Bukkit.getPluginManager().registerEvents(new ChatFunctionEvent(), this);
      Bukkit.getPluginManager().registerEvents(new InvisibleItemFrameEvent(), this);
      Bukkit.getPluginManager().registerEvents(new ChairEvent(), this);
      Bukkit.getPluginManager().registerEvents(new ArmorStandHandsEvent(), this);
      Bukkit.getPluginManager().registerEvents(new CrowEvent(), this);
      Bukkit.getPluginManager().registerEvents(new ExpBottleEvent(), this);
      Bukkit.getPluginManager().registerEvents(new ShowItemEvent(), this);
      Bukkit.getPluginManager().registerEvents(new TeleportEvent(), this);
      Bukkit.getPluginManager().registerEvents(new PlayerRecordEvent(), this);
      Bukkit.getPluginManager().registerEvents(new Experience(), this);
      Bukkit.getPluginManager().registerEvents(new CreativeTakeListener(), this);

      commands.put("fly", new Fly());
      commands.put("god", new God());
      commands.put("save", new Save());
      commands.put("more", new More());
      commands.put("inspect", new Inspect());
      commands.put("inspect-ender", commands.get("inspect"));
      commands.put("enderchest", new EnderChest());
      commands.put("suicide", new Suicide());
      commands.put("hat", new Hat());
      commands.put("showitem", new ShowItem());
      commands.put("skull", new Skull());
      commands.put("tpa", new Tpa());
      commands.put("tpahere", new TpaHere());
      commands.put("rtp", new Rtp());
      commands.put("back", new Back());
      commands.put("home", new Home());
      commands.put("homes", new Homes());
      commands.put("sethome", new SetHome());
      commands.put("delhome", new DelHome());
      commands.put("setwarp", new SetWarp());
      commands.put("delwarp", new DelWarp());
      commands.put("warp", new Warp());
      commands.put("warps", new Warps());
      commands.put("mute", new Mute());
      commands.put("unmute", commands.get("mute"));
      commands.put("kickall", new Kickall());
      commands.put("inv", new Invisible());
      commands.put("essd", new Control());

      // 注册命令
      commands.forEach((key, value) -> {
         ((PluginCommand)Objects.requireNonNull(Bukkit.getPluginCommand(key))).setExecutor(value);
      });

      if (config.getRecipesCrowbar()) {
         this.getServer().addRecipe(Crowbar.getRecipe());
      }

      if (config.getRecipesInvisibleItemFrame()) {
         this.getServer().addRecipe(InvisibleItemFrame.getRecipe());
         this.getServer().addRecipe(InvisibleGlowItemFrame.getRecipe());
      }

      if (config.getRecipesStackedEnchantBook()) {
         this.getServer().addRecipe(StackedEnchantBook.getRecipe());
      }

      if (config.getRecipesLightBlock()) {
         this.getServer().addRecipe(LightBlock.getRecipe());
      }

      config.ApplyForceLoadChunks();
      XLogger.info("EssentialsD 已加载");
      XLogger.info("版本: " + this.getPluginMeta().getVersion() + " Redesigned");
      XLogger.info("  ______                    _   _       _     _____");
      XLogger.info(" |  ____|                  | | (_)     | |   |  __ \\");
      XLogger.info(" | |__   ___ ___  ___ _ __ | |_ _  __ _| |___| |  | |");
      XLogger.info(" |  __| / __/ __|/ _ \\ '_ \\| __| |/ _` | / __| |  | |");
      XLogger.info(" | |____\\__ \\__ \\  __/ | | | |_| | (_| | \\__ \\ |__| |");
      XLogger.info(" |______|___/___/\\___|_| |_|\\__|_|\\__,_|_|___/_____/");
      XLogger.info("");
   }

   public void onDisable() {
   }
}
