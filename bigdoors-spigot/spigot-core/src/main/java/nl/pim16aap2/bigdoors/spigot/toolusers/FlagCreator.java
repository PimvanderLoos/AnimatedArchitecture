package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Flag;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeFlag;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorTypeFlag}.
 *
 * @author Pim
 **/
public class FlagCreator extends BigDoorCreator
{
    public FlagCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player, final @Nullable String name)
    {
        super(plugin, player, name);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected AbstractDoorBase create(final @NotNull AbstractDoorBase.DoorData doorData)
    {
        return new Flag(doorData, false);
    }

    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_FLAG_INIT);
    }

    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_FLAG_STICKLORE);
    }

    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_FLAG_INIT);
    }

    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_FLAG_STEP1);
    }

    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_FLAG_STEP2);
    }

    @Override
    @NotNull
    protected String getStep3()
    {
        return messages.getString(Message.CREATOR_FLAG_STEP3);
    }

    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_FLAG_SUCCESS);
    }
}
