package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a user finding which {@link AbstractDoorBase}s have their power block in a location.
 *
 * @author Pim
 **/
public class PowerBlockInspector extends ToolUser
{
    public PowerBlockInspector(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player, final long doorUID)
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

        BigDoors.get().getPowerBlockManager().doorsFromPowerBlockLoc(
            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getWorld().getUID()).whenComplete(
            (doorList, throwable) ->
            {
                if (doorList.size() == 0)
                    return;
                ((SubCommandInfo) plugin.getCommand(CommandData.INFO)).execute(player, doorList);
                setIsDone(true);
            });
    }
}
