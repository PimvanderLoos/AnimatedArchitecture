package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;

public final class MyBlockData
{
    private NMSBlock_Vall block;
    private boolean canRot;
    private float radius;
    private CustomCraftFallingBlock_Vall fBlock;
    private Location startLocation;
    private float startAngle;

    public MyBlockData(CustomCraftFallingBlock_Vall newFBlock, float radius, NMSBlock_Vall newBlock,
        boolean canRot, Location startLocation, float startAngle)
    {
        block = newBlock;
        fBlock = newFBlock;
        this.radius = radius;
        this.canRot = canRot;
        this.startLocation = startLocation;
        this.startAngle = startAngle;
    }

    public void setFBlock(CustomCraftFallingBlock_Vall block)
    {
        fBlock = block;
    }

    public CustomCraftFallingBlock_Vall getFBlock()
    {
        return fBlock;
    }

    public void killFBlock()
    {
        if (fBlock != null)
            fBlock.remove();
    }

    public float getRadius()
    {
        return radius;
    }

    public NMSBlock_Vall getBlock()
    {
        return block;
    }

    public boolean canRot()
    {
        return canRot;
    }

    public Location getStartLocation()
    {
        // block.getStartLocation() acts as a reference. I don't want that, so return a
        // copy instead.
        return new Location(startLocation.getWorld(), startLocation.getX(), startLocation.getY(), startLocation.getZ());
    }

    public double getStartX()
    {
        return startLocation.getX();
    }

    public double getStartY()
    {
        return startLocation.getY();
    }

    public double getStartZ()
    {
        return startLocation.getZ();
    }

    public float getStartAngle()
    {
        return startAngle;
    }

    public void setStartAngle(float newVal)
    {
        startAngle = newVal;
    }

    @Override
    public String toString()
    {
        String ret = "";
        ret += "********************************\n";
        ret += "  radius = " + radius + "\n";
        try
        {
            ret += "  block = " + block.toString() + "\n";
        }
        catch (Exception unhandled)
        {
            ret += "  block = " + "...\n";
        }
        ret += "  canRot = " + canRot + "\n";
        ret += "  startLoc = " + startLocation + "\n";
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
