package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.tooluser.Procedure;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorBoolean;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorString;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
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
    protected Vector3DiConst firstPos, engine, powerblock;
    protected RotateDirection opendir;
    protected IPWorld world;
    protected boolean isOpen = false;
    protected boolean isLocked = false;

    /**
     * Factory for the {@link IStep} that sets the name.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factorySetName;
    /**
     * Factory for the {@link IStep} that sets the first position of the area of the door.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factorySetFirstPos;
    /**
     * Factory for the {@link IStep} that sets the second position of the area of the door, thus completing the {@link
     * Cuboid}.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factorySetSecondPos;
    /**
     * Factory for the {@link IStep} that sets the position of the door's engine.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factorySetEnginePos;
    /**
     * Factory for the {@link IStep} that sets the position of the door's power block.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factorySetPowerBlockPos;
    /**
     * Factory for the {@link IStep} that sets the open direction of the door.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factorySetOpenDir;
    /**
     * Factory for the {@link IStep} that allows the player to confirm or reject the price of the door.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factoryConfirmPrice;
    /**
     * Factory for the {@link IStep} that completes this process.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory<Creator> factoryCompleteProcess;

    protected Creator(final @NotNull IPPlayer player)
    {
        super(player);
    }

    @Override
    protected void init()
    {
        factorySetName =
            new Step.Factory<Creator>("SET_NAME")
                .stepExecutor(new StepExecutorString(this::completeNamingStep));

        factorySetFirstPos =
            new Step.Factory<Creator>("SET_FIRST_POST")
                .stepExecutor(new StepExecutorPLocation(this::setFirstPos));

        factorySetSecondPos =
            new Step.Factory<Creator>("SET_SECOND_POS")
                .stepExecutor(new StepExecutorPLocation(this::setSecondPos));

        factorySetEnginePos =
            new Step.Factory<Creator>("SET_ENGINE_POS")
                .stepExecutor(new StepExecutorPLocation(this::completeSetEngineStep));

        factorySetPowerBlockPos =
            new Step.Factory<Creator>("SET_POWER_BLOCK_POS")
                .stepExecutor(new StepExecutorPLocation(this::completeSetPowerBlockStep));

        factorySetOpenDir =
            new Step.Factory<Creator>("SET_OPEN_DIRECTION")
                .stepExecutor(new StepExecutorString(this::completeSetOpenDirStep))
                .messageVariableRetrievers(Collections.singletonList(this::getOpenDirections));

        factoryConfirmPrice =
            new Step.Factory<Creator>("CONFIRM_DOOR_PRICE")
                .stepExecutor(new StepExecutorBoolean(this::confirmPrice))
                .skipCondition(this::skipConfirmPrice)
                .messageVariableRetrievers(
                    Collections.singletonList(() -> String.format("%.2f", getPrice().orElse(0))));

        factoryCompleteProcess =
            new Step.Factory<Creator>("COMPLETE_CREATION_PROCESS")
                .stepExecutor(new StepExecutorVoid(this::completeCreationProcess))
                .waitForUserInput(false);
    }

    /**
     * Constructs the {@link AbstractDoorBase.DoorData} for the current door. This is the same for all doors.
     *
     * @return The {@link AbstractDoorBase.DoorData} for the current door.
     */
    protected final @NotNull AbstractDoorBase.DoorData constructDoorData()
    {
        // TODO: Make sure all variables are set.
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

        cleanUpProcess();
        return true;
    }

    protected abstract void giveTool();

    /**
     * Completes the naming step for this {@link Creator}. This means that it'll set the name, go to the next step, and
     * give the user the creator tool.
     * <p>
     * Note that there are some requirements that the name must meet. See {@link Util#isValidDoorName(String)}.
     *
     * @param str The desired name of the door.
     * @return True if the naming step was finished successfully.
     */
    protected boolean completeNamingStep(final @NotNull String str)
    {
        if (!Util.isValidDoorName(str))
            return false; // TODO: Inform the user.

        name = str;
        procedure.goToNextStep();
        giveTool();
        return true;
    }

    /**
     * Sets the first location of the selection and advances the procedure if successful.
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
        procedure.goToNextStep();
        return true;
    }

    /**
     * Sets the second location of the selection and advances the procedure if successful.
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

        final @NotNull OptionalInt sizeLimit = LimitsManager.getLimit(player, Limit.DOOR_SIZE);
        if (sizeLimit.isPresent() && cuboid.getVolume() > sizeLimit.getAsInt())
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_AREATOOBIG, cuboid.getVolume().toString(),
                                                  Integer.toString(sizeLimit.getAsInt())));
            return false;
        }

        if (!playerHasAccessToCuboid(cuboid, world))
            return false;

        procedure.goToNextStep();
        return true;
    }

    /**
     * Attempts to buy the door for the player and advances the procedure if successful.
     * <p>
     * Note that if the player does not end up buying the door, either because of insufficient funds or because they
     * rejected the offer, the current step is NOT advanced!
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
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_INSUFFICIENTFUNDS,
                                                  String.format("%.2f", getPrice().orElse(0))));
            shutdown();
            return true;
        }

        procedure.goToNextStep();
        return true;
    }

    /**
     * Parses the selected open direction from a String.
     * <p>
     * If the String is an integer value, it will try to get the {@link RotateDirection} at the corresponding index in
     * the list of valid open directions as obtained from {@link DoorType#getValidOpenDirections()}.
     * <p>
     * If the String is not an integer value, it will try to match it to the name of a {@link RotateDirection}. Note
     * that it has to be an exact match.
     *
     * @param str The name or index of the selected open direction.
     * @return The selected {@link RotateDirection}, if it exists.
     */
    // TODO: Do not match against the enum names of RotateDirection, but against localized RotateDirection names.
    protected @NotNull Optional<RotateDirection> parseOpenDirection(final @NotNull String str)
    {
        final @NotNull String openDirName = str.toUpperCase();
        final @NotNull OptionalInt idOpt = Util.parseInt(str);

        final @NotNull List<RotateDirection> validOpenDirs = getValidOpenDirections();

        if (idOpt.isPresent())
        {
            int id = idOpt.getAsInt();
            if (id < 0 || id >= validOpenDirs.size())
            {
                PLogger.get().debug(
                    getClass().getSimpleName() + ": Player " + player.getUUID().toString() + " selected ID: " + id +
                        " out of " + validOpenDirs.size() + " options.");
                return Optional.empty();
            }

            return Optional.of(validOpenDirs.get(id));
        }

        return RotateDirection.getRotateDirection(openDirName).flatMap(
            foundOpenDir -> validOpenDirs.contains(foundOpenDir) ?
                            Optional.of(foundOpenDir) : Optional.empty());
    }

    /**
     * Attempts to complete the step that sets the {@link #opendir}. It uses the open direction as parsed from a String
     * using {@link #parseOpenDirection(String)} if possible.
     * <p>
     * If no valid open direction for this type can be found, nothing changes.
     *
     * @param str The name or index of the {@link RotateDirection} that was selected by the player.
     * @return True if the {@link #opendir} was set successfully.
     */
    protected boolean completeSetOpenDirStep(final @NotNull String str)
    {
        return parseOpenDirection(str).map(
            foundOpenDir ->
            {
                opendir = foundOpenDir;
                procedure.goToNextStep();
                return true;
            }).orElse(false);
    }

    /**
     * Constructs the door at the end of the creation process.
     *
     * @return The newly-created door.
     */
    protected abstract @NotNull AbstractDoorBase constructDoor();

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
        //       There's nothing that can be done about failure anyway.
        DoorRegistry.get().addDoorBase(door).whenComplete(
            (newDoor, throwable) ->
            {
                if (!newDoor.isPresent())
                    PLogger.get().severe("Failed to insert door after creation!");
            });
    }

    /**
     * Obtains the type of door this creator will create.
     *
     * @return The type of door that will be created.
     */
    protected abstract @NotNull DoorType getDoorType();

    /**
     * Attempts to buy the door for the current player.
     *
     * @return True if the player has bought the door or if the economy is not enabled.
     */
    protected boolean buyDoor()
    {
        if (cuboid == null)
            return false;

        if (!BigDoors.get().getPlatform().getEconomyManager().isEconomyEnabled())
            return true;

        return BigDoors.get().getPlatform().getEconomyManager()
                       .buyDoor(player, world, getDoorType(), cuboid.getVolume());
    }

    /**
     * Gets the price of the door based on its volume. If the door is free because the price is <= 0 or the {@link
     * IEconomyManager} is disabled, the price will be empty.
     *
     * @return The price of the door if a positive price could be found.
     */
    protected @NotNull OptionalDouble getPrice()
    {
        // TODO: Perhaps this should be cached.
        if (cuboid == null || !BigDoors.get().getPlatform().getEconomyManager().isEconomyEnabled())
            return OptionalDouble.empty();
        return BigDoors.get().getPlatform().getEconomyManager().getPrice(getDoorType(), cuboid.getVolume());
    }

    /**
     * Checks if the step that asks the user to confirm that they want to buy the door should be skipped.
     * <p>
     * It should be skipped if the door is free for whatever reason. See {@link #getPrice()}.
     *
     * @return True if the step that asks the user to confirm that they want to buy the door should be skipped.
     */
    protected boolean skipConfirmPrice()
    {
        return !getPrice().isPresent();
    }

    /**
     * Gets the list of available open directions for the {@link DoorType} that is being created in the following
     * format:
     * <p>
     * "idx: RotateDirection\n"
     *
     * @return The list of valid open directions for this type, each on their own line.
     */
    protected @NotNull String getOpenDirections()
    {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (RotateDirection rotateDirection : getValidOpenDirections())
            sb.append(idx++).append(": ").append(messages.getString(rotateDirection.getMessage())).append("\n");
        return sb.toString();
    }

    /**
     * Gets the list of valid open directions for this type. It returns a subset of {@link
     * DoorType#getValidOpenDirections()} based on the current physical aspects of the {@link AbstractDoorBase}.
     *
     * @return The list of valid open directions for this type given its current physical dimensions.
     */
    protected @NotNull List<RotateDirection> getValidOpenDirections()
    {
        return getDoorType().getValidOpenDirections();
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the second position of the {@link
     * AbstractDoorBase} that is being created.
     *
     * @param loc The selected location of the engine.
     * @return True if the location of the area was set successfully.
     */
    protected boolean completeSetPowerBlockStep(final @NotNull IPLocationConst loc)
    {
        if (!loc.getWorld().getUID().equals(world.getUID()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final @NotNull Vector3Di pos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (cuboid.isPosInsideCuboid(pos))
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_POWERBLOCKINSIDEDOOR));
            return false;
        }

        final @NotNull OptionalInt distanceLimit = LimitsManager.getLimit(player, Limit.POWERBLOCK_DISTANCE);
        final double distance;
        if (distanceLimit.isPresent() &&
            (distance = cuboid.getCenter().getDistance(pos)) > distanceLimit.getAsInt())
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_POWERBLOCKTOOFAR,
                                                  String.format("%.2f", distance),
                                                  Integer.toString(distanceLimit.getAsInt())));
            return false;
        }

        powerblock = pos;

        procedure.goToNextStep();
        removeTool();
        return true;
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the location of the engine for the {@link
     * AbstractDoorBase} that is being created.
     *
     * @param loc The selected location of the engine.
     * @return True if the location of the engine was set successfully.
     */
    protected boolean completeSetEngineStep(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final @NotNull Vector3Di pos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!cuboid.clone().changeDimensions(1, 1, 1).isPosInsideCuboid(pos))
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_INVALIDROTATIONPOINT));
            return false;
        }

        engine = pos;
        procedure.goToNextStep();
        return true;
    }
}
