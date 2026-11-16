package dev.che1s.payday;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * SQLite history implementation shared by the three PayDay branches.
 */
public class HistoryManager {

    private static final String COUNT_TABLE = "player_counts";

    private final File dataFolder;
    private final Logger log;
    private final Set<String> knownColumns = new HashSet<String>();
    private Connection connection;

    public HistoryManager(File dataFolder, Logger log) {
        this.dataFolder = dataFolder;
        this.log = log;
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        connect();
        createTables();
    }

    public boolean isAvailable() {
        return connection != null;
    }

    public void reload() {
        close();
        knownColumns.clear();
        connect();
        createTables();
    }

    public void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            log.severe("Could not close PayDay SQLite history: " + exception.getMessage());
        } finally {
            connection = null;
        }
    }

    public String getCurrentTimeString() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public synchronized void recordExecution(String playerName, String command,
                                              String timeString) {
        if (connection == null || playerName == null || playerName.isEmpty()) {
            return;
        }
        String player = playerName.toLowerCase();
        String timestamp = timeString == null || timeString.isEmpty()
                ? getCurrentTimeString() : timeString;
        addHistoryRecord(player, "在 " + timestamp + " 执行了 " + command);
        String column = commandToColumn(command);
        ensureColumn(column);
        incrementCount(player, column);
    }

    public synchronized List<String> getHistory(String playerName) {
        List<String> result = new ArrayList<String>();
        if (connection == null || playerName == null) {
            return result;
        }
        String sql = "SELECT entry FROM history WHERE player = ? ORDER BY id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("entry"));
                }
            }
        } catch (SQLException exception) {
            log.severe("Could not read PayDay history: " + exception.getMessage());
        }
        return result;
    }

    public synchronized int getCount(String playerName, String command) {
        if (connection == null || playerName == null) {
            return 0;
        }
        String column = commandToColumn(command);
        ensureColumn(column);
        String sql = "SELECT " + column + " FROM " + COUNT_TABLE + " WHERE player = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            log.severe("Could not read PayDay purchase count: " + exception.getMessage());
            return 0;
        }
    }

    public int getPayDayCount(String playerName) {
        return getCount(playerName, "/payday");
    }

    public int getPayNoonCount(String playerName) {
        return getCount(playerName, "/paynoon");
    }

    public int getPayNightCount(String playerName) {
        return getCount(playerName, "/paynight");
    }

    public int getPayWeatherCount(String playerName) {
        return getCount(playerName, "/payweather");
    }

    private void connect() {
        connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            File database = new File(dataFolder, "history.db");
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + database.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL;");
                statement.execute("PRAGMA busy_timeout=5000;");
            }
        } catch (Exception exception) {
            log.severe("Could not connect to PayDay SQLite history. "
                    + "The server must provide the SQLite JDBC driver: "
                    + exception.getMessage());
        }
    }

    private void createTables() {
        if (connection == null) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + COUNT_TABLE + " ("
                    + "player TEXT PRIMARY KEY, "
                    + "payday_count INTEGER NOT NULL DEFAULT 0, "
                    + "paynoon_count INTEGER NOT NULL DEFAULT 0, "
                    + "paynight_count INTEGER NOT NULL DEFAULT 0, "
                    + "payweather_count INTEGER NOT NULL DEFAULT 0)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "player TEXT NOT NULL, entry TEXT NOT NULL)");
        } catch (SQLException exception) {
            log.severe("Could not create PayDay SQLite tables: " + exception.getMessage());
        }
        for (String column : new String[]{"payday_count", "paynoon_count",
                "paynight_count", "payweather_count"}) {
            knownColumns.add(column);
        }
    }

    private void ensureColumn(String column) {
        if (connection == null || knownColumns.contains(column)) {
            return;
        }
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "PRAGMA table_info(" + COUNT_TABLE + ")")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) {
                    knownColumns.add(column);
                    return;
                }
            }
        } catch (SQLException exception) {
            log.severe("Could not inspect PayDay count columns: " + exception.getMessage());
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + COUNT_TABLE + " ADD COLUMN "
                    + column + " INTEGER NOT NULL DEFAULT 0");
            knownColumns.add(column);
        } catch (SQLException exception) {
            log.severe("Could not add PayDay count column: " + exception.getMessage());
        }
    }

    private void addHistoryRecord(String player, String entry) {
        if (connection == null) {
            return;
        }
        String sql = "INSERT INTO history(player, entry) VALUES(?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, player);
            ps.setString(2, entry);
            ps.executeUpdate();
        } catch (SQLException exception) {
            log.severe("Could not write PayDay history: " + exception.getMessage());
        }
    }

    private void incrementCount(String player, String column) {
        if (connection == null) {
            return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT OR IGNORE INTO " + COUNT_TABLE + "(player) VALUES(?)")) {
            insert.setString(1, player);
            insert.executeUpdate();
        } catch (SQLException exception) {
            log.severe("Could not create PayDay count row: " + exception.getMessage());
            return;
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE " + COUNT_TABLE + " SET " + column + " = " + column
                        + " + 1 WHERE player = ?")) {
            update.setString(1, player);
            update.executeUpdate();
        } catch (SQLException exception) {
            log.severe("Could not update PayDay count: " + exception.getMessage());
        }
    }

    private String commandToColumn(String command) {
        String name = command == null ? "" : command;
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        return safeColumn(name + "_count");
    }

    private String safeColumn(String value) {
        return value.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
