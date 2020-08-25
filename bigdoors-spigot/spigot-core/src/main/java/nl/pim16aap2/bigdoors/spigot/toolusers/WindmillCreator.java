package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Windmill;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeWindmill;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorTypeWindmill}.
 *
 * @author Pim
 **/
public class WindmillCreator extends BigDoorCreator
{
    public WindmillCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                           final @Nullable String name)
    {
        super(plugin, player, name);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected AbstractDoorBase create(final @NotNull AbstractDoorBase.DoorData doorData)
    {
        return new Windmill(doorData);
    }

    @Override
    protected void updateEngineLoc()
    {
        // No updating of the engine location is required for this type.
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    protected Vector3Di getPowerBlockLoc()
    {
        return engine;
    }

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

    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_WINDMILL_INIT);
    }

    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STICKLORE);
    }

    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_WINDMILL_INIT);
    }

    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP1);
    }

    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP2);
    }

    @Override
    @NotNull
    protected String getStep3()
    {
        return messages.getString(Message.CREATOR_WINDMILL_STEP3);
    }

    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_WINDMILL_SUCCESS);
    }
}
