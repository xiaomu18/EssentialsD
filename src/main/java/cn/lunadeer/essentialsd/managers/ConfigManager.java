package cn.lunadeer.essentialsd.managers;

import cn.lunadeer.essentialsd.EssentialsD;
import cn.lunadeer.minecraftpluginutils.Scheduler;
import cn.lunadeer.minecraftpluginutils.XLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ConfigManager {
   private final EssentialsD _plugin;
   private FileConfiguration _file;
   private boolean _debug;
   private float _exp_bottle_ratio;
   private Boolean _combine_exp_orbs_enable;
   private Float _combine_exp_orbs_radius;
   private Boolean _no_exp_cool_down;
   private List<String> _force_load_chunks;
   private Integer _chunk_operate_delay;
   private Integer _tp_tpa_expire;
   private Integer _tp_delay;
   private Integer _tp_cool_down;
   private Integer _tp_rtp_radius;
   private List<String> _tp_world_blacklist;
   private Boolean _chair_enable;
   private Integer _chair_max_width;
   private Boolean _chair_sign_check;
   private Float _chair_sit_height;
   private Boolean _recipes_crowbar;
   private Boolean _recipes_invisible_item_frame;
   private Boolean _recipes_light_block;
   private Boolean _recipes_stacked_enchant_book;
   private Integer _home_limit_amount;
   private List<String> _home_world_blacklist;
   private String _db_type;
   private String _db_host;
   private String _db_port;
   private String _db_user;
   private String _db_pass;
   private String _db_name;
   public Boolean chat_func_enable;
   public String chat_format;
   public List<String> forbidWords;
   public Map<String, String> replaceWords;
   public String forbidMessage;
   public long COOLDOWN_MS;
   public Boolean self_deception_mode;
   public long CMD_COOLDOWN_MS;
   public List<String> CMD_BANNED_LIST;
   public Boolean CMD_ENABLE;
   public String CMD_CD_MESSAGE;
   public String allow_minimessage_perm;

   public List<Material> forbidTakeItems = new ArrayList<>();
   public Boolean forbidNBTItem;

   public ConfigManager(EssentialsD plugin) {
      this._plugin = plugin;
      this._plugin.saveDefaultConfig();
      this.reload();
   }

   public void reload() {
      this._plugin.reloadConfig();
      this._file = this._plugin.getConfig();

      this._debug = this._file.getBoolean("Debug", false);
      XLogger.setDebug(this.isDebug());

      this._exp_bottle_ratio = (float)this._file.getDouble("ExpBottleRatio", 1.0F);
      this._combine_exp_orbs_enable = this._file.getBoolean("CombineExpOrbs.Enable", false);
      this._combine_exp_orbs_radius = (float)this._file.getDouble("CombineExpOrbs.Radius", 1.5F);
      this._no_exp_cool_down = this._file.getBoolean("NoExpCoolDown", false);
      this._force_load_chunks = this._file.getStringList("ForceLoadChunks");
      this._chunk_operate_delay = this._file.getInt("ChunkOperateDelay", 10);
      this._tp_delay = this._file.getInt("Teleport.Delay", 0);
      this._tp_cool_down = this._file.getInt("Teleport.CoolDown", 0);
      this._tp_tpa_expire = this._file.getInt("Teleport.TpaExpire", 30);
      this._tp_rtp_radius = this._file.getInt("Teleport.RtpRadius", 1000);
      this._tp_world_blacklist = this._file.getStringList("Teleport.WorldBlackList");
      this._chair_enable = this._file.getBoolean("Chair.Enable", true);
      this._chair_max_width = this._file.getInt("Chair.MaxWidth", 4);
      this._chair_sign_check = this._file.getBoolean("Chair.SignCheck", true);
      this._chair_sit_height = (float)this._file.getDouble("Chair.SitHeight", -0.95);
      this._recipes_crowbar = this._file.getBoolean("Recipes.CrowBar", true);
      this._recipes_invisible_item_frame = this._file.getBoolean("Recipes.InvisibleItemFrame", true);
      this._recipes_light_block = this._file.getBoolean("Recipes.LightBlock", true);
      this._recipes_stacked_enchant_book = this._file.getBoolean("Recipes.StackedEnchantBook", true);
      this._home_limit_amount = this._file.getInt("HomeLimit.Amount", 5);
      this._home_world_blacklist = this._file.getStringList("HomeLimit.WorldBlacklist");

      this.chat_func_enable = this._file.getBoolean("chat-func.Enable", false);
      this.chat_format = this._file.getString("chat-func.ChatFormat", "<%player_name%> ");
      this.allow_minimessage_perm = this._file.getString("chat-func.allow-minimessage-perm", "essd.chat.allow-use-minimessage");

      this.COOLDOWN_MS = this._file.getLong("chat-func.cooldown", 2000);

      this.CMD_ENABLE = this._file.getBoolean("command-manager.Enable", false);
      this.CMD_COOLDOWN_MS = this._file.getLong("command-manager.cooldown", 2000);
      this.CMD_BANNED_LIST = this._file.getStringList("command-manager.banned_command");
      this.CMD_CD_MESSAGE = this._file.getString("command-manager.cd_message");

      this.forbidWords = this._file.getStringList("chat-func.forbid-keywords");
      this.replaceWords = get_replace_words();
      this.self_deception_mode = this._file.getBoolean("chat-func.self-deception-mode");
      this.forbidMessage = this._file.getString("chat-func.forbid-message");
      List<String> forbidItemNames = this._file.getStringList("Creative-ItemTake.banned-item");

      forbidTakeItems.clear();
      forbidItemNames.forEach(itemName -> {
         forbidTakeItems.add(Material.getMaterial(itemName));
      });
      this.forbidNBTItem = this._file.getBoolean("Creative-ItemTake.banned-nbt");

      this._db_type = this._file.getString("Database.Type", "sqlite");
      if (!this._db_type.equals("pgsql") && !this._db_type.equals("sqlite")) {
         XLogger.err("当前数据库只支持 pgsql 或 sqlite，已重置为 sqlite");
         this.setDbType("sqlite");
      }

      this._db_host = this._file.getString("Database.Host", "localhost");
      this._db_port = this._file.getString("Database.Port", "5432");
      this._db_name = this._file.getString("Database.Name", "dominion");
      this._db_user = this._file.getString("Database.User", "postgres");
      this._db_pass = this._file.getString("Database.Pass", "postgres");
   }

   private Map<String, String> get_replace_words() {
      Map<String, String> wordReplacements = new HashMap<>();

      // 获取 replace-words 列表
      List<?> replaceList = this._file.getList("chat-func.replace-words", new ArrayList<>());

      for (Object entryObj : replaceList) {
         if (entryObj instanceof List<?>) {
            List<?> pair = (List<?>) entryObj;

            // 确保是包含两个元素的列表
            if (pair.size() >= 2) {
               Object key = pair.get(0);
               Object value = pair.get(1);

               // 确保元素是字符串类型
               if (key instanceof String && value instanceof String) {
                  wordReplacements.put(
                          (String) key,
                          (String) value
                  );
               }
            }
         }
      }

      return wordReplacements;
   }

   private void saveAll() {
      this._plugin.saveConfig();
   }

   public Boolean isDebug() {
      return this._debug;
   }

   public void setDebug(Boolean debug) {
      this._debug = debug;
      this._file.set("Debug", debug);
      this._plugin.saveConfig();
   }

   public float getExpBottleRatio() {
      return this._exp_bottle_ratio;
   }

   public void setExpBottleRatio(float ratio) {
      this._exp_bottle_ratio = ratio;
      this._file.set("ExpBottleRatio", ratio);
      this._plugin.saveConfig();
   }

   public Boolean getCombineExpOrbs() {
      return this._combine_exp_orbs_enable;
   }

   public void setCombineExpOrbs(Boolean combine) {
      this._combine_exp_orbs_enable = combine;
      this._file.set("CombineExpOrbs.Enable", combine);
      this._plugin.saveConfig();
   }

   public Float getCombineExpOrbsRadius() {
      return this._combine_exp_orbs_radius;
   }

   public void setCombineExpOrbsRadius(Float radius) {
      this._combine_exp_orbs_radius = radius;
      this._file.set("CombineExpOrbs.Radius", radius);
      this._plugin.saveConfig();
   }

   public Boolean getNoExpCoolDown() {
      return this._no_exp_cool_down;
   }

   public void setNoExpCoolDown(Boolean no_cool_down) {
      this._no_exp_cool_down = no_cool_down;
      this._file.set("NoExpCoolDown", no_cool_down);
      this._plugin.saveConfig();

      for(Player player : EssentialsD.instance.getServer().getOnlinePlayers()) {
         if (EssentialsD.config.getNoExpCoolDown()) {
            player.setExpCooldown(0);
         } else {
            player.setExpCooldown(1);
         }
      }

   }

   public Integer getChunkOperateDelay() {
      return this._chunk_operate_delay;
   }

   public void setChunkOperateDelay(Integer delay) {
      this._chunk_operate_delay = delay;
      this._file.set("ChunkOperateDelay", delay);
      this._plugin.saveConfig();
   }

   public Integer getTpDelay() {
      return this._tp_delay;
   }

   public Integer getTpCoolDown() {
      return this._tp_cool_down;
   }

   public Integer getTpTpaExpire() {
      return this._tp_tpa_expire;
   }

   public Integer getTpRtpRadius() {
      return this._tp_rtp_radius;
   }

   public List<String> getTpWorldBlackList() {
      return this._tp_world_blacklist;
   }

   public Boolean getChairEnable() {
      return this._chair_enable;
   }

   public Integer getChairMaxWidth() {
      return this._chair_max_width;
   }

   public Boolean getChairSignCheck() {
      return this._chair_sign_check;
   }

   public Float getChairSitHeight() {
      return this._chair_sit_height;
   }

   public Boolean getRecipesCrowbar() {
      return this._recipes_crowbar;
   }

   public Boolean getRecipesInvisibleItemFrame() {
      return this._recipes_invisible_item_frame;
   }

   public Boolean getRecipesLightBlock() {
      return this._recipes_light_block;
   }

   public Boolean getRecipesStackedEnchantBook() {
      return this._recipes_stacked_enchant_book;
   }

   public void ApplyForceLoadChunks() {
      if (this._chunk_operate_delay < 0) {
         XLogger.info("加载区块操作已禁用");
      } else {
         Scheduler.runTaskLater(() -> {
            int count = 0;

            for(World world : EssentialsD.instance.getServer().getWorlds()) {
               XLogger.debug("清除所有强加载区块: " + world.getName());

               for(Chunk chunk : world.getForceLoadedChunks()) {
                  ++count;
                  world.setChunkForceLoaded(chunk.getX(), chunk.getZ(), false);
               }
            }

            XLogger.info("清除所有强加载区块: " + count);

            for(String s : this._force_load_chunks) {
               String[] split = s.split(":");
               if (split.length != 3) {
                  XLogger.warn("ForceLoadChunks 配置错误: " + s);
               } else {
                  String world_name = split[0];
                  int x = Integer.parseInt(split[1]);
                  int z = Integer.parseInt(split[2]);
                  World world = this._plugin.getServer().getWorld(world_name);
                  if (world == null) {
                     XLogger.warn("ForceLoadChunks 配置错误: 世界 " + world_name + " 不存在");
                  } else {
                     world.setChunkForceLoaded(x, z, true);
                     XLogger.info("标记强加载区块: " + world_name + " " + x + " " + z);
                  }
               }
            }

         }, this._chunk_operate_delay * 20);
      }
   }

   public Integer getHomeLimitAmount() {
      return this._home_limit_amount;
   }

   public List<String> getHomeWorldBlacklist() {
      return this._home_world_blacklist;
   }

   public String getDbType() {
      return this._db_type;
   }

   public void setDbType(String db_type) {
      this._db_type = db_type;
      this._file.set("Database.Type", db_type);
      this._plugin.saveConfig();
   }

   public String getDbHost() {
      return this._db_host;
   }

   public String getDbPort() {
      return this._db_port;
   }

   public String getDbName() {
      return this._db_name;
   }

   public void setDbUser(String db_user) {
      this._db_user = db_user;
      this._file.set("Database.User", db_user);
      this._plugin.saveConfig();
   }

   public String getDbUser() {
      if (this._db_user.contains("@")) {
         this.setDbUser("'" + this._db_user + "'");
      }

      return this._db_user;
   }

   public void setDbPass(String db_pass) {
      this._db_pass = db_pass;
      this._file.set("Database.Pass", db_pass);
      this._plugin.saveConfig();
   }

   public String getDbPass() {
      if (this._db_pass.contains("@")) {
         this.setDbPass("'" + this._db_pass + "'");
      }

      return this._db_pass;
   }
}
