package nl.pim16aap2.bigDoors.NMS;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.BlockRotatable;
import net.minecraft.server.v1_14_R1.EnumBlockRotation;
import net.minecraft.server.v1_14_R1.EnumDirection.EnumAxis;
import net.minecraft.server.v1_14_R1.IBlockData;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.XMaterial;

public class NMSBlock_V1_14_R1 extends net.minecraft.server.v1_14_R1.Block implements NMSBlock
{
    private IBlockData blockData;
    private CraftBlockData craftBlockData;
    private XMaterial xmat;
    private Location loc;

    public NMSBlock_V1_14_R1(World world, int x, int y, int z)
    {
        super(net.minecraft.server.v1_14_R1.Block.Info
            .a(((CraftWorld) world).getHandle().getType(new BlockPosition(x, y, z)).getBlock()));

        loc = new Location(world, x, y, z);

        // If the block is waterlogged (i.e. it has water inside), unwaterlog it.
        craftBlockData = (CraftBlockData) ((CraftBlock) world.getBlockAt(x, y, z)).getBlockData();
        if (craftBlockData instanceof Waterlogged)
            ((Waterlogged) craftBlockData).setWaterlogged(false);

        constructBlockDataFromBukkit();

        xmat = XMaterial.matchXMaterial(world.getBlockAt(x, y, z).getType().toString()).orElse(XMaterial.BEDROCK);
        // Update iBlockData in NMS Block.
        super.o(blockData);
    }

    @Override
    public boolean canRotate()
    {
        return craftBlockData instanceof MultipleFacing;
    }

    @Override
    public void rotateBlock(RotateDirection rotDir)
    {
        EnumBlockRotation rot;
        switch (rotDir)
        {
        case CLOCKWISE:
            rot = EnumBlockRotation.CLOCKWISE_90;
            break;
        case COUNTERCLOCKWISE:
            rot = EnumBlockRotation.COUNTERCLOCKWISE_90;
            break;
        default:
            rot = EnumBlockRotation.NONE;
        }
        blockData = blockData.a(rot);
    }

    private void constructBlockDataFromBukkit()
    {
        blockData = craftBlockData.getState();
    }

    @Override
    public void putBlock(Location loc)
    {
        this.loc = loc;

        if (craftBlockData instanceof MultipleFacing)
            updateCraftBlockDataMultipleFacing();

        ((CraftWorld) loc.getWorld()).getHandle()
            .setTypeAndData(new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), blockData, 1);
    }

    private void updateCraftBlockDataMultipleFacing()
    {
        Set<BlockFace> allowedFaces = ((MultipleFacing) craftBlockData).getAllowedFaces();
        allowedFaces.forEach((K) ->
        {
            org.bukkit.block.Block otherBlock = loc.clone().add(K.getModX(), K.getModY(), K.getModZ()).getBlock();
            CraftBlockData otherData = (CraftBlockData) ((CraftBlock) otherBlock).getBlockData();

            if (K.equals(BlockFace.UP))
                ((MultipleFacing) craftBlockData).setFace(K, true);
            else if (otherBlock.getType().isSolid())
            {
                ((MultipleFacing) craftBlockData).setFace(K, true);
                if (otherData instanceof MultipleFacing &&
                    (otherBlock.getType().equals(xmat.parseMaterial()) ||
                     (craftBlockData instanceof org.bukkit.block.data.type.Fence &&
                      otherData instanceof org.bukkit.block.data.type.Fence)))
                    if (((MultipleFacing) otherData).getAllowedFaces().contains(K.getOppositeFace()))
                    {
                        ((MultipleFacing) otherData).setFace(K.getOppositeFace(), true);
                        ((CraftBlock) otherBlock).setBlockData(otherData);
                    }
            }
            else
                ((MultipleFacing) craftBlockData).setFace(K, false);
        });
        constructBlockDataFromBukkit();
    }

    @Override
    public void rotateBlockUpDown(boolean NS)
    {
        EnumAxis axis = blockData.get(BlockRotatable.AXIS);
        EnumAxis newAxis = axis;
        switch (axis)
        {
        case X:
            newAxis = NS ? EnumAxis.X : EnumAxis.Y;
            break;
        case Y:
            newAxis = NS ? EnumAxis.Z : EnumAxis.X;
            break;
        case Z:
            newAxis = NS ? EnumAxis.Y : EnumAxis.Z;
            break;
        }
        blockData = blockData.set(BlockRotatable.AXIS, newAxis);
    }

    @Override
    public void rotateCylindrical(RotateDirection rotDir)
    {
        if (rotDir.equals(RotateDirection.CLOCKWISE))
            blockData = blockData.a(EnumBlockRotation.CLOCKWISE_90);
        else
            blockData = blockData.a(EnumBlockRotation.COUNTERCLOCKWISE_90);
    }

    @Override
    public String toString()
    {
        return blockData.toString();
    }

    @Override
    public void deleteOriginalBlock()
    {
        loc.getWorld().getBlockAt(loc).setType(Material.AIR);
    }
}
