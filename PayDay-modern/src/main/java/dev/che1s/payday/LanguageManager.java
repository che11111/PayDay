package dev.che1s.payday;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Uses one server-wide language selected from the JVM locale or config.
 */
final class LanguageManager {

    static final List<String> SUPPORTED = Collections.unmodifiableList(Arrays.asList(
            "en", "zh_cn", "ja", "ko", "zh_tw", "de", "fr", "es", "ru"));

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private final JavaPlugin plugin;
    private final Map<String, YamlConfiguration> translations =
            new LinkedHashMap<String, YamlConfiguration>();
    private String configuredLanguage = "en";

    LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        translations.clear();
        for (String language : SUPPORTED) {
            String resourcePath = "lang/" + language + ".yml";
            File file = new File(new File(plugin.getDataFolder(), "lang"),
                    language + ".yml");
            if (!file.isFile()) {
                plugin.saveResource(resourcePath, false);
            }

            YamlConfiguration current = YamlConfiguration.loadConfiguration(file);
            YamlConfiguration bundled = loadBundled(resourcePath);
            if (bundled != null) {
                current.setDefaults(bundled);
            }
            translations.put(language, current);
        }
        configuredLanguage = resolveConfigured(
                plugin.getConfig().getString("language", "auto"));
    }

    String get(CommandSender sender, String key, String... replacements) {
        return value(configuredLanguage, key, replacements);
    }

    String player(Player player, String key, String... replacements) {
        return get(player, key, replacements);
    }

    String console(String key, String... replacements) {
        return value(configuredLanguage, key, replacements);
    }

    String language(CommandSender sender) {
        return configuredLanguage;
    }

    private String value(String language, String key, String... replacements) {
        YamlConfiguration config = translations.get(language);
        String message = config == null ? null : config.getString(key);
        if (message == null) {
            YamlConfiguration fallback = translations.get("en");
            message = fallback == null ? null : fallback.getString(key);
        }
        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + key);
            message = key;
        }
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(
                    "{" + replacements[index] + "}",
                    replacements[index + 1] == null ? "" : replacements[index + 1]);
        }
        return color(message);
    }

    private String color(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1).toUpperCase(Locale.ROOT);
            StringBuilder legacy = new StringBuilder("§x");
            for (char character : hex.toCharArray()) {
                legacy.append('§').append(character);
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(legacy.toString()));
        }
        matcher.appendTail(output);
        return ChatColor.translateAlternateColorCodes('&', output.toString());
    }

    private String resolveConfigured(String configured) {
        if (configured == null || configured.trim().isEmpty()
                || "auto".equalsIgnoreCase(configured.trim())) {
            return normalizeLocale(Locale.getDefault().toString());
        }

        String normalized = configured.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_');
        if (normalized.endsWith(".yml")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (SUPPORTED.contains(normalized)) {
            return normalized;
        }

        plugin.getLogger().warning("Unsupported language '" + configured
                + "', falling back to English.");
        return "en";
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.trim().isEmpty()) {
            return "en";
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_');
        if (SUPPORTED.contains(normalized)) {
            return normalized;
        }
        if (normalized.startsWith("zh")) {
            return normalized.contains("tw")
                    || normalized.contains("hk")
                    || normalized.contains("mo")
                    || normalized.contains("hant")
                    ? "zh_tw" : "zh_cn";
        }
        int separator = normalized.indexOf('_');
        String base = separator < 0 ? normalized : normalized.substring(0, separator);
        return SUPPORTED.contains(base) ? base : "en";
    }

    private YamlConfiguration loadBundled(String resourcePath) {
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) {
            plugin.getLogger().warning("Missing bundled language file: " + resourcePath);
            return null;
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not read language file: " + resourcePath);
            return null;
        }
    }
}
