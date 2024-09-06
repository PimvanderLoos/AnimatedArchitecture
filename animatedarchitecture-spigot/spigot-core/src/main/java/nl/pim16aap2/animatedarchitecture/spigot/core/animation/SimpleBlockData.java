package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.animatedarchitecture.core.util.BlockFace;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotUtil;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Stairs;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents the data of a block that is animated.
 * <p>
 * This class is used to store the data of a block that is animated. These data are used to rotate the block in the
 * direction of the animation and to place the block back in the world when the animation finishes.
 */
@Flogger
public class SimpleBlockData implements IAnimatedBlockData
{
    private final IExecutor executor;
    @Getter
    private final BlockData blockData;
    private final AnimatedBlockDisplay animatedBlock;
    private final @Nullable Consumer<IAnimatedBlockData> blockDataRotator;
    private final Vector3Di originalPosition;
    private final World bukkitWorld;

    @AssistedInject
    private SimpleBlockData(
        @Assisted AnimatedBlockDisplay animatedBlock,
        @Assisted @Nullable Consumer<IAnimatedBlockData> blockDataRotator,
        @Assisted World bukkitWorld,
        @Assisted Vector3Di position,
        IExecutor executor)
    {
        this.executor = executor;
        this.animatedBlock = animatedBlock;
        this.blockDataRotator = blockDataRotator;
        this.originalPosition = position;
        this.bukkitWorld = bukkitWorld;
        this.blockData = bukkitWorld.getBlockAt(position.x(), position.y(), position.z()).getBlockData();
    }

    @Override
    public synchronized boolean canRotate()
    {
        return blockData instanceof Orientable ||
            blockData instanceof Directional ||
            blockData instanceof MultipleFacing;
    }

    @Override
    public synchronized boolean rotateBlock(MovementDirection movementDirection, int times)
    {
        // When rotating stairs vertically, they need to be rotated twice, as they cannot point up/down.
        switch (blockData)
        {
            case Stairs stairs when MovementDirection.isCardinalDirection(movementDirection) ->
                rotateDirectional(stairs, movementDirection, 2 * times);
            case Orientable orientable -> rotateOrientable(orientable, movementDirection, times);
            case Directional directional -> rotateDirectional(directional, movementDirection, times);
            case MultipleFacing multipleFacing -> rotateMultipleFacing(multipleFacing, movementDirection, times);
            default ->
            {
                return false;
            }
        }
        return true;
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
    private synchronized void rotateOrientable(Orientable bd, MovementDirection dir, int steps)
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
    private synchronized void rotateDirectional(Directional bd, MovementDirection dir, int steps)
    {
        final @Nullable var rotationFunction = BlockFace.getRotationFunction(dir);
        if (rotationFunction == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Failed to get face from vector '%s'. Rotations will not work as expected!",
                dir
            );
            return;
        }

        final org.bukkit.block.BlockFace newFace = SpigotUtil.getBukkitFace(
            BlockFace.rotate(SpigotUtil.getBlockFace(bd.getFacing()), steps, rotationFunction));
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
    private synchronized void rotateMultipleFacing(MultipleFacing bd, MovementDirection dir, int steps)
    {
        final @Nullable var rotationFunction = BlockFace.getRotationFunction(dir);
        if (rotationFunction == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Failed to get face from vector '%s'. Rotations will not work as expected!",
                dir
            );
            return;
        }

        final Set<org.bukkit.block.BlockFace> currentFaces = bd.getFaces();
        final Set<org.bukkit.block.BlockFace> allowedFaces = bd.getAllowedFaces();
        currentFaces.forEach((blockFace) -> bd.setFace(blockFace, false));
        currentFaces.forEach((blockFace) ->
        {
            final org.bukkit.block.BlockFace newFace =
                SpigotUtil.getBukkitFace(BlockFace.rotate(SpigotUtil.getBlockFace(blockFace), steps, rotationFunction));

            if (allowedFaces.contains(newFace))
                bd.setFace(newFace, true);
        });

        // This should never be disabled. The center column of a cobble wall, for
        // example, would be invisible otherwise.
        if (allowedFaces.contains(org.bukkit.block.BlockFace.UP))
            bd.setFace(org.bukkit.block.BlockFace.UP, true);
    }

    private void putBlock(Vector3Di loc, BlockData blockData)
    {
        final Block block = this.bukkitWorld.getBlockAt(loc.x(), loc.y(), loc.z());

        if (!loc.equals(this.originalPosition) && blockData instanceof Waterlogged waterlogged)
            waterlogged.setWaterlogged(block.isLiquid());

        block.setBlockData(blockData);
    }

    @Override
    public synchronized void putBlock(IVector3D loc)
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async block placement! THIS IS A BUG!");
            return;
        }

        animatedBlock.forEachHook("preBlockPlace", IAnimatedBlockHook::preBlockPlace);
        putBlock(loc.floor().toInteger(), getNewBlockData());
        animatedBlock.forEachHook("postBlockPlace", IAnimatedBlockHook::postBlockPlace);
    }

    private synchronized BlockData getNewBlockData()
    {
        try
        {
            if (canRotate() && this.blockDataRotator != null)
                this.blockDataRotator.accept(this);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to obtain rotated block data for block: '%s'", blockData);
        }
        return blockData;
    }

    private void deleteOriginalBlock(boolean applyPhysics)
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async block removal! THIS IS A BUG!");
            return;
        }
        this.bukkitWorld
            .getBlockAt(originalPosition.x(), originalPosition.y(), originalPosition.z())
            .setType(Material.AIR, applyPhysics);
    }

    @Override
    public synchronized void deleteOriginalBlock()
    {
        animatedBlock.forEachHook("prePutBlock", IAnimatedBlockHook::preDeleteOriginalBlock);
        deleteOriginalBlock(false);
        animatedBlock.forEachHook("postPutBlock", IAnimatedBlockHook::postDeleteOriginalBlock);
    }

    @Override
    public synchronized void postProcessStructureRemoval()
    {
        if (this.animatedBlock.isOnEdge())
            deleteOriginalBlock(true);
    }

    /**
     * Factory for creating {@link SimpleBlockData} instances.
     */
    @AssistedFactory
    public interface IFactory
    {
        /**
         * Creates a new {@link SimpleBlockData} instance.
         *
         * @param animatedBlock
         *     The {@link AnimatedBlockDisplay} that this block data belongs to.
         * @param blockDataRotator
         *     The block data rotator. This is used to rotate the block data.
         * @param bukkitWorld
         *     The world the block is in.
         * @param position
         *     The start position of the block.
         * @return The created {@link SimpleBlockData} instance.
         */
        SimpleBlockData create(
            AnimatedBlockDisplay animatedBlock,
            @Nullable Consumer<IAnimatedBlockData> blockDataRotator,
            World bukkitWorld,
            Vector3Di position
        );
    }
}
