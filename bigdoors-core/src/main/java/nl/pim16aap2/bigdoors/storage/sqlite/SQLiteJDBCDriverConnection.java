package nl.pim16aap2.bigdoors.storage.sqlite;

import com.google.common.io.Files;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.spigotutil.DoorOwner;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("null") // Eclipse likes to complain about connections potentially being null,
// but it's not a problem.
public class SQLiteJDBCDriverConnection
{
    private final BigDoors plugin;
    private final File dbFile;
    private final String url;
    private static final String DRIVER = "org.sqlite.JDBC";
    private static final int DATABASE_VERSION = 5;

    private static final int DOOR_ID = 1;
    private static final int DOOR_NAME = 2;
    private static final int DOOR_WORLD = 3;
    private static final int DOOR_OPEN = 4;
    private static final int DOOR_MIN_X = 5;
    private static final int DOOR_MIN_Y = 6;
    private static final int DOOR_MIN_Z = 7;
    private static final int DOOR_MAX_X = 8;
    private static final int DOOR_MAX_Y = 9;
    private static final int DOOR_MAX_Z = 10;
    private static final int DOOR_ENG_X = 11;
    private static final int DOOR_ENG_Y = 12;
    private static final int DOOR_ENG_Z = 13;
    private static final int DOOR_LOCKED = 14;
    private static final int DOOR_TYPE = 15;
    private static final int DOOR_ENG_SIDE = 16;
    private static final int DOOR_POWER_X = 17;
    private static final int DOOR_POWER_Y = 18;
    private static final int DOOR_POWER_Z = 19;
    private static final int DOOR_OPEN_DIR = 20;
    private static final int DOOR_AUTO_CLOSE = 21;
    private static final int DOOR_CHUNK_HASH = 22;
    private static final int DOOR_BLOCKS_TO_MOVE = 23;

    private static final int PLAYERS_ID = 1;
    private static final int PLAYERS_UUID = 2;
    private static final int PLAYERS_NAME = 3;

    @SuppressWarnings("unused")
    private static final int UNION_ID = 1;
    private static final int UNION_PERM = 2;
    private static final int UNION_PLAYER_ID = 3;
    private static final int UNION_DOOR_ID = 4;

    private final String dbName;
    private boolean enabled = true;
    private AtomicBoolean locked = new AtomicBoolean(false);
    private static final String FAKEUUID = "0000";

    public SQLiteJDBCDriverConnection(final BigDoors plugin, final String dbName)
    {
        this.plugin = plugin;
        this.dbName = dbName;
        dbFile = new File(plugin.getDataFolder(), dbName);
        url = "jdbc:sqlite:" + dbFile;
        init();
        upgrade();
    }

    // Establish a connection.
    private Connection getConnection()
    {
        if (!enabled)
        {
            plugin.getPLogger()
                  .severe("Database disabled! This probably means an upgrade failed! Please contact pim16aap2.");
            return null;
        }
        if (locked.get())
        {
            plugin.getPLogger()
                  .severe("Database locked! Please try again later! Please contact pim16aap2 if the issue persists.");
            return null;
        }
        Connection conn = null;
        try
        {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys=ON");
        }
        catch (SQLException ex)
        {
            plugin.getPLogger().severe("53: Failed to open connection!");
        }
        catch (ClassNotFoundException e)
        {
            plugin.getPLogger().severe("57: Failed to open connection: CLass not found!!");
        }
        return conn;
    }

    // Initialize the tables.
    private void init()
    {
        if (!dbFile.exists())
            try
            {
                dbFile.createNewFile();
                plugin.getPLogger().warn("New file created at " + dbFile);
            }
            catch (IOException e)
            {
                plugin.getPLogger().warn("File write error: " + dbFile);
            }

        // Table creation
        Connection conn = null;
        try
        {
            conn = getConnection();

            // Check if the doors table already exists. If it does, assume the rest exists
            // as well
            // and don't set it up.
            if (!conn.getMetaData().getTables(null, null, "doors", new String[]{"TABLE"}).next())
            {
                Statement stmt1 = conn.createStatement();
                String sql1 = "CREATE TABLE IF NOT EXISTS doors "
                        + "(id            INTEGER    PRIMARY KEY autoincrement, " +
                        " name          TEXT       NOT NULL, "
                        + " world         TEXT       NOT NULL, " + " isOpen        INTEGER    NOT NULL, "
                        + " xMin          INTEGER    NOT NULL, " + " yMin          INTEGER    NOT NULL, "
                        + " zMin          INTEGER    NOT NULL, " + " xMax          INTEGER    NOT NULL, "
                        + " yMax          INTEGER    NOT NULL, " + " zMax          INTEGER    NOT NULL, "
                        + " engineX       INTEGER    NOT NULL, " + " engineY       INTEGER    NOT NULL, "
                        + " engineZ       INTEGER    NOT NULL, " + " isLocked      INTEGER    NOT NULL, "
                        + " type          INTEGER    NOT NULL, " + " engineSide    INTEGER    NOT NULL, "
                        + " powerBlockX   INTEGER    NOT NULL, " + " powerBlockY   INTEGER    NOT NULL, "
                        + " powerBlockZ   INTEGER    NOT NULL, " + " openDirection INTEGER    NOT NULL, "
                        + " autoClose     INTEGER    NOT NULL, " + " chunkHash     INTEGER    NOT NULL, "
                        + " blocksToMove  INTEGER    NOT NULL) ";
                stmt1.executeUpdate(sql1);
                stmt1.close();

                Statement stmt2 = conn.createStatement();
                String sql2 = "CREATE TABLE IF NOT EXISTS players "
                        + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, " + " playerUUID  TEXT       NOT NULL,"
                        + " playerName  TEXT       NOT NULL)";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                Statement stmt3 = conn.createStatement();
                String sql3 = "CREATE TABLE IF NOT EXISTS sqlUnion "
                        + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, " + " permission  INTEGER    NOT NULL, "
                        + " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE, "
                        + " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE,"
                        + " unique (playerID, doorUID))";
                stmt3.executeUpdate(sql3);
                stmt3.close();
                setDBVersion(conn, DATABASE_VERSION);
            }
        }
        catch (SQLException e)
        {
            logMessage("203", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("213", e);
            }
            catch (Exception e)
            {
                logMessage("217", e);
            }
        }
    }

