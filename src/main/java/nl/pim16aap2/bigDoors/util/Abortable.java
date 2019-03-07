package nl.pim16aap2.bigDoors.util;

import org.bukkit.scheduler.BukkitTask;

public interface Abortable
{
	public void abort(boolean onDisable);
	public void abort();
	
	public void setTask(BukkitTask task);
	public BukkitTask getTask();
}
