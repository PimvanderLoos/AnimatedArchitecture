package nl.pim16aap2.bigdoors.spigot.v1_14_R1;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.WorldServer;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * V1_14_R1 implementation of {@link INMSBlock}.
 *
 * @author Pim
 * @see INMSBlock
 */
public class NMSBlock_V1_14_R1 extends net.minecraft.server.v1_14_R1.Block implements INMSBlock
{
    private IBlockData blockData;
    private final BlockData bukkitBlockData;
    private final Material mat;
    private final Location loc;
    private final CraftWorld craftWorld;

    /**
     * Constructs a {@link NMSBlock_V1_14_R1}. Wraps the NMS block found in the given world at the provided
     * coordinates.
     *
     * @param pWorld The world the NMS block is in.
     * @param x      The x coordinate of the NMS block.
     * @param y      The y coordinate of the NMS block.
     * @param z      The z coordinate of the NMS block.
     */
    NMSBlock_V1_14_R1(final @NotNull PWorldSpigot pWorld, final int x, final int y, final int z)
    {
        super(net.minecraft.server.v1_14_R1.Block.Info
                  .a(((CraftWorld) pWorld.getBukkitWorld()).getHandle().getType(new BlockPosition(x, y, z))
                                                           .getBlock()));

        World bukkitWorld = SpigotAdapter.getBukkitWorld(pWorld);
        craftWorld = (CraftWorld) bukkitWorld;
        loc = new Location(bukkitWorld, x, y, z);

        bukkitBlockData = bukkitWorld.getBlockAt(x, y, z).getBlockData();
        if (bukkitBlockData instanceof Waterlogged)
            ((Waterlogged) bukkitBlockData).setWaterlogged(false);

        constructBlockDataFromBukkit();
        mat = bukkitWorld.getBlockAt(x, y, z).getType();

        // Update iBlockData in NMS Block.
        super.o(blockData);
    }

    /**
     * Gets the NMS BlockData from the current {@link NMSBlock_V1_14_R1#bukkitBlockData}
     */
    private void constructBlockDataFromBukkit()
    {
        blockData = ((CraftBlockData) bukkitBlockData).getState();
    }

    /**
     * Gets the IBlockData (NMS) of this block.
     *
     * @return The IBlockData (NMS) of this block.
     */
    @NotNull
    IBlockData getMyBlockData()
    {
        return blockData;
    }

    @Override
    public boolean canRotate()
    {
        return bukkitBlockData instanceof Orientable || bukkitBlockData instanceof Directional ||
            bukkitBlockData instanceof MultipleFacing;
    }

    @Override
    public void rotateBlock(final @NotNull RotateDirection rotDir)
    {
        BlockData bd = bukkitBlockData;
        // When rotating stairs vertically, they need to be rotated twice, as they cannot point up/down.
        if (bd instanceof Stairs &&
            (rotDir.equals(RotateDirection.NORTH) || rotDir.equals(RotateDirection.EAST) ||
                rotDir.equals(RotateDirection.SOUTH) || rotDir.equals(RotateDirection.WEST)))
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

    /**
     * Places the block at a given location.
     *
     * @param loc The location where the block will be placed.
     */
    @Override
    public void putBlock(@NotNull IPLocationConst loc)
    {
        World bukkitWorld = SpigotAdapter.getBukkitWorld(loc.getWorld());
        if (bukkitWorld == null)
        {
            PLogger.get().logThrowable(new NullPointerException());
            return;
        }

        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        WorldServer worldNMS = craftWorld.getHandle();
        IBlockData old = worldNMS.getType(blockPosition);

        // Place the block, and don't apply physics.
        if (worldNMS.setTypeAndData(blockPosition, blockData, 1042))
            worldNMS.getMinecraftWorld().notify(blockPosition, old, blockData, 3);
    }

    /**
     * Rotates {@link Orientable} blockData in the provided {@link RotateDirection}.
     *
     * @param bd  The {@link Orientable} blockData that will be rotated.
     * @param dir The {@link RotateDirection} the blockData will be rotated in.
     */
    private void rotateOrientable(final @NotNull Orientable bd, final @NotNull RotateDirection dir)
    {
        rotateOrientable(bd, dir, 1);
    }

    /**
     * Rotates {@link Orientable} blockData in the provided {@link RotateDirection}.
     *
     * @param bd    The {@link Orientable} blockData that will be rotated.
     * @param dir   The {@link RotateDirection} the blockData will be rotated in.
     * @param steps the number of times the blockData will be rotated in the given direction.
     */
    private void rotateOrientable(final @NotNull Orientable bd, final @NotNull RotateDirection dir, int steps)
    {
        Axis currentAxis = bd.getAxis();
        Axis newAxis = currentAxis;
        // Every 2 steps results in the same outcome.
        steps = steps % 2;
        if (steps == 0)
            return;

        while (steps-- > 0)
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

    /**
     * Rotates {@link Directional} blockData in the provided {@link RotateDirection}.
     *
     * @param bd  The {@link Directional} blockData that will be rotated.
     * @param dir The {@link RotateDirection} the blockData will be rotated in.
     */
    private void rotateDirectional(final @NotNull Directional bd, final @NotNull RotateDirection dir)
    {
        rotateDirectional(bd, dir, 1);
    }

    /**
     * Rotates {@link Directional} blockData in the provided {@link RotateDirection}.
     *
     * @param bd    The {@link Directional} blockData that will be rotated.
     * @param dir   The {@link RotateDirection} the blockData will be rotated in.
     * @param steps the number of times the blockData will be rotated in the given direction.
     */
    private void rotateDirectional(final @NotNull Directional bd, final @NotNull RotateDirection dir, int steps)
    {
        BlockFace newFace = SpigotUtil.getBukkitFace(
            PBlockFace.rotate(SpigotUtil.getPBlockFace(bd.getFacing()), steps, PBlockFace.getDirFun(dir)));
        if (bd.getFaces().contains(newFace))
            bd.setFacing(newFace);
    }

    /**
     * Rotates {@link MultipleFacing} blockData in the provided {@link RotateDirection}.
     *
     * @param bd  The {@link MultipleFacing} blockData that will be rotated.
     * @param dir The {@link RotateDirection} the blockData will be rotated in.
     */
    private void rotateMultiplefacing(final @NotNull MultipleFacing bd, final @NotNull RotateDirection dir)
    {
        rotateMultiplefacing(bd, dir, 1);
    }

    /**
     * Rotates {@link MultipleFacing} blockData in the provided {@link RotateDirection}.
     *
     * @param bd    The {@link MultipleFacing} blockData that will be rotated.
     * @param dir   The {@link RotateDirection} the blockData will be rotated in.
     * @param steps the number of times the blockData will be rotated in the given direction.
     */
    private void rotateMultiplefacing(final @NotNull MultipleFacing bd, final @NotNull RotateDirection dir, int steps)
    {
        Set<BlockFace> currentFaces = bd.getFaces();
        Set<BlockFace> allowedFaces = bd.getAllowedFaces();
        currentFaces.forEach((blockFace) -> bd.setFace(blockFace, false));
        currentFaces.forEach(
            (blockFace) ->
            {
                BlockFace newFace = SpigotUtil.getBukkitFace(
                    PBlockFace.rotate(SpigotUtil.getPBlockFace(blockFace), steps, PBlockFace.getDirFun(dir)));
                if (allowedFaces.contains(newFace))
                    bd.setFace(newFace, true);
            });

        // This should never be disabled. The center column of a cobble wall, for
        // example, would be invisible otherwise.
        if (allowedFaces.contains(BlockFace.UP))
            bd.setFace(BlockFace.UP, true);
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
