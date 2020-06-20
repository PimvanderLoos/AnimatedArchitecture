package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.RevolvingDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeRevolvingDoor;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorTypeRevolvingDoor}.
 *
 * @author Pim
 **/
public class RevolvingDoorCreator extends BigDoorCreator
{
    public RevolvingDoorCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
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
        return new RevolvingDoor(doorData, 4);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        if (loc.getBlockX() < one.getX() || loc.getBlockX() > two.getX() ||
            loc.getBlockY() < one.getY() || loc.getBlockY() > two.getY() ||
            loc.getBlockZ() < one.getZ() || loc.getBlockZ() > two.getZ())
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        int xDepth = Math.abs(one.getX() - loc.getBlockX());
        int zDepth = Math.abs(one.getZ() - loc.getBlockZ());

        // Check if it's not just one block deep in horizontal axis.
        return xDepth > 0 && zDepth > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep3()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STEP3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_SUCCESS);
    }
}
