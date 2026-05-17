package et.xiaomu.essentialsd.services;

import cn.lunadeer.utils.XLogger;
import et.xiaomu.essentialsd.EssentialsD;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LocalizationService {
    private static final String DEFAULT_LOCALE = "zh_CN";
    private static final Set<String> BUNDLED_LOCALES = Set.of(DEFAULT_LOCALE, "en_US");

    private final EssentialsD plugin;
    private final Set<String> missingKeysLogged = ConcurrentHashMap.newKeySet();
    private YamlConfiguration currentLocale;
    private YamlConfiguration fallbackLocale;
    private String locale = DEFAULT_LOCALE;

    public LocalizationService(EssentialsD plugin) {
        this.plugin = plugin;
        reload();
    }

    public synchronized void reload() {
        ensureBundledLocaleFiles();
        missingKeysLogged.clear();

        fallbackLocale = loadLocaleConfiguration(DEFAULT_LOCALE);
        String configuredLocale = plugin.getConfig().getString("Locale", DEFAULT_LOCALE);
        if (configuredLocale == null || configuredLocale.isBlank()) {
            configuredLocale = DEFAULT_LOCALE;
        }

        locale = configuredLocale;
        if (DEFAULT_LOCALE.equalsIgnoreCase(configuredLocale)) {
            currentLocale = fallbackLocale;
            return;
        }

        File localeFile = new File(getLocalesDirectory(), configuredLocale + ".yml");
        if (!localeFile.exists()) {
            XLogger.warn("未找到语言文件 locales/%s.yml，已回退到 %s", configuredLocale, DEFAULT_LOCALE);
            currentLocale = fallbackLocale;
            return;
        }
        currentLocale = YamlConfiguration.loadConfiguration(localeFile);
    }

    public String getLocale() {
        return locale;
    }

    public String get(String key) {
        String value = currentLocale == null ? null : currentLocale.getString(key);
        if (value != null) {
            return value;
        }

        value = fallbackLocale == null ? null : fallbackLocale.getString(key);
        if (value != null) {
            warnMissingKeyOnce(key, locale);
            return value;
        }

        warnMissingKeyOnce(key, DEFAULT_LOCALE);
        return key;
    }

    public String format(String key, Object... args) {
        String template = get(key);
        if (args == null || args.length == 0) {
            return template;
        }
        return String.format(template, args);
    }

    private void ensureBundledLocaleFiles() {
        File localesDir = getLocalesDirectory();
        if (!localesDir.exists() && !localesDir.mkdirs()) {
            XLogger.warn("无法创建语言目录: %s", localesDir.getAbsolutePath());
            return;
        }

        for (String bundledLocale : BUNDLED_LOCALES) {
            File localeFile = new File(localesDir, bundledLocale + ".yml");
            if (!localeFile.exists()) {
                plugin.saveResource("locales/" + bundledLocale + ".yml", false);
            }
        }
    }

    private File getLocalesDirectory() {
        return new File(plugin.getDataFolder(), "locales");
    }

    private YamlConfiguration loadLocaleConfiguration(String localeCode) {
        File localeFile = new File(getLocalesDirectory(), localeCode + ".yml");
        if (!localeFile.exists()) {
            XLogger.warn("未找到默认语言文件 locales/%s.yml", localeCode);
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(localeFile);
    }

    private void warnMissingKeyOnce(String key, String sourceLocale) {
        if (missingKeysLogged.add(key)) {
            XLogger.warn("语言键缺失: %s (locale=%s)", key, sourceLocale);
        }
    }
}
