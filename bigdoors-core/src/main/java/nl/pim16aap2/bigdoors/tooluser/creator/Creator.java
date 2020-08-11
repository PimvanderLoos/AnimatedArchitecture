package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;


// TODO: Store a function that retrieves which step comes after the current one in the IStep implementations.
//       In most cases, this can be extraordinarily simple (just return currentIDX + 1), but in same cases, it might be
//       necessary to go to a specific step (e.g. skipping price confirmation). Adding "int getNextStep()" to the IStep
//       interface would make this a lot easier. It would also circumvent the issue of the awkward "bla bla step is
//       incremented by 1 if successful". Granted, it would still required a modifier, "bla bla the next step is
//       selected if successful", but it's still much better.
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

    @Override
    protected void prepareNextStep()
    {
        getCurrentStep().ifPresent(this::sendMessage);
        if (stepIDX == (procedure.size() - 1))
            completeCreationProcess();
    }

    /**
     * Constructs the {@link AbstractDoorBase.DoorData} for the current door. This is the same for all doors.
     *
     * @return The {@link AbstractDoorBase.DoorData} for the current door.
     */
    protected final AbstractDoorBase.DoorData constructDoorData()
    {
        final boolean isOpen = false;
        final boolean isLocked = false;
        final long doorUID = -1;
        final @NotNull DoorOwner owner = new DoorOwner(doorUID, player.getUUID(), player.getName(), 0);
        return new AbstractDoorBase.DoorData(doorUID, name, cuboid.getMin(), cuboid.getMax(), engine, powerblock, world,
                                             isOpen, opendir, owner, isLocked);
    }

    /**
     * Completes the creation process. It'll construct and insert the door and complete the {@link ToolUser} process.
     *
     * @return True, so that it fits the functional interface being used for the steps.
     * <p>
     * If the insertion fails for whatever reason, it'll just be ignored, because at that point, there's no sense in
     * continuing the creation process anyway.
     */
    protected boolean completeCreationProcess()
    {
        // Only insert the door if the ToolUser hasn't been shut down yet.
        // It'll still call completeProcess() to make sure it's cleaned up properly.
        // This should've been done already, but just in case...
        if (active)
            insertDoor(constructDoor());

        completeProcess();
        return true;
    }

    /**
     * Sets the first location of the selection and advances the procedure by one.
     *
     * @param loc The first location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected boolean setFirstPos(final @NotNull IPLocationConst loc)
    {
        if (!playerHasAccessToLocation(loc))
            return false;

        world = loc.getWorld();
        firstPos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        stepIDX += 1;
        return true;
    }

    /**
     * Sets the second location of the selection and advances the procedure by one if successful.
     *
     * @param loc The second location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected boolean setSecondPos(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        cuboid = new Cuboid(new Vector3Di(firstPos),
                            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

        if (!isSizeAllowed(cuboid.getVolume()))
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_AREATOOBIG, cuboid.getVolume().toString()));
            return false;
        }

        if (!playerHasAccessToCuboid(cuboid, world))
            return false;

        stepIDX += 1;
        return true;
    }

    /**
     * Attempts to buy the door for the player and advances the procedure by one if successful.
     * <p>
     * Note that if the player does not end up buying the door, either because of insufficient funds or because they
     * rejected the offer, the current step is NOT incremented!
     *
     * @param confirm Whether or not the player confirmed they want to buy this door.
     * @return Always returns true, because either they can and do buy the door, or they cannot or refuse to buy the
     * door and the process is aborted.
     */
    protected boolean confirmPrice(final boolean confirm)
    {
        if (!confirm)
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_CANCELLED));
            shutdown();
            return true;
        }
        if (!buyDoor())
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_INSUFFICIENTFUNDS));
            shutdown();
            return true;
        }

        stepIDX += 1;
        return true;
    }

    protected Optional<RotateDirection> parseOpenDirection(final @NotNull String str)
    {
        final @NotNull String openDirName = str.toUpperCase();
        final @NotNull OptionalInt idOpt = Util.parseInt(str);

        if (idOpt.isPresent())
        {
            int id = idOpt.getAsInt();
            if (id < 0 || id > getDoorType().getValidOpenDirections().size())
            {
                PLogger.get().debug(
                    getClass().getSimpleName() + ": Player " + player.getUUID().toString() + " selected ID: " + id +
                        " out of " + getDoorType().getValidOpenDirections().size() + " options.");
                return Optional.empty();
            }

            return Optional.of(getDoorType().getValidOpenDirections().get(id));
        }

        return RotateDirection.getRotateDirection(openDirName).flatMap(
            foundOpenDir -> DoorTypeBigDoor.get().isValidOpenDirection(foundOpenDir) ?
                            Optional.of(foundOpenDir) : Optional.empty());
    }

    /**
     * Constructs the door at the end of the creation process.
     *
     * @return The newly-created door.
     */
    @NotNull
    protected abstract AbstractDoorBase constructDoor();

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
        //       Or maybe just finish it anyway and send whatever message once it is done.
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

    /**
     * Attempts to buy the door for the current player.
     *
     * @return True if the player has bought the door or if the economy is not enabled.
     */
    protected boolean buyDoor()
    {
        if (cuboid == null)
            return false;

        if (!isEconomyEnabled())
            return true;

        return BigDoors.get().getPlatform().getEconomyManager()
                       .buyDoor(player, world, getDoorType(), cuboid.getVolume());
    }

    protected OptionalDouble getPrice()
    {
        // TODO: Perhaps this should be cached.
        if (cuboid == null || !isEconomyEnabled())
            return OptionalDouble.empty();
        return BigDoors.get().getPlatform().getEconomyManager().getPrice(getDoorType(), cuboid.getVolume());
    }

    protected final boolean isEconomyEnabled()
    {
        return BigDoors.get().getPlatform().getEconomyManager().isEconomyEnabled();
    }

    protected String getOpenDirections()
    {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (RotateDirection rotateDirection : getDoorType().getValidOpenDirections())
            sb.append(idx++).append(": ").append(messages.getString(rotateDirection.getMessage())).append("\n");
        return sb.toString();
    }
}
