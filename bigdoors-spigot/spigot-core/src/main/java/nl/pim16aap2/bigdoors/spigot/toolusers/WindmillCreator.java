package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorType#WINDMILL}.
 *
 * @author Pim
 **/
public class WindmillCreator extends BigDoorCreator
{
    public WindmillCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                           final @Nullable String name)
    {
        super(plugin, player, name, DoorType.WINDMILL);
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
    @NotNull
    @Override
    protected Vector3Di getPowerBlockLoc()
    {
        return engine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        // Check if the engine selection is valid. In the case of a windmill, all this
        // means is that the selected block should be within x/y/z bounds.
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
        // Check if the second position is valid (door is 1 deep).
        int xDepth = Math.abs(one.getX() - loc.getBlockX());
        int yDepth = Math.abs(one.getY() - loc.getBlockY());
        int zDepth = Math.abs(one.getZ() - loc.getBlockZ());

        // Check if it's only 1 deep in exactly 1 direction.
        return (xDepth == 0 ^ zDepth == 0 ^ yDepth == 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_WINDMILL_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_WINDMILL_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep3()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_WINDMILL_SUCCESS);
    }
}
