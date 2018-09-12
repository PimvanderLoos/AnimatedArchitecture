package nl.pim16aap2.bigDoors.storage.sqlite;

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
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;

public class SQLiteJDBCDriverConnection
{
	private BigDoors plugin;
	private File     dataFolder;
	private String   url;
	public static final String DRIVER = "org.sqlite.JDBC"; 

	public SQLiteJDBCDriverConnection(BigDoors plugin, String dbName)
	{
		this.plugin     = plugin;
		this.dataFolder = new File(plugin.getDataFolder(), dbName);
		this.url        = "jdbc:sqlite:" + dataFolder;
		init();
		update();
	}
	
	// Establish a connection.
	public Connection getConnection()
	{
		Connection conn = null;
		try
		{
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection(url);
			conn.createStatement().execute("PRAGMA foreign_keys=ON");
		}
		catch (SQLException ex)
		{
			plugin.getMyLogger().logMessage("46: Failed to open connection!", true, false);
		}
		catch (ClassNotFoundException e)
		{
			plugin.getMyLogger().logMessage("50: Failed to open connection: CLass not found!!", true, false);
		}
		return conn;
	}
	
	// Initialize the tables.
	public void init()
	{
		if (!dataFolder.exists())
		{
			try
			{
				dataFolder.createNewFile();
				plugin.getMyLogger().logMessage("New file created at " + dataFolder);
			}
			catch (IOException e)
			{
				plugin.getMyLogger().logMessage("File write error: " + dataFolder);
			}
		}

		// Table creation
		Connection conn = null;
		try
		{
			conn = getConnection();

			Statement stmt1 	= conn.createStatement();	
			String sql1 		= "CREATE TABLE IF NOT EXISTS doors "   
							+ "(id          INTEGER    PRIMARY KEY autoincrement, " 
		          			+ " name        TEXT       NOT NULL, "       
		          			+ " world       TEXT       NOT NULL, "     
		          			+ " isOpen      INTEGER    NOT NULL, " 
		          			+ " xMin        INTEGER    NOT NULL, " 
		          			+ " yMin        INTEGER    NOT NULL, " 
		          			+ " zMin        INTEGER    NOT NULL, " 
		          			+ " xMax        INTEGER    NOT NULL, " 
		          			+ " yMax        INTEGER    NOT NULL, " 
		          			+ " zMax        INTEGER    NOT NULL, " 
		          			+ " engineX     INTEGER    NOT NULL, " 
		          			+ " engineY     INTEGER    NOT NULL, " 
		          			+ " engineZ     INTEGER    NOT NULL, " 
		          			+ " isLocked    INTEGER    NOT NULL, "
		          			+ " type        INTEGER    NOT NULL,"
		          			+ " engineSide  INTEGER    NOT NULL, "
				  			+ " powerBlockX INTEGER    NOT NULL, " 
				  			+ " powerBlockY INTEGER    NOT NULL, " 
				  			+ " powerBlockZ INTEGER    NOT NULL) "; 
			stmt1.executeUpdate(sql1);
			stmt1.close();
			
			Statement stmt2 	= conn.createStatement();
            String sql2		= "CREATE TABLE IF NOT EXISTS players " 
			            		+ "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, "      
			            		+ " playerUUID  TEXT       NOT NULL)";
			stmt2.executeUpdate(sql2);
			stmt2.close();
			
			Statement stmt3 	= conn.createStatement();
			String sql3     	= "CREATE TABLE IF NOT EXISTS sqlUnion "
					     	+ "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, "
					     	+ " permission  INTEGER    NOT NULL, "
					     	+ " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE, "
					     	+ " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE)";
			stmt3.executeUpdate(sql3);
			stmt3.close();
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("114: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("124: " + e.getMessage());
			}
			catch (Exception e)
			{
				plugin.getMyLogger().logMessage("128: " + e.getMessage());
			}
		}
	}
	
