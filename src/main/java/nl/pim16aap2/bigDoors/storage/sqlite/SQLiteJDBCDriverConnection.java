package nl.pim16aap2.bigDoors.storage.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Util;

public class SQLiteJDBCDriverConnection
{
	BigDoors plugin;
	String   dbName;
	String   path;
	File     dataFolder;
	String   url;

	public SQLiteJDBCDriverConnection(BigDoors plugin, String dbName)
	{
		this.plugin     = plugin;
		this.path       = "/" + plugin.getDataFolder() + dbName + ".db";
		this.dbName     = dbName;
		this.dataFolder = new File(plugin.getDataFolder(), dbName + ".db");
		this.url        = "jdbc:sqlite:" + dataFolder;
	}
	
	// Remove all doors owned by player playerUUID with the provided name.
	// Name can NOT be null!
	public void removeDoors(String playerUUID, String name)
	{
		if (name == null)
			return;
		// Prepare arrayList of doors.
		Connection conn = null;
		try
		{
			conn = DriverManager.getConnection(url);
			// get all doors owned by player playerUUID.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				// Get the door attached to this entry.
				int doorUID = rs.getInt(4);
				PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM doors WHERE doorUID = '" + doorUID + "';");
				ResultSet rs2 = ps2.executeQuery();
				// Check if this door matches the provided name, if a name was provided.
				while (rs2.next())
					if (rs2.getString(2).equals(name))
					{
						conn = DriverManager.getConnection(url);
						conn.setAutoCommit(false);
						String delDoorFromPlayers = "DELETE FROM players WHERE doorUID='" + doorUID +"';";
						conn.prepareStatement(delDoorFromPlayers).executeUpdate();
						conn.commit();
						
						String delDoor = "DELETE FROM doors WHERE doorUID='" + doorUID + "';";
						conn.prepareStatement(delDoor).executeUpdate();
						conn.commit();
					}
				ps2.close();
				rs2.close();
			}
			ps.close();
			rs.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("113 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("123 " + e.getMessage());
			}
		}
	}
	
	public Door doorFromEngineLoc(int engineX, int engineY, int engineZ)
	{
		// Prepare door.
		Door door       = null;
		Connection conn = null;
		try
		{
			conn = DriverManager.getConnection(url);
			// Get the door associated with the x/y/z location of the engine block (block with lowest y-pos of rotation point).
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE engineX = '" + engineX + 
					                                                         "' AND engineY = '" + engineY + 
					                                                         "' AND engineZ = '" + engineZ + "';");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				int doorUID = rs.getInt(1);
				door = newDoorFromRS(rs, doorUID);
			}
			ps.close();
			rs.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("113 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("123 " + e.getMessage());
			}
		}
		return door;
	}
	
	// Remove a door with the provided doorUID.
	public void removeDoor(int doorUID)
	{
		Connection conn = null;		
		try
		{
			conn = DriverManager.getConnection(url);
			conn.setAutoCommit(false);
			String delDoorFromPlayers = "DELETE FROM players WHERE doorUID='" + doorUID +"';";
			conn.prepareStatement(delDoorFromPlayers).executeUpdate();
			conn.commit();
			
			// Doing the same thing twice? Seems so. Using only one of these first method only sets the door value in the player table to -1, while
			// Using only the second method doesn't remove either the door or the player entry at all.
			// TODO: Sort this shit out. Also gives an error that this is not implemented by JDBC. Weird stuff.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE doorUID = '" + doorUID + "';");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				rs.deleteRow();
			rs.close();
				
			String delDoor = "DELETE FROM doors WHERE doorUID='" + doorUID + "';";
			conn.prepareStatement(delDoor).executeUpdate();
			conn.commit();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("72 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("82 " + e.getMessage());
			}
		}
	}
	
	// Get the door with the provided doorUID.
	public Door getDoor(int doorUID)
	{
		Door door = null;
		Connection conn = null;
		
		try
		{
			conn = DriverManager.getConnection(url);
			// Get all doors with the provided doorUID (which can be only 1) and return it.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE doorUID = '" + doorUID + "';");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				door = (newDoorFromRS(rs, doorUID));
			ps.close();
			rs.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("109 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("119 " + e.getMessage());
			}
		}
		
		return door;
	}
	
	// Construct a new door from a resultset.
	public Door newDoorFromRS(ResultSet rs, int doorUID)
	{
		try
		{
			return new Door(null, Bukkit.getServer().getWorld(UUID.fromString(rs.getString(3))), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13), rs.getString(2), (rs.getInt(4) == 1 ? true : false), doorUID, false);
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("132 " + e.getMessage());
			return null;
		}
	}
	
	public ArrayList<Door> getDoors(String playerUUID, String name)
	{
		return getDoors(playerUUID, name, 0, Integer.MAX_VALUE);
	}
	
	// Get a list of doors owned by player playerUUID and named name.
	// Name can be null, in which case it will return all doors by the player with any name.
	public ArrayList<Door> getDoors(String playerUUID, String name, int start, int end)
	{
		// Prepare arrayList of doors.
		ArrayList<Door> doors = new ArrayList<Door>();
		Connection conn = null;
		try
		{
			conn = DriverManager.getConnection(url);
			// get all doors owned by player playerUUID.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
			ResultSet rs = ps.executeQuery();
			int count = 0;
			while (rs.next())
			{
				// Get the door attached to this entry.
				int doorUID = rs.getInt(4);
				PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM doors WHERE doorUID = '" + doorUID + "';");
				ResultSet rs2 = ps2.executeQuery();
				// Check if this door matches the provided name, if a name was provided.
				while (rs2.next())
				{
					if ((name == null || (name != null && rs2.getString(2).equals(name))) && count >= start && count <= end)
						doors.add(newDoorFromRS(rs2, doorUID));
					count += 1;
				}
				ps2.close();
				rs2.close();
			}
			ps.close();
			rs.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("113 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("123 " + e.getMessage());
			}
		}
		return doors;
	}
	
	
	public boolean isNameAvailable(String name, String playerUUID)
	{
		// TODO: Check if the player already have a door with the provided name?
		// Discussion: Perhaps this check should be removed. If door-sharing is implemented, this wouldn't work.
		// However, how would opening doors using commands work then? Should names be completely deprecated? If so, what to use as identifier?
		// Perhaps use names by default and require ID specification when dealing with "overloaded" names?
		
//		Connection conn = null;
		boolean ret = true;	// Return true, as idc much about name availability atm. Keeping this function in case I change my mind.
		
		return ret;
	}
	
	// Update the door at doorUID with the provided coordinates and open status.
	public void updateDoorCoords(int doorUID, int isOpen, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax)
	{
		Connection conn = null;
		try
		{
			conn = DriverManager.getConnection(url);
			conn.setAutoCommit(false);
			String update = "UPDATE doors SET " 
							+   "xMin='" 			+ xMin 
							+ "',yMin='" 			+ yMin 
							+ "',zMin='" 			+ zMin 
							+ "',xMax='" 			+ xMax 
							+ "',yMax='" 			+ yMax 
							+ "',zMax='"	 			+ zMax 
							+ "' WHERE doorUID='"	+ doorUID + "';";
			conn.prepareStatement(update).executeUpdate();
			conn.commit();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("288 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("298 " + e.getMessage());
			}
		}
	}
	
	// Insert a new door in the db.
	public void insert(Door door, String playerUUID)
	{
		Connection conn = null;
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
		
		try
		{
			conn = DriverManager.getConnection(url);
			//Log that SQLite connection has been established
		}
		catch(SQLException e)
		{
			//Log that SQLite connection has failed, and terminate the plugin
			plugin.getMyLogger().logMessage("139 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch(SQLException e)
			{
				plugin.getMyLogger().logMessage("149 " + e.getMessage());
			}
		}

		// Table creation
		try
		{
			conn = DriverManager.getConnection(url);
			Statement stmt    = conn.createStatement();
			String sqlDoors   = "CREATE TABLE IF NOT EXISTS doors (\n"   + " doorUID integer PRIMARY KEY autoincrement,\n" + " name text NOT NULL,\n"       + " world text NOT NULL,\n"     + " isOpen int NOT NULL,\n" + " xMin int NOT NULL,\n" + " yMin int NOT NULL,\n" + " zMin int NOT NULL,\n" + " xMax int NOT NULL,\n" + " yMax int NOT NULL,\n" + " zMax int NOT NULL,\n" + " engineX int NOT NULL,\n" + " engineY int NOT NULL,\n" + " engineZ int NOT NULL\n" + ");";
			String sqlPlayers = "CREATE TABLE IF NOT EXISTS players (\n" + " id integer primary key autoincrement,\n"      + " playerUUID text NOT NULL,\n" + " permission int NOT NULL,\n" + " doorUID int NOT NULL\n" + ");";
			
			stmt.execute(sqlDoors);
			stmt.execute(sqlPlayers);

			conn.close();
		}
		catch (SQLException e)
		{
			plugin.getMyLogger().logMessage("166 " + e.getMessage());
		}

		int doorUID = -1;

		// Adding a door
		try
		{
			conn = DriverManager.getConnection(url);
			boolean duplicate = false;
			// First find the UID of the door to be deletd.
			String worldUID   = door.getWorld().getUID().toString();
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE name = '" + door.getName().toString() + "';");
			ResultSet rs      = ps.executeQuery();
			int minX = door.getMinimum().getBlockX(), minY = door.getMinimum().getBlockY(), minZ = door.getMinimum().getBlockZ();
			int maxX = door.getMaximum().getBlockX(), maxY = door.getMaximum().getBlockY(), maxZ = door.getMaximum().getBlockZ();
			while (rs.next())
			{
				// If the min positions and the max positions are the same and they have the same name, it's the same door
				if ((rs.getInt("xMin") == minX && rs.getInt("yMin") == minY && rs.getInt("zMin") == minZ) &&
				    (rs.getInt("xMax") == maxX && rs.getInt("yMax") == maxY && rs.getInt("zMax") == maxZ))
				{
					duplicate = true;
					Util.messagePlayer(Bukkit.getPlayer(UUID.fromString(playerUUID)), ChatColor.RED, "Unable to add door as it already exists!");
				}
			}
			ps.close();
			rs.close();
			
			if (!duplicate)
			{
				String doorInsertsql = "INSERT INTO doors(doorUID,name,world,isOpen,xMin,yMin,zMin,xMax,yMax,zMax,engineX,engineY,engineZ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
				PreparedStatement doorstatement = conn.prepareStatement(doorInsertsql);
				//ID is auto-incremented, so we'll have to fetch that in a bit.
				
				doorstatement.setString(2, door.getName());
				doorstatement.setString(3, door.getWorld().getUID().toString());
				doorstatement.setInt(4,    door.getStatus() == true ? 1 : 0);
				doorstatement.setInt(5,    door.getMinimum().getBlockX());
				doorstatement.setInt(6,    door.getMinimum().getBlockY());
				doorstatement.setInt(7,    door.getMinimum().getBlockZ());
				doorstatement.setInt(8,    door.getMaximum().getBlockX());
				doorstatement.setInt(9,    door.getMaximum().getBlockY());
				doorstatement.setInt(10,   door.getMaximum().getBlockZ());
				doorstatement.setInt(11,   door.getEngine().getBlockX());
				doorstatement.setInt(12,   door.getEngine().getBlockY());
				doorstatement.setInt(13,   door.getEngine().getBlockZ());
				
				doorstatement.executeUpdate();
				doorstatement.close();
				
				ps = conn.prepareStatement("SELECT * FROM doors WHERE name = '" + door.getName().toString() + "';");
				rs = ps.executeQuery();
				while (rs.next())
				{
					if (rs.getString("world").equalsIgnoreCase(worldUID.toLowerCase()) && rs.getInt("xMin") == minX && rs.getInt("yMin") == minY && rs.getInt("zMin") == minZ)
					{
						doorUID = rs.getInt(1);
						break;
					}
				}
				rs.close();
				ps.close();
			}
			conn.close();
		}

		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("213 " + e.getMessage());
		}
		
		try
		{
			conn = DriverManager.getConnection(url);
			boolean duplicate = false;
			// First find the UID of the door to be deletd.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "' AND doorUID = '" + doorUID + "';");
			ResultSet rs      = ps.executeQuery();
			while (rs.next())
			{	
				duplicate = true;
				Util.messagePlayer(Bukkit.getPlayer(UUID.fromString(playerUUID)), ChatColor.RED, "Unable to add door to user as this combination already exists!");
			}
			ps.close();
			rs.close();
			
			if (!duplicate)
			{
				PreparedStatement pstmtPlayers = conn.prepareStatement("INSERT INTO players(id, playerUUID, permission, doorUID) VALUES(?,?,?,?)");
				pstmtPlayers.setString(2, playerUUID);	// Add playerUUID to table.
				pstmtPlayers.setInt(3, 0);				// Add permission level 0 (owner level). It's a new door, so playerUUID == owner.
				pstmtPlayers.setInt(4, doorUID);			// Add doorUID. Because... Well, it's all about them doors, ya know... I mean, it's in the plugin name after all.
				try
				{
					pstmtPlayers.executeUpdate();
					pstmtPlayers.close();
				}
				catch (SQLException e)
				{
					plugin.getMyLogger().logMessage("480 " + e.getMessage());
				}
				pstmtPlayers.close();
			}
			conn.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("318 " + e.getMessage());
		}
	}

	// Get the number of doors owned by this player.
	// If name is null, it will ignore doornames, otherwise it will return the number of doors with the provided name.
	public long countDoors(String playerUUID, String name)
	{
		long count = 0;
		Connection conn = null;
		try
		{
			conn = DriverManager.getConnection(url);
			// get all doors owned by player playerUUID.
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				// Get the door attached to this entry.
				int doorUID = rs.getInt(4);
				PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM doors WHERE doorUID = '" + doorUID + "';");
				ResultSet rs2 = ps2.executeQuery();
				// Check if this door matches the provided name, if a name was provided.
				while (rs2.next())
					if (name == null || name != null && rs2.getString(2).equals(name))
						++count;
				ps2.close();
				rs2.close();
			}
			ps.close();
			rs.close();
		}
		catch(SQLException e)
		{
			plugin.getMyLogger().logMessage("451 " + e.getMessage());
		}
		finally
		{
			try
			{
				conn.close();
			}
			catch (SQLException e)
			{
				plugin.getMyLogger().logMessage("461 " + e.getMessage());
			}
		}
		return count;
	}
}
