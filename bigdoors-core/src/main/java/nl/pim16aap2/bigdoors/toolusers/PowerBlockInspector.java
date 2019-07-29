package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a user finding which {@link DoorBase}s have their power block in a location.
 *
 * @author Pim
 **/
public class PowerBlockInspector extends ToolUser
{
    public PowerBlockInspector(final @NotNull BigDoors plugin, final @NotNull Player player, final long doorUID)
    {
        super(plugin, player);
        this.doorUID = doorUID;
        giveToolToPlayer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_PBINSPECTOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolLore()
    {
        return messages.getString(Message.CREATOR_PBINSPECTOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void triggerFinishUp()
    {
        finishUp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selector(final @NotNull Location loc)
    {
        done = true;
        List<DoorBase> doors = plugin.getDatabaseManager().doorsFromPowerBlockLoc(loc, loc.getWorld().getUID());
        if (doors.size() == 0)
            return;

        ((SubCommandInfo) plugin.getCommand(CommandData.INFO)).execute(player, doors);
        setIsDone(true);
    }
}
