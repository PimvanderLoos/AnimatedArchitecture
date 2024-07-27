package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery.IAnimatedBlockRecoveryData;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.blockstate.BlockStateManipulator;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.LocationSpigot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Represents an animated block display. These are the blocks that the player sees moving around in the world.
 */
@Flogger
public final class AnimatedBlockDisplay implements IAnimatedBlockSpigot
{
    private final BlockDisplayHelper blockDisplayHelper;

    private final SimpleBlockData blockData;
    private final IAnimatedBlockRecoveryData recoveryData;

    @Getter
    private final RotatedPosition finalPosition;
    @Getter
    private final float radius;
    @Getter
    private final boolean onEdge;
    private final List<IAnimatedBlockHook> hooks;
    private final IExecutor executor;
    @Getter
    private final RotatedPosition startPosition;
    @Getter
    private final IWorld world;
    @Getter
    private final World bukkitWorld;

    @GuardedBy("this")
    private @Nullable BlockDisplay entity;
    @GuardedBy("this")
    private RotatedPosition previousTarget;
    @GuardedBy("this")
    private RotatedPosition currentTarget;

    public AnimatedBlockDisplay(
        BlockStateManipulator blockStateManipulator,
        BlockDisplayHelper blockDisplayHelper,
        IExecutor executor,
        AnimatedBlockHookManager animatedBlockHookManager,
        @Nullable Consumer<IAnimatedBlockData> blockDataRotator,
        RotatedPosition startPosition,
        IWorld world,
        RotatedPosition finalPosition,
        boolean onEdge,
        float radius)
    {
        this.blockDisplayHelper = blockDisplayHelper;
        this.executor = executor;
        this.startPosition = startPosition;
        this.world = world;
        this.bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "Bukkit World");
        this.onEdge = onEdge;
        this.finalPosition = finalPosition;
        this.radius = radius;
        this.currentTarget = this.startPosition;
        this.previousTarget = currentTarget;

        this.blockData = new SimpleBlockData(
            blockStateManipulator,
            this,
            executor,
            blockDataRotator,
            bukkitWorld,
            this.startPosition.position().floor().toInteger()
        );

        this.recoveryData = IAnimatedBlockRecoveryData.of(
            bukkitWorld,
            this.startPosition.position().floor().toInteger(),
            this.blockData.getBlockData(),
            this.blockData.getBlockState()
        );

        this.hooks = animatedBlockHookManager.instantiateHooks(this);
    }

    @GuardedBy("this")
    private void spawn0()
    {
        if (entity != null)
        {
            log.atFine().withStackTrace(StackSize.FULL).log("Entity is already spawned, killing it first!");
            kill0();
        }

        this.entity = blockDisplayHelper.spawn(
            recoveryData,
            executor,
            bukkitWorld,
            currentTarget,
            blockData.getBlockData()
        );

        this.entity.setViewRange(2.5F);
    }

    @Override
    public synchronized void spawn()
    {
        executor.assertMainThread("Animated blocks must be spawned on the main thread!");
        forEachHook("preSpawn", IAnimatedBlockHook::preSpawn);
        spawn0();
        forEachHook("postSpawn", IAnimatedBlockHook::postSpawn);
    }

    @GuardedBy("this")
    private void kill0()
    {
        if (entity != null)
        {
            entity.remove();
            entity = null;
        }
    }

    @Override
    public synchronized void kill()
    {
        executor.assertMainThread("Animated blocks must be killed on the main thread!");
        forEachHook("preKill", IAnimatedBlockHook::preKill);
        kill0();
        forEachHook("postKill", IAnimatedBlockHook::postKill);
    }

    @Override
    public synchronized int getTicksLived()
    {
        return entity == null ? -1 : entity.getTicksLived();
    }

    @Override
    public synchronized void respawn()
    {
        executor.assertMainThread("Animated blocks must be respawned on the main thread!");

        forEachHook("preRespawn", IAnimatedBlockHook::preRespawn);
        kill0();
        spawn0();
        forEachHook("postRespawn", IAnimatedBlockHook::postRespawn);
    }

    @Override
    public synchronized void moveToTarget(RotatedPosition target)
    {
        forEachHook("preMove", hook -> hook.preMove(target));
        blockDisplayHelper.moveToTarget(entity, startPosition, target);
        cycleTargets(target);
        forEachHook("onMoved", hook -> hook.postMove(target));
    }

    private synchronized void cycleTargets(RotatedPosition newTarget)
    {
        this.previousTarget = currentTarget;
        this.currentTarget = newTarget;
    }

    @Override
    public synchronized boolean isAlive()
    {
        return entity != null;
    }

    @Override
    public IAnimatedBlockData getAnimatedBlockData()
    {
        return this.blockData;
    }

    @Override
    public Material getMaterial()
    {
        return this.blockData.getBlockData().getMaterial();
    }

    @Override
    public synchronized @Nullable Entity getEntity()
    {
        return entity;
    }

    @Override
    public ILocation getLocation()
    {
        final Vector3Dd positionSnapshot = getCurrentPosition();
        return new LocationSpigot(bukkitWorld, positionSnapshot.x(), positionSnapshot.y(), positionSnapshot.z());
    }

    @Override
    public Vector3Dd getPosition()
    {
        return getCurrentPosition();
    }

    @Override
    public synchronized Vector3Dd getCurrentPosition()
    {
        return currentTarget.position();
    }

    @Override
    public synchronized Vector3Dd getPreviousPosition()
    {
        return previousTarget.position();
    }

    @Override
    public synchronized Vector3Dd getPreviousTarget()
    {
        return previousTarget.position();
    }

    synchronized void forEachHook(String actionName, Consumer<IAnimatedBlockHook> call)
    {
        for (final IAnimatedBlockHook hook : hooks)
        {
            log.atFinest().log("Executing '%s' for hook '%s'!", actionName, hook.getName());
            try
            {
                call.accept(hook);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to execute '%s' for hook '%s'!", actionName, hook.getName());
            }
        }
    }
}
