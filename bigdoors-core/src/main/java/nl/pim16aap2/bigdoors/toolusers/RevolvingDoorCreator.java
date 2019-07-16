package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents players in the process of creating doors. Objects of this class are instantiated when the
 * createdoor command is used and they are destroyed after The creation process has been completed successfully or the
 * timer ran out. In EventHandlers this class is used To check whether a user that is left-clicking is a DoorCreator &&
 * tell this class a left-click happened.
 **/
public class RevolvingDoorCreator extends BigDoorCreator
{
    public RevolvingDoorCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name);
        type = DoorType.REVOLVINGDOOR;
    }

    // Check if the engine selection is valid. In the case of a windmill, all this
    // means is that the selected block should be within x/y/z bounds.
    @Override
    protected boolean isEngineValid(Location loc)
    {
        if (loc.getBlockX() < one.getBlockX() || loc.getBlockX() > two.getBlockX() ||
                loc.getBlockY() < one.getBlockY() || loc.getBlockY() > two.getBlockY() ||
                loc.getBlockZ() < one.getBlockZ() || loc.getBlockZ() > two.getBlockZ())
            return false;
        return true;
    }

    // Check if the second position is valid (door is 1 deep).
    @Override
    protected boolean isPosTwoValid(Location loc)
    {
        int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

        // Check if it's not just one block deep in horizontal axis.
        return xDepth > 0 && zDepth > 0;
    }
    
    @Override
    protected @NotNull String getInitMessage()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_INIT);
    }

    @Override
    protected @NotNull String getStickLore()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STICKLORE);
    }

    @Override
    protected @NotNull String getStickReceived()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_INIT);
    }

    @Override
    protected @NotNull String getStep1()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STEP1);
    }

    @Override
    protected @NotNull String getStep2()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STEP2);
    }

    @Override
    protected @NotNull String getStep3()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_STEP3);
    }

    @Override
    protected @NotNull String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_REVOLVINGDOOR_SUCCESS);
    }
}
