package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.Synchronized;
import net.minecraft.server.v1_15_R1.Block;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.WorldServer;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
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
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * V1_15_R1 implementation of {@link INMSBlock}.
 *
 * @author Pim
 * @see INMSBlock
 */
public class NMSBlock_V1_15_R1 extends Block implements INMSBlock
{
    @SuppressWarnings("unused") // Appears unused, but it's referenced in annotations.
    private final Object blockDataLock = new Object();
    private final IPLogger logger;

    @GuardedBy("blockDataLock")
    private IBlockData blockData;
    private final BlockData bukkitBlockData;
    private final Location loc;
    private final CraftWorld craftWorld;

    private static Block.Info newBlockInfo(PWorldSpigot pWorld, BlockPosition blockPosition)
    {
        final CraftWorld craftWorld = (CraftWorld) Util.requireNonNull(pWorld.getBukkitWorld(), "BukkitWorld");
        final Block block = craftWorld.getHandle().getType(blockPosition).getBlock();
        return net.minecraft.server.v1_15_R1.Block.Info.a(block);
    }

    /**
     * Constructs a {@link NMSBlock_V1_15_R1}. Wraps the NMS block found in the given world at the provided
     * coordinates.
     *
     * @param pWorld
     *     The world the NMS block is in.
     * @param x
     *     The x coordinate of the NMS block.
     * @param y
     *     The y coordinate of the NMS block.
     * @param z
     *     The z coordinate of the NMS block.
     */
    NMSBlock_V1_15_R1(IPLogger logger, PWorldSpigot pWorld, int x, int y, int z)
    {
        super(newBlockInfo(pWorld, new BlockPosition(x, y, z)));
        this.logger = logger;

        final World bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(pWorld),
                                                      "Spigot world of world: " + pWorld);

        craftWorld = (CraftWorld) bukkitWorld;
        loc = new Location(bukkitWorld, x, y, z);

        bukkitBlockData = bukkitWorld.getBlockAt(x, y, z).getBlockData();
        if (bukkitBlockData instanceof Waterlogged waterlogged)
            waterlogged.setWaterlogged(false);

        constructBlockDataFromBukkit();
    }

    /**
     * Gets the NMS BlockData from the current {@link NMSBlock_V1_15_R1#bukkitBlockData}
     */
    @Synchronized("blockDataLock")
    private void constructBlockDataFromBukkit()
    {
        blockData = ((CraftBlockData) bukkitBlockData).getState();
    }

    /**
     * Gets the IBlockData (NMS) of this block.
     *
     * @return The IBlockData (NMS) of this block.
     */
    @Synchronized("blockDataLock")
    IBlockData getMyBlockData()
    {
        return blockData;
    }

    @Override
    @Synchronized("blockDataLock")
    public boolean canRotate()
    {
        return bukkitBlockData instanceof Orientable || bukkitBlockData instanceof Directional ||
            bukkitBlockData instanceof MultipleFacing;
    }

    @Override
    @Synchronized("blockDataLock")
//    @SuppressWarnings("squid:S2602") //
    public void rotateBlock(RotateDirection rotDir)
    {
        final BlockData bd = bukkitBlockData;
        // When rotating stairs vertically, they need to be rotated twice, as they cannot point up/down.
        if (bd instanceof Stairs &&
            (rotDir.equals(RotateDirection.NORTH) || rotDir.equals(RotateDirection.EAST) ||
                rotDir.equals(RotateDirection.SOUTH) || rotDir.equals(RotateDirection.WEST)))
            rotateDirectional((Directional) bd, rotDir, 2);
        else if (bd instanceof Orientable orientable)
            rotateOrientable(orientable, rotDir);
        else if (bd instanceof Directional directional)
            rotateDirectional(directional, rotDir);
        else if (bd instanceof MultipleFacing multipleFacing)
            rotateMultipleFacing(multipleFacing, rotDir);
        else
            return;
        constructBlockDataFromBukkit();
    }

    /**
     * Places the block at a given location.
     *
     * @param loc
     *     The location where the block will be placed.
     */
    @Override
    @Synchronized("blockDataLock")
    public void putBlock(IPLocation loc)
    {
        final BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        final WorldServer worldNMS = craftWorld.getHandle();
        final IBlockData old = worldNMS.getType(blockPosition);

        // Place the block, and don't apply physics.
        if (worldNMS.setTypeAndData(blockPosition, blockData, 1042))
            worldNMS.getMinecraftWorld().notify(blockPosition, old, blockData, 3);
    }

    /**
     * Rotates {@link Orientable} blockData in the provided {@link RotateDirection}.
     *
     * @param bd
     *     The {@link Orientable} blockData that will be rotated.
     * @param dir
     *     The {@link RotateDirection} the blockData will be rotated in.
     */
    @GuardedBy("blockDataLock")
    private void rotateOrientable(Orientable bd, RotateDirection dir)
    {
        rotateOrientable(bd, dir, 1);
    }

    /**
     * Rotates {@link Orientable} blockData in the provided {@link RotateDirection}.
     *
     * @param bd
     *     The {@link Orientable} blockData that will be rotated.
     * @param dir
     *     The {@link RotateDirection} the blockData will be rotated in.
     * @param steps
     *     the number of times the blockData will be rotated in the given direction.
     */
    @GuardedBy("blockDataLock")
    private void rotateOrientable(Orientable bd, RotateDirection dir, @SuppressWarnings("SameParameterValue") int steps)
    {
        final Axis currentAxis = bd.getAxis();
        Axis newAxis = currentAxis;
        // Every 2 steps results in the same outcome.
        int realSteps = steps % 2;
        if (realSteps == 0)
            return;

        while (realSteps-- > 0)
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
     * @param bd
     *     The {@link Directional} blockData that will be rotated.
     * @param dir
     *     The {@link RotateDirection} the blockData will be rotated in.
     */
    @GuardedBy("blockDataLock")
    private void rotateDirectional(Directional bd, RotateDirection dir)
    {
        rotateDirectional(bd, dir, 1);
    }

    /**
     * Rotates {@link Directional} blockData in the provided {@link RotateDirection}.
     *
     * @param bd
     *     The {@link Directional} blockData that will be rotated.
     * @param dir
     *     The {@link RotateDirection} the blockData will be rotated in.
     * @param steps
     *     the number of times the blockData will be rotated in the given direction.
     */
    @GuardedBy("blockDataLock")
    private void rotateDirectional(Directional bd, RotateDirection dir, int steps)
    {
        final @Nullable var mappedDir = PBlockFace.getDirFun(dir);
        if (mappedDir == null)
        {
            logger.logThrowable(
                new IllegalStateException("Failed to get face from vector " + dir +
                                              ". Rotations will not work as expected!"));
            return;
        }

        final BlockFace newFace = SpigotUtil.getBukkitFace(
            PBlockFace.rotate(SpigotUtil.getPBlockFace(bd.getFacing()), steps, mappedDir));
        if (bd.getFaces().contains(newFace))
            bd.setFacing(newFace);
    }

    /**
     * Rotates {@link MultipleFacing} blockData in the provided {@link RotateDirection}.
     *
     * @param bd
     *     The {@link MultipleFacing} blockData that will be rotated.
     * @param dir
     *     The {@link RotateDirection} the blockData will be rotated in.
     */
    @GuardedBy("blockDataLock")
    private void rotateMultipleFacing(MultipleFacing bd, RotateDirection dir)
    {
        rotateMultipleFacing(bd, dir, 1);
    }

    /**
     * Rotates {@link MultipleFacing} blockData in the provided {@link RotateDirection}.
     *
     * @param bd
     *     The {@link MultipleFacing} blockData that will be rotated.
     * @param dir
     *     The {@link RotateDirection} the blockData will be rotated in.
     * @param steps
     *     the number of times the blockData will be rotated in the given direction.
     */
    @GuardedBy("blockDataLock")
    private void rotateMultipleFacing(MultipleFacing bd, RotateDirection dir,
                                      @SuppressWarnings("SameParameterValue") int steps)
    {
        final @Nullable var mappedDir = PBlockFace.getDirFun(dir);
        if (mappedDir == null)
        {
            logger.logThrowable(
                new IllegalStateException("Failed to get face from vector " + dir +
                                              ". Rotations will not work as expected!"));
            return;
        }

        final Set<BlockFace> currentFaces = bd.getFaces();
        final Set<BlockFace> allowedFaces = bd.getAllowedFaces();
        currentFaces.forEach((blockFace) -> bd.setFace(blockFace, false));
        currentFaces.forEach(
            (blockFace) ->
            {
                final BlockFace newFace = SpigotUtil.getBukkitFace(
                    PBlockFace.rotate(SpigotUtil.getPBlockFace(blockFace), steps, mappedDir));
                if (allowedFaces.contains(newFace))
                    bd.setFace(newFace, true);
            });

        // This should never be disabled. The center column of a cobble wall, for
        // example, would be invisible otherwise.
        if (allowedFaces.contains(BlockFace.UP))
            bd.setFace(BlockFace.UP, true);
    }

    @Override
    @Synchronized("blockDataLock")
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
