package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.SlidingDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeSlidingDoor;
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
 * Represents a user creating a {@link DoorTypeSlidingDoor}.
 *
 * @author Pim
 **/
public class SlidingDoorCreator extends Creator
{
    public SlidingDoorCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                              final @Nullable String name)
    {
        super(plugin, player, name);
        if (name == null)
            SpigotUtil.messagePlayer(player, messages.getString(Message.CREATOR_GENERAL_GIVENAME));
        else
            giveToolToPlayer();
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected AbstractDoorBase create(final @NotNull AbstractDoorBase.DoorData doorData)
    {
        return new SlidingDoor(doorData, 0, 0, 0);
    }

    @Override
    protected boolean isReadyToConstructDoor()
    {
        return one != null && two != null && engine != null;
    }

    /**
     * Puts the engine location in the center.
     */
    protected void updateEngineLoc()
    {
        int xMid = one.getX() + (two.getX() - one.getX()) / 2;
        int zMid = one.getZ() + (two.getZ() - one.getZ()) / 2;
        int yMin = one.getY();
        engine = new Vector3Di(xMid, yMin, zMid);
    }

    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        return true;
    }

    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        return true;
    }

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
            SpigotUtil.messagePlayer(player, getStep2());
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

    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STEP1);
    }

    @Override
    protected @NotNull String getToolLore()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STICKLORE);
    }

    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_INIT);
    }

    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STICKLORE);
    }

    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_INIT);
    }

    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STEP1);
    }

    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_STEP2);
    }

    @Override
    @NotNull
    protected String getStep3()
    {
        return "";
    }

    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_SLIDINGDOOR_SUCCESS);
    }
}
