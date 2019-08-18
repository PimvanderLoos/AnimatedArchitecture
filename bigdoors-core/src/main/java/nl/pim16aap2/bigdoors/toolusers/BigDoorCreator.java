package nl.pim16aap2.bigdoors.toolusers;


import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link DoorType#BIGDOOR}.
 *
 * @author Pim
 **/
public class BigDoorCreator extends Creator
{
    public BigDoorCreator(final @NotNull BigDoors plugin, final @NotNull Player player, final @Nullable String name)
    {
        super(plugin, player, name, DoorType.BIGDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReadyToConstructDoor()
    {
        return one != null && two != null && engine != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        if (loc.getBlockY() < one.getBlockY() || loc.getBlockY() > two.getBlockY())
            return false;

        // For a regular door, the engine should be on one of the outer pillars of the
        // door.
        int xDepth = Math.abs(one.getBlockX() - two.getBlockX());
        int zDepth = Math.abs(one.getBlockZ() - two.getBlockZ());

        if (xDepth == 0 && loc.getBlockX() == one.getBlockX())
            return loc.getBlockZ() == one.getBlockZ() || loc.getBlockZ() == two.getBlockZ();
        else if (zDepth == 0 && loc.getBlockZ() == one.getBlockZ())
            return loc.getBlockX() == one.getBlockX() || loc.getBlockX() == two.getBlockX();
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
        int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
        int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());

        // If the door is only 1 block high, it's a drawbridge.
        if (yDepth == 0)
            return false;
        // Check if it's only 1 deep in exactly 1 direction (single moving pillar is
        // useless).
        return xDepth == 0 ^ zDepth == 0;
    }

    /**
     * Updates the {@link Location} of the engine. For a {@link DoorType#BIGDOOR}, for example, this sets the Y-value of
     * the engine coordinates to the 1 block under lowest Y-value of the {@link DoorBase}.
     */
    protected void updateEngineLoc()
    {
        engine.setY(one.getBlockY());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selector(final @NotNull Location loc)
    {
        if (isUnnamed() || !creatorHasPermissionInLocation(loc))
            return;

        if (one == null)
        {
            one = loc;
            String[] message = getStep2().split("\n");
            SpigotUtil.messagePlayer(player, message);
        }
        else if (two == null)
        {
            if (isPosTwoValid(loc) && one != loc)
            {
                two = loc;
                // extending classes might want to have this be optional. This way they can just set engine
                // in isPosTwoValid() if possible, in which case the next print statement is skipped.
                if (engine == null)
                {
                    String[] message = getStep3().split("\n");
                    SpigotUtil.messagePlayer(player, message);
                }
                super.minMaxFix();
            }
            else
                sendInvalidPointMessage();
        }
        // If the engine position has not been determined yet
        else if (engine == null)
        {
            if (isEngineValid(loc))
            {
                engine = loc;
                updateEngineLoc();
                setIsDone(true);
            }
            else
                sendInvalidRotationMessage();
        }
        else
            setIsDone(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolLore()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep3()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_STEP3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_BIGDOOR_SUCCESS);
    }
}
