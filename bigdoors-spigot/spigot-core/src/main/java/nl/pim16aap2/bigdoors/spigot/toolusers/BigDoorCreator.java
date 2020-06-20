package nl.pim16aap2.bigdoors.spigot.toolusers;


import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.doors.EDoorType;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link EDoorType#BIGDOOR}.
 *
 * @author Pim
 **/
public class BigDoorCreator extends Creator
{
    public BigDoorCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                          final @Nullable String name)
    {
        super(plugin, player, name);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    protected AbstractDoorBase create(final @NotNull AbstractDoorBase.DoorData doorData)
    {
        return new BigDoor(doorData, 0, 0, PBlockFace.EAST);
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
        if (loc.getBlockY() < one.getY() || loc.getBlockY() > two.getY())
            return false;

        // For a regular door, the engine should be on one of the outer pillars of the
        // door.
        int xDepth = Math.abs(one.getX() - two.getX());
        int zDepth = Math.abs(one.getZ() - two.getZ());

        if (xDepth == 0 && loc.getBlockX() == one.getX())
            return loc.getBlockZ() == one.getZ() || loc.getBlockZ() == two.getZ();
        else if (zDepth == 0 && loc.getBlockZ() == one.getZ())
            return loc.getBlockX() == one.getX() || loc.getBlockX() == two.getX();
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isPosTwoValid(final @NotNull Location loc)
    {
        int xDepth = Math.abs(one.getX() - loc.getBlockX());
        int yDepth = Math.abs(one.getY() - loc.getBlockY());
        int zDepth = Math.abs(one.getZ() - loc.getBlockZ());

        // If the door is only 1 block high, it's a drawbridge.
        if (yDepth == 0)
            return false;
        // Check if it's only 1 deep in exactly 1 direction (single moving pillar is
        // useless).
        return xDepth == 0 ^ zDepth == 0;
    }

    /**
     * Updates the {@link Location} of the engine. For a {@link EDoorType#BIGDOOR}, for example, this sets the Y-value
     * of the engine coordinates to the 1 block under lowest Y-value of the {@link AbstractDoorBase}.
     */
    protected void updateEngineLoc()
    {
        engine.setY(one.getY());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selector(final @NotNull Location loc)
    {
        if (isUnnamed() || !creatorHasPermissionInLocation(loc))
            return;

        if (world == null)
            world = SpigotAdapter.wrapWorld(loc.getWorld());

        if (one == null)
        {
            one = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            String[] message = getStep2().split("\n");
            SpigotUtil.messagePlayer(player, message);
        }
        else if (two == null)
        {
            if (isPosTwoValid(loc) && !one.equals(loc))
            {
                two = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
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
                engine = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
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
