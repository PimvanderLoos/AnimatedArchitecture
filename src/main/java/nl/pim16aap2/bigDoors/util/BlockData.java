package nl.pim16aap2.bigDoors.util;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock_Vall;

public class BlockData
{
	private Material    				       mat;
	private double      				    radius;
	private CustomCraftFallingBlock_Vall	fBlock;
	private MaterialData         	   matData;
	private Byte     				 blockByte;
	
	public BlockData(Material mat, Byte blockByte, CustomCraftFallingBlock_Vall fBlock, double radius, MaterialData matData)
	{
		this.mat       = mat;
		this.fBlock    = fBlock;
		this.radius    = radius;
		this.matData   = matData;
		this.blockByte = blockByte;
	}

	public void setFBlock(CustomCraftFallingBlock_Vall block)	{	this.fBlock = block;		}
	public CustomCraftFallingBlock_Vall getFBlock()    		{	return this.fBlock;		}
	public Material getMat()     							{	return this.mat;			}
	public MaterialData getMatData()							{	return this.matData;		}
	public double getRadius()								{	return this.radius;		}
	public Byte getBlockByte()								{	return this.blockByte;	}
}
