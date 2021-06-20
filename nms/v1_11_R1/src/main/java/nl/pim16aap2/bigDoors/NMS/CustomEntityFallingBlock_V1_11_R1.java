package nl.pim16aap2.bigDoors.NMS;

import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.Blocks;
import net.minecraft.server.v1_11_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_11_R1.Entity;
import net.minecraft.server.v1_11_R1.EnumMoveType;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.Material;
import net.minecraft.server.v1_11_R1.MathHelper;
import net.minecraft.server.v1_11_R1.MinecraftKey;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.util.CraftMagicNumbers;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import javax.annotation.Nullable;

/*
 * This is a custom falling block entity.
 * Changes:
 * - It does not die after a specified (600) amount of ticks.
 * - Easy kill() function to kill it.
 * - Not affected by gravity.
 * - Does not hurt entities (by default, anyway).
 * - NoClip enabled
 */

@SuppressWarnings("hiding")
public class CustomEntityFallingBlock_V1_11_R1 extends net.minecraft.server.v1_11_R1.EntityFallingBlock implements CustomEntityFallingBlock
{
    private        IBlockData block      ;
    private int    fallHurtMax    = 40   ;
    private float  fallHurtAmount = 2.0F ;
    public boolean dropItem       = false;
    public boolean hurtEntities   = false;
    public NBTTagCompound tileEntityData ;
    private org.bukkit.World  bukkitWorld;

    public CustomEntityFallingBlock_V1_11_R1(org.bukkit.World world)
    {
        super((net.minecraft.server.v1_11_R1.World) world);
        setNoGravity(true);
        noclip = true;
        bukkitWorld  = world;
        spawn();
    }

    @SuppressWarnings("deprecation")
    public CustomEntityFallingBlock_V1_11_R1(org.bukkit.World world, org.bukkit.Material mat, double d0, double d1, double d2, byte data)
    {
        super(((CraftWorld) world).getHandle(), d0, d1, d2, CraftMagicNumbers.getBlock(mat).fromLegacyData(data));
        block  = CraftMagicNumbers.getBlock(mat).fromLegacyData(data);
        i      = true;
        setSize(0.98F, 0.98F);
        setPosition(d0, d1 + (1.0F - length) / 2.0F, d2);
        motX   = 0.0D;
        motY   = 0.0D;
        motZ   = 0.0D;
        lastX  = d0;
        lastY  = d1;
        lastZ  = d2;
        setNoGravity(true);
        noclip = true;
        bukkitWorld  = world;
        spawn();
    }

    public void spawn()
    {
        ((CraftWorld) bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
    }

    @Override
    public boolean inBlock()
    {
        return false;
    }

    @Override
    public void collide(Entity entity)
    {
        if (!x(entity))
        {
            if (!entity.noclip && !noclip)
            {
                    double d0 = entity.locX - locX;
                    double d1 = entity.locZ - locZ;
                    double d2 = MathHelper.a(d0, d1);

                if (d2 >= 0.009999999776482582D)
                {
                    d2 = MathHelper.sqrt(d2);
                    d0 /= d2;
                    d1 /= d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D)
                        d3 = 1.0D;

                    d0 *= d3;
                    d1 *= d3;
                    d0 *= 0.05000000074505806D;
                    d1 *= 0.05000000074505806D;
                    d0 *= 1.0F - R;
                    d1 *= 1.0F - R;
                    if (!isVehicle())
                        this.f(-d0, 0.0D, -d1);

                    if (!entity.isVehicle())
                        entity.f(d0, 0.0D, d1);
                }
            }
        }
    }

    public boolean bd()
    {
        return false;
    }

    @Override
    public void a(BlockPosition blockposition)
    {
        datawatcher.set(CustomEntityFallingBlock_V1_11_R1.d, blockposition);
    }

    @Override
    protected boolean playStepSound()
    {
        return false;
    }

    @Override
    protected void i()
    {
        datawatcher.register(CustomEntityFallingBlock_V1_11_R1.d, BlockPosition.ZERO);
    }

    @Override
    public boolean isInteractable()
    {
        return !dead;
    }

