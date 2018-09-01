package nl.pim16aap2.bigDoors.NMS.v1_13_R1;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;

import net.minecraft.server.v1_13_R1.BlockPosition;
import net.minecraft.server.v1_13_R1.EnumBlockRotation;
import net.minecraft.server.v1_13_R1.IBlockData;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class NMSBlock_V1_13_R1 extends net.minecraft.server.v1_13_R1.Block implements NMSBlock_Vall
{
//	private Block       nmsBlock;
	private IBlockData blockData;
	
	public NMSBlock_V1_13_R1(World world, int x, int y, int z)
	{
		super(net.minecraft.server.v1_13_R1.Block.Info.a(((CraftWorld) world).getHandle().getType(new BlockPosition(x, y, z)).getBlock()));
		this.blockData = ((CraftWorld) world).getHandle().getType(new BlockPosition(x, y, z));
//		this.nmsBlock  = blockData.getBlock();
		super.v(blockData);
	}
	
	public void rotateBlock(RotateDirection rotDir)
	{
		EnumBlockRotation rot;
		switch(rotDir)
		{
		case CLOCKWISE:
			rot = EnumBlockRotation.CLOCKWISE_90;
			break;
		case COUNTERCLOCKWISE:
			rot = EnumBlockRotation.COUNTERCLOCKWISE_90;
			break;
		default:
			rot = EnumBlockRotation.NONE;
		}
		this.blockData = blockData.a(rot);
//		this.nmsBlock  = blockData.getBlock();
//		Bukkit.broadcastMessage("BlockData = " + blockData);
//		BlockPosition blockPos = null;
//		EnumBlockFaceShape face = blockData.c(blockData, blockPos, EnumDirection.UP);
//		return this.nmsBlock;
	}

	@Override
	public void putBlock(Location loc)
	{
		((CraftWorld) loc.getWorld()).getHandle().setTypeAndData(
				new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), this.blockData, 1);
	}

	@Override
	public void rotateBlockUpDown(DoorDirection openDirection, RotateDirection upDown)
	{
		// TODO Auto-generated method stub
	}
}
