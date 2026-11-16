package dev.che1s.payday;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Folia entity scheduler adapter.
 *
 * This class is loaded reflectively only when Folia is detected, so the plugin
 * can still run on legacy Bukkit servers that do not have the Folia API.
 */
final class FoliaScheduler {

    private FoliaScheduler() {
    }

    static void runForPlayer(JavaPlugin plugin, Player player, Runnable task) {
        player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
    }
}