	// Get the permission level for a given player for a given door.
	public int getPermission(String playerUUID, long doorUID)
	{
		Connection conn = null;
		int ret = -1;
		try
		{
			conn = getConnection();
			// Get the player ID as used in the sqlUnion table.
			PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
			ResultSet rs1         = ps1.executeQuery();
			while (rs1.next())
			{
				int playerID = rs1.getInt(1);
				// Select all doors from the sqlUnion table that have the previously found player as owner.
				PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "' AND doorUID = '" + doorUID + "';");
				ResultSet rs2         = ps2.executeQuery();
				while (rs2.next())
					ret = rs2.getInt(2);
				ps2.close();
				rs2.close();
			}
			ps1.close();
			rs1.close();
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("297: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("307: " + e.getMessage());
			}
		}
		
		return ret;
	}
	
	// Construct a new door from a resultset.
	public Door newDoorFromRS(ResultSet rs, long doorUID, int permission)
	{
		try
		{
			return new Door(null, Bukkit.getServer().getWorld(UUID.fromString(rs.getString(3))), rs.getInt(5), 
							rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), 
							rs.getInt(12), rs.getInt(13), rs.getString(2), (rs.getInt(4) == 1 ? true : false), 
							doorUID, (rs.getInt(14) == 1 ? true : false), permission, rs.getInt(15), DoorDirection.valueOf(rs.getInt(16)), 
							rs.getInt(17), rs.getInt(18), rs.getInt(19));
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("144: " + e.getMessage());
			return null;
		}
	}
	
	// Construct a new door from a resultset.
	public Door newDoorFromRS(ResultSet rs, long doorUID, int permission, String playerUUID)
	{
		try
		{
			return new Door(UUID.fromString(playerUUID), Bukkit.getServer().getWorld(UUID.fromString(rs.getString(3))), rs.getInt(5), 
							rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), 
							rs.getInt(12), rs.getInt(13), rs.getString(2), (rs.getInt(4) == 1 ? true : false), 
							doorUID, (rs.getInt(14) == 1 ? true : false), permission, rs.getInt(15), DoorDirection.valueOf(rs.getInt(16)),
							rs.getInt(17), rs.getInt(18), rs.getInt(19));
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("160: " + e.getMessage());
			return null;
		}
	}
	
	// Remove a door with a given ID.
	public void removeDoor(long doorID)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			String deleteDoor    = "DELETE FROM doors WHERE id = '" + doorID + "';";
			PreparedStatement ps = conn.prepareStatement(deleteDoor);
			ps.executeUpdate();
			ps.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("179: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("189: " + e.getMessage());
			}
		}
	}
	
	// Remove a door with a given name, owned by a certain player.
	public void removeDoor(String playerUUID, String doorName)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			// Get the player ID as used in the sqlUnion table.
			PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
			ResultSet rs1         = ps1.executeQuery();
			while (rs1.next())
			{
				int playerID = rs1.getInt(1);
				// Select all doors from the sqlUnion table that have the previously found player as owner.
				PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "';");
				ResultSet rs2         = ps2.executeQuery();
				while (rs2.next())
				{
					// Delete all doors with the provided name owned by the provided player.
					PreparedStatement ps3 = conn.prepareStatement("DELETE FROM doors WHERE id = '" + rs2.getInt(4) 
															                + "' AND name = '" + doorName + "';");
					ps3.executeUpdate();
					ps3.close();
				}
				ps2.close();
				rs2.close();
			}
			ps1.close();
			rs1.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("226: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("236: " + e.getMessage());
			}
		}
	}

	// Get the door that has a powerBlock at the provided coordinates.
	public Door doorFromPowerBlockLoc(Location loc)
	{
		// Prepare door and connection.
		Door door       = null;
		Connection conn = null;
		try
		{
			conn = getConnection();
			// Get the door associated with the x/y/z location of the power block block.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE powerBlockX = '" + loc.getBlockX()	 + 
					                                                         "' AND powerBlockY = '" + loc.getBlockY()	 + 
					                                                         "' AND powerBlockZ = '" + loc.getBlockZ()	 +
					                                                         "' AND world = '"       + loc.getWorld().getUID().toString() + "';");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				long doorUID = rs.getLong(1);
				door = newDoorFromRS(rs, doorUID, -1);
			}
			ps.close();
			rs.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("265: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("275: " + e.getMessage());
			}
		}
		return door;
	}
	
	// Get Door from a doorID.
	public Door getDoor(long doorID)
	{
		Door door = null;
		
		Connection conn = null;
		try
		{
			conn = getConnection();
			PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors WHERE id = '" + doorID + "';");
			ResultSet rs1         = ps1.executeQuery();
			while (rs1.next())
			{
				String foundPlayerUUID = null;
				PreparedStatement ps2  = conn.prepareStatement("SELECT * FROM sqlUnion WHERE doorUID = '" + rs1.getLong(1)
																                     + "' AND permission = '" + 0 + "';");
				ResultSet rs2          = ps2.executeQuery();
				while (rs2.next())
				{
					PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM players WHERE id = '" + rs2.getInt(3) + "';");
					ResultSet rs3         = ps3.executeQuery();
					while (rs3.next())
						foundPlayerUUID   = rs3.getString(2);
					ps3.close();
					rs3.close();
				}
				ps2.close();
				rs2.close();
				
				door = this.newDoorFromRS(rs1, rs1.getLong(1), 0, foundPlayerUUID);
			}
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("297: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("307: " + e.getMessage());
			}
		}
		return door;
	}
	
	// Get ALL doors owned by a given playerUUID.
	public ArrayList<Door> getDoors(String playerUUID, String name)
	{
		return getDoors(playerUUID, name, 0, Long.MAX_VALUE);
	}
	
	// Get all doors with a given name.
	public ArrayList<Door> getDoors(String name)
	{
		ArrayList<Door> doors = new ArrayList<Door>();
		
		Connection conn = null;
		try
		{
			conn = getConnection();
			
			PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors WHERE name = '" + name + "';");
			ResultSet rs1         = ps1.executeQuery();
			
			while (rs1.next())
			{
				String foundPlayerUUID = null;
				int    permission      = -1;
				PreparedStatement ps2  = conn.prepareStatement("SELECT * FROM sqlUnion WHERE doorUID = '" + rs1.getLong(1) + "';");
				ResultSet rs2          = ps2.executeQuery();
				while (rs2.next())
				{
					permission            = rs2.getInt(2);
					PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM players WHERE id = '" + rs2.getInt(3) + "';");
					ResultSet rs3         = ps3.executeQuery();
					while (rs3.next())
						foundPlayerUUID   = rs3.getString(2);
					ps3.close();
					rs3.close();
				}
				ps2.close();
				rs2.close();
				
				doors.add(this.newDoorFromRS(rs1, rs1.getLong(1), permission, foundPlayerUUID));
			}
			ps1.close();
			rs1.close();
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("357: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("367: " + e.getMessage());
			}
		}
		return doors;
	}
	
	// Get all doors associated with this player in a given range. Name can be null
	public ArrayList<Door> getDoors(String playerUUID, String name, long start, long end)
	{
		ArrayList<Door> doors = new ArrayList<Door>();
		
		Connection conn = null;
		try
		{
			conn = getConnection();
			
			int playerID          = -1;
			PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
			ResultSet rs1         = ps1.executeQuery();
			while (rs1.next())
				playerID = rs1.getInt(1);
			
			PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "';");
			ResultSet rs2         = ps2.executeQuery();
			int count             = 0;
			while (rs2.next())
			{
				PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM doors WHERE id = '" + rs2.getInt(4) + "';");
				ResultSet rs3         = ps3.executeQuery();
				while (rs3.next())
				{
					if ((name == null || (name != null && rs3.getString(2).equals(name))) && count >= start && count <= end)
						doors.add(newDoorFromRS(rs3, rs3.getLong(1), rs2.getInt(2), playerUUID));
					++count;
				}
				rs3.close();
				rs3.close();
			}
			ps2.close();
			rs2.close();
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("357: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("367: " + e.getMessage());
			}
		}
		return doors;
	}
	
	// Update the door at doorUID with the provided coordinates and open status.
	public void updateDoorCoords(long doorID, int isOpen, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, DoorDirection engSide)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			String update = "UPDATE doors SET " 
					+   "xMin='" 			+ xMin 
					+ "',yMin='" 			+ yMin 
					+ "',zMin='" 			+ zMin 
					+ "',xMax='" 			+ xMax 
					+ "',yMax='" 			+ yMax 
					+ "',zMax='"	 			+ zMax 
					+ "',engineSide='"   	+ DoorDirection.getValue(engSide)
					+ "' WHERE id = '"	    + doorID + "';";
			conn.prepareStatement(update).executeUpdate();
			conn.commit();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("394: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("404: " + e.getMessage());
			}
		}
	}
	
	// Update the door with doorUID with the provided coordinates and open status.
	public void updateDoorCoords(long doorID, int isOpen, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			String update = "UPDATE doors SET " 
					+   "xMin='" 			+ xMin 
					+ "',yMin='" 			+ yMin 
					+ "',zMin='" 			+ zMin 
					+ "',xMax='" 			+ xMax 
					+ "',yMax='" 			+ yMax 
					+ "',zMax='"	 			+ zMax 
					+ "' WHERE id = '"	    + doorID + "';";
			conn.prepareStatement(update).executeUpdate();
			conn.commit();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("394: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("404: " + e.getMessage());
			}
		}
	}
	
	// Update the door with UID doorUID's Power Block Location with the provided coordinates and open status.
	public void updateDoorPowerBlockLoc(long doorID, int xPos, int yPos, int zPos)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			String update = "UPDATE doors SET " 
							+   "powerBlockX='" 	+ xPos 
							+ "',powerBlockY='" 	+ yPos 
							+ "',powerBlockZ='" 	+ zPos
							+ "' WHERE id = '"	+ doorID + "';";
			conn.prepareStatement(update).executeUpdate();
			conn.commit();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("394: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("404: " + e.getMessage());
			}
		}
	}

	// Check if a given location already contains a power block or not. 
	// Returns false if it's already occupied.
	public boolean isPowerBlockLocationEmpty(Location loc)
	{
		// Prepare door and connection.
		Connection conn = null;
		try
		{
			conn = getConnection();
			// Get the door associated with the x/y/z location of the power block block.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE powerBlockX = '" + loc.getBlockX()	 + 
					                                                         "' AND powerBlockY = '" + loc.getBlockY()	 + 
					                                                         "' AND powerBlockZ = '" + loc.getBlockZ()	 +
					                                                         "' AND world = '"       + loc.getWorld().getUID().toString() + "';");
			ResultSet rs = ps.executeQuery();
			boolean isAvailable = true;
			while (rs.next())
				isAvailable = false;
			ps.close();
			rs.close();
			return isAvailable;
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("265: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("275: " + e.getMessage());
			}
		}
		
		return false;
	}
	
	// Update the door at doorUID with the provided new lockstatus.
	public void setLock(long doorID, boolean newLockStatus)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			conn.setAutoCommit(false);
			String update 	= "UPDATE doors SET " 
							+   "isLocked='" + (newLockStatus == true ? 1 : 0)
							+ "' WHERE id='" + doorID + "';";
			conn.prepareStatement(update).executeUpdate();
			conn.commit();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("425: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("435: " + e.getMessage());
			}
		}
	}
	
	
	// Insert a new door in the db.
	public void insert(Door door)
	{
		Connection conn = null;
		try
		{
			conn = getConnection();
			
			long playerID = -1;
			PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + door.getPlayerUUID().toString() + "';");
			ResultSet rs1         = ps1.executeQuery();
			
			while (rs1.next())
				playerID = rs1.getLong(1);
			ps1.close();
			rs1.close();
			
			if (playerID == -1)
			{
				Statement stmt2 = conn.createStatement();
				String sql2     = "INSERT INTO players (playerUUID) "
						        + "VALUES ('" + door.getPlayerUUID().toString() + "');";
				stmt2.executeUpdate(sql2);
				stmt2.close();
				
				String query         = "SELECT last_insert_rowid() AS lastId";
				PreparedStatement p2 = conn.prepareStatement(query);
				ResultSet rs2        = p2.executeQuery();
				playerID             = rs2.getLong("lastId");
				stmt2.close();
			}
			
			String doorInsertsql	= "INSERT INTO doors(name,world,isOpen,xMin,yMin,zMin,xMax,yMax,zMax,engineX,engineY,engineZ,isLocked,type,engineSide,powerBlockX,powerBlockY,powerBlockZ) "
								+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			PreparedStatement doorstatement = conn.prepareStatement(doorInsertsql);
						
			doorstatement.setString(1, door.getName());
			doorstatement.setString(2, door.getWorld().getUID().toString());
			doorstatement.setInt(3,    door.getStatus() == true ? 1 : 0);
			doorstatement.setInt(4,    door.getMinimum().getBlockX());
			doorstatement.setInt(5,    door.getMinimum().getBlockY());
			doorstatement.setInt(6,    door.getMinimum().getBlockZ());
			doorstatement.setInt(7,    door.getMaximum().getBlockX());
			doorstatement.setInt(8,    door.getMaximum().getBlockY());
			doorstatement.setInt(9,    door.getMaximum().getBlockZ());
			doorstatement.setInt(10,   door.getEngine().getBlockX());
			doorstatement.setInt(11,   door.getEngine().getBlockY());
			doorstatement.setInt(12,   door.getEngine().getBlockZ());
			doorstatement.setInt(13,   door.isLocked() == true ? 1 : 0);
			doorstatement.setInt(14,   door.getType());
			// Get -1 if the door has no engineSide (normal doors don't use it)
			doorstatement.setInt(15,   door.getEngSide() == null ? -1 : DoorDirection.getValue(door.getEngSide()));
			doorstatement.setInt(16,   door.getEngine().getBlockX());
			doorstatement.setInt(17,   door.getEngine().getBlockY() - 1); // Power Block Location is 1 block below the engine, by default.
			doorstatement.setInt(18,   door.getEngine().getBlockZ());
			
			doorstatement.executeUpdate();
			doorstatement.close();
			
			String query         = "SELECT last_insert_rowid() AS lastId";
			PreparedStatement p2 = conn.prepareStatement(query);
			ResultSet rs2        = p2.executeQuery();
			Long doorID          = rs2.getLong("lastId");
			p2.close();
			rs2.close();
			
			Statement stmt3 = conn.createStatement();
			String sql3     = "INSERT INTO sqlUnion (permission, playerID, doorUID) "
			                + "VALUES ('" + door.getPermission() + "', '" + playerID + "', '" + doorID + "');";
			stmt3.executeUpdate(sql3);
			stmt3.close();
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("509: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("519: " + e.getMessage());
			}
		}
	}
	
	// Get the number of doors owned by this player.
	// If name is null, it will ignore door names, otherwise it will return the number of doors with the provided name.
	public long countDoors(String playerUUID, String name)
	{
		long count = 0;
		Connection conn = null;
		try
		{
			conn = getConnection();
			// Get the player ID as used in the sqlUnion table.
			PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
			ResultSet rs1         = ps1.executeQuery();
			while (rs1.next())
			{
				int playerID = rs1.getInt(1);
				// Select all doors from the sqlUnion table that have the previously found player as owner.
				PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "';");
				ResultSet rs2         = ps2.executeQuery();
				while (rs2.next())
				{
					// Retrieve the door with the provided ID.
					PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM doors WHERE id = '" + rs2.getInt(4) + "';");
					ResultSet rs3         = ps3.executeQuery();
					// Check if this door matches the provided name, if a name was provided.
					while (rs3.next())
						if (name == null || name != null && rs3.getString(2).equals(name))
							++count;
					ps3.close();
					rs3.close();
				}
				ps2.close();
				rs2.close();
			}
			ps1.close();
			rs1.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("562: " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("572: " + e.getMessage());
			}
		}
		return count;
	}
	
	// Add columns and such when needed (e.g. upgrades from older versions).
	private void update()
	{
		Connection conn = null;
		try
		{
			String addColumn;
			conn = DriverManager.getConnection(url);
			DatabaseMetaData md 	= conn.getMetaData();
			Statement stmt 		= conn.createStatement();
			
			ResultSet rs 		= md.getColumns(null, null, "doors", "type");
			if (!rs.next())
			{
				plugin.getMyLogger().logMessage("Updating database! Adding type!", true, false);
				addColumn		= "ALTER TABLE doors "
								+ "ADD COLUMN type int NOT NULL DEFAULT 0";
				stmt.execute(addColumn);
			}

			rs 					= md.getColumns(null, null, "doors", "engineSide");
			if (!rs.next())
			{
				plugin.getMyLogger().logMessage("Updating database! Adding engineSide!", true, false);
				addColumn     	= "ALTER TABLE doors "
			                  	+ "ADD COLUMN engineSide int NOT NULL DEFAULT -1";
				stmt.execute(addColumn);
			}
			
			rs 					= md.getColumns(null, null, "doors", "powerBlockX");
			if (!rs.next())
			{
				plugin.getMyLogger().logMessage("Updating database! Adding powerBlockLoc!", true, false);
				addColumn      	= "ALTER TABLE doors "
								+ "ADD COLUMN powerBlockX int NOT NULL DEFAULT -1";
				stmt.execute(addColumn);
				addColumn      	= "ALTER TABLE doors "
								+ "ADD COLUMN powerBlockY int NOT NULL DEFAULT -1";
				stmt.execute(addColumn);
				addColumn      	= "ALTER TABLE doors "
								+ "ADD COLUMN powerBlockZ int NOT NULL DEFAULT -1";
				stmt.execute(addColumn);
				PreparedStatement	ps1 = conn.prepareStatement("SELECT * FROM doors;");
				ResultSet rs1	= 	ps1.executeQuery();
				String update;
				
				while (rs1.next())
				{
					long UID 	= rs1.getLong(1);
					int x    	= rs1.getInt(11);
					int y    	= rs1.getInt(12) - 1;
					int z    	= rs1.getInt(13);
					update   	= "UPDATE doors SET " 
							 	+   "powerBlockX='" + x 
							 	+ "',powerBlockY='" + y
							 	+ "',powerBlockZ='" + z
							 	+ "' WHERE id = '"  + UID + "';";
					conn.prepareStatement(update).executeUpdate();
				}
				ps1.close();
				rs1.close();
			}
			stmt.close();
			rs.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("724 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch(SQLException e)
			{
				plugin.getMyLogger().logMessage("734 " + e.getMessage());
			}
		}
	}
	
//	private void addBlockLocValues()
//	{
//		Connection conn = null;
//		try
//		{
//			conn = DriverManager.getConnection(url);
//			PreparedStatement	ps1 = conn.prepareStatement("SELECT * FROM doors");
//			ResultSet rs1	= 	ps1.executeQuery();
//			String update;
//			while (rs1.next())
//			{
//				long UID = rs1.getLong(1);
//				int x    = rs1.getInt(11);
//				int y    = rs1.getInt(12) - 1;
//				int z    = rs1.getInt(13);
//				
//				update = "UPDATE doors SET " 
//						+   "powerBlockX='" 			+ x 
//						+ "',powerBlockY='" 			+ y
//						+ "',powerBlockZ='" 			+ z
//						+ "' WHERE id = '"	    + UID + "';";
//				conn.prepareStatement(update).executeUpdate();
//				
//			}
//			ps1.close();
//			rs1.close();
//		}
//		catch(SQLException e)
//		{
//			plugin.getMyLogger().logMessage("788 " + e.getMessage());
//		}
//		finally
//		{
//			try
//			{
//				conn.close();
//			}
//			catch(SQLException e)
//			{
//				plugin.getMyLogger().logMessage("798 " + e.getMessage());
//			}
//		}
//	}
}