    @Override
    public void A_()
    {
        if (block.getMaterial() == Material.AIR)
            die();
        else
        {
            lastX = locX;
            lastY = locY;
            lastZ = locZ;
            BlockPosition blockposition;

            move(EnumMoveType.SELF, motX, motY, motZ);
            motX *= 0.9800000190734863D;
            motY *= 0.9800000190734863D;
            motZ *= 0.9800000190734863D;
            if (!world.isClientSide)
            {
                blockposition = new BlockPosition(this);
                if (onGround)
                {
                    // Do nothing here, it should never be considered to be on the ground anyway.
                }
                else if (ticksLived > 100 && !world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256))
                    die();
            }
        }
    }

    @Override
    public void e(float f, float f1)
    {
        // This method takes care of calculating damage, so ignore it.
    }

    @Override
    protected void b(NBTTagCompound nbttagcompound)
    {
        Block block = this.block != null ? this.block.getBlock() : Blocks.AIR;
        MinecraftKey minecraftkey = Block.REGISTRY.b(block);

        nbttagcompound.setString ("Block",          minecraftkey == null ? "" : minecraftkey.toString());
        nbttagcompound.setByte   ("Data",           (byte) block.toLegacyData(this.block));
        nbttagcompound.setInt    ("Time",           ticksLived);
        nbttagcompound.setBoolean("DropItem",       dropItem);
        nbttagcompound.setBoolean("HurtEntities",   hurtEntities);
        nbttagcompound.setFloat  ("FallHurtAmount", fallHurtAmount);
        nbttagcompound.setInt    ("FallHurtMax",    fallHurtMax);
        if (tileEntityData != null)
            nbttagcompound.set("TileEntityData", tileEntityData);

    }

    @SuppressWarnings("deprecation")
    @Override
    protected void a(NBTTagCompound nbttagcompound)
    {
        int i = nbttagcompound.getByte("Data") & 255;

        if (nbttagcompound.hasKeyOfType("Block", 8))
            block  = Block.getByName(nbttagcompound.getString("Block")).fromLegacyData(i);
        else if (nbttagcompound.hasKeyOfType("TileID", 99))
            block  = Block.getById(nbttagcompound.getInt("TileID")).fromLegacyData(i);
        else
            block  = Block.getById(nbttagcompound.getByte("Tile") & 255).fromLegacyData(i);

        ticksLived = nbttagcompound.getInt("Time");
        Block block     = this.block.getBlock();

        if (nbttagcompound.hasKeyOfType("HurtEntities", 99))
        {
            hurtEntities   = nbttagcompound.getBoolean("HurtEntities");
            fallHurtAmount = nbttagcompound.getFloat  ("FallHurtAmount");
            fallHurtMax    = nbttagcompound.getInt    ("FallHurtMax");
        }
//        else if (block == Blocks.ANVIL)
//            this.hurtEntities = true;

        if (nbttagcompound.hasKeyOfType("DropItem", 99))
            dropItem = nbttagcompound.getBoolean("DropItem");

        if (nbttagcompound.hasKeyOfType("TileEntityData", 10))
            tileEntityData = nbttagcompound.getCompound("TileEntityData");

        if (block == null || block.getBlockData().getMaterial() == Material.AIR)
            this.block = Blocks.SAND.getBlockData();
    }

    @Override
    public void a(boolean flag)
    {
        hurtEntities = flag;
    }

    @Override
    public void appendEntityCrashDetails(CrashReportSystemDetails crashreportsystemdetails)
    {
        super.appendEntityCrashDetails(crashreportsystemdetails);
        if (block != null)
        {
            Block block = this.block.getBlock();

            crashreportsystemdetails.a("Immitating block ID",   Integer.valueOf(Block.getId(block)));
            crashreportsystemdetails.a("Immitating block data", Integer.valueOf(block.toLegacyData(this.block)));
        }
    }

    @Override
    @Nullable
    public IBlockData getBlock()
    {
        return block;
    }

    @Override
    public boolean bu()
    {
        return true;
    }
}
