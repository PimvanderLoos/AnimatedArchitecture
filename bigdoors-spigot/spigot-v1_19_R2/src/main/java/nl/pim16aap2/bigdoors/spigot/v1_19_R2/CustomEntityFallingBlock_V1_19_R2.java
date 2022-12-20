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
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.PlayerChunkMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3D;
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

import java.util.List;
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
    private final IPWorld pWorld;
    private final List<IAnimatedBlockHook> hooks;
    @ToString.Exclude
    private @Nullable PlayerChunkMap.EntityTracker tracker;
    @ToString.Exclude
    private final WorldServer worldServer;

    private EntityInLevelCallback entityInLevelCallback = EntityInLevelCallback.a;

    @Getter
    private Vector3Dd previousPosition;
    @Getter
    private Vector3Dd currentPosition;

    private final IPLocation startLocation;
    private final Vector3Dd startPosition;
    private final Vector3Dd finalPosition;

    public CustomEntityFallingBlock_V1_19_R2(
        IPWorld pWorld, World world, double posX, double posY, double posZ, float radius, float startAngle,
        boolean onEdge, AnimationContext context, AnimatedBlockHookManager animatedBlockHookManager,
        Vector3Dd finalPosition)
    {
        super(EntityTypes.F, ((CraftWorld) world).getHandle());
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
            new NMSBlock_V1_19_R2(worldServer, (int) Math.floor(posX), (int) Math.round(posY), (int) Math.floor(posZ));
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
                CustomEntityFallingBlock_V1_19_R2.this.entityInLevelCallback.a();
            }

            @Override
            public void a(RemovalReason removalReason) // onRemove
            {
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
    }

    private synchronized void handleDeath()
    {
        this.cN().forEach(Entity::q); // Remove passengers
        forEachHook("onDie", IAnimatedBlockHook::onDie);
    }

    @Override
    public synchronized void spawn()
    {
        worldServer.addFreshEntity(this, SpawnReason.CUSTOM);
        tracker = Util.requireNonNull(worldServer.k().a.L.get(getEntityId()), "entity tracker");
        dw(); // Mark alive
        setEntityInLevelCallback();

        forEachHook("onSpawn", IAnimatedBlockHook::onSpawn);
    }

    @Override
    public synchronized void respawn()
    {
        if (tracker == null)
        {
            log.at(Level.SEVERE).withStackTrace(StackSize.FULL)
               .log("Trying to respawn an animated block that has not been spawned yet!");
            return;
        }

        final var packet = new PacketPlayOutSpawnEntity(this, 70);
        tracker.a(packet);
    }

    private synchronized void cyclePositions(Vector3Dd newPosition)
    {
        previousPosition = currentPosition;
        currentPosition = newPosition;

        // Update current and last x/y/z values in entity class.
        p(newPosition.x(), newPosition.y(), newPosition.z()); // setPosRaw

        forEachHook("onMoved", hook -> hook.onMoved(newPosition));
    }

    @Override
    public synchronized boolean teleport(Vector3Dd newPosition, Vector3Dd rotation)
    {
        if (!isAlive())
            return false;

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

        forEachHook("onTeleport", hook -> hook.onTeleport(newPosition));
        cyclePositions(newPosition);

        return true;
    }

    @Override
    public synchronized void l()
    {
        if (!isAlive())
            return;

        if (animatedBlockData.getMyBlockData().h())
            kill();
        else
        {
            final Vec3D mot = super.de();
            if (Math.abs(mot.c) < 0.001 && Math.abs(mot.d) < 0.001 && Math.abs(mot.e) < 0.001)
                return;

            if (getTicksAlive() > 12_000)
                kill();

            a(EnumMoveType.a, di());
            cyclePositions(getRawCurrentLocation());
        }
        forEachHook("postTick", IAnimatedBlockHook::postTick);
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
        return bp();
    }

    @Override
    public Vector3Dd getVelocity()
    {
        final var vec = de();
        return new Vector3Dd(vec.c, vec.d, vec.e);
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
    public Vector3Dd getPVelocity()
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

    private int getTicksAlive()
    {
        return super.b;
    }
}
