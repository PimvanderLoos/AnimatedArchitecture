package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FlagCreator extends BigDoorCreator
{
    public FlagCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name);
        type = DoorType.FLAG;
    }

    @Override
    protected @NotNull String getInitMessage()
    {
        return messages.getString(Message.CREATOR_FLAG_INIT);
    }

    @Override
    protected @NotNull String getStickLore()
    {
        return messages.getString(Message.CREATOR_FLAG_STICKLORE);
    }

    @Override
    protected @NotNull String getStickReceived()
    {
        return messages.getString(Message.CREATOR_FLAG_INIT);
    }

    @Override
    protected @NotNull String getStep1()
    {
        return messages.getString(Message.CREATOR_FLAG_STEP1);
    }

    @Override
    protected @NotNull String getStep2()
    {
        return messages.getString(Message.CREATOR_FLAG_STEP2);
    }

    @Override
    protected @NotNull String getStep3()
    {
        return messages.getString(Message.CREATOR_FLAG_STEP3);
    }

    @Override
    protected @NotNull String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_FLAG_SUCCESS);
    }
}
