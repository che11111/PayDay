package dev.che1s.payday;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Beta 1.7.3 clients can only render English, so game.yml is always English.
 * Console messages use console-<language>.yml selected server-wide by config
 * or the JVM locale.
 */
final class LanguageService {

    static final List<String> SUPPORTED = Collections.unmodifiableList(Arrays.asList(
            "en", "zh_cn", "zh_tw", "ja", "ko", "de", "fr", "es", "ru"));

    private final PayDayPlugin plugin;
    private Configuration game;
    private Configuration console;
    private String gamePrefix = "";
    private String consolePrefix = "[PayDay]";
    private String consoleLanguage = "en";

    LanguageService(PayDayPlugin plugin) {
        this.plugin = plugin;
    }

    void load() {
        File folder = new File(plugin.getDataFolder(), "lang");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getServer().getLogger().warning(
                    "Could not create language directory: " + folder.getAbsolutePath());
        }

        ensureFile(folder, "game.yml");
        for (String language : SUPPORTED) {
            ensureFile(folder, "console-" + language + ".yml");
        }

        ConsoleSelection selection = consoleSelection();
        consoleLanguage = selection.language;
        ConfigurationNode languageNode = plugin.getConfiguration().getNode("language");
        String gameFile = languageNode == null
                ? "game.yml"
                : languageNode.getString("game", "game.yml");
        if (gameFile == null || !"game.yml".equalsIgnoreCase(gameFile.trim())) {
            plugin.getServer().getLogger().warning(
                    "Beta 1.7.3 in-game language is fixed to game.yml.");
        }
        game = load(new File(folder, "game.yml"));
        console = load(new File(folder, "console-" + consoleLanguage + ".yml"));
        if (console == null && !"en".equals(consoleLanguage)) {
            console = load(new File(folder, "console-en.yml"));
            consoleLanguage = "en";
        }
        if (game == null) {
            game = new Configuration(new File(folder, "game.yml"));
            game.load();
        }
        if (console == null) {
            console = game;
        }
        gamePrefix = color(game.getString("prefix", ""));
        consolePrefix = color(console.getString("prefix", "[PayDay]"));
    }

    String game(String key, String fallback, String... replacements) {
        String value = gameValue(key, fallback, replacements);
        return gamePrefix.isEmpty() ? value : gamePrefix + " " + value;
    }

    String gameValue(String key, String fallback, String... replacements) {
        String value = game == null ? fallback : game.getString(key, fallback);
        return color(replace(value, replacements));
    }

    String console(String key, String fallback, String... replacements) {
        return consolePrefix + " " + consoleValue(key, fallback, replacements);
    }

    String consoleValue(String key, String fallback, String... replacements) {
        String value = console == null ? fallback : console.getString(key, fallback);
        return color(replace(value, replacements));
    }

    void send(CommandSender sender, String key, String fallback, String... replacements) {
        sender.sendMessage(sender instanceof Player
                ? game(key, fallback, replacements)
                : console(key, fallback, replacements));
    }

    void info(String key, String fallback, String... replacements) {
        plugin.getServer().getLogger().info(
                plain(console(key, fallback, replacements)));
    }

    void warning(String key, String fallback, String... replacements) {
        plugin.getServer().getLogger().warning(
                plain(console(key, fallback, replacements)));
    }

    void severe(String key, String fallback, String... replacements) {
        plugin.getServer().getLogger().severe(
                plain(console(key, fallback, replacements)));
    }

    private ConsoleSelection consoleSelection() {
        ConfigurationNode languageNode = plugin.getConfiguration().getNode("language");
        String configured = languageNode == null
                ? null
                : languageNode.getString("console", null);
        if (configured == null || configured.trim().isEmpty()) {
            configured = plugin.getConfiguration().getString("language", "auto");
        }
        if (configured == null || configured.trim().isEmpty()
                || "auto".equalsIgnoreCase(configured.trim())) {
            return new ConsoleSelection(normalizeLocale(Locale.getDefault().toString()));
        }

        String normalized = configured.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_');
        int slash = Math.max(normalized.lastIndexOf('/'),
                normalized.lastIndexOf('\\'));
        if (slash >= 0) {
            normalized = normalized.substring(slash + 1);
        }
        if (normalized.endsWith(".yml")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.startsWith("console-")) {
            normalized = normalized.substring("console-".length());
        }
        if (SUPPORTED.contains(normalized)) {
            return new ConsoleSelection(normalized);
        }
        plugin.getServer().getLogger().warning(
                "Unsupported console language '" + configured + "', falling back to English.");
        return new ConsoleSelection("en");
    }

    static String normalizeLocale(String locale) {
        if (locale == null || locale.trim().isEmpty()) {
            return "en";
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT).replace('-', '_');
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

    private Configuration load(File file) {
        if (!file.isFile()) {
            return null;
        }
        Configuration configuration = new Configuration(file);
        configuration.load();
        return configuration;
    }

    private String replace(String value, String... replacements) {
        String result = value == null ? "" : value;
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            result = result.replace(
                    "{" + replacements[index] + "}",
                    replacements[index + 1] == null ? "" : replacements[index + 1]);
        }
        return result;
    }

    private String color(String value) {
        String input = value == null ? "" : value;
        StringBuilder output = new StringBuilder();
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (character == '&' && index + 1 < input.length()) {
                int code = Character.digit(input.charAt(index + 1), 16);
                if (code >= 0) {
                    output.append(ChatColor.getByCode(code));
                    index++;
                    continue;
                }
            }
            output.append(character);
        }
        return output.toString();
    }

    private String plain(String value) {
        StringBuilder output = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\u00A7') {
                if (index + 1 < value.length()) {
                    index++;
                }
            } else {
                output.append(character);
            }
        }
        return output.toString();
    }

    private void ensureFile(File folder, String name) {
        File target = new File(folder, name);
        if (target.isFile()) {
            return;
        }

        InputStream input = null;
        OutputStream output = null;
        try {
            input = plugin.getClass().getClassLoader()
                    .getResourceAsStream("lang/" + name);
            if (input != null) {
                output = new FileOutputStream(target);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
            }
        } catch (IOException exception) {
            plugin.getServer().getLogger().warning(
                    "Could not create lang/" + name + ": " + exception.getMessage());
        } finally {
            close(input);
            close(output);
        }
    }

    private void close(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Nothing useful can be done while copying bundled resources.
        }
    }

    private static final class ConsoleSelection {
        private final String language;

        private ConsoleSelection(String language) {
            this.language = language;
        }
    }
}
