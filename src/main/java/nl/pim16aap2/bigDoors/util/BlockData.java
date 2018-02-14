package nl.pim16aap2.bigDoors.util;

import org.bukkit.Material;

import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock;

public class BlockData
{
	private Material    			mat;
	private double      			radius;
	private CustomCraftFallingBlock	fBlock;
	private Byte     			blockByte;
	
	public BlockData(Material mat, Byte blockByte, CustomCraftFallingBlock fBlock, double radius)
	{
		this.radius    = radius;
		this.mat       = mat;
		this.blockByte = blockByte;
		this.fBlock    = fBlock;
	}

	public void setFBlock(CustomCraftFallingBlock block) 	{	this.fBlock = block;		}
	public CustomCraftFallingBlock getFBlock()			{	return this.fBlock;		}
	public Material getMat()     					{	return this.mat;			}
	public double getRadius()						{	return this.radius;		}
	public Byte getBlockByte()						{	return this.blockByte;	}
}
