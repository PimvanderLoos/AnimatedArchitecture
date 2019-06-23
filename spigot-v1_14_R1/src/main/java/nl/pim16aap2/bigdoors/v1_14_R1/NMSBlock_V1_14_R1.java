package nl.pim16aap2.bigdoors.v1_14_R1;

import java.util.Set;
import java.util.function.Function;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.IBlockData;
import nl.pim16aap2.bigdoors.api.NMSBlock_Vall;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class NMSBlock_V1_14_R1 extends net.minecraft.server.v1_14_R1.Block implements NMSBlock_Vall
{
    private IBlockData blockData;
    private BlockData bukkitBlockData;
    private Material mat;
    private Location loc;

    public NMSBlock_V1_14_R1(org.bukkit.World world, int x, int y, int z)
    {
        super(net.minecraft.server.v1_14_R1.Block.Info.a(((CraftWorld) world).getHandle().getType(new BlockPosition(x, y, z)).getBlock()));

        loc = new Location(world, x, y, z);

        bukkitBlockData = world.getBlockAt(x, y, z).getBlockData();
        if (bukkitBlockData instanceof Waterlogged)
            ((Waterlogged) bukkitBlockData).setWaterlogged(false);

        constructBlockDataFromBukkit();
        mat = world.getBlockAt(x, y, z).getType();

        // Update iBlockData in NMS Block.
        super.o(blockData);
    }

    private void constructBlockDataFromBukkit()
    {
        blockData = ((CraftBlockData) bukkitBlockData).getState();
    }

    public IBlockData getMyBlockData()
    {
        return blockData;
    }

    @Override
    public boolean canRotate()
    {
        return bukkitBlockData instanceof Orientable ||
            bukkitBlockData instanceof Directional ||
            bukkitBlockData instanceof MultipleFacing;
    }

    @Override
    public void rotateBlock(RotateDirection rotDir)
    {
        BlockData bd = bukkitBlockData;
        // When rotating stairs vertically, they need to be rotated twice, as they cannot
        // point up/down.
        if (bd instanceof Stairs && (rotDir.equals(RotateDirection.NORTH) ||
            rotDir.equals(RotateDirection.EAST) || rotDir.equals(RotateDirection.SOUTH) ||
            rotDir.equals(RotateDirection.WEST)))
            rotateDirectional((Directional) bd, rotDir, 2);
        else if (bd instanceof Orientable)
            rotateOrientable((Orientable) bd, rotDir);
        else if (bd instanceof Directional)
            rotateDirectional((Directional) bd, rotDir);
        else if (bd instanceof MultipleFacing)
            rotateMultiplefacing((MultipleFacing) bd, rotDir);
        else
            return;
        constructBlockDataFromBukkit();
    }

    private void rotateOrientable(Orientable bd, RotateDirection dir)
    {
        rotateOrientable(bd, dir, 1);
    }

    private void rotateOrientable(Orientable bd, RotateDirection dir, int steps)
    {
        Axis currentAxis = bd.getAxis();
        Axis newAxis = currentAxis;
        // Every 2 steps results in the same outcome.
        steps = steps % 2;
        if (steps == 0)
            return;

        while (steps --> 0)
        {
            if (dir.equals(RotateDirection.NORTH) || dir.equals(RotateDirection.SOUTH))
            {
                if (currentAxis.equals(Axis.Z))
                    newAxis = Axis.Y;
                else if (currentAxis.equals(Axis.Y))
                    newAxis = Axis.Z;
            }
            else if (dir.equals(RotateDirection.EAST) || dir.equals(RotateDirection.WEST))
            {
                if (currentAxis.equals(Axis.X))
                    newAxis = Axis.Y;
                else if (currentAxis.equals(Axis.Y))
                    newAxis = Axis.X;
            }
            else if (dir.equals(RotateDirection.CLOCKWISE) || dir.equals(RotateDirection.COUNTERCLOCKWISE))
            {
                if (bd.getAxis().equals(Axis.X))
                    newAxis = Axis.Z;
                else if (bd.getAxis().equals(Axis.Z))
                    newAxis = Axis.X;
            }
        }
        if (bd.getAxes().contains(newAxis))
            bd.setAxis(newAxis);
    }

    private void rotateDirectional(Directional bd, RotateDirection dir)
    {
        rotateDirectional(bd, dir, 1);
    }

    private void rotateDirectional(Directional bd, RotateDirection dir, int steps)
    {
        BlockFace newFace = SpigotUtil.getBukkitFace(rotate(SpigotUtil.getMyBlockFace(bd.getFacing()), steps, getDirFun(dir)));
        if (bd.getFaces().contains(newFace))
            bd.setFacing(newFace);
    }

    private void rotateMultiplefacing(MultipleFacing bd, RotateDirection dir)
    {
        rotateMultiplefacing(bd, dir, 1);
    }

    private void rotateMultiplefacing(MultipleFacing bd, RotateDirection dir, int steps)
    {
        Set<BlockFace> currentFaces = bd.getFaces();
        Set<BlockFace> allowedFaces = bd.getAllowedFaces();
        currentFaces.forEach((K) -> bd.setFace(K, false));
        currentFaces.forEach((K) ->
        {
            BlockFace newFace = SpigotUtil.getBukkitFace(rotate(SpigotUtil.getMyBlockFace(K), steps, getDirFun(dir)));
            if (allowedFaces.contains(newFace))
                bd.setFace(newFace, true);
        });

        // This should never be disabled. The center column of a cobble wall, for example, would be invisible otherwise.
        if (allowedFaces.contains(BlockFace.UP))
            bd.setFace(BlockFace.UP, true);
    }

    private Function<MyBlockFace, MyBlockFace> getDirFun(RotateDirection rotDir)
    {
        switch(rotDir)
        {
        case NORTH:
            return MyBlockFace::rotateVerticallyNorth;
        case EAST:
            return MyBlockFace::rotateVerticallyEast;
        case SOUTH:
            return MyBlockFace::rotateVerticallySouth;
        case WEST:
            return MyBlockFace::rotateVerticallyWest;
        case CLOCKWISE:
            return MyBlockFace::rotateClockwise;
        case COUNTERCLOCKWISE:
            return MyBlockFace::rotateCounterClockwise;
        case DOWN:
        case UP:
        case NONE:
        default:
            return null;
        }
    }

    private MyBlockFace rotate(MyBlockFace mbf, int steps, Function<MyBlockFace, MyBlockFace> dir)
    {
        // Every 4 steps results in the same outcome.
        steps = steps % 4;
        if (steps == 0)
            return mbf;

        MyBlockFace newFace = mbf;
        while (steps --> 0)
            newFace = dir.apply(mbf);
        return newFace;
    }

    @Override
    public void putBlock(Location loc)
    {
        ((CraftWorld) loc.getWorld()).getHandle().setTypeAndData(
                new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), blockData, 1);
        if (SpigotUtil.needsRefresh(mat))
        {
            loc.getWorld().getBlockAt(loc).setType(Material.AIR);
            loc.getWorld().getBlockAt(loc).setType(mat);
        }
    }

    @Override
    public String toString()
    {
        return blockData.toString();
    }

    @Override
    public void deleteOriginalBlock()
    {
        loc.getBlock().setType(Material.AIR);
    }
}
