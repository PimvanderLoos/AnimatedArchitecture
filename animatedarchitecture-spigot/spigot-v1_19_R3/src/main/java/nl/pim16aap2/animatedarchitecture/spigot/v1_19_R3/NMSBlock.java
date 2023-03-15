package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.Synchronized;
import lombok.extern.flogger.Flogger;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.animatedarchitecture.core.util.BlockFace;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotUtil;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.block.data.CraftBlockData;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * v1_19_R3 implementation of {@link IAnimatedBlockData}.
 *
 * @author Pim
 * @see IAnimatedBlockData
 */
@Flogger
public class NMSBlock extends BlockBase implements IAnimatedBlockData
{
    @SuppressWarnings("unused") // Appears unused, but it's referenced in annotations.
    private final Object blockDataLock = new Object();
    private final CustomEntityFallingBlock animatedBlock;
    private final IExecutor executor;
    private final WorldServer worldServer;
    private final World bukkitWorld;

    @GuardedBy("blockDataLock")
    private IBlockData blockData;
    private final org.bukkit.block.data.BlockData bukkitBlockData;
    private final Location loc;

    private static BlockBase.Info newBlockInfo(CraftWorld craftWorld, BlockPosition blockPosition)
    {
        final Block block = craftWorld.getHandle().a_(blockPosition).b();
        return BlockBase.Info.a((BlockBase) block);
    }

    /**
     * Constructs a {@link NMSBlock}. Wraps the NMS block found in the given world at the provided coordinates.
     *
     * @param worldServer
     *     The world the NMS block is in.
     * @param x
     *     The x coordinate of the NMS block.
     * @param y
     *     The y coordinate of the NMS block.
     * @param z
     *     The z coordinate of the NMS block.
     */
    NMSBlock(
        CustomEntityFallingBlock animatedBlock, IExecutor executor, WorldServer worldServer, int x, int y, int z)
    {
        super(newBlockInfo(worldServer.getWorld(), new BlockPosition(x, y, z)));
        this.animatedBlock = animatedBlock;
        this.executor = executor;
        this.worldServer = worldServer;
        this.bukkitWorld = worldServer.getWorld();

        loc = new Location(worldServer.getWorld(), x, y, z);

        bukkitBlockData = worldServer.getWorld().getBlockAt(x, y, z).getBlockData();
        if (bukkitBlockData instanceof Waterlogged waterlogged)
            waterlogged.setWaterlogged(false);

        constructBlockDataFromBukkit();
    }

    /**
     * Gets the NMS BlockData from the current {@link NMSBlock#bukkitBlockData}
     */
    @Synchronized("blockDataLock")
    private void constructBlockDataFromBukkit()
    {
        blockData = ((CraftBlockData) bukkitBlockData).getState();
    }

    /**
     * @param blockData
     *     The new block data to apply.
     */
    @Synchronized("blockDataLock")
    void setBlockData(IBlockData blockData)
    {
        this.blockData = blockData;
        constructBlockDataFromBukkit();
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
        return bukkitBlockData instanceof Orientable ||
            bukkitBlockData instanceof Directional ||
            bukkitBlockData instanceof MultipleFacing;
    }

    @Override
    @Synchronized("blockDataLock")
    public boolean rotateBlock(MovementDirection movementDirection, int times)
    {
        final org.bukkit.block.data.BlockData bd = bukkitBlockData;
        // When rotating stairs vertically, they need to be rotated twice, as they cannot point up/down.
        if (bd instanceof Stairs &&
            (movementDirection.equals(MovementDirection.NORTH) || movementDirection.equals(MovementDirection.EAST) ||
                movementDirection.equals(MovementDirection.SOUTH) || movementDirection.equals(MovementDirection.WEST)))
            rotateDirectional((Directional) bd, movementDirection, 2 * times);
        else if (bd instanceof Orientable orientable)
            rotateOrientable(orientable, movementDirection, times);
        else if (bd instanceof Directional directional)
            rotateDirectional(directional, movementDirection, times);
        else if (bd instanceof MultipleFacing multipleFacing)
            rotateMultipleFacing(multipleFacing, movementDirection, times);
        else
            return false;
        constructBlockDataFromBukkit();
        return true;
    }

