package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class PowerBlockInspector extends ToolUser
{
    public PowerBlockInspector(BigDoors plugin, Player player, long doorUID)
    {
        super(plugin, player);
        this.doorUID = doorUID;
        triggerGiveTool();
    }

    @Override
    protected void triggerGiveTool()
    {
        giveToolToPlayer(messages.getString(Message.CREATOR_PBINSPECTOR_STICKLORE).split("\n"),
                         messages.getString(Message.CREATOR_PBINSPECTOR_INIT).split("\n"));
    }

    @Override
    protected void triggerFinishUp()
    {
        finishUp();
    }

    @Override
    public void selector(Location loc)
    {
        done = true;
        List<DoorBase> doors = plugin.getDatabaseManager().doorsFromPowerBlockLoc(loc, loc.getWorld().getUID());
        if (doors.size() == 0)
            return;

        ((SubCommandInfo) plugin.getCommand(CommandData.INFO)).execute(player, doors);
        setIsDone(true);
    }
}
