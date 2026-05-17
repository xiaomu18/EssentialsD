package et.xiaomu.essentialsd.managers;

import et.xiaomu.essentialsd.EssentialsD;
import et.xiaomu.essentialsd.utils.MuteDuration;
import cn.lunadeer.utils.XLogger;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {
    private static final String CHAT_CONFIG_NAME = "chat.yml";

    private final EssentialsD _plugin;
    private FileConfiguration _file;
    private FileConfiguration _chatFile;
    private boolean _debug;
    private float _exp_bottle_ratio;
    private Boolean _combine_exp_orbs_enable;
    private Float _combine_exp_orbs_radius;
    private Boolean _no_exp_cool_down;
    private Integer _tp_tpa_expire;
    private Integer _tp_delay;
    private Integer _tp_cool_down;
    private Integer _tp_rtp_cool_down;
    private Integer _show_item_cool_down;
    private Integer _tp_rtp_radius;
    private Boolean _tp_log_player_teleport;
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
    private Boolean _force_vanish_in_different_gamemode;
    private Boolean _vanish_disable_collidable;
    private Boolean _vanish_cancel_container_animation;
    private Boolean _vanish_allow_vanisher_attack_player;
    private Boolean _vanish_block_private_message_to_vanished;
    private Boolean _vanish_enhanced_mode;
    private Boolean _chat_pure_enabled;
    public Boolean chat_func_enable;
    public List<String> forbidWords;
    public Map<String, String> replaceWords;
    public long chat_anti_spam_cooldown_ms;
    public boolean chat_anti_spam_base_on_ip;
    public long chat_rate_limit_duration_ms;
    public int chat_rate_limit_value;
    public boolean chat_repeat_interceptor_enable;
    public int chat_repeat_interceptor_sample_hits;
    public double chat_repeat_interceptor_similarity_threshold;
    public int chat_repeat_interceptor_sample_expiration_ms;
    public boolean chat_self_deception_enable;
    public boolean chat_self_deception_show_to_same_ip_players;
    public long CMD_COOLDOWN_MS;
    public List<String> CMD_BANNED_LIST;
    public Boolean CMD_ENABLE;
    public String allow_minimessage_perm;
    public int chat_max_length;
    public String MUTE_DEFAULT_DURATION;
    public String MUTE_MODE;
    public List<String> MUTE_BLOCKED_COMMANDS;
    private String _locale;

    public List<Material> forbidTakeItems = new ArrayList<>();
    public Boolean forbidNBTItem;

    public ConfigManager(EssentialsD plugin) {
        this._plugin = plugin;
        this._plugin.saveDefaultConfig();
        ensureChatConfigExists();
        this.reload();
    }

    public void reload() {
        reloadConfigOnly();
        reloadChatOnly();
    }

    public void reloadConfigOnly() {
        this._plugin.reloadConfig();
        this._file = this._plugin.getConfig();
        loadMainConfig();
    }

    public void reloadChatOnly() {
        ensureChatConfigExists();
        this._chatFile = YamlConfiguration.loadConfiguration(getChatConfigFile());
        loadChatConfig();
    }

    private void loadMainConfig() {
        this._debug = this._file.getBoolean("Debug", false);
        XLogger.setDebug(this.isDebug());
        this._locale = this._file.getString("Locale", "zh_CN");

        this._exp_bottle_ratio = (float) this._file.getDouble("ExpBottleRatio", 1.0F);
        this._combine_exp_orbs_enable = this._file.getBoolean("CombineExpOrbs.Enable", false);
        this._combine_exp_orbs_radius = (float) this._file.getDouble("CombineExpOrbs.Radius", 1.5F);
        this._no_exp_cool_down = this._file.getBoolean("NoExpCoolDown", false);
        this._tp_delay = this._file.getInt("Teleport.Delay", 0);
        this._tp_cool_down = this._file.getInt("Teleport.CoolDown", 0);
        this._tp_rtp_cool_down = this._file.getInt("Teleport.RtpCoolDown", 0);
        this._show_item_cool_down = Math.max(0, this._file.getInt("show-item-cooldown", 0));
        this._tp_tpa_expire = this._file.getInt("Teleport.TpaExpire", 30);
        this._tp_rtp_radius = this._file.getInt("Teleport.RtpRadius", 1000);
        this._tp_log_player_teleport = this._file.getBoolean("Teleport.log-player-teleport", false);
        this._tp_world_blacklist = this._file.getStringList("Teleport.WorldBlackList");
        this._chair_enable = this._file.getBoolean("Chair.Enable", true);
        this._chair_max_width = this._file.getInt("Chair.MaxWidth", 4);
        this._chair_sign_check = this._file.getBoolean("Chair.SignCheck", true);
        this._chair_sit_height = (float) this._file.getDouble("Chair.SitHeight", -0.95);
        this._recipes_crowbar = this._file.getBoolean("Recipes.CrowBar", true);
        this._recipes_invisible_item_frame = this._file.getBoolean("Recipes.InvisibleItemFrame", true);
        this._recipes_light_block = this._file.getBoolean("Recipes.LightBlock", true);
        this._recipes_stacked_enchant_book = this._file.getBoolean("Recipes.StackedEnchantBook", true);
        this._home_limit_amount = this._file.getInt("HomeLimit.Amount", 5);
        this._home_world_blacklist = this._file.getStringList("HomeLimit.WorldBlackList");
        if (this._home_world_blacklist.isEmpty()) {
            this._home_world_blacklist = this._file.getStringList("HomeLimit.WorldBlacklist");
        }
        if (this._file.contains("vanish.force-in-different-gamemode")) {
            this._force_vanish_in_different_gamemode = this._file.getBoolean("vanish.force-in-different-gamemode", false);
        } else {
            this._force_vanish_in_different_gamemode = this._file.getBoolean("force-vanish-in-different-gamemode", false);
        }
        if (this._file.contains("vanish.disable-collidable")) {
            this._vanish_disable_collidable = this._file.getBoolean("vanish.disable-collidable", true);
        } else {
            this._vanish_disable_collidable = this._file.getBoolean("Vanish.DisableCollidable", true);
        }
        this._vanish_cancel_container_animation = this._file.getBoolean("vanish.cancel-container-animation", false);
        this._vanish_allow_vanisher_attack_player = this._file.getBoolean("vanish.allow-vanisher-attack-player", true);
        this._vanish_block_private_message_to_vanished = this._file.getBoolean("vanish.block-private-message-to-vanished", false);
        this._vanish_enhanced_mode = this._file.getBoolean("vanish.enhanced-mode", false);

        this.CMD_ENABLE = this._file.getBoolean("command-manager.Enable", false);
        this.CMD_COOLDOWN_MS = this._file.getLong("command-manager.cooldown", 2000);
        this.CMD_BANNED_LIST = normalizeCommands(this._file.getStringList("command-manager.banned_command"));
        List<String> forbidItemNames = this._file.getStringList("Creative-ItemTake.banned-item");

        forbidTakeItems.clear();
        forbidItemNames.forEach(itemName -> {
            forbidTakeItems.add(Material.getMaterial(itemName));
        });
        this.forbidNBTItem = this._file.getBoolean("Creative-ItemTake.banned-nbt");

        this._db_type = this._file.getString("Database.Type", "sqlite");
        if (!this._db_type.equals("pgsql") && !this._db_type.equals("sqlite")) {
            XLogger.error("当前数据库只支持 pgsql 或 sqlite，已重置为 sqlite");
            this.setDbType("sqlite");
        }

        this._db_host = this._file.getString("Database.Host", "localhost");
        this._db_port = this._file.getString("Database.Port", "5432");
        this._db_name = this._file.getString("Database.Name", "dominion");
        this._db_user = this._file.getString("Database.User", "postgres");
        this._db_pass = this._file.getString("Database.Pass", "postgres");
    }

    private void loadChatConfig() {
        this.chat_func_enable = this._chatFile.getBoolean("Enable", false);
        this._chat_pure_enabled = this._chatFile.getBoolean("pure.enable", false);
        this.allow_minimessage_perm = this._chatFile.getString("allow-minimessage-perm", "essd.chat.allow-use-minimessage");

        this.chat_anti_spam_cooldown_ms = Math.max(0L, getChatLong("anti-spam.cooldown", "cooldown", 2000L));
        this.chat_anti_spam_base_on_ip = this._chatFile.getBoolean("anti-spam.base-on-ip", false);
        this.chat_rate_limit_duration_ms = getChatDurationMillis("anti-spam.rate-limit.duration", 300.0D);
        this.chat_rate_limit_value = Math.max(0, this._chatFile.getInt("anti-spam.rate-limit.value", 0));
        this.chat_repeat_interceptor_enable = this._chatFile.getBoolean("anti-spam.repeat-interceptor.enable", false);
        this.chat_repeat_interceptor_sample_hits = Math.max(1, this._chatFile.getInt("anti-spam.repeat-interceptor.sample-hits", 5));
        this.chat_repeat_interceptor_similarity_threshold = clampThreshold(
                this._chatFile.getDouble("anti-spam.repeat-interceptor.similarity-threshold", 0.9D),
                "chat.yml anti-spam.repeat-interceptor.similarity-threshold"
        );
        this.chat_repeat_interceptor_sample_expiration_ms = Math.max(0, this._chatFile.getInt("anti-spam.repeat-interceptor.sample-expiration", 300000));
        this.chat_max_length = this._chatFile.getInt("max-length", 256);

        this.forbidWords = this._chatFile.getStringList("forbid-keywords").stream()
                .map(word -> word == null ? "" : word.trim().toLowerCase(Locale.ROOT))
                .filter(word -> !word.isBlank())
                .toList();
        this.replaceWords = getReplaceWords(this._chatFile);
        this.chat_self_deception_enable = getLegacyBooleanOrNested("self-deception-mode.enable", "self-deception-mode", false);
        this.chat_self_deception_show_to_same_ip_players = this._chatFile.getBoolean("self-deception-mode.show-to-same-ip-players", false);
        this.MUTE_DEFAULT_DURATION = this._chatFile.getString("mute.default-duration", "permanent");
        if (!MuteDuration.parse(this.MUTE_DEFAULT_DURATION).isValid()) {
            XLogger.warn("chat.yml mute.default-duration 配置无效，已回退为 permanent");
            this.MUTE_DEFAULT_DURATION = "permanent";
        }
        this.MUTE_MODE = normalizeMuteMode(this._chatFile.getString("mute.mode", "block"));
        this.MUTE_BLOCKED_COMMANDS = normalizeCommands(this._chatFile.getStringList("mute.blocked-commands"));
    }

    public FileConfiguration getConfig() {
        return this._file;
    }

    public FileConfiguration getChatConfig() {
        return this._chatFile;
    }

    private void ensureChatConfigExists() {
        if (!getChatConfigFile().exists()) {
            this._plugin.saveResource(CHAT_CONFIG_NAME, false);
        }
    }

    private File getChatConfigFile() {
        return new File(this._plugin.getDataFolder(), CHAT_CONFIG_NAME);
    }

    private Map<String, String> getReplaceWords(FileConfiguration file) {
        Map<String, String> wordReplacements = new HashMap<>();

        // 获取 replace-words 列表
        List<?> replaceList = file.getList("replace-words", new ArrayList<>());

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

    private long getChatLong(String primaryPath, String legacyPath, long defaultValue) {
        if (this._chatFile.contains(primaryPath)) {
            return this._chatFile.getLong(primaryPath, defaultValue);
        }
        if (legacyPath != null && this._chatFile.contains(legacyPath)) {
            return this._chatFile.getLong(legacyPath, defaultValue);
        }
        return defaultValue;
    }

    private long getChatDurationMillis(String path, double defaultSeconds) {
        double seconds = this._chatFile.getDouble(path, defaultSeconds);
        if (seconds < 0.0D) {
            XLogger.warn("%s 配置不能小于 0，已回退为 0", path);
            return 0L;
        }
        return Math.round(seconds * 1000.0D);
    }

    private boolean getLegacyBooleanOrNested(String primaryPath, String legacyBooleanPath, boolean defaultValue) {
        if (this._chatFile.contains(primaryPath)) {
            return this._chatFile.getBoolean(primaryPath, defaultValue);
        }
        if (legacyBooleanPath != null && this._chatFile.isBoolean(legacyBooleanPath)) {
            return this._chatFile.getBoolean(legacyBooleanPath, defaultValue);
        }
        return defaultValue;
    }

    private double clampThreshold(double value, String path) {
        if (value < 0.0D || value > 1.0D) {
            XLogger.warn("%s 配置超出范围，已限制到 0.0-1.0", path);
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private String normalizeMuteMode(String mode) {
        if (mode == null) {
            return "block";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("2") || normalized.equals("self_deception")) {
            return "self-deception";
        }
        if (!normalized.equals("block") && !normalized.equals("1") && !normalized.equals("self-deception")) {
            XLogger.warn("chat.yml mute.mode 配置无效，已回退为 block");
            return "block";
        }
        return normalized.equals("1") ? "block" : normalized;
    }

    private List<String> normalizeCommands(List<String> commands) {
        List<String> normalized = new ArrayList<>();
        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String value = command.trim().toLowerCase(Locale.ROOT);
            while (value.startsWith("/")) {
                value = value.substring(1);
            }
            int namespaceIndex = value.indexOf(':');
            if (namespaceIndex >= 0 && namespaceIndex + 1 < value.length()) {
                value = value.substring(namespaceIndex + 1);
            }
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
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

        for (Player player : EssentialsD.instance.getServer().getOnlinePlayers()) {
            if (EssentialsD.config.getNoExpCoolDown()) {
                player.setExpCooldown(0);
            } else {
                player.setExpCooldown(1);
            }
        }

    }
    public Integer getTpDelay() {
        return this._tp_delay;
    }

    public Integer getTpCoolDown() {
        return this._tp_cool_down;
    }

    public Integer getTpRtpCoolDown() {
        return this._tp_rtp_cool_down;
    }

    public Integer getShowItemCoolDown() {
        return this._show_item_cool_down;
    }

    public Integer getTpTpaExpire() {
        return this._tp_tpa_expire;
    }

    public Integer getTpRtpRadius() {
        return this._tp_rtp_radius;
    }

    public Boolean getTpLogPlayerTeleport() {
        return this._tp_log_player_teleport;
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

    public Boolean getForceVanishInDifferentGamemode() {
        return this._force_vanish_in_different_gamemode;
    }

    public Boolean getVanishDisableCollidable() {
        return this._vanish_disable_collidable;
    }

    public Boolean getVanishCancelContainerAnimation() {
        return this._vanish_cancel_container_animation;
    }

    public Boolean getVanishAllowVanisherAttackPlayer() {
        return this._vanish_allow_vanisher_attack_player;
    }

    public Boolean getVanishBlockPrivateMessageToVanished() {
        return this._vanish_block_private_message_to_vanished;
    }

    public Boolean getVanishEnhancedMode() {
        return this._vanish_enhanced_mode;
    }

    public Boolean getChatPureEnabled() {
        return this._chat_pure_enabled;
    }

    public String getLocale() {
        return this._locale;
    }
}
