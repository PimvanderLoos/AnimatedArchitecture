package nl.pim16aap2.bigDoors.util;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;

public class MyBlockData
{
    private Material                        mat;
    private NMSBlock_Vall                 block;
    private int                          canRot;
    private int                          startY;
    private double                       radius;
    private CustomCraftFallingBlock_Vall fBlock;
    private MaterialData                matData;
    private Byte                      blockByte;

    public MyBlockData(Material mat, Byte blockByte, CustomCraftFallingBlock_Vall fBlock, double radius,
                       MaterialData matData, NMSBlock_Vall block, int canRot, int startY)
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

    public MyBlockData(Material mat)
    {
        this.mat = mat;
        canRot   = 0;
    }

    public void setFBlock(CustomCraftFallingBlock_Vall block)  {  fBlock = block;              }
    public CustomCraftFallingBlock_Vall getFBlock()            {  return fBlock;               }
    public Material getMat()                                   {  return mat;                  }
    public MaterialData getMatData()                           {  return matData;              }
    public double getRadius()                                  {  return radius;               }
    public Byte getBlockByte()                                 {  return blockByte;            }
    public void setBlockByte(Byte blockByte)                   {  this.blockByte = blockByte;  }
    public NMSBlock_Vall getBlock()                            {  return block;                }
    public int canRot()                                        {  return canRot;               }
    public int getStartY()                                     {  return startY;               }

    @Override
    public String toString()
    {
        String ret = "";
        ret += "********************************\n";
        ret += "  mat = "        + mat       + "\n";
        ret += "  matData = "    + matData   + "\n";
        ret += "  radius = "     + radius    + "\n";
        ret += "  blockByte = "  + blockByte + "\n";
        try
        {
            ret += "  block = "  + block.toString()  + "\n";
        }
        catch (Exception unhandled)
        {
            ret += "  block = "  + "...\n";
        }
        ret += "  canRot = "     + canRot    + "\n";
        ret += "  startY = "     + startY    + "\n";
        try
        {
            ret += "  fBlock = " + fBlock.toString() + "\n";
        }
        catch (Exception unhandled)
        {
            ret += "  fBlock = " + "...\n";
        }
        ret += "********************************\n";
        return ret;
    }
}
