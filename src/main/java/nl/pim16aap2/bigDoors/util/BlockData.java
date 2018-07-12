package nl.pim16aap2.bigDoors.util;

import org.bukkit.Material;

import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock_Vall;

public class BlockData
{
	private Material    				       mat;
	private double      				    radius;
	private CustomCraftFallingBlock_Vall	fBlock;
	private Byte     				 blockByte;
	
	public BlockData(Material mat, Byte blockByte, CustomCraftFallingBlock_Vall fBlock, double radius)
	{
		this.mat       = mat;
		this.fBlock    = fBlock;
		this.radius    = radius;
		this.blockByte = blockByte;
	}

	public void setFBlock(CustomCraftFallingBlock_Vall block)	{	this.fBlock = block;		}
	public CustomCraftFallingBlock_Vall getFBlock()    		{	return this.fBlock;		}
	public Material getMat()     							{	return this.mat;			}
	public double getRadius()								{	return this.radius;		}
	public Byte getBlockByte()								{	return this.blockByte;	}
}
