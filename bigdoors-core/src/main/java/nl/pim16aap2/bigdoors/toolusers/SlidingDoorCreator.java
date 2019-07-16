package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SlidingDoorCreator extends Creator
{
    public SlidingDoorCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name, DoorType.SLIDINGDOOR);
        if (name == null)
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_GIVENAME));
        else
            triggerGiveTool();
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

    // Make sure the second position is not the same as the first position
    protected boolean isPositionValid(Location loc)
    {
        if (one == null && two == null)
            return true;
        return !loc.equals(one);
    }

    // Take care of the selection points.
    @Override
    public void selector(Location loc)
    {
        if (!hasName() || !creatorHasPermissionInLocation(loc) || !isPositionValid(loc))
            return;

        if (one == null)
        {
            one = loc;
            SpigotUtil.messagePlayer(player, getStep2());
        }
        else
            two = loc;

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
        return messages.getString(Message.CREATOR_SLIDINGDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStickLore()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStickReceived()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep1()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep2()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STEP2);
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
        return messages.getString(Message.CREATOR_SLIDINGDOOR_SUCCESS);
    }
}
