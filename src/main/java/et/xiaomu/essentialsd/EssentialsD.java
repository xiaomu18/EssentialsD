package et.xiaomu.essentialsd;

import et.xiaomu.essentialsd.commands.*;
import et.xiaomu.essentialsd.commands.home.*;
import et.xiaomu.essentialsd.commands.tp.Back;
import et.xiaomu.essentialsd.commands.tp.Rtp;
import et.xiaomu.essentialsd.commands.tp.Tpa;
import et.xiaomu.essentialsd.commands.tp.TpaCancel;
import et.xiaomu.essentialsd.commands.tp.TpaHere;
import et.xiaomu.essentialsd.commands.warp.DelWarp;
import et.xiaomu.essentialsd.commands.warp.SetWarp;
import et.xiaomu.essentialsd.commands.warp.Warp;
import et.xiaomu.essentialsd.commands.warp.Warps;
import et.xiaomu.essentialsd.events.*;
import et.xiaomu.essentialsd.managers.ConfigManager;
import et.xiaomu.essentialsd.managers.DatabaseTables;
import et.xiaomu.essentialsd.managers.inspect.InspectManager;
import et.xiaomu.essentialsd.managers.MuteManager;
import et.xiaomu.essentialsd.managers.TeleportManager;
import et.xiaomu.essentialsd.managers.VanishManager;
import et.xiaomu.essentialsd.hooks.EssentialsDPlaceholderExpansion;
import et.xiaomu.essentialsd.recipes.*;
import cn.lunadeer.utils.DatabaseManager;
import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.Scheduler;
import cn.lunadeer.utils.XLogger;
import cn.lunadeer.utils.stui.TextUserInterfaceManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class EssentialsD extends JavaPlugin {
    public static EssentialsD instance;
    public static ConfigManager config;
    public static TeleportManager tpManager;
    public static DatabaseManager database;
    public static MuteManager muteManager;
    public static InspectManager inspectManager;
    public static VanishManager vanishManager;
    private EssentialsDPlaceholderExpansion placeholderExpansion;
    public static Map<String, CommandExecutor> commands = new HashMap<>();

    public static String buildDate = "unknown";
    public static String gitCommit = "unknown";
    public static String gitBranch = "unknown";
    public static String projectUrl = "https://github.com/xiaomu18/EssentialsD";

    public void onEnable() {
        instance = this;
        new XLogger(instance);
        loadBuildInfo();
        config = new ConfigManager(instance);
        new Notification(instance);
        database = new DatabaseManager(this, DatabaseManager.TYPE.valueOf(config.getDbType().toUpperCase()), config.getDbHost(), config.getDbPort(), config.getDbName(), config.getDbUser(), config.getDbPass());
        DatabaseTables.migrate();
        muteManager = new MuteManager();
        new Scheduler(this);
        tpManager = new TeleportManager();
        inspectManager = new InspectManager();
        vanishManager = new VanishManager();
        new TextUserInterfaceManager(this);
        registerPlaceholderExpansion();
        registerPlaceholderExpansionBootstrap();

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
        Bukkit.getPluginManager().registerEvents(new CommandPreprocessEvent(), this);
        Bukkit.getPluginManager().registerEvents(new InspectInventoryEvent(), this);
        Bukkit.getPluginManager().registerEvents(new VanishEvent(), this);

        commands.put("fly", new Fly());
        commands.put("flyspeed", new FlySpeed());
        commands.put("clear", new Clear());
        commands.put("god", new God());
        commands.put("info", new Info());
        commands.put("save", new Save());
        commands.put("heal", new Heal());
        commands.put("more", new More());
        commands.put("inspect", new Inspect());
        commands.put("enderchest", new EnderChest());
        commands.put("suicide", new Suicide());
        commands.put("hat", new Hat());
        commands.put("showitem", new ShowItem());
        commands.put("skull", new Skull());
        commands.put("sit", new Sit());
        commands.put("tpa", new Tpa());
        commands.put("tpahere", new TpaHere());
        commands.put("tpacancel", new TpaCancel());
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
        commands.put("vanish", new Vanish());
        commands.put("gamemode", new Gamemode());
        commands.put("essd", new Control());
        commands.put("home-editor", new HomeEditor());

        // 注册命令
        commands.forEach((key, value) -> {
            PluginCommand command = Bukkit.getPluginCommand(key);

            if (command == null) {
                getLogger().warning("注册命令 " + key + " 时失败, 此命令将不可使用...");
            } else {
                command.setExecutor(value);
            }
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
        XLogger.info("EssentialsD v" + this.getPluginMeta().getVersion() + " Reloaded By xiaomu18");
        XLogger.info("  ______                    _   _       _     _____");
        XLogger.info(" |  ____|                  | | (_)     | |   |  __ \\");
        XLogger.info(" | |__   ___ ___  ___ _ __ | |_ _  __ _| |___| |  | |");
        XLogger.info(" |  __| / __/ __|/ _ \\ '_ \\| __| |/ _` | / __| |  | |");
        XLogger.info(" | |____\\__ \\__ \\  __/ | | | |_| | (_| | \\__ \\ |__| |");
        XLogger.info(" |______|___/___/\\___|_| |_|\\__|_|\\__,_|_|___/_____/");
        XLogger.info("");
    }

    public void onDisable() {
        XLogger.info("EssentialsD 正在关闭");
        shutdownComponent("检查背包会话", () -> {
            if (inspectManager != null) {
                inspectManager.shutdown();
            }
        });
        shutdownComponent("隐身状态", () -> {
            if (vanishManager != null) {
                vanishManager.shutdown();
            }
        });
        shutdownComponent("PlaceholderAPI Hook", () -> {
            if (placeholderExpansion != null) {
                placeholderExpansion.unregister();
                placeholderExpansion = null;
            }
        });
        shutdownComponent("传送缓存", () -> {
            if (tpManager != null) {
                tpManager.shutdown();
            }
        });
        shutdownComponent("调度任务", Scheduler::cancelAll);
        shutdownComponent("数据库连接", () -> {
            if (database != null) {
                database.close();
            }
        });
        commands.clear();
        tpManager = null;
        inspectManager = null;
        vanishManager = null;
        muteManager = null;
        database = null;
        config = null;
        instance = null;
        XLogger.info("EssentialsD 已卸载");
    }

    public String getBuildDate() {
        return buildDate;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    private void loadBuildInfo() {
        try (InputStream input = this.getResource("build-info.properties")) {
            if (input == null) {
                return;
            }
            Properties properties = new Properties();
            properties.load(input);
            String buildDate = properties.getProperty("build.date");
            if (buildDate != null && !buildDate.isBlank() && !buildDate.contains("${")) {
                EssentialsD.buildDate = buildDate;
            }
            String gitCommit = properties.getProperty("git.commit");
            if (gitCommit != null && !gitCommit.isBlank() && !gitCommit.contains("${")) {
                EssentialsD.gitCommit = gitCommit;
            }
            String gitBranch = properties.getProperty("git.branch");
            if (gitBranch != null && !gitBranch.isBlank()) {
                EssentialsD.gitBranch = gitBranch;
            }
        } catch (IOException ignored) {
        }
    }

    private void shutdownComponent(String name, Runnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            XLogger.error("关闭 %s 时发生异常: %s", name, throwable.getMessage());
            XLogger.error(throwable);
        }
    }

    private void registerPlaceholderExpansion() {
        if (placeholderExpansion != null) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        placeholderExpansion = new EssentialsDPlaceholderExpansion(this);
        if (placeholderExpansion.register()) {
            XLogger.info("PlaceholderAPI 变量已注册");
            return;
        }
        XLogger.warn("PlaceholderAPI 变量注册失败");
        placeholderExpansion = null;
    }

    private void registerPlaceholderExpansionBootstrap() {
        if (placeholderExpansion != null) {
            return;
        }
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                if (!"PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName())) {
                    return;
                }
                registerPlaceholderExpansion();
                if (placeholderExpansion != null) {
                    PluginEnableEvent.getHandlerList().unregister(this);
                }
            }
        }, this);
    }
}
