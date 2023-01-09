package nl.pim16aap2.bigdoors.spigot.v1_19_R2;

import com.google.common.flogger.StackSize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutEntity;
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
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.api.IAnimatedBlockSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.util.CraftMagicNumbers;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

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
    @Getter
    private final NMSBlock_V1_19_R2 animatedBlockData;
    @Getter
    private final org.bukkit.World bukkitWorld;
    @Getter
    private final AnimationContext context;
    @Getter
    private final float radius;
    @Getter
    private final float startAngle;
    @Getter
    private final boolean onEdge;
    private final IPExecutor executor;
    private final IPWorld pWorld;
    private final List<IAnimatedBlockHook> hooks;
    @ToString.Exclude
    private @Nullable PlayerChunkMap.EntityTracker tracker;

    // net.minecraft.server.level.ServerLevel
    @ToString.Exclude
    private final WorldServer worldServer;

    private EntityInLevelCallback entityInLevelCallback = EntityInLevelCallback.a;
    private @Nullable EntityInLevelCallback entityInLevelCallbackSectionManager;

    @Getter
    private Vector3Dd previousPosition;
    @Getter
    private Vector3Dd currentPosition;

    private final AtomicReference<@Nullable Vector3Dd> teleportedTo = new AtomicReference<>();

    private final IPLocation startLocation;
    private final Vector3Dd startPosition;
    private final Vector3Dd finalPosition;

    public CustomEntityFallingBlock_V1_19_R2(
        IPExecutor executor, IPWorld pWorld, World world, double posX, double posY, double posZ, float radius,
        float startAngle,
        boolean onEdge, AnimationContext context, AnimatedBlockHookManager animatedBlockHookManager,
        Vector3Dd finalPosition)
    {
        super(EntityTypes.F, ((CraftWorld) world).getHandle());
        this.executor = executor;
        this.pWorld = pWorld;
        bukkitWorld = world;
        this.radius = radius;
        this.startAngle = startAngle;
        this.onEdge = onEdge;
        this.context = context;
        this.finalPosition = finalPosition;
        worldServer = ((CraftWorld) bukkitWorld).getHandle();
        // Do not round x and z because they are at half blocks; Given x;z 10;5, the block will be spawned at
        // 10.5;5.5. Rounding it would retrieve the blocks at 11;6.
        this.animatedBlockData =
            new NMSBlock_V1_19_R2(executor, worldServer, (int) Math.floor(posX), (int) Math.round(posY),
                                  (int) Math.floor(posZ));
        this.startLocation = new PLocationSpigot(new Location(bukkitWorld, posX, posY, posZ));
        this.startPosition = new Vector3Dd(startLocation.getX(), startLocation.getY(), startLocation.getZ());

        previousPosition = new Vector3Dd(posX, posY, posZ);
        currentPosition = previousPosition;

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

    private void spawn0()
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
            log.at(Level.SEVERE).withStackTrace(StackSize.FULL)
               .log("Trying to respawn an animated block that has not been spawned yet!");
            return;
        }

        if (entityInLevelCallbackSectionManager == null)
        {
            log.at(Level.SEVERE).withStackTrace(StackSize.FULL)
               .log("entityInLevelCallbackSectionManager is null! Blocks cannot be respawned!");
            return;
        }

        entityInLevelCallbackSectionManager.a(RemovalReason.b);

        spawn0();

        forEachHook("onRespawn", IAnimatedBlockHook::onRespawn);
    }

    private synchronized void setPosRaw(Vector3Dd newPosition)
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
        p(newPosition.x(), newPosition.y(), newPosition.z()); // setPosRaw
    }

    private synchronized void cyclePositions(Vector3Dd newPosition)
    {
        previousPosition = currentPosition;
        currentPosition = newPosition;

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

    private void relativeTeleport(Vector3Dd newPosition)
    {
        final double deltaX = newPosition.x() - currentPosition.x();
        final double deltaY = newPosition.y() - currentPosition.y();
        final double deltaZ = newPosition.z() - currentPosition.z();

        final short relX = (short) ((int) MathHelper.c(deltaX * 4096.0));
        final short relY = (short) ((int) MathHelper.c(deltaY * 4096.0));
        final short relZ = (short) ((int) MathHelper.c(deltaZ * 4096.0));

        final PacketPlayOutEntity.PacketPlayOutRelEntityMove tpPacket =
            new PacketPlayOutEntity.PacketPlayOutRelEntityMove(getEntityId(), relX, relY, relZ, false);

        if (tracker != null)
            tracker.a(tpPacket);
    }

    @Override
    public synchronized boolean teleport(Vector3Dd newPosition, Vector3Dd rotation)
    {
        if (!isAlive())
            return false;

        teleportedTo.set(newPosition);
        relativeTeleport(newPosition);
        forEachHook("onTeleport", hook -> hook.onTeleport(newPosition));

        return true;
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

        handleTeleport();
        a(EnumMoveType.a, di());
        cyclePositions(getRawCurrentLocation());

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
            log.at(Level.SEVERE).withCause(e)
               .log("Failed to modify entity tracker! Animated block movement will probably be choppy!");
        }
    }

    private void forEachHook(String actionName, Consumer<IAnimatedBlockHook> call)
    {
        for (final IAnimatedBlockHook hook : hooks)
        {
            log.at(Level.FINEST).log("Executing '%s' for hook '%s'!", actionName, hook.getName());
            try
            {
                call.accept(hook);
            }
            catch (Exception e)
            {
                log.at(Level.SEVERE).withCause(e)
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
    public boolean teleport(Vector3Dd newPosition, Vector3Dd rotation, TeleportMode teleportMode)
    {
        return teleport(newPosition, rotation);
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
    public Vector3Dd getPosition()
    {
        return getCurrentPosition();
    }

    @Override
    public Vector3Dd getVelocity()
    {
        final Vec3D bukkitVelocity = de();
        return new Vector3Dd(bukkitVelocity.c, bukkitVelocity.d, bukkitVelocity.e);
    }

    @Override
    public void setVelocity(Vector3Dd vector)
    {
        f(new Vec3D(vector.x(), vector.y(), vector.z()));
        D = true;
    }

    @Override
    public IBlockData i()
    {
        return this.animatedBlockData.getMyBlockData();
    }

    @Override
    public Vector3Dd getStartPosition()
    {
        return startPosition;
    }

    @Override
    public Vector3Dd getFinalPosition()
    {
        return finalPosition;
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
}