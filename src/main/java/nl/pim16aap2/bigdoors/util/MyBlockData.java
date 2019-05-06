package nl.pim16aap2.bigdoors.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import nl.pim16aap2.bigdoors.nms.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigdoors.nms.NMSBlock_Vall;

public class MyBlockData
{
    private Material                        mat;
    private NMSBlock_Vall                 block;
    private int                          canRot;
    private double                       radius;
    private CustomCraftFallingBlock_Vall fBlock;
    private MaterialData                matData;
    private Byte                      blockByte;
    private Location              startLocation;

    public MyBlockData(Material mat, Byte blockByte, CustomCraftFallingBlock_Vall fBlock, double radius,
                       MaterialData matData, NMSBlock_Vall block, int canRot, Location startLocation)
    {
        this.mat       = mat;
        this.block     = block;
        this.fBlock    = fBlock;
        this.radius    = radius;
        this.canRot    = canRot;
        this.matData   = matData;
        this.blockByte = blockByte;
        this.startLocation = startLocation;
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

    public Location getStartLocation()
    {
        // block.getStartLocation() acts as a reference. I don't want that, so return a copy instead.
        return new Location(startLocation.getWorld(),
                            startLocation.getX(),
                            startLocation.getY(),
                            startLocation.getZ());
    }

    public double getStartX()  {  return startLocation.getX();  }
    public double getStartY()  {  return startLocation.getY();  }
    public double getStartZ()  {  return startLocation.getZ();  }

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
        ret += "  canRot = "     + canRot        + "\n";
        ret += "  startLoc = "   + startLocation + "\n";
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
