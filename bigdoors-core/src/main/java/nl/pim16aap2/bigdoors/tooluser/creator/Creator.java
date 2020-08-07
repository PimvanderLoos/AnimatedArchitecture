package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

public abstract class Creator extends ToolUser
{
    protected String name;
    protected Cuboid cuboid;
    protected IVector3DiConst firstPos, engine, powerblock;
    protected RotateDirection opendir;
    protected IPWorld world;

    protected Creator(final @NotNull IPPlayer player)
    {
        super(player);
    }

    protected boolean isSizeAllowed(final int blockCount)
    {
        return getLimit() < 1 || blockCount <= getLimit();
    }

    protected int getLimit(/* Limit limitType (e.g. Limit.DOOR_SIZE) */)
    {
        // TODO: Implement.
        return -1;
    }

    /**
     * Verifies that the world of the selected location matches the world that this door is being created in.
     *
     * @param loc The location to check.
     * @return True if the location is in the same world this door is being created in.
     */
    protected boolean verifyWorldMatch(final @NotNull IPLocationConst loc)
    {
        if (world.getUID().equals(loc.getWorld().getUID()))
            return true;
        PLogger.get().debug("World mismatch in ToolUser for player: " + player.getUUID().toString());
        return false;
    }

    /**
     * Takes care of inserting the door.
     *
     * @param door The door to send to the {@link DatabaseManager}.
     */
    protected void insertDoor(final @NotNull AbstractDoorBase door)
    {
        // TODO: Don't complete the process until the CompletableFuture has an actual result.
        DatabaseManager.get().addDoorBase(door).whenComplete(
            (result, throwable) ->
            {
                if (!result)
                    PLogger.get().severe("Failed to insert door after creation!");
            });
    }

    /**
     * Obtains the type of door this creator will create.
     *
     * @return The type of door that will be created.
     */
    @NotNull
    protected abstract DoorType getDoorType();

    public int getPrice()
    {
        if (cuboid == null)
            return -1;
        // TODO: Implement.
//        return BigDoors.get().getPlatform().getEconomyManager().getPrice(getDoorType(), cuboid.getVolume());
        return 1;
    }

    public String getOpenDirections()
    {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (RotateDirection rotateDirection : getDoorType().getValidOpenDirections())
            sb.append(idx++).append(": ").append(messages.getString(rotateDirection.getMessage())).append("\n");
        return sb.toString();
    }
}
