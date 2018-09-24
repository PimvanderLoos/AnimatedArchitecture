package nl.pim16aap2.bigDoors.waitForCommand;

import org.bukkit.entity.Player;

public interface WaitForCommand
{
	public String getCommand();
	
	public boolean executeCommand(String[] args);
	
	public Player getPlayer();
}
