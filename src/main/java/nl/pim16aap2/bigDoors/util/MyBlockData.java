package nl.pim16aap2.bigDoors.util;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;

public class MyBlockData
{
	private Material    				       mat;
	private NMSBlock_Vall               	 block;
	private int                        	canRot;
	private int                   	    startY;
	private double      				    radius;
	private CustomCraftFallingBlock_Vall	fBlock;
	private MaterialData         	   matData;
	private Byte     				 blockByte;
	
	public MyBlockData(Material mat, Byte blockByte, CustomCraftFallingBlock_Vall fBlock, double radius, MaterialData matData, NMSBlock_Vall block, int canRot, int startY)
	{
		this.mat       = mat;
		this.block     = block;
		this.startY    = startY;
		this.canRot    = canRot;
		this.fBlock    = fBlock;
		this.radius    = radius;
		this.matData   = matData;
		this.blockByte = blockByte;
	}

	public void setFBlock(CustomCraftFallingBlock_Vall block)	{	this.fBlock = block;			}
	public CustomCraftFallingBlock_Vall getFBlock()    		{	return this.fBlock;			}
	public Material getMat()     							{	return this.mat;				}
	public MaterialData getMatData()							{	return this.matData;			}
	public double getRadius()								{	return this.radius;			}
	public Byte getBlockByte()								{	return this.blockByte;		}
	public void setBlockByte(Byte blockByte)					{	this.blockByte = blockByte;	}
	public NMSBlock_Vall getBlock()							{	return this.block;			}
	public int canRot()										{	return this.canRot;			}
	public int getStartY()									{	return this.startY;			}
}
