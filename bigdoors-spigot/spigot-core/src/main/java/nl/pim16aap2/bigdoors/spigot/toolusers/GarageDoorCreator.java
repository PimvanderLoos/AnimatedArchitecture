package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorType#GARAGEDOOR}.
 *
 * @author Pim
 **/
public class GarageDoorCreator extends BigDoorCreator
{
    public GarageDoorCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                             final @Nullable String name)
    {
        super(plugin, player, name);
        type = DoorType.GARAGEDOOR;
    }

    // TODO: When an "open" garage door (i.e. flat against the ceiling) is created,
    // Put the engine height at the selected block - length of not-selected axis
    // (if x was selected, use zLen), -2. Also, put the engine 1 further in the
    // Selected axis. I.e. if xSel == xMin, xEng = xMin - 1. If xSel == xMax,
    // xEng = xMax + 1.

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateEngineLoc()
    {
        engine = one;
    }

    // TODO: Allow creation of "open" garagedoors (i.e. flat against the ceiling).

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        int xDepth = Math.abs(one.getX() - loc.getBlockX());
        int yDepth = Math.abs(one.getY() - loc.getBlockY());
        int zDepth = Math.abs(one.getZ() - loc.getBlockZ());

        if (yDepth > 0)
            updateEngineLoc();

        // Check if it's only 1 deep in exactly 1 direction and at least 2 blocks high.
        return (xDepth == 0 ^ zDepth == 0) && yDepth > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_GARAGEDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_GARAGEDOOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_GARAGEDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_GARAGEDOOR_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_GARAGEDOOR_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep3()
    {
        return messages.getString(Message.CREATOR_GARAGEDOOR_STEP3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_GARAGEDOOR_SUCCESS);
    }
}
