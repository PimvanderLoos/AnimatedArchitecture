package nl.pim16aap2.bigDoors.NMS;

import org.bukkit.Location;

// TODO: Dude... Really?
import net.minecraft.server.v1_13_R1.Block;
import net.minecraft.server.v1_13_R1.IBlockData;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public interface NMSBlock_Vall
{
	public Block getNMSBlock();
	public IBlockData getBlockData();
	public Block rotateBlock(RotateDirection rotDir);
	public void putBlock(Location loc);
	public void rotateBlockUpDown(DoorDirection openDirection, RotateDirection upDown);
}