    private long getPlayerID(final Connection conn, final String playerUUID) throws SQLException
    {
        long playerID = -1;
        PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
        ResultSet rs1 = ps1.executeQuery();
        while (rs1.next())
            playerID = rs1.getLong(PLAYERS_ID);
        ps1.close();
        rs1.close();
        return playerID;
    }

    // Get the permission level for a given player for a given door.
    public int getPermission(final String playerUUID, final long doorUID)
    {
        Connection conn = null;
        int ret = -1;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID);
            if (playerID == -1)
                return -1;

            // Select all doors from the sqlUnion table that have the previously found
            // player as owner.
            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID
                                                                  + "' AND doorUID = '" + doorUID + "';");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next())
                ret = rs2.getInt(UNION_PERM);
            ps2.close();
            rs2.close();
        }
        catch (SQLException e)
        {
            logMessage("244", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("254", e);
            }
        }

        return ret;
    }

    // Construct a new door from a resultset.
    private DoorBase newDoorBaseFromRS(final ResultSet rs, final DoorOwner doorOwner)
    {
        try
        {
            DoorBase door = DoorType.valueOf(rs.getInt(DOOR_TYPE)).getNewDoor(plugin, doorOwner.getDoorUID());

            World world = Bukkit.getServer().getWorld(UUID.fromString(rs.getString(DOOR_WORLD)));
            door.setWorld(world);
            door.setMinimum(new Location(world, rs.getInt(DOOR_MIN_X), rs.getInt(DOOR_MIN_Y), rs.getInt(DOOR_MIN_Z)));
            door.setMaximum(new Location(world, rs.getInt(DOOR_MAX_X), rs.getInt(DOOR_MAX_Y), rs.getInt(DOOR_MAX_Z)));
            door.setEngineSide(PBlockFace.valueOf(rs.getInt(DOOR_ENG_SIDE)));
            door.setEngineLocation(new Location(world, rs.getInt(DOOR_ENG_X), rs.getInt(DOOR_ENG_Y),
                                                rs.getInt(DOOR_ENG_Z)));
            door.setPowerBlockLocation(new Location(world, rs.getInt(DOOR_POWER_X), rs.getInt(DOOR_POWER_Y),
                                                    rs.getInt(DOOR_POWER_Z)));

            door.setName(rs.getString(DOOR_NAME));
            door.setOpenStatus(rs.getInt(DOOR_OPEN) == 1 ? true : false);
            door.setLock(rs.getInt(DOOR_LOCKED) == 1 ? true : false);
            door.setDoorOwner(doorOwner);
            door.setOpenDir(RotateDirection.valueOf(rs.getInt(DOOR_OPEN_DIR)));
            door.setAutoClose(rs.getInt(DOOR_AUTO_CLOSE));
            door.setBlocksToMove(rs.getInt(DOOR_BLOCKS_TO_MOVE));

            return door;
        }
        catch (SQLException e)
        {
            logMessage("282", e);
            return null;
        }
    }

    // Remove a door with a given ID.
    public void removeDoor(final long doorID)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            String deleteDoor = "DELETE FROM doors WHERE id = '" + doorID + "';";
            PreparedStatement ps = conn.prepareStatement(deleteDoor);
            ps.executeUpdate();
            ps.close();
        }
        catch (SQLException e)
        {
            plugin.getPLogger().logException(e, String.valueOf(271));
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("311", e);
            }
        }
    }

    // Remove a door with a given name, owned by a certain player.
    public void removeDoor(final String playerUUID, final String doorName)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID);
            if (playerID == -1)
                return;

            // Select all doors from the sqlUnion table that have the previously found
            // player as owner.
            PreparedStatement ps2 = conn
                    .prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "';");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next())
            {
                // Delete all doors with the provided name owned by the provided player.
                PreparedStatement ps3 = conn.prepareStatement("DELETE FROM doors WHERE id = '"
                                                                      + rs2.getInt(UNION_DOOR_ID) + "' AND name = '" +
                                                                      doorName + "';");
                ps3.executeUpdate();
                ps3.close();
            }
            ps2.close();
            rs2.close();
        }
        catch (SQLException e)
        {
            logMessage("343", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("353", e);
            }
        }
    }

    /**
     * Gets the total number of doors own by the given player.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @return The total number of doors own by the given player.
     */
    public int getDoorCountForPlayer(@NotNull UUID playerUUID)
    {
        try (Connection conn = getConnection())
        {
            int count = 0;
            long playerID = getPlayerID(conn, playerUUID.toString());
            ResultSet rs = conn
                    .prepareStatement("SELECT COUNT(*) AS total FROM sqlUnion WHERE playerID = '" + playerID + "';")
                    .executeQuery();
            if (rs.next())
                count = rs.getInt("total");
            rs.close();
            return count;
        }
        catch (SQLException e)
        {
            logMessage("378", e);
        }
        return -1;
    }

    /**
     * Gets the number of doors own by the given player with the given name.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @param doorName   The name of the door to search for.
     * @return The number of doors own by the given player with the given name.
     */
    public int getDoorCountForPlayer(@NotNull UUID playerUUID, @NotNull String doorName)
    {
        try (Connection conn = getConnection())
        {
            int count = 0;
            long playerID = getPlayerID(conn, playerUUID.toString());
            ResultSet rs = conn.prepareStatement("SELECT COUNT(*) AS total FROM sqlUnion INNER JOIN doors ON "
                                                         + "doors.id = sqlUnion.doorUID WHERE playerID = '" + playerID +
                                                         "' AND doors.name = '" + doorName
                                                         + "';").executeQuery();
            if (rs.next())
                count = rs.getInt("total");
            rs.close();
            return count;
        }
        catch (SQLException e)
        {
            logMessage("419", e);
        }
        return -1;
    }

    /**
     * Gets the total number of doors with the given name regardless of who owns them.
     *
     * @param doorName The name of the doors to search for.
     * @return The total number of doors with the given name.
     */
    public int getDoorCountByName(@NotNull String doorName)
    {
        try (Connection conn = getConnection())
        {
            int count = 0;
            ResultSet rs = conn.prepareStatement("SELECT COUNT(*) as total FROM doors WHERE name = '" + doorName + "';")
                               .executeQuery();
            if (rs.next())
                count = rs.getInt("total");
            rs.close();
            return count;
        }
        catch (SQLException e)
        {
            logMessage("402", e);
        }
        return -1;
    }

    /**
     * Gets the door with the given UID for the given player at the level of ownership this player has over this door,
     * if any.
     *
     * @param playerUUID The UUID of the player.
     * @param doorUID    The UID of the door to retrieve.
     * @return The door if it exists and if the player is an owner of it.
     */
