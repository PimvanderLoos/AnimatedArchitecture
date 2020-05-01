package nl.pim16aap2.bigdoors.spigot.toolusers;

import nl.pim16aap2.bigdoors.doors.EDoorType;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a user creating a {@link EDoorType#DRAWBRIDGE}.
 *
 * @author Pim
 **/
public class DrawbridgeCreator extends Creator
{
    public DrawbridgeCreator(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player,
                             final @Nullable String name)
    {
        super(plugin, player, name, EDoorType.DRAWBRIDGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReadyToConstructDoor()
    {
        return one != null && two != null && engine != null && engineSide != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEngineValid(final @NotNull Location loc)
    {
        // One is always the bottom one, so this makes sure it's on the bottom row.
        if (loc.getBlockY() != one.getY())
            return false;

        if (loc.equals(engine))
            return false;

        boolean onEdge = loc.getBlockX() == one.getX() ||
            loc.getBlockX() == two.getX() ||
            loc.getBlockZ() == one.getZ() ||
            loc.getBlockZ() == two.getZ();

        if (!onEdge)
            return false;

        boolean inArea = Util.between(loc.getBlockX(), one.getX(), two.getX()) &&
            Util.between(loc.getBlockZ(), one.getZ(), two.getZ());

        if (!inArea)
            return false;

        int xDepth = Math.abs(one.getX() - two.getX());
        int yDepth = Math.abs(one.getY() - two.getY());
        int zDepth = Math.abs(one.getZ() - two.getZ());

        if (yDepth == 0)
        {
            if (xDepth == 0)
            {
                if (loc.equals(one))
                {
                    engine = one;
                    engineSide = PBlockFace.NORTH;
                }
                else if (loc.equals(two))
                {
                    engine = two;
                    engineSide = PBlockFace.SOUTH;
                }
                return engineSide != null;
            }
            else if (zDepth == 0)
            {
                if (loc.equals(one))
                {
                    engine = one;
                    engineSide = PBlockFace.WEST;
                }
                else if (loc.equals(two))
                {
                    engine = two;
                    engineSide = PBlockFace.EAST;
                }
                return engineSide != null;
            }
        }

        // Check if there is any ambiguity. This happens when a corner was selected.
        if (engine == null)
        {
            // Check if a corner was selected.
            int posX = loc.getBlockX();
            int posZ = loc.getBlockZ();

            if (loc.equals(one) || loc.equals(two) || // "bottom left" or "top right" (on 2d grid)
                (posX == one.getX() && posZ == two.getZ()) || // "top left"
                (posX == two.getX() && posZ == one.getZ()))
                engine = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            else
            {
                if (posZ == one.getZ())
                    engineSide = PBlockFace.NORTH;
                else if (posZ == two.getZ())
                    engineSide = PBlockFace.SOUTH;
                else if (posX == one.getX())
                    engineSide = PBlockFace.WEST;
                else if (posX == two.getX())
                    engineSide = PBlockFace.EAST;
                updateEngineLoc();
            }
            return true;
        }

        // If an engine point has already been selected but an engine side wasn't determined yet.
        if (loc.equals(engine))
            return false;

        int posXa = engine.getX();
        int posZa = engine.getZ();

        // Engine axis should be on 1 axis only.
        Vector3Di vector = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).subtract(engine);
        vector.normalize();

        if (Math.abs(vector.getX() + vector.getY() + vector.getZ()) != 1)
            return false;

        // First figure out which corner was selected.
        if (engine.equals(one)) // NORTH / WEST Possible
        {
            if (vector.getX() == 1)
                engineSide = PBlockFace.NORTH;
            else if (vector.getZ() == 1)
                engineSide = PBlockFace.WEST;
        }
        else if (engine.equals(two)) // EAST / SOUTH Possible
        {
            if (vector.getX() == -1)
                engineSide = PBlockFace.SOUTH;
            else if (vector.getZ() == -1)
                engineSide = PBlockFace.EAST;
        }
        else if (posXa == one.getX() && posZa == two.getZ()) // SOUTH / WEST Possible
        {
            if (vector.getX() == 1)
                engineSide = PBlockFace.SOUTH;
            else if (vector.getZ() == -1)
                engineSide = PBlockFace.WEST;
        }
        else if (posXa == two.getX() && posZa == one.getZ()) // NORTH / EAST Possible
        {
            if (vector.getX() == -1)
                engineSide = PBlockFace.NORTH;
            else if (vector.getZ() == 1)
                engineSide = PBlockFace.EAST;
        }
        else
            return false;
        updateEngineLoc();
        return engineSide != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolReceivedMessage()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NotNull String getToolLore()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_STICKLORE);
    }

    /**
     * Puts the engine location in the center.
     */
    protected void updateEngineLoc()
    {
        if (engineSide == null || engine == null)
            return;

        // Make sure the power point is in the middle.
        if (engineSide == PBlockFace.NORTH || engineSide == PBlockFace.SOUTH)
        {
            openDirection = Util.getRotateDirection(engineSide);
            engine.setX(one.getX() + (two.getX() - one.getX()) / 2);
        }
        else
        {
            openDirection = Util.getRotateDirection(engineSide);
            engine.setZ(one.getZ() + (two.getZ() - one.getZ()) / 2);
        }
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

        if (yDepth == 0)
            return xDepth != 0 || zDepth != 0;
        return xDepth != 0 ^ zDepth != 0;
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
            String[] message = getStep1().split("\n");
            SpigotUtil.messagePlayer(player, message);
        }
        else if (two == null)
        {
            if (isPosTwoValid(loc))
            {
                two = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                // If it's up, it's closed.
                if (Math.abs(one.getY() - two.getY()) > 0)
                    isOpen = false;
                else
                    isOpen = true;

                String[] message = getStep2().split("\n");
                SpigotUtil.messagePlayer(player, message);
                minMaxFix();
            }
            else
                sendInvalidRotationMessage();
        }
        // If the engine position has not been determined yet
        else if (engine == null)
        {
            if (isEngineValid(loc))
            {
                engine = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                // If the engine side was found, print finish message.
                if (engineSide != null)
                {
                    updateEngineLoc();
                    setIsDone(true);
                }
                // If the engine side could not be determined, branch out for additional information.
                else
                    SpigotUtil.messagePlayer(player, getStep3());
            }
            else
                sendInvalidRotationMessage();
        }
        // If it's a draw bridge and the engine side wasn't determined yet.
        else if (engineSide == null)
        {
            if (isEngineValid(loc))
            {
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
    @NotNull
    protected String getInitMessage()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickLore()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_STICKLORE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStickReceived()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_INIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep1()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_STEP1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep2()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_STEP2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getStep3()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_STEP3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    protected String getSuccessMessage()
    {
        return messages.getString(Message.CREATOR_DRAWBRIDGE_SUCCESS);
    }
}
