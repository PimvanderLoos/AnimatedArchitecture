package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents players in the process of creating doors. Objects of this class are instantiated when the
 * createdoor command is used and they are destroyed after The creation process has been completed successfully or the
 * timer ran out. In EventHandlers this class is used To check whether a user that is left-clicking is a DoorCreator &&
 * tell this class a left-click happened.
 **/
public class WindmillCreator extends BigDoorCreator
{
    public WindmillCreator(BigDoors plugin, Player player, String name)
    {
        super(plugin, player, name);
        type = DoorType.WINDMILL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateEngineLoc()
    {
        // No updating of the engine location is required for this type.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Location getPowerBlockLoc(World world)
    {
        return engine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(Location loc)
    {
        // Check if the engine selection is valid. In the case of a windmill, all this
        // means is that the selected block should be within x/y/z bounds.
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
    protected boolean isPosTwoValid(Location loc)
    {
        // Check if the second position is valid (door is 1 deep).
        int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
        int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

        // Check if it's only 1 deep in exactly 1 direction.
        return (xDepth == 0 ^ zDepth == 0 ^ yDepth == 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getInitMessage()
    {
        return messages.getString(Message.CREATOR_WINDMILL_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStickLore()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStickReceived()
    {
        return messages.getString(Message.CREATOR_WINDMILL_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep1()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep2()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getStep3()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_WINDMILL_SUCCESS);
    }
}
