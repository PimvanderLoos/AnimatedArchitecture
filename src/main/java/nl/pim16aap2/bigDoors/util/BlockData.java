package nl.pim16aap2.bigDoors.util;

import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;

public class BlockData
{
	private Material    	mat;
	private double      	radius;
	private FallingBlock	fBlock;
	private Byte     	blockByte;
	
	public BlockData(Material mat, Byte blockByte, FallingBlock fBlock, double radius)
	{
		this.radius    = radius;
		this.mat       = mat;
		this.blockByte = blockByte;
		this.fBlock    = fBlock;
	}

	public void setFBlock(FallingBlock block) 	{	this.fBlock = block;		}
	public FallingBlock getFBlock()				{	return this.fBlock;		}
	public Material getMat()     				{	return this.mat;			}
	public double getRadius()					{	return this.radius;		}
	public Byte getBlockByte()					{	return this.blockByte;	}
}
