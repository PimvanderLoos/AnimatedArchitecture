package nl.pim16aap2.bigdoors.toolusers;


import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating an {@link DoorType#ELEVATOR}.
 *
 * @author Pim
 **/
public class ElevatorCreator extends Creator
{
    public ElevatorCreator(final @NotNull BigDoors plugin, final @NotNull Player player, final @Nullable String name)
    {
        super(plugin, player, name, DoorType.ELEVATOR);
        super.init();
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
     * Updates the location of the engine.
     */
    protected void updateEngineLoc()
    {
        int xMid = one.getBlockX() + (two.getBlockX() - one.getBlockX()) / 2;
        int zMid = one.getBlockZ() + (two.getBlockZ() - one.getBlockZ()) / 2;
        int yMin = one.getBlockY();
        engine = new Location(one.getWorld(), xMid, yMin, zMid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_ELEVATOR_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolLore()
    {
        return messages.getString(Message.CREATOR_ELEVATOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selector(final @NotNull Location loc)
    {
        if (isUnnamed() || !creatorHasPermissionInLocation(loc) || !isPosTwoValid(loc))
            return;

        if (one == null)
        {
            one = loc;
            SpigotUtil.messagePlayer(player, getStep1());
        }
        else
            two = loc;

        if (one != null && two != null)
        {
            minMaxFix();
            updateEngineLoc();
            setIsDone(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_ELEVATOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_ELEVATOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_ELEVATOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_ELEVATOR_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_ELEVATOR_STEP2);
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
        return messages.getString(Message.CREATOR_ELEVATOR_SUCCESS);
    }
}