    @Override
    @Synchronized("blockDataLock")
    public void putBlock(IVector3D position)
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async block placement! THIS IS A BUG!");
            return;
        }
        final BlockPosition blockPosition = BlockPosition.a(position.xD(), position.yD(), position.zD());

        // net.minecraft.world.level.block.state.BlockState getBlockState(net.minecraft.core.BlockPos)
        final IBlockData old = worldServer.a_(blockPosition);

        // Place the block, and don't apply physics.
        // setBlock(net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState,int)
        if (worldServer.a(blockPosition, blockData, 1042))
            // sendBlockUpdated(net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState,
            //                  net.minecraft.world.level.block.state.BlockState,int)
            worldServer.getMinecraftWorld().a(blockPosition, old, blockData, 3);

        animatedBlock.forEachHook("putBlock", IAnimatedBlockHook::postBlockPlace);
    }

    /**
     * Rotates {@link Orientable} blockData in the provided {@link MovementDirection}.
     *
     * @param bd
     *     The {@link Orientable} blockData that will be rotated.
     * @param dir
     *     The {@link MovementDirection} the blockData will be rotated in.
     * @param steps
     *     the number of times the blockData will be rotated in the given direction.
     */
    @GuardedBy("blockDataLock")
    private void rotateOrientable(Orientable bd, MovementDirection dir, int steps)
    {
        final Axis currentAxis = bd.getAxis();
        Axis newAxis = currentAxis;
        // Every 2 steps results in the same outcome.
        int realSteps = steps % 2;
        if (realSteps == 0)
            return;

        while (realSteps-- > 0)
        {
            if (dir.equals(MovementDirection.NORTH) || dir.equals(MovementDirection.SOUTH))
            {
                if (currentAxis.equals(Axis.Z))
                    newAxis = Axis.Y;
                else if (currentAxis.equals(Axis.Y))
                    newAxis = Axis.Z;
            }
            else if (dir.equals(MovementDirection.EAST) || dir.equals(MovementDirection.WEST))
            {
                if (currentAxis.equals(Axis.X))
                    newAxis = Axis.Y;
                else if (currentAxis.equals(Axis.Y))
                    newAxis = Axis.X;
            }
            else if (dir.equals(MovementDirection.CLOCKWISE) || dir.equals(MovementDirection.COUNTERCLOCKWISE))
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
     * Rotates {@link Directional} blockData in the provided {@link MovementDirection}.
     *
     * @param bd
     *     The {@link Directional} blockData that will be rotated.
     * @param dir
     *     The {@link MovementDirection} the blockData will be rotated in.
     * @param steps
     *     the number of times the blockData will be rotated in the given direction.
     */
    @GuardedBy("blockDataLock")
    private void rotateDirectional(Directional bd, MovementDirection dir, int steps)
    {
        final @Nullable var mappedDir = BlockFace.getDirFun(dir);
        if (mappedDir == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Failed to get face from vector '%s'. Rotations will not work as expected!", dir);
            return;
        }

        final org.bukkit.block.BlockFace newFace = SpigotUtil.getBukkitFace(
            BlockFace.rotate(SpigotUtil.getBlockFace(bd.getFacing()), steps, mappedDir));
        if (bd.getFaces().contains(newFace))
            bd.setFacing(newFace);
    }

    /**
     * Rotates {@link MultipleFacing} blockData in the provided {@link MovementDirection}.
     *
     * @param bd
     *     The {@link MultipleFacing} blockData that will be rotated.
     * @param dir
     *     The {@link MovementDirection} the blockData will be rotated in.
     * @param steps
     *     the number of times the blockData will be rotated in the given direction.
     */
    @GuardedBy("blockDataLock")
    private void rotateMultipleFacing(MultipleFacing bd, MovementDirection dir, int steps)
    {
        final @Nullable var mappedDir = BlockFace.getDirFun(dir);
        if (mappedDir == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Failed to get face from vector '%s'. Rotations will not work as expected!", dir);
            return;
        }

        final Set<org.bukkit.block.BlockFace> currentFaces = bd.getFaces();
        final Set<org.bukkit.block.BlockFace> allowedFaces = bd.getAllowedFaces();
        currentFaces.forEach((blockFace) -> bd.setFace(blockFace, false));
        currentFaces.forEach(
            (blockFace) ->
            {
                final org.bukkit.block.BlockFace newFace = SpigotUtil.getBukkitFace(
                    BlockFace.rotate(SpigotUtil.getBlockFace(blockFace), steps, mappedDir));
                if (allowedFaces.contains(newFace))
                    bd.setFace(newFace, true);
            });

        // This should never be disabled. The center column of a cobble wall, for
        // example, would be invisible otherwise.
        if (allowedFaces.contains(org.bukkit.block.BlockFace.UP))
            bd.setFace(org.bukkit.block.BlockFace.UP, true);
    }

    @Override
    @Synchronized("blockDataLock")
    public String toString()
    {
        return blockData.toString();
    }

    @Override
    public void deleteOriginalBlock(boolean applyPhysics)
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async block removal! THIS IS A BUG!");
            return;
        }

        if (!applyPhysics)
        {
            bukkitWorld.getBlockAt(loc).setType(Material.AIR, false);
        }
        else
        {
            bukkitWorld.getBlockAt(loc).setType(Material.CAVE_AIR, false);
            bukkitWorld.getBlockAt(loc).setType(Material.AIR, true);
        }

        animatedBlock.forEachHook("deleteOriginalBlock", IAnimatedBlockHook::postDeleteOriginalBlock);
    }

    @Override
    public Item k()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Block q()
    {
        throw new UnsupportedOperationException();
    }
}
