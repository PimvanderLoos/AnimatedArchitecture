package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.Blocks;
import net.minecraft.server.v1_15_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.GameProfileSerializer;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntity;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_15_R1.PlayerChunkMap;
import net.minecraft.server.v1_15_R1.Vec3D;
import net.minecraft.server.v1_15_R1.WorldServer;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.api.IAnimatedBlockSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.Nullable;

/**
 * V1_15_R1 implementation of {@link IAnimatedBlock}.
 *
 * @author Pim
 * @see IAnimatedBlock
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CustomEntityFallingBlock_V1_15_R1 extends net.minecraft.server.v1_15_R1.EntityFallingBlock
    implements IAnimatedBlockSpigot
{
    // ticksLived is also a field in NMS.EntityFallingBlock. However, we want to override that on purpose.
    @SuppressWarnings("squid:S2387")
    @Setter
    private int ticksLived;

    // tileEntityData is also a field in NMS.EntityFallingBlock. However, we want to override that on purpose.
    @SuppressWarnings("squid:S2387")
    @ToString.Exclude
    private @Nullable NBTTagCompound tileEntityData;

    @Getter
    private final NMSBlock_V1_15_R1 animatedBlockData;
    private int fallHurtMax;
    private float fallHurtAmount;
    @Getter
    private final org.bukkit.World bukkitWorld;
    @Getter
    private final float radius;
    @Getter
    private final float startAngle;
    @Getter
    private final boolean placementDeferred;
    private final IPWorld pWorld;
    @ToString.Exclude
    private @Nullable PlayerChunkMap.EntityTracker tracker;
    @ToString.Exclude
    private final WorldServer worldServer;

    @Getter
    private Vector3Dd previousPosition;
    @Getter
    private Vector3Dd currentPosition;

    private final IPLocation startLocation;
    private final Vector3Dd startPosition;

    public CustomEntityFallingBlock_V1_15_R1(
        IPWorld pWorld, World world, double d0, double d1, double d2, float radius, float startAngle,
        boolean placementDeferred)
        throws Exception
    {
        super(EntityTypes.FALLING_BLOCK, ((CraftWorld) world).getHandle());
        this.pWorld = pWorld;
        bukkitWorld = world;
        this.radius = radius;
        this.startAngle = startAngle;
        this.placementDeferred = placementDeferred;
        worldServer = ((CraftWorld) bukkitWorld).getHandle();
        // Do not round x and z because they are at half blocks; Given x;z 10;5, the block will be spawned at
        // 10.5;5.5. Rounding it would retrieve the blocks at 11;6.
        this.animatedBlockData =
            new NMSBlock_V1_15_R1(worldServer, (int) Math.floor(d0), (int) Math.round(d1), (int) Math.floor(d2));
        this.startLocation = new PLocationSpigot(new Location(bukkitWorld, d0, d1, d2));
        this.startPosition = new Vector3Dd(startLocation.getX(), startLocation.getY(), startLocation.getZ());
        i = true;
        setPosition(d0, d1 + (1.0F - getHeight()) / 2.0F, d2);
        setNoGravity(true);
        fallHurtMax = 0;
        fallHurtAmount = 0.0F;
        setMot(0, 0, 0);
        lastX = d0;
        lastY = d1;
        lastZ = d2;

        previousPosition = new Vector3Dd(d0, d1, d2);
        currentPosition = previousPosition;

        noclip = true;
        a(new BlockPosition(this));
    }

    @Override
    public synchronized void die()
    {
        for (final Entity ent : passengers)
            ent.stopRiding();
        dead = true;
    }

    @Override
    public synchronized void spawn()
    {
        ((org.bukkit.craftbukkit.v1_15_R1.CraftWorld) bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
        tracker = Util.requireNonNull(worldServer.getChunkProvider().playerChunkMap.trackedEntities.get(getId()),
                                      "entity tracker");
        dead = false;
    }

    @Override
    public synchronized void respawn()
    {
        // TODO: Ensure that this works as intended.
        Util.requireNonNull(tracker, "EntityTracker").broadcast(new PacketPlayOutSpawnEntity(this));
        dead = false;
    }

    private synchronized void cyclePositions(Vector3Dd newPosition)
    {
        previousPosition = currentPosition;
        currentPosition = newPosition;
        // Update current and last x/y/z values in entity class.
        f(newPosition.x(), newPosition.y(), newPosition.z());
    }

    @Override
    public synchronized boolean teleport(Vector3Dd newPosition, Vector3Dd rotation)
    {
        if (dead)
            return false;

        final double deltaX = newPosition.x() - currentPosition.x();
        final double deltaY = newPosition.y() - currentPosition.y();
        final double deltaZ = newPosition.z() - currentPosition.z();

        final short relX = (short) ((int) PacketPlayOutEntity.a(deltaX));
        final short relY = (short) ((int) PacketPlayOutEntity.a(deltaY));
        final short relZ = (short) ((int) PacketPlayOutEntity.a(deltaZ));

        final PacketPlayOutEntity.PacketPlayOutRelEntityMove tpPacket =
            new PacketPlayOutEntity.PacketPlayOutRelEntityMove(getId(), relX, relY, relZ, false);

        if (tracker != null)
            tracker.broadcast(tpPacket);

        cyclePositions(newPosition);

        return true;
    }

    @Override
    protected boolean playStepSound()
    {
        return false;
    }

    @Override
    public synchronized boolean isInteractable()
    {
        return !dead;
    }

    @Override
    public synchronized void tick()
    {
        if (dead)
            return;

        if (animatedBlockData.getMyBlockData().isAir())
            die();
        else
        {
            final Vec3D mot = super.getMot();
            if (Math.abs(mot.x) < 0.001 && Math.abs(mot.y) < 0.001 && Math.abs(mot.z) < 0.001)
                return;

            final Vector3Dd newLocation = currentPosition.add(mot.x, mot.y, mot.z);
            final double locY = locY();
            if (++ticksLived > 100 && (locY < 1 || locY > 256) || ticksLived > 12_000)
                die();

            cyclePositions(newLocation);
        }
    }

    @Override
    public boolean b(float f, float f1)
    {
        return false;
    }

    @Override
    protected void b(NBTTagCompound nbttagcompound)
    {
        nbttagcompound.set("BlockState", GameProfileSerializer.a(animatedBlockData.getMyBlockData()));
        nbttagcompound.setInt("Time", ticksLived);
        nbttagcompound.setBoolean("DropItem", false);
        nbttagcompound.setBoolean("HurtEntities", false);
        nbttagcompound.setFloat("FallHurtAmount", fallHurtAmount);
        nbttagcompound.setInt("FallHurtMax", fallHurtMax);
        if (tileEntityData != null)
            nbttagcompound.set("TileEntityData", tileEntityData);

    }

    @Override
    protected void a(NBTTagCompound nbttagcompound)
    {
        animatedBlockData.setBlockData(GameProfileSerializer.d(nbttagcompound.getCompound("BlockState")));
        ticksLived = nbttagcompound.getInt("Time");
        if (nbttagcompound.hasKeyOfType("HurtEntities", 99))
        {
            fallHurtAmount = nbttagcompound.getFloat("FallHurtAmount");
            fallHurtMax = nbttagcompound.getInt("FallHurtMax");
        }

        if (nbttagcompound.hasKeyOfType("TileEntityData", 10))
            tileEntityData = nbttagcompound.getCompound("TileEntityData");

        if (animatedBlockData.getMyBlockData().isAir())
            animatedBlockData.setBlockData(Blocks.SAND.getBlockData());
    }

    @Override
    public boolean isAlive()
    {
        return !dead;
    }

    @Override
    public Vector3Dd getVelocity()
    {
        final var vec = getMot();
        return new Vector3Dd(vec.x, vec.y, vec.z);
    }

    @Override
    public IPWorld getPWorld()
    {
        return pWorld;
    }

    @Override
    public void a(boolean flag)
    {
        // ignored
    }

    @Override
    public void appendEntityCrashDetails(CrashReportSystemDetails crashreportsystemdetails)
    {
        super.appendEntityCrashDetails(crashreportsystemdetails);
        crashreportsystemdetails.a("Imitating BlockState", animatedBlockData.toString());
    }

    @Override
    public Material getMaterial()
    {
        return Util.requireNonNull(CraftMagicNumbers.getMaterial(animatedBlockData.getMyBlockData().getBlock()),
                                   "Material");
    }

    @Override
    public boolean teleport(Vector3Dd newPosition, Vector3Dd rotation, TeleportMode teleportMode)
    {
        return teleport(newPosition, rotation);
    }

    @Override
    public void kill()
    {
        die();
    }

    @Override
    public IPLocation getPLocation()
    {
        return SpigotAdapter.wrapLocation(new Location(bukkitWorld, locX(), locY(), locZ(), yaw, pitch));
    }

    @Override
    public Vector3Dd getPosition()
    {
        return getCurrentPosition();
    }

    @Override
    public Vector3Dd getPVelocity()
    {
        final Vec3D bukkitVelocity = getMot();
        return new Vector3Dd(bukkitVelocity.getX(), bukkitVelocity.getY(), bukkitVelocity.getZ());
    }

    @Override
    public void setVelocity(Vector3Dd vector)
    {
        setMot(new Vec3D(vector.x(), vector.y(), vector.z()));
        velocityChanged = true;
    }

    @Override
    public IBlockData getBlock()
    {
        return this.animatedBlockData.getMyBlockData();
    }

    @Override
    public Vector3Dd getStartPosition()
    {
        return startPosition;
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
}
