package nl.pim16aap2.bigDoors.util;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;

public class MyBlockData
{
	private Material    				       mat;
	private NMSBlock_Vall               	 block;
	private NMSBlock_Vall                block2;
	private double      				    radius;
	private CustomCraftFallingBlock_Vall	fBlock;
	private MaterialData         	   matData;
	private Byte     				 blockByte;
	
	public MyBlockData(Material mat, Byte blockByte, CustomCraftFallingBlock_Vall fBlock, double radius, MaterialData matData, NMSBlock_Vall block)
	{
		this.mat       = mat;
		this.block     = block;
		this.block2    = null;
		this.fBlock    = fBlock;
		this.radius    = radius;
		this.matData   = matData;
		this.blockByte = blockByte;
	}
	
	public MyBlockData(Material mat, Byte blockByte, CustomCraftFallingBlock_Vall fBlock, double radius, MaterialData matData, NMSBlock_Vall block, NMSBlock_Vall block2)
	{
		this(mat, blockByte, fBlock, radius, matData, block);
		this.block2 = block2;
	}

	public void setFBlock(CustomCraftFallingBlock_Vall block)	{	this.fBlock = block;		}
	public CustomCraftFallingBlock_Vall getFBlock()    		{	return this.fBlock;		}
	public Material getMat()     							{	return this.mat;			}
	public MaterialData getMatData()							{	return this.matData;		}
	public double getRadius()								{	return this.radius;		}
	public Byte getBlockByte()								{	return this.blockByte;	}
	public NMSBlock_Vall getBlock()							{	return this.block;		}
	public NMSBlock_Vall getBlock2()							{	return this.block2;		}
}
