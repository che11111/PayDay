package dev.che1s.payday;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
                copyBundledResource(resourcePath, file);
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
            plugin.getServer().getLogger().warning("Missing translation key: " + key);
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
        return translateAlternateColorCodes(output.toString());
    }

    private String translateAlternateColorCodes(String message) {
        StringBuilder translated = new StringBuilder(message.length());
        for (int index = 0; index < message.length(); index++) {
            char character = message.charAt(index);
            if (character == '&' && index + 1 < message.length()) {
                ChatColor color = colorForCode(message.charAt(index + 1));
                if (color != null) {
                    translated.append(color);
                    index++;
                    continue;
                }
            }
            translated.append(character);
        }
        return translated.toString();
    }

    private ChatColor colorForCode(char code) {
        switch (Character.toLowerCase(code)) {
            case '0': return ChatColor.BLACK;
            case '1': return ChatColor.DARK_BLUE;
            case '2': return ChatColor.DARK_GREEN;
            case '3': return ChatColor.DARK_AQUA;
            case '4': return ChatColor.DARK_RED;
            case '5': return ChatColor.DARK_PURPLE;
            case '6': return ChatColor.GOLD;
            case '7': return ChatColor.GRAY;
            case '8': return ChatColor.DARK_GRAY;
            case '9': return ChatColor.BLUE;
            case 'a': return ChatColor.GREEN;
            case 'b': return ChatColor.AQUA;
            case 'c': return ChatColor.RED;
            case 'd': return ChatColor.LIGHT_PURPLE;
            case 'e': return ChatColor.YELLOW;
            case 'f': return ChatColor.WHITE;
            case 'k': return ChatColor.MAGIC;
            case 'l': return ChatColor.BOLD;
            case 'm': return ChatColor.STRIKETHROUGH;
            case 'n': return ChatColor.UNDERLINE;
            case 'o': return ChatColor.ITALIC;
            case 'r': return ChatColor.RESET;
            default: return null;
        }
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

        plugin.getServer().getLogger().warning("Unsupported language '" + configured
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
        try {
            InputStream stream = plugin.getResource(resourcePath);
            if (stream == null) {
                plugin.getServer().getLogger().warning(
                        "Missing bundled language file: " + resourcePath);
                return null;
            }
            return loadConfiguration(stream);
        } catch (IOException | ReflectiveOperationException exception) {
            plugin.getServer().getLogger().warning(
                    "Could not read language file: " + resourcePath);
            return null;
        }
    }

    private void copyBundledResource(String resourcePath, File target) {
        target.getParentFile().mkdirs();
        try (InputStream input = plugin.getResource(resourcePath)) {
            if (input == null) {
                return;
            }
            try (OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, count);
                }
            }
        } catch (IOException exception) {
            plugin.getServer().getLogger().warning(
                    "Could not create language file " + target.getName() + ": "
                            + exception.getMessage());
        }
    }

    private YamlConfiguration loadConfiguration(InputStream stream)
            throws IOException, ReflectiveOperationException {
        try {
            Method method = YamlConfiguration.class.getMethod(
                    "loadConfiguration", InputStream.class);
            return (YamlConfiguration) method.invoke(null, stream);
        } catch (NoSuchMethodException unavailable) {
            Method method = YamlConfiguration.class.getMethod(
                    "loadConfiguration", Reader.class);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return (YamlConfiguration) method.invoke(null, reader);
            }
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw exception;
        }
    }
}
