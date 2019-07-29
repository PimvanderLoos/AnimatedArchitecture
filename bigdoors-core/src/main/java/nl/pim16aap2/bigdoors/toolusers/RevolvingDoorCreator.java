package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorType#REVOLVINGDOOR}.
 *
 * @author Pim
 **/
public class RevolvingDoorCreator extends BigDoorCreator
{
    public RevolvingDoorCreator(final @NotNull BigDoors plugin, final @NotNull Player player,
                                final @Nullable String name)
    {
        super(plugin, player, name);
        type = DoorType.REVOLVINGDOOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        if (loc.getBlockX() < one.getBlockX() || loc.getBlockX() > two.getBlockX() ||
            loc.getBlockY() < one.getBlockY() || loc.getBlockY() > two.getBlockY() ||
            loc.getBlockZ() < one.getBlockZ() || loc.getBlockZ() > two.getBlockZ())
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

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
