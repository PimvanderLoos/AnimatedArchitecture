package nl.pim16aap2.bigdoors.toolusers;


import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorType#CLOCK}.
 *
 * @author Pim
 **/
public class ClockCreator extends BigDoorCreator
{
    private int xDepth = 0, yDepth = 0, zDepth = 0;
    private boolean NS = false;

    public ClockCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player, final @Nullable String name)
    {
        super(plugin, player, name, DoorType.CLOCK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReadyToConstructDoor()
    {
        return one != null && two != null && engine != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        int centerX = one.getX() + (xDepth) / 2;
        int centerY = one.getY() + (yDepth) / 2;
        int centerZ = one.getZ() + (zDepth) / 2;

        boolean validY = centerY == loc.getBlockY();
        // Remember, NS means it rotates along the z axis (north south),
        // so it must be 2 blocks deep in the x axis (east west).
        boolean validX =
            NS ? Util.between(loc.getBlockX(), one.getX(), two.getX()) : centerX == loc.getBlockX();
        boolean validZ =
            NS ? centerZ == loc.getBlockZ() : Util.between(loc.getBlockZ(), one.getZ(), two.getZ());

        return validX && validY && validZ;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateEngineLoc()
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * For Clocks, this means that the area is an upright square (x and y or z and y dimensions must match) and it must
     * have a depth of exactly 2 blocks in the remaining direction.
     * <p>
     * Also, the square must be an uneven number of blocks and at least 3 by 3.
     */
    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        xDepth = Math.abs(one.getX() - loc.getBlockX()) + 1;
        yDepth = Math.abs(one.getY() - loc.getBlockY()) + 1;
        zDepth = Math.abs(one.getZ() - loc.getBlockZ()) + 1;

        // When yDepth == zDepth, it means that the clock must rotate along the Z (north south) axis.
        NS = yDepth == zDepth;

        return (yDepth > 2 && yDepth % 2 == 1) &&
            ((!NS && zDepth == 2) || (NS && xDepth == 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_CLOCK_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getToolLore()
    {
        return messages.getString(Message.CREATOR_CLOCK_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_CLOCK_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_CLOCK_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_CLOCK_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_CLOCK_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_CLOCK_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep3()
    {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_CLOCK_SUCCESS);
    }
}
