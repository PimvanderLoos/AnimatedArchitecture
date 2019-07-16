package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PortcullisCreator extends Creator
{
    public PortcullisCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, DoorType.PORTCULLIS);
        super.init();
    }

    @Override
    protected boolean isReadyToCreateDoor()
    {
        return one != null && two != null && engine != null;
    }

    // Make sure the power point is in the middle.
    protected void setEngine()
    {
        int xMid = one.getBlockX() + (two.getBlockX() - one.getBlockX()) / 2;
        int zMid = one.getBlockZ() + (two.getBlockZ() - one.getBlockZ()) / 2;
        int yMin = one.getBlockY();
        engine = new Location(one.getWorld(), xMid, yMin, zMid);
    }

    // Check if the second position is valid (door is 1 deep).
    protected boolean isPosTwoValid(Location loc)
    {
        return true;
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        if (!hasName() || !creatorHasPermissionInLocation(loc))
            return;

        if (one == null)
        {
            one = loc;
            SpigotUtil.messagePlayer(player, getStep1());
        }
        else
        {
            if (isPosTwoValid(loc))
                two = loc;
            else
                super.sendInvalidPointMessage();
        }

        if (one != null && two != null)
        {
            minMaxFix();
            setEngine();
            setIsDone(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getInitMessage()
    {
        return messages.getString(Message.CREATOR_PORTCULLIS_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStickLore()
    {
        return messages.getString(Message.CREATOR_PORTCULLIS_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStickReceived()
    {
        return messages.getString(Message.CREATOR_PORTCULLIS_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep1()
    {
        return messages.getString(Message.CREATOR_PORTCULLIS_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep2()
    {
        return messages.getString(Message.CREATOR_PORTCULLIS_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep3()
    {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_PORTCULLIS_SUCCESS);
    }
}
