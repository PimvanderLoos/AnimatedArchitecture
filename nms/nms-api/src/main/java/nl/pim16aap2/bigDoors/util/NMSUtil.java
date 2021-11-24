package nl.pim16aap2.bigDoors.util;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;

import javax.annotation.Nullable;
import java.util.Objects;

public final class NMSUtil
{
    private NMSUtil()
    {
    }

    /**
     * Rotates a CraftBlockData instance vertically in a given direction.
     *
     * Note that this directly updates the provided craftBlockData instance (if rotation is deemed necessary).
     *
     * @param openDirection  The direction of the rotation to apply.
     * @param craftBlockData The CraftBlockData instance to try to rotate.
     */
    public static void rotateVerticallyInDirection(DoorDirection openDirection, Object craftBlockData)
    {
        if (!(craftBlockData instanceof Directional))
            return; // Nothing we can do

        final Directional directional = (Directional) craftBlockData;
        final BlockFace currentBlockFace = directional.getFacing();
        final @Nullable BlockFace newBlockFace;

        final BlockFace openingDirFace = openDirection.getBlockFace();
        final BlockFace oppositeDirFace =
            Objects.requireNonNull(DoorDirection.getOpposite(openDirection)).getBlockFace();

        if (currentBlockFace == openingDirFace)
            newBlockFace = BlockFace.DOWN;
        else if (currentBlockFace == oppositeDirFace)
            newBlockFace = BlockFace.UP;
        else if (currentBlockFace == BlockFace.UP)
            newBlockFace = openingDirFace;
        else if (currentBlockFace == BlockFace.DOWN)
            newBlockFace = oppositeDirFace;
        else
            return; // Nothing to do

        if (directional.getFaces().contains(newBlockFace))
            directional.setFacing(newBlockFace);
    }
}
