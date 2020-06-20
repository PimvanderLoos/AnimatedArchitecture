package nl.pim16aap2.bigdoors.spigot.toolusers;


import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Elevator;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeElevator;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating an {@link DoorTypeElevator}.
 *
 * @author Pim
 **/
public class ElevatorCreator extends Creator
{
    public ElevatorCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                           final @Nullable String name)
    {
        super(plugin, player, name);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    protected AbstractDoorBase create(final @NotNull AbstractDoorBase.DoorData doorData)
    {
        return new Elevator(doorData, 0, 0, 0);
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
        int xMid = one.getX() + (two.getX() - one.getX()) / 2;
        int zMid = one.getZ() + (two.getZ() - one.getZ()) / 2;
        int yMin = one.getY();
        engine = new Vector3Di(xMid, yMin, zMid);
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

        if (world == null)
            world = SpigotAdapter.wrapWorld(loc.getWorld());

        if (one == null)
        {
            one = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            SpigotUtil.messagePlayer(player, getStep1());
        }
        else
            two = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

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
