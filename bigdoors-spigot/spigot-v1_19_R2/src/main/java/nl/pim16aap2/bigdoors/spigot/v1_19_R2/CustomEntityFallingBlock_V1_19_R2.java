package nl.pim16aap2.bigdoors.spigot.v1_19_R2;

import com.google.common.flogger.StackSize;
import io.netty.buffer.Unpooled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.server.level.EntityTrackerEntry;
import net.minecraft.server.level.PlayerChunkMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.Vec3D;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.animatedblock.AnimationContext;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.bigdoors.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.api.IAnimatedBlockSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.util.CraftMagicNumbers;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.GuardedBy;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * v1_19_R2 implementation of {@link IAnimatedBlock}.
 *
 * @author Pim
 * @see IAnimatedBlock
 */
@Flogger
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomEntityFallingBlock_V1_19_R2 extends EntityFallingBlock implements IAnimatedBlockSpigot
{
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final IPExecutor executor;

    @Getter
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private final AnimationContext context;

    @Getter
    private final NMSBlock_V1_19_R2 animatedBlockData;

    @Getter
    private final org.bukkit.World bukkitWorld;

    @Getter
    private final float radius;

    @Getter
    private final float startAngle;

    private final BlockMover.MovementMethod movementMethod;

    @Getter
    private final boolean onEdge;

    private final IPWorld pWorld;

    private final List<IAnimatedBlockHook> hooks;

    // net.minecraft.server.level.ServerLevel
    @ToString.Exclude
    private final WorldServer worldServer;

    @GuardedBy("this")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private @Nullable PlayerChunkMap.EntityTracker tracker;

    // No need to lock for these two, as they can only be accessed on the main thread.
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private EntityInLevelCallback entityInLevelCallback = EntityInLevelCallback.a;
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private @Nullable EntityInLevelCallback entityInLevelCallbackSectionManager;

    @GuardedBy("this")
    private Vector3Dd previousPosition;

    @GuardedBy("this")
    private Vector3Dd currentPosition;

    @GuardedBy("this")
    private Vector3Dd previousTarget;

    @GuardedBy("this")
    private Vector3Dd currentTarget;

    private final AtomicReference<@Nullable Vector3Dd> teleportedTo = new AtomicReference<>();

    @Getter
    private final IPLocation startLocation;

    @Getter
    private final Vector3Dd startPosition;

    @Getter
    private final Vector3Dd finalPosition;

    public CustomEntityFallingBlock_V1_19_R2(
        IPExecutor executor, IPWorld pWorld, World world, double posX, double posY, double posZ, float radius,
        float startAngle, BlockMover.MovementMethod movementMethod,
        boolean onEdge, AnimationContext context, AnimatedBlockHookManager animatedBlockHookManager,
        Vector3Dd finalPosition)
    {
        super(EntityTypes.F, ((CraftWorld) world).getHandle());
        this.executor = executor;
        this.pWorld = pWorld;
        bukkitWorld = world;
        this.radius = radius;
        this.startAngle = startAngle;
        this.movementMethod = movementMethod;
        this.onEdge = onEdge;
        this.context = context;
        this.finalPosition = finalPosition;
        worldServer = ((CraftWorld) bukkitWorld).getHandle();
        // Do not round x and z because they are at half blocks; Given x;z 10;5, the block will be spawned at
        // 10.5;5.5. Rounding it would retrieve the blocks at 11;6.
        this.animatedBlockData =
            new NMSBlock_V1_19_R2(this, executor, worldServer, (int) Math.floor(posX), (int) Math.round(posY),
                                  (int) Math.floor(posZ));
        this.startLocation = new PLocationSpigot(new Location(bukkitWorld, posX, posY, posZ));
        this.startPosition = new Vector3Dd(startLocation.getX(), startLocation.getY(), startLocation.getZ());

        previousPosition = new Vector3Dd(posX, posY, posZ);
        currentPosition = previousPosition;
        previousTarget = previousPosition;
        currentTarget = previousPosition;

        this.f(posX, posY, posZ);
        super.b = 0;
        super.aq = false;
        super.Q = true;
        this.e(true);
        this.f(new Vec3D(0.0D, 0.0D, 0.0D));
        this.a(new BlockPosition(this.dk(), this.dm(), this.dq()));

        this.hooks = animatedBlockHookManager.instantiateHooks(this);
    }

    @Override
    public synchronized void a(RemovalReason entityRemovalReason)
    {
        this.b(entityRemovalReason);
    }

    private void setEntityInLevelCallback()
    {
        super.a(new EntityInLevelCallback()
        {
            @Override
            public void a() // onMove
            {
                if (!executor.isMainThread())
                {
                    // Async position updates cause async move callbacks.
                    // This can result in server crashes when the entity
                    // gets moved to a different EntitySection.
                    log.atSevere().withStackTrace(StackSize.FULL).log("Caught async move callback! THIS IS A BUG!");
                    return;
                }
                CustomEntityFallingBlock_V1_19_R2.this.entityInLevelCallback.a();
            }

            @Override
            public void a(RemovalReason removalReason) // onRemove
            {
                if (!executor.isMainThread())
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log("Caught async remove callback! THIS IS A BUG!");
                    return;
                }

                CustomEntityFallingBlock_V1_19_R2.this.entityInLevelCallback.a(removalReason);
                CustomEntityFallingBlock_V1_19_R2.this.handleDeath();
            }
        });
    }

    // We use the real entityInLevelCallback for our own purposes,
    // however, we still need access to the original callback.
    // The original callback is set from PersistentEntitySectionManager,
    @Override
    public void a(EntityInLevelCallback entityInLevelCallback)
    {
        this.entityInLevelCallback = entityInLevelCallback;
        if (entityInLevelCallback.getClass().getEnclosingClass() == PersistentEntitySectionManager.class)
            this.entityInLevelCallbackSectionManager = entityInLevelCallback;
    }

    private synchronized void handleDeath()
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async death! THIS IS A BUG!");
            return;
        }

        this.cN().forEach(Entity::q); // Remove passengers
        forEachHook("onDie", IAnimatedBlockHook::onDie);
    }

    private synchronized void spawn0()
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async spawn! THIS IS A BUG!");
            return;
        }

        worldServer.addFreshEntity(this, SpawnReason.CUSTOM);
        dA(); // Entity#unsetRemoved()
        setEntityInLevelCallback();

        tracker = Util.requireNonNull(worldServer.k().a.L.get(getEntityId()), "entity tracker");
        modifyEntityTracker(tracker);
    }

    @Override
    public synchronized void spawn()
    {
        spawn0();
        forEachHook("onSpawn", IAnimatedBlockHook::onSpawn);
    }

    @Override
    public synchronized void respawn()
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async respawn! THIS IS A BUG!");
            return;
        }

        if (tracker == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Trying to respawn an animated block that has not been spawned yet!");
            return;
        }

        if (entityInLevelCallbackSectionManager == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("entityInLevelCallbackSectionManager is null! Blocks cannot be respawned!");
            return;
        }

        entityInLevelCallbackSectionManager.a(RemovalReason.b);

        spawn0();

        forEachHook("onRespawn", IAnimatedBlockHook::onRespawn);
    }

    private synchronized void setPosRaw(IVector3D newPosition)
    {
        if (!executor.isMainThread())
        {
            // Async position updates cause async move callbacks.
            // This can result in server crashes when the entity
            // gets moved to a different EntitySection.
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async position update! THIS IS A BUG!");
            return;
        }

        // Update current and last x/y/z values in entity class.
        p(newPosition.xD(), newPosition.yD(), newPosition.zD()); // setPosRaw
    }

    private synchronized void cyclePositions(Vector3Dd newPosition)
    {
        previousPosition = currentPosition;
        currentPosition = newPosition;
    }

    private synchronized void cycleTargets(Vector3Dd newTarget)
    {
        previousTarget = currentTarget;
        currentTarget = newTarget;
    }

    private synchronized void cycleAndUpdatePositions(Vector3Dd newPosition)
    {
        cyclePositions(newPosition);
        setPosRaw(newPosition);
        forEachHook("onMoved", hook -> hook.onMoved(newPosition));
    }

    @Override
    public void inactiveTick()
    {
        this.l();
    }

    @Override
    public void postTick()
    {
    }

    private synchronized void relativeTeleport(IVector3D from, IVector3D to)
    {
        final double deltaX = to.xD() - from.xD();
        final double deltaY = to.yD() - from.yD();
        final double deltaZ = to.zD() - from.zD();

        final short relX = (short) ((int) MathHelper.c(deltaX * 4096.0));
        final short relY = (short) ((int) MathHelper.c(deltaY * 4096.0));
        final short relZ = (short) ((int) MathHelper.c(deltaZ * 4096.0));

        final PacketPlayOutEntity.PacketPlayOutRelEntityMove tpPacket =
            new PacketPlayOutEntity.PacketPlayOutRelEntityMove(getEntityId(), relX, relY, relZ, false);

        if (tracker != null)
            tracker.a(tpPacket);
    }

    private synchronized void absoluteTeleport(IVector3D to, IVector3D rotation)
    {
        if (tracker == null)
            return;

        // int + 3 * double + 2 * byte + 1 * boolean = 4 + 3 * 8 + 2 + 1 = 31 bytes
        final PacketDataSerializer dataSerializer = new PacketDataSerializer(Unpooled.directBuffer(31));

        dataSerializer.d(getEntityId());
        dataSerializer.writeDouble(to.xD());
        dataSerializer.writeDouble(to.yD());
        dataSerializer.writeDouble(to.zD());
        dataSerializer.writeByte((byte) ((int) (rotation.yD() * 256.0F / 360.0F)));
        dataSerializer.writeByte((byte) ((int) (rotation.xD() * 256.0F / 360.0F)));
        dataSerializer.writeBoolean(false);

        tracker.a(new PacketPlayOutEntityTeleport(dataSerializer));
    }

    /**
     * Handles a teleport action.
     * <p>
     * If this animated block was not teleported, nothing happens. Otherwise, the positions are updated.
     */
    private void handleTeleport()
    {
        final @Nullable Vector3Dd teleportedToCopy = teleportedTo.getAndSet(null);
        if (teleportedToCopy == null)
            return;
        setPosRaw(teleportedToCopy);
    }

    @Override
    public synchronized void l()
    {
        if (!executor.isMainThread())
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Caught async tick! THIS IS A BUG!");
            return;
        }

        if (!isAlive())
            return;

        forEachHook("preTick", IAnimatedBlockHook::preTick);

        ++b;
        handleTeleport();
        a(EnumMoveType.a, di());
        cycleAndUpdatePositions(getRawCurrentLocation());

        forEachHook("postTick", IAnimatedBlockHook::postTick);
    }

    private void modifyEntityTracker(PlayerChunkMap.EntityTracker tracker)
    {
        try
        {
            final Field entryField = tracker.getClass().getDeclaredField("b");
            entryField.setAccessible(true);
            final EntityTrackerEntry entityTrackerEntry =
                Objects.requireNonNull((EntityTrackerEntry) entryField.get(tracker));

            final Field updateInterval = EntityTrackerEntry.class.getDeclaredField("e");
            updateInterval.setAccessible(true);
            updateInterval.set(entityTrackerEntry, Integer.MAX_VALUE);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e)
               .log("Failed to modify entity tracker! Animated block movement will probably be choppy!");
        }
    }

    @Override
    public void moveToTarget(Vector3Dd target, int ticksRemaining)
    {
        cycleTargets(target);
        movementMethod.apply(this, target, ticksRemaining);
    }

    @Override
    public synchronized boolean teleport(Vector3Dd newPosition, Vector3Dd rotation, TeleportMode teleportMode)
    {
        if (!isAlive())
            return false;

        final var from = Objects.requireNonNullElse(teleportedTo.getAndSet(newPosition), currentPosition);
        cyclePositions(newPosition);
        if (teleportMode == TeleportMode.RELATIVE)
            relativeTeleport(from, newPosition);
        else
            absoluteTeleport(newPosition, rotation);
        forEachHook("onTeleport", hook -> hook.onTeleport(from, newPosition));
        return true;
    }

    @Override
    public synchronized void setVelocity(Vector3Dd vector)
    {
        f(new Vec3D(vector.x(), vector.y(), vector.z()));
        D = true;
    }

    @Override
    public int getTicksLived()
    {
        return this.b;
    }

    void forEachHook(String actionName, Consumer<IAnimatedBlockHook> call)
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
                log.atSevere().withCause(e)
                   .log("Failed to execute '%s' for hook '%s'!", actionName, hook.getName());
            }
        }
    }

    @Override
    protected void b(NBTTagCompound nbttagcompound)
    {
        nbttagcompound.a("BlockState", GameProfileSerializer.a(animatedBlockData.getMyBlockData()));
        nbttagcompound.a("Time", b);
        nbttagcompound.a("DropItem", false);
        nbttagcompound.a("HurtEntities", aq);
        nbttagcompound.a("FallHurtAmount", 0.0f);
        nbttagcompound.a("FallHurtMax", 0);
        if (d != null)
            nbttagcompound.a("TileEntityData", d);
    }

    @Override
    protected void a(NBTTagCompound nbttagcompound)
    {
        animatedBlockData.setBlockData(GameProfileSerializer.a(this.s.a(Registries.e), nbttagcompound.p("BlockState")));
        b = nbttagcompound.h("Time");

        if (nbttagcompound.b("TileEntityData", 10))
            super.d = nbttagcompound.p("TileEntityData");
    }

    @Override
    public boolean isAlive()
    {
        return br();
    }

    @Override
    public IPWorld getPWorld()
    {
        return pWorld;
    }

    @Override
    public void a(final CrashReportSystemDetails crashreportsystemdetails)
    {
        super.a(crashreportsystemdetails);
        crashreportsystemdetails.a("Imitating BlockState", animatedBlockData.toString());
    }

    @Override
    public Material getMaterial()
    {
        return Util.requireNonNull(CraftMagicNumbers.getMaterial(animatedBlockData.getMyBlockData().b()), "Material");
    }

    @Override
    public void kill()
    {
        aj();
    }

    private Vector3Dd getRawCurrentLocation()
    {
        return new Vector3Dd(dk(), dm(), dq());
    }

    @Override
    public IPLocation getPLocation()
    {
        return SpigotAdapter.wrapLocation(new Location(bukkitWorld, dk(), dm(), dq(), dv(), dx()));
    }

    @Override
    public synchronized Vector3Dd getPosition()
    {
        return getCurrentPosition();
    }

    @Override
    public IBlockData i()
    {
        return this.animatedBlockData.getMyBlockData();
    }

    @Override
    public double getStartX()
    {
        return startLocation.getX();
    }

    @Override
    public double getStartY()
    {
        return startLocation.getY();
    }

    @Override
    public double getStartZ()
    {
        return startLocation.getZ();
    }

    private int getEntityId()
    {
        return super.ah();
    }

    @Override
    public synchronized Vector3Dd getPreviousPosition()
    {
        return previousPosition;
    }

    @Override
    public synchronized Vector3Dd getCurrentPosition()
    {
        return currentPosition;
    }

    @Override
    public synchronized Vector3Dd getPreviousTarget()
    {
        return previousTarget;
    }

    @SuppressWarnings("unused") // It is used, by equals/hashCode/toString. Just not visibly so.
    public synchronized Vector3Dd getCurrentTarget()
    {
        return currentTarget;
    }
}
