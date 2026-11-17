package dev.che1s.payday;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Uses Folia's entity scheduler when available and Bukkit's scheduler otherwise.
 */
final class SchedulerCompat {

    private static final Method FOLIA_RUN = findFoliaRunMethod();

    private SchedulerCompat() {
    }

    static void runForPlayer(JavaPlugin plugin, Player player, Runnable task) {
        if (FOLIA_RUN != null) {
            try {
                FOLIA_RUN.invoke(null, plugin, player, task);
            } catch (IllegalAccessException | InvocationTargetException exception) {
                plugin.getServer().getLogger().severe(
                        "Could not schedule a Folia player task: "
                        + exception.getMessage());
            }
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    private static Method findFoliaRunMethod() {
        try {
            ClassLoader loader = SchedulerCompat.class.getClassLoader();
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false, loader);
            Class<?> scheduler = Class.forName(
                    "dev.che1s.payday.FoliaScheduler", true, loader);
            Method method = scheduler.getDeclaredMethod(
                    "runForPlayer", JavaPlugin.class, Player.class, Runnable.class);
            method.setAccessible(true);
            return method;
        } catch (ClassNotFoundException | LinkageError | NoSuchMethodException
                 | SecurityException exception) {
            return null;
        }
    }
}