//    public Optional<DoorBase> getDoor(Optional<UUID> playerUUID, final long doorUID)
    public @Nullable DoorBase getDoor(@Nullable UUID playerUUID, final long doorUID)
    {
        DoorBase door = null;

        Connection conn = null;
        try
        {
            conn = getConnection();

            DoorOwner doorOwner;
            // If no player is specified, get the lowest tier permission and the original
            // creator.
            if (playerUUID == null)
                doorOwner = getOwnerOfDoor(conn, doorUID);
            else
                doorOwner = getOwnerOfDoor(conn, doorUID, playerUUID);

            if (doorOwner != null)
            {
                PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM doors WHERE id = '" + doorUID + "';");
                ResultSet rs3 = ps3.executeQuery();

                if (rs3.next())
                    door = newDoorBaseFromRS(rs3, doorOwner);
                ps3.close();
                rs3.close();
            }
        }
        catch (SQLException e)
        {
            logMessage("521", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("531", e);
            }
        }
        return door;
    }

    // Get ALL doors owned by a given playerUUID.

    /**
     * Gets all the doors owned by the the given player with the given name.
     *
     * @param playerUUID The UUID of the player to search for.
     * @param name       The name of the doors to search for.
     * @return All doors owned by the given player with the given name.
     */
    public ArrayList<DoorBase> getDoors(@NotNull final UUID playerUUID, @NotNull final String name)
    {
        return getDoors(playerUUID.toString(), name, 0);
    }

    /**
     * Gets all the doors owned by the the given player.
     *
     * @param playerUUID The UUID of the player to search for.
     * @return All doors owned by the given player.
     */
    public ArrayList<DoorBase> getDoors(@NotNull final UUID playerUUID)
    {
        return getDoors(playerUUID.toString(), null, 0);
    }

    /**
     * Gets all the doors with the given name, regardless of who owns them.
     *
     * @param name The name of the doors to search for.
     * @return All doors with the given name.
     */
    public ArrayList<DoorBase> getDoors(final String name)
    {
        ArrayList<DoorBase> doors = new ArrayList<>();

        Connection conn = null;
        try
        {
            conn = getConnection();

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors WHERE name = '" + name + "';");
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next())
            {
                String foundPlayerUUID = null;
                int permission = -1;
                PreparedStatement ps2 = conn
                        .prepareStatement("SELECT * FROM sqlUnion WHERE doorUID = '" + rs1.getLong(DOOR_ID) + "';");
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next())
                {
                    permission = rs2.getInt(UNION_PERM);
                    PreparedStatement ps3 = conn
                            .prepareStatement(
                                    "SELECT * FROM players WHERE id = '" + rs2.getInt(UNION_PLAYER_ID) + "';");
                    ResultSet rs3 = ps3.executeQuery();
                    while (rs3.next())
                        foundPlayerUUID = rs3.getString(PLAYERS_UUID);
                    ps3.close();
                    rs3.close();
                }
                ps2.close();
                rs2.close();

                DoorOwner owner = getOwnerOfDoor(conn, rs1.getLong(DOOR_ID), UUID.fromString(foundPlayerUUID),
                                                 permission);
                if (owner != null)
                    doors.add(newDoorBaseFromRS(rs1, owner));
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException e)
        {
            logMessage("582", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("592", e);
            }
        }
        return doors;
    }

    // Get all doors associated with this player in a given range. Name can be null
    public @Nullable ArrayList<DoorBase> getDoors(final String playerUUID, @Nullable final String doorName,
                                                  int maxPermission)
    {
        ArrayList<DoorBase> doors = new ArrayList<>();

        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID);

            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID
                                                                  + "' AND permission <= '" + maxPermission + "';");
            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next())
            {
                PreparedStatement ps3 = conn
                        .prepareStatement("SELECT * FROM doors WHERE id = '" + rs2.getInt(UNION_DOOR_ID) + "';");
                ResultSet rs3 = ps3.executeQuery();

                while (rs3.next())
                {
                    if ((doorName == null || rs3.getString(DOOR_NAME).equals(doorName)))
                    {
                        DoorOwner doorOwner = getOwnerOfDoor(conn, rs3.getLong(DOOR_ID),
                                                             UUID.fromString(playerUUID), rs2.getInt(UNION_PERM));
                        if (doorOwner != null)
                            doors.add(newDoorBaseFromRS(rs3, doorOwner));
                    }
                }
                ps3.close();
                rs3.close();
            }
            ps2.close();
            rs2.close();
        }
        catch (SQLException e)
        {
            logMessage("631", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("641", e);
            }
        }
        return doors;
    }

    /**
     * Gets the {@link DoorBase} with the given name owned by the given player. If the player owns 0 or more than 1
     * doors with the given name, no doors are returned at all!
     *
     * @param playerUUID The UUID of the player whose {@link DoorBase} will be obtained.
     * @param doorName   The name of the {@link DoorBase} to look for.
     * @return The {@link DoorBase} with the given name owned by this player if exactly 1 such DoorBase exists.
     */
    public DoorBase getDoor(final String playerUUID, final String doorName) throws TooManyDoorsException
    {
        return getDoor(playerUUID, doorName, Integer.MAX_VALUE);
    }

    /**
     * Gets the {@link DoorBase} with the given name owned by the given player. If the player owns 0 or more than 1
     * doors with the given name, no doors are returned at all!
     *
     * @param playerUUID    The UUID of the player whose {@link DoorBase} will be obtained.
     * @param doorName      The name of the {@link DoorBase} to look for.
     * @param maxPermission The highest level of ownership this player can have over the door being looked for. If set
     *                      to 0, for example, only door doors where the given player has ownership level 0 (= creator)
     *                      are included in the search.
     * @return The {@link DoorBase} with the given name owned by this player if exactly 1 such DoorBase exists.
     */
    public @Nullable DoorBase getDoor(final String playerUUID, final String doorName, int maxPermission)
            throws TooManyDoorsException
    {
        DoorBase door = null;
        int count = 0;

        try (Connection conn = getConnection())
        {
            long playerID = getPlayerID(conn, playerUUID);

            ResultSet rs2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID
                                                          + "' AND permission <= '" + maxPermission + "';")
                                .executeQuery();

            while (rs2.next())
            {
                ResultSet rs3 = conn
                        .prepareStatement("SELECT * FROM doors WHERE id = '" + rs2.getInt(UNION_DOOR_ID) + "';")
                        .executeQuery();

                while (rs3.next())
                {
                    ++count;
                    if (count > 1)
                        break;

                    if ((doorName == null || rs3.getString(DOOR_NAME).equals(doorName)))
                    {
                        DoorOwner doorOwner = getOwnerOfDoor(conn, rs3.getLong(DOOR_ID),
                                                             UUID.fromString(playerUUID), rs2.getInt(UNION_PERM));
                        if (doorOwner != null)
                            door = newDoorBaseFromRS(rs3, doorOwner);
                    }
                }
                rs3.close();
            }
            rs2.close();
        }
        catch (SQLException e)
        {
            logMessage("621", e);
        }
        if (count > 1)
            throw new TooManyDoorsException();
        return door;
    }

    public void updatePlayerName(final String playerUUID, final String playerName)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();

            PreparedStatement ps = conn
                    .prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID.toString() + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                if (rs.getString(PLAYERS_NAME) == null || !rs.getString(PLAYERS_NAME).equals(playerName))
                {
                    conn.setAutoCommit(false);
                    String update = "UPDATE players SET " + "playerName='" + playerName + "' WHERE playerUUID = '"
                            + playerUUID.toString() + "';";
                    conn.prepareStatement(update).executeUpdate();
                    conn.commit();
                }
            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            logMessage("671", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("681", e);
            }
        }
    }

    public UUID getUUIDFromName(final String playerName)
    {
        UUID uuid = null;
        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn
                    .prepareStatement("SELECT * FROM players WHERE playerName = '" + playerName + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                uuid = UUID.fromString(rs.getString(PLAYERS_UUID));
            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            logMessage("703", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("713", e);
            }
        }
        return uuid;
    }

    public UUID getPlayerUUID(final String playerName)
    {
        UUID playerUUID = null;
        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn
                    .prepareStatement("SELECT * FROM players WHERE playerName = '" + playerName + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                playerUUID = UUID.fromString(rs.getString(PLAYERS_UUID));
            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            logMessage("736", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("746", e);
            }
        }
        return playerUUID;
    }

    public String getPlayerName(final String playerUUID)
    {
        String playerName = null;
        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn
                    .prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID.toString() + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                playerName = rs.getString(PLAYERS_NAME);
            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            logMessage("736", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("746", e);
            }
        }
        return playerName;
    }

    private DoorOwner getOwnerOfDoor(final Connection conn, final long doorUID, final UUID playerUUID, int permission)
            throws SQLException
    {
        PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
        ResultSet rs1 = ps1.executeQuery();

        if (!rs1.next())
        {
            ps1.close();
            rs1.close();
            return null;
        }

        DoorOwner doorOwner = new DoorOwner(doorUID, playerUUID, rs1.getString(PLAYERS_NAME), permission);

        ps1.close();
        rs1.close();
        return doorOwner;
    }

    private DoorOwner getOwnerOfDoor(final Connection conn, final long doorUID, final UUID playerUUID)
            throws SQLException
    {
        PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
        ResultSet rs1 = ps1.executeQuery();

        if (!rs1.next())
        {
            ps1.close();
            rs1.close();
            return null;
        }

        PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE doorUID = '" + doorUID
                                                              + "' AND playerID = '" + rs1.getInt(PLAYERS_ID) + "';");
        ResultSet rs2 = ps2.executeQuery();

        if (!rs2.next())
        {
            ps1.close();
            rs1.close();
            ps2.close();
            rs2.close();
            return null;
        }

        DoorOwner doorOwner = new DoorOwner(doorUID, UUID.fromString(rs1.getString(PLAYERS_UUID)),
                                            rs1.getString(PLAYERS_NAME), rs2.getInt(UNION_PERM));

        ps1.close();
        rs1.close();
        ps2.close();
        rs2.close();
        return doorOwner;
    }

    private DoorOwner getOwnerOfDoor(final Connection conn, final long doorUID) throws SQLException
    {
        DoorOwner doorOwner = null;

        String command = "SELECT * FROM sqlUnion WHERE doorUID = '" + doorUID + "' AND permission = '" + 0 + "';";
        PreparedStatement ps1 = conn.prepareStatement(command);
        ResultSet rs1 = ps1.executeQuery();

        while (rs1.next())
        {
            PreparedStatement ps2 = conn
                    .prepareStatement("SELECT * FROM players WHERE id = '" + rs1.getInt(UNION_PLAYER_ID) + "';");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next())
                doorOwner = new DoorOwner(doorUID, UUID.fromString(rs2.getString(PLAYERS_UUID)),
                                          rs2.getString(PLAYERS_NAME), rs1.getInt(UNION_PERM));
            ps2.close();
            rs2.close();
        }
        ps1.close();
        rs1.close();
        return doorOwner;
    }

    public DoorOwner getOwnerOfDoor(final long doorUID)
    {
        DoorOwner doorOwner = null;

        Connection conn = null;
//      plugin.getPLogger().warn("271: " + SpigotUtil.exceptionToString(e));
        try
        {
            conn = getConnection();
            doorOwner = getOwnerOfDoor(conn, doorUID);
        }
        catch (SQLException e)
        {
            logMessage("788", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("798", e);
            }
        }
        return doorOwner;
    }

    public HashMap<Long, Long> getPowerBlockData(final long chunkHash)
    {
        HashMap<Long, Long> doors = new HashMap<>();

        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE chunkHash = '" + chunkHash + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                long locationHash = SpigotUtil.locationHash(rs.getInt(DOOR_POWER_X), rs.getInt(DOOR_POWER_Y),
                                                            rs.getInt(DOOR_POWER_Z),
                                                            UUID.fromString(rs.getString(DOOR_WORLD)));
                doors.put(locationHash, rs.getLong(DOOR_ID));
            }
            ps.close();
            rs.close();
        }
        catch (SQLException e)
        {
            logMessage("828", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("838", e);
            }
        }
        return doors;
    }

    public void updateDoorBlocksToMove(final long doorID, final int blocksToMove)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "blocksToMove='" + blocksToMove + "' WHERE id = '" + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            logMessage("859", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("869", e);
            }
        }
    }

    // Update the door at doorUID with the provided coordinates and open status.
    public void updateDoorCoords(final long doorID, final boolean isOpen, final int xMin, final int yMin,
                                 final int zMin, final int xMax, final int yMax, final int zMax,
                                 final PBlockFace engSide)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "xMin='" + xMin + "',yMin='" + yMin + "',zMin='" + zMin + "',xMax='"
                    + xMax + "',yMax='" + yMax + "',zMax='" + zMax + "',isOpen='" + (isOpen == true ? 1 : 0)
                    + "',engineSide='" + (engSide == null ? -1 : PBlockFace.getValue(engSide)) + "' WHERE id = '" +
                    doorID
                    + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            logMessage("897", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("907", e);
            }
        }
    }

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    public void updateDoorAutoClose(final long doorID, final int autoClose)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "autoClose='" + autoClose + "' WHERE id = '" + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            logMessage("928", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("938", e);
            }
        }
    }

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    public void updateDoorOpenDirection(final long doorID, final RotateDirection openDir)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "openDirection='" + RotateDirection.getValue(openDir)
                    + "' WHERE id = '" + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            logMessage("958", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("966", e);
            }
        }
    }

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    public void updateDoorPowerBlockLoc(final long doorID, final int xPos, final int yPos, final int zPos,
                                        final UUID worldUUID)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "powerBlockX='" + xPos + "',powerBlockY='" + yPos + "',powerBlockZ='"
                    + zPos + "',chunkHash='" + SpigotUtil.chunkHashFromLocation(xPos, zPos, worldUUID) +
                    "' WHERE id = '"
                    + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            logMessage("992", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1002", e);
            }
        }
    }

    // Check if a given location already contains a power block or not.
    // Returns false if it's already occupied.
    public boolean isPowerBlockLocationEmpty(final Location loc)
    {
        // Prepare door and connection.
        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE powerBlockX = '" + loc.getBlockX()
                                                                 + "' AND powerBlockY = '" + loc.getBlockY() +
                                                                 "' AND powerBlockZ = '" + loc.getBlockZ()
                                                                 + "' AND world = '" +
                                                                 loc.getWorld().getUID().toString() + "';");
            ResultSet rs = ps.executeQuery();
            boolean isAvailable = true;

            if (rs.next())
                isAvailable = false;

            ps.close();
            rs.close();
            return isAvailable;
        }
        catch (SQLException e)
        {
            logMessage("1033", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1043", e);
            }
        }

        return false;
    }

    // Update the door at doorUID with the provided new lockstatus.
    public void setLock(final long doorID, final boolean newLockStatus)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "isLocked='" + (newLockStatus == true ? 1 : 0) + "' WHERE id='"
                    + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            logMessage("1066", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1076", e);
            }
        }
    }

    // Insert a new door in the db.
    public void insert(final DoorBase door)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();

            long playerID = getPlayerID(conn, door.getPlayerUUID().toString());

            if (playerID == -1)
            {
                Statement stmt2 = conn.createStatement();
                String sql2 = "INSERT INTO players (playerUUID, playerName) " + "VALUES ('"
                        + door.getPlayerUUID().toString() + "', '" + SpigotUtil.nameFromUUID(door.getPlayerUUID()) +
                        "');";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                String query = "SELECT last_insert_rowid() AS lastId";
                PreparedStatement ps2 = conn.prepareStatement(query);
                ResultSet rs2 = ps2.executeQuery();
                playerID = rs2.getLong("lastId");
                ps2.close();
                rs2.close();
            }

            String doorInsertsql = "INSERT INTO doors(name,world,isOpen,xMin,yMin,zMin,xMax,yMax,zMax,"
                    + "engineX,engineY,engineZ,isLocked,type,engineSide,powerBlockX,powerBlockY,powerBlockZ,"
                    + "openDirection,autoClose,chunkHash,blocksToMove) "
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement doorstatement = conn.prepareStatement(doorInsertsql);

            doorstatement.setString(DOOR_NAME - 1, door.getName());
            doorstatement.setString(DOOR_WORLD - 1, door.getWorld().getUID().toString());
            doorstatement.setInt(DOOR_OPEN - 1, door.isOpen() == true ? 1 : 0);
            doorstatement.setInt(DOOR_MIN_X - 1, door.getMinimum().getBlockX());
            doorstatement.setInt(DOOR_MIN_Y - 1, door.getMinimum().getBlockY());
            doorstatement.setInt(DOOR_MIN_Z - 1, door.getMinimum().getBlockZ());
            doorstatement.setInt(DOOR_MAX_X - 1, door.getMaximum().getBlockX());
            doorstatement.setInt(DOOR_MAX_Y - 1, door.getMaximum().getBlockY());
            doorstatement.setInt(DOOR_MAX_Z - 1, door.getMaximum().getBlockZ());
            doorstatement.setInt(DOOR_ENG_X - 1, door.getEngine().getBlockX());
            doorstatement.setInt(DOOR_ENG_Y - 1, door.getEngine().getBlockY());
            doorstatement.setInt(DOOR_ENG_Z - 1, door.getEngine().getBlockZ());
            doorstatement.setInt(DOOR_LOCKED - 1, door.isLocked() == true ? 1 : 0);
            doorstatement.setInt(DOOR_TYPE - 1, DoorType.getValue(door.getType()));
            // Set -1 if the door has no engineSide (normal doors don't use it)
            doorstatement.setInt(DOOR_ENG_SIDE - 1,
                                 door.getEngineSide() == null ? -1 : PBlockFace.getValue(door.getEngineSide()));
            doorstatement.setInt(DOOR_POWER_X - 1, door.getEngine().getBlockX());
            doorstatement.setInt(DOOR_POWER_Y - 1, door.getEngine().getBlockY() - 1); // Power Block Location is 1 block
            // below the engine, by default.
            doorstatement.setInt(DOOR_POWER_Z - 1, door.getEngine().getBlockZ());
            doorstatement.setInt(DOOR_OPEN_DIR - 1, RotateDirection.getValue(door.getOpenDir()));
            doorstatement.setInt(DOOR_AUTO_CLOSE - 1, door.getAutoClose());
            doorstatement.setLong(DOOR_CHUNK_HASH - 1, door.getPowerBlockChunkHash());
            doorstatement.setLong(DOOR_BLOCKS_TO_MOVE - 1, door.getBlocksToMove());

            doorstatement.executeUpdate();
            doorstatement.close();

            String query = "SELECT last_insert_rowid() AS lastId";
            PreparedStatement ps2 = conn.prepareStatement(query);
            ResultSet rs2 = ps2.executeQuery();
            Long doorID = rs2.getLong("lastId");
            ps2.close();
            rs2.close();

            Statement stmt3 = conn.createStatement();
            String sql3 = "INSERT INTO sqlUnion (permission, playerID, doorUID) " + "VALUES ('" + door.getPermission()
                    + "', '" + playerID + "', '" + doorID + "');";
            stmt3.executeUpdate(sql3);
            stmt3.close();
        }
        catch (SQLException e)
        {
            logMessage("1153", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1163", e);
            }
        }
    }

    // Insert a new door in the db.
    public boolean removeOwner(final long doorUID, final String playerUUID)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID.toString());

            if (playerID == -1)
                plugin.getPLogger().severe("Trying to remove player " + playerUUID.toString() + " as ownwer of door "
                                                   + doorUID + ". But player does not exist!");
            else
            {
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM sqlUnion WHERE " + "playerID = '" + playerID
                                                                      + "' AND doorUID = '" + doorUID +
                                                                      "' AND permission > '" + 0 +
                                                                      "';"); // The creator cannot be
                // removed as owner
                ps2.execute();
                ps2.close();
            }

        }
        catch (SQLException e)
        {
            logMessage("1193", e);
            return false;
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1204", e);
                return false;
            }
        }
        return true;
    }

    public ArrayList<DoorOwner> getOwnersOfDoor(final long doorUID, final UUID playerUUID)
    {
        ArrayList<DoorOwner> ret = new ArrayList<>();
        Connection conn = null;
        try
        {
            conn = getConnection();
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE doorUID = '" + doorUID + "';");
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next())
            {
                DoorOwner owner = getOwnerOfDoor(conn, doorUID, playerUUID, rs1.getInt(UNION_PERM));
                if (owner == null)
                {
                    plugin.getPLogger().warn(String.format("Could not find owners for doorUID: %d", doorUID)
                                                     + String.format("Offending playerUUID: \"%s\".",
                                                                     playerUUID.toString()));
                }
                else
                    ret.add(owner);
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException e)
        {
            logMessage("1234", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1244", e);
            }
        }

        return ret;
    }

    // Insert a new door in the db.
    public void addOwner(final long doorUID, final UUID playerUUID, final int permission)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID.toString());

            if (playerID == -1)
            {
                Statement stmt2 = conn.createStatement();
                String sql2 = "INSERT INTO players (playerUUID, playerName) " + "VALUES ('" + playerUUID.toString()
                        + "', '" + SpigotUtil.nameFromUUID(playerUUID) + "');";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                String query = "SELECT last_insert_rowid() AS lastId";
                PreparedStatement ps2 = conn.prepareStatement(query);
                ResultSet rs2 = ps2.executeQuery();
                playerID = rs2.getLong("lastId");
                ps2.close();
                rs2.close();
            }

            PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID
                                                                  + "' AND doorUID = '" + doorUID + "';");
            ResultSet rs3 = ps3.executeQuery();

            // If it already exists, update the permission, if needed.
            if (rs3.next())
            {
                if (rs3.getInt(UNION_PERM) != permission)
                {
                    Statement stmt4 = conn.createStatement();
                    String sql4 = "UPDATE sqlUnion SET permission = '" + permission + "' WHERE playerID = '" + playerID
                            + "' AND doorUID = '" + doorUID + "';";
                    stmt4.executeUpdate(sql4);
                    stmt4.close();
                }
            }
            else
            {
                Statement stmt4 = conn.createStatement();
                String sql4 = "INSERT INTO sqlUnion (permission, playerID, doorUID) " + "VALUES ('" + permission
                        + "', '" + playerID + "', '" + doorUID + "');";
                stmt4.executeUpdate(sql4);
                stmt4.close();
            }
            ps3.close();
            rs3.close();
        }
        catch (SQLException e)
        {
            logMessage("1306", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1316", e);
            }
        }
    }

    // Add columns and such when needed (e.g. upgrades from older versions).
    private void upgrade()
    {
        Connection conn = null;
        boolean replaceTempPlayerNames = false;
        try
        {
            conn = DriverManager.getConnection(url);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA user_version;");
            int dbVersion = rs.getInt(1);
            stmt.close();
            rs.close();

            if (dbVersion == DATABASE_VERSION)
            {
                conn.close();
                return;
            }

            // If an update is required and backups are enabled, make a backup.
            if (dbVersion != DATABASE_VERSION && plugin.getConfigLoader().dbBackup())
            {
                conn.close();
                if (!makeBackup())
                    return;
                conn = DriverManager.getConnection(url);
            }

            if (dbVersion < 1)
                upgradeToV1(conn);

            if (dbVersion < 2)
                upgradeToV2(conn);

            if (dbVersion < 3)
                upgradeToV3(conn);

            if (dbVersion < 4)
                upgradeToV4(conn);

            if (dbVersion < 5)
            {
                conn.close();
                upgradeToV5();
                replaceTempPlayerNames = true;
                conn = DriverManager.getConnection(url);
            }

            // If the database upgrade to V5 got interrupted in a previous attempt, the
            // fakeUUID
            // will still be in the database. If so, simply continue filling in player names
            // in the db.
            if (!replaceTempPlayerNames && fakeUUIDExists(conn))
                replaceTempPlayerNames = true;

            // Do this at the very end, so the db version isn't altered if anything fails.
            if (dbVersion != DATABASE_VERSION)
                setDBVersion(conn, DATABASE_VERSION);
        }
        catch (SQLException e)
        {
            logMessage("1414", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1424", e);
            }
        }
        if (replaceTempPlayerNames)
            replaceTempPlayerNames();
    }

    private boolean fakeUUIDExists(final Connection conn)
    {
        try
        {
            return conn.createStatement()
                       .executeQuery("SELECT COUNT(*) as total FROM players WHERE playerUUID = '" + FAKEUUID + "';")
                       .getInt("total") > 0;
        }
        catch (SQLException e)
        {
            logMessage("1461", e);
        }
        return false;
    }

    private boolean makeBackup()
    {
        File dbFileBackup = new File(plugin.getDataFolder(), dbName + ".BACKUP");
        // Only the most recent backup is kept, so delete the old one if a new one needs
        // to be created.
        if (dbFileBackup.exists())
            dbFileBackup.delete();
        try
        {
            Files.copy(dbFile, dbFileBackup);
        }
        catch (IOException e)
        {
            plugin.getPLogger().logException(e, "Failed to create backup of the database! "
                    + "Database upgrade aborted and access is disabled!");
            enabled = false;
            return false;
        }
        return true;
    }

    private void setDBVersion(final Connection conn, final int version)
    {
        try
        {
            conn.createStatement().execute("PRAGMA user_version = " + version + ";");
        }
        catch (SQLException e)
        {
            logMessage("1458", e);
        }
    }

    private void upgradeToV1(final Connection conn)
    {
        try
        {
            String addColumn;

            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getColumns(null, null, "doors", "type");

            if (!rs.next())
            {
                plugin.getPLogger().warn("Upgrading database! Adding type!");
                addColumn = "ALTER TABLE doors " + "ADD COLUMN type int NOT NULL DEFAULT 0";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "engineSide");
            if (!rs.next())
            {
                plugin.getPLogger().warn("Upgrading database! Adding engineSide!");
                addColumn = "ALTER TABLE doors " + "ADD COLUMN engineSide int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "powerBlockX");
            if (!rs.next())
            {
                plugin.getPLogger().warn("Upgrading database! Adding powerBlockLoc!");
                addColumn = "ALTER TABLE doors " + "ADD COLUMN powerBlockX int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN powerBlockY int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN powerBlockZ int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);

                PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
                ResultSet rs1 = ps1.executeQuery();
                String update;

                while (rs1.next())
                {
                    long UID = rs1.getLong(DOOR_ID);
                    int x = rs1.getInt(DOOR_ENG_X);
                    int y = rs1.getInt(DOOR_ENG_Y) - 1;
                    int z = rs1.getInt(DOOR_ENG_Z);
                    update = "UPDATE doors SET " + "powerBlockX='" + x + "',powerBlockY='" + y + "',powerBlockZ='" + z
                            + "' WHERE id = '" + UID + "';";
                    conn.prepareStatement(update).executeUpdate();
                }
                ps1.close();
                rs1.close();
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "openDirection");
            if (!rs.next())
            {
                plugin.getPLogger().warn("Upgrading database! Adding openDirection!");
                addColumn = "ALTER TABLE doors " + "ADD COLUMN openDirection int NOT NULL DEFAULT 0";
                conn.createStatement().execute(addColumn);

                plugin.getPLogger()
                      .warn("Upgrading database! Swapping open-status of drawbridges to conform to the new standard!");
                String update = "UPDATE doors SET " + "isOpen='" + 2 + "' WHERE isOpen = '" + 0 + "' AND type = '"
                        + DoorType.getValue(DoorType.DRAWBRIDGE) + "';";
                conn.createStatement().execute(update);
                update = "UPDATE doors SET " + "isOpen='" + 0 + "' WHERE isOpen = '" + 1 + "' AND type = '"
                        + DoorType.getValue(DoorType.DRAWBRIDGE) + "';";
                conn.createStatement().execute(update);
                update = "UPDATE doors SET " + "isOpen='" + 1 + "' WHERE isOpen = '" + 2 + "' AND type = '"
                        + DoorType.getValue(DoorType.DRAWBRIDGE) + "';";
                conn.createStatement().execute(update);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "autoClose");
            if (!rs.next())
            {
                plugin.getPLogger().warn("Upgrading database! Adding autoClose!");
                addColumn = "ALTER TABLE doors " + "ADD COLUMN autoClose int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "chunkHash");
            if (!rs.next())
            {
                plugin.getPLogger().warn("Upgrading database! Adding chunkHash!");
                addColumn = "ALTER TABLE doors " + "ADD COLUMN chunkHash int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);

                PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
                ResultSet rs1 = ps1.executeQuery();
                String update;

                while (rs1.next())
                {
                    long UID = rs1.getLong(DOOR_ID);
                    UUID worldUUID = UUID.fromString(rs1.getString(DOOR_WORLD));
                    int x = rs1.getInt(DOOR_POWER_X);
                    int z = rs1.getInt(DOOR_POWER_Z);

                    update = "UPDATE doors SET " + "chunkHash='" + SpigotUtil.chunkHashFromLocation(x, z, worldUUID)
                            + "' WHERE id = '" + UID + "';";
                    conn.prepareStatement(update).executeUpdate();
                }
                ps1.close();
                rs1.close();
            }
            rs.close();
            plugin.getPLogger().warn("Database has been upgraded to V1!");
        }
        catch (SQLException e)
        {
            logMessage("1593", e);
        }
    }

    private void upgradeToV2(final Connection conn)
    {
        try
        {
            String addColumn;
            plugin.getPLogger().warn("Upgrading database to V2! Adding blocksToMove!");
            addColumn = "ALTER TABLE doors " + "ADD COLUMN blocksToMove int NOT NULL DEFAULT 0";
            conn.createStatement().execute(addColumn);
        }
        catch (SQLException e)
        {
            logMessage("1238", e);
        }
    }

    /*
     * Right, so this is quite annoying. SQLite only supports a
     * "limited subset of ALTER TABLE". Source:
     * https://www.sqlite.org/lang_altertable.html
     *
     * The main "ALTER TABLE" instruction I need, is to add the UNIQUE constraint to
     * the playerID and doorUID columns of the sqlUnion. V2 did not have this
     * constraint, while v3+ does require it.
     *
     * So instead of simply updating the table, the table will be recreated with the
     * uniqueness constraint added. Why change something something small when you
     * can just copy all the data, right? I hate speed. Maybe I should just copy all
     * database data every 5 seconds.
     */
    private void upgradeToV3(final Connection conn)
    {
        try
        {
            plugin.getPLogger().warn("Upgrading database to V3! Recreating sqlUnion!");
            conn.setAutoCommit(false);
            // Rename sqlUnion.
            conn.createStatement().execute("ALTER TABLE sqlUnion RENAME TO sqlUnion_old;");

            // Create updated version of sqlUnion.
            conn.createStatement()
                .execute("CREATE TABLE IF NOT EXISTS sqlUnion " + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, "
                                 + " permission  INTEGER    NOT NULL, "
                                 + " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE, "
                                 + " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE,"
                                 + " unique (playerID, doorUID));");

            // Copy data from old sqlUnion to new sqlUnion.
            conn.createStatement().execute("INSERT INTO sqlUnion SELECT * FROM sqlUnion_old;");

            // Get rid of old sqlUnion.
            conn.createStatement().execute("DROP TABLE IF EXISTS 'sqlUnion_old';");
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch (SQLException e)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException e1)
            {
                logMessage("1285", e1);
            }
            logMessage("1287", e);
        }
    }

    private void upgradeToV4(final Connection conn)
    {
        try
        {
            String addColumn;
            plugin.getPLogger().warn("Upgrading database to V4! Adding playerName!");
            addColumn = "ALTER TABLE players " + "ADD COLUMN playerName TEXT DEFAULT NULL";
            conn.createStatement().execute(addColumn);
        }
        catch (SQLException e)
        {
            logMessage("1420", e);
        }
    }

    /*
     * In V5 of the DB, the NOT NULL constraint has been added to the playerName
     * attribute of the players table. All NULL values get replaced by a temporary,
     * randomly chosen unique player name. All these temporary names then get
     * replaced by their actual names on a separate thread. While updating on the
     * secondary thread, the DB is locked.
     */
    private void upgradeToV5()
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            plugin.getPLogger().warn("Upgrading database to V5!");

            String countStr = "SELECT COUNT(*) AS total FROM players WHERE playerName IS NULL";
            int count = conn.createStatement().executeQuery(countStr).getInt("total");

            // Make sure there aren't any NULL values in the players database.
            if (count > 0)
            {
                // First, find a name that does not exist in the database already.
                // Do this by generating random strings and checking if it exists until
                // We encounter one that doesn't.
                String fakeName = Util.randomInsecureString(12);
                boolean exists = true;
                while (exists)
                {
                    PreparedStatement ps = conn
                            .prepareStatement("SELECT * FROM players WHERE playerName = '" + fakeName + "';");
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next())
                        exists = false;
                    ps.close();
                    rs.close();
                    if (exists)
                        fakeName = Util.randomInsecureString(12);
                }
                plugin.getPLogger().warn("UpgradeToV5: Using fakeName = " + fakeName);

                Statement stmt = conn.createStatement();
                String sql = "INSERT INTO players (playerUUID, playerName) " + "VALUES ('" + FAKEUUID + "', '"
                        + fakeName + "');";
                stmt.executeUpdate(sql);
                stmt.close();

                String update = "UPDATE players SET playerName='" + fakeName + "' WHERE playerName IS NULL;";
                conn.prepareStatement(update).executeUpdate();
            }
        }
        catch (SQLException e)
        {
            logMessage("1745", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("1755", e);
            }
        }

        Connection con = null;
        try
        {
            con = DriverManager.getConnection(url);
            con.setAutoCommit(false);
            // Rename sqlUnion.
            con.createStatement().execute("ALTER TABLE players RENAME TO players_old;");

            // Create updated version of sqlUnion.
            con.createStatement()
               .execute("CREATE TABLE IF NOT EXISTS players " + "(id          INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + " playerUUID  TEXT    NOT NULL," + " playerName  TEXT    NOT NULL)");

            // Copy data from old sqlUnion to new sqlUnion.
            con.createStatement().execute("INSERT INTO players SELECT * FROM players_old;");

            // Get rid of old sqlUnion.
            con.createStatement().execute("DROP TABLE IF EXISTS 'players_old';");
            con.commit();
            con.setAutoCommit(true);
        }
        catch (SQLException e)
        {
            try
            {
                con.rollback();
            }
            catch (SQLException e1)
            {
                logMessage("1770", e1);
            }
            logMessage("1772", e);
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (SQLException e)
            {
                logMessage("1781", e);
            }
        }
    }

    /*
     * Part of the upgrade to V5 of the database. First lock the database so nothing
     * else can interfere. Then find the temporary name from the fake player (that
     * was stored in the db). All fake names then get replaced by the real names. Or
     * the last ones they used, anyway.
     */
    private void replaceTempPlayerNames()
    {
        locked.set(true);
        final Thread thread = new Thread(() ->
                                         {
                                             Connection conn = null;
                                             try
                                             {
                                                 // Database is in a locked state, so a simple getConnection() call won't work.
                                                 // So do it again manually here.
                                                 Class.forName(DRIVER);
                                                 conn = DriverManager.getConnection(url);
                                                 conn.createStatement().execute("PRAGMA foreign_keys=ON");

                                                 ResultSet rs1 = conn.createStatement()
                                                                     .executeQuery(
                                                                             "SELECT * FROM players WHERE playerUUID='" +
                                                                                     FAKEUUID + "';");
                                                 String fakeName = null;
                                                 while (rs1.next())
                                                     fakeName = rs1.getString("playerName");
                                                 rs1.close();

                                                 ResultSet rs2 = conn.createStatement()
                                                                     .executeQuery(
                                                                             "SELECT * FROM players WHERE playerName='" +
                                                                                     fakeName + "';");
                                                 while (rs2.next())
                                                 {
                                                     if (rs2.getString("playerUUID").equals(FAKEUUID))
                                                         continue;
                                                     UUID playerUUID = UUID.fromString(rs2.getString("playerUUID"));
                                                     String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();

                                                     String update = "UPDATE players SET playerName='" + playerName +
                                                             "' WHERE playerUUID='"
                                                             + playerUUID.toString() + "';";
                                                     conn.prepareStatement(update).executeUpdate();
                                                 }
                                                 rs2.close();

                                                 String deleteFakePlayer =
                                                         "DELETE FROM players WHERE playerUUID = '" + FAKEUUID + "';";
                                                 conn.createStatement().executeUpdate(deleteFakePlayer);
                                             }
                                             catch (SQLException | ClassNotFoundException e)
                                             {
                                                 logMessage("1729", e);
                                             }
                                             finally
                                             {
                                                 try
                                                 {
                                                     conn.close();
                                                 }
                                                 catch (SQLException e)
                                                 {
                                                     logMessage("1739", e);
                                                 }
                                                 locked.set(false);
                                             }
                                         });
        thread.start();
    }

    private void logMessage(String str, Exception e)
    {
        if (!locked.get())
            plugin.getPLogger().logException(e, str);
        else
            plugin.getPLogger().warn("Database locked! Failed at: " + str + ". Message: " + e.getMessage());
    }
}
