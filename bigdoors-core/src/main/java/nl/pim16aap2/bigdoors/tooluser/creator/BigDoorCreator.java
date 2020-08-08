package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.tooluser.IProcedure;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.step.StepConfirm;
import nl.pim16aap2.bigdoors.tooluser.step.StepPLocation;
import nl.pim16aap2.bigdoors.tooluser.step.StepString;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

public class BigDoorCreator extends Creator
{
    @NotNull
    private static final DoorType doorType = DoorTypeBigDoor.get();

    public BigDoorCreator(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name == null)
            setProcedure(Procedure.SET_NAME);
        else
            setName(name);
    }

    public BigDoorCreator(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    @NotNull
    protected DoorType getDoorType()
    {
        return doorType;
    }

    @Override
    @NotNull
    public String getStepMessage(final @NotNull Step step)
    {
        return Procedure.getProcedure(procedure.indexOf(step)).map(
            procedure -> procedure.getMessage(this))
                        .orElse("ERROR: Failed to find procedure from idx!");
    }

    @Override
    @NotNull
    protected List<Step> constructProcedure()
    {
        final @NotNull List<Step> procedure = new ArrayList<>(Procedure.getValues().size());
        Procedure.getValues().forEach(proc -> procedure.add(proc.getStep(this)));
        return procedure;
    }

    private void setProcedure(final @NotNull Procedure nextStep)
    {
        stepIDX = nextStep.ordinal();
        getCurrentStep().ifPresent(this::sendMessage);
    }

    private boolean setName(final @NotNull String str)
    {
        name = str;
        setProcedure(Procedure.SET_FIRST_POS);
        return true;
    }

    private boolean setFirstPos(final @NotNull IPLocationConst loc)
    {
        world = loc.getWorld();
        firstPos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        setProcedure(Procedure.SET_SECOND_POS);
        return true;
    }

    private boolean setSecondPos(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        cuboid = new Cuboid(new Vector3Di(firstPos),
                            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

        if (!isSizeAllowed(cuboid.getVolume()))
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_AREATOOBIG, cuboid.getVolume().toString()));
            return false;
        }

        setProcedure(Procedure.SET_ENGINE_POS);
        return true;
    }

    private boolean setEnginePos(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        final @NotNull Vector3Di pos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!cuboid.isPosInsideCuboid(pos))
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_INVALIDROTATIONPOINT));
            return false;
        }

        engine = pos;
        setProcedure(Procedure.SET_POWER_BLOCK_POS);
        return true;
    }

    private boolean setPowerBlockPos(final @NotNull IPLocationConst loc)
    {
        if (!loc.getWorld().getUID().equals(world.getUID()))
            return false;

        final @NotNull Vector3Di pos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (cuboid.isPosInsideCuboid(pos))
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_POWERBLOCKINSIDEDOOR));
            return false;
        }
        powerblock = pos;

        setProcedure(Procedure.SET_OPEN_DIR);
        return true;
    }

    private boolean setOpenDir(final @NotNull String str)
    {
        String openDirName = str.toUpperCase();

        OptionalInt idOpt = Util.parseInt(str);
        if (idOpt.isPresent())
        {
            int id = idOpt.getAsInt();
            if (id < 0 || id > getDoorType().getValidOpenDirections().size())
            {
                PLogger.get().debug(
                    getClass().getSimpleName() + ": Player " + player.getUUID().toString() + " selected ID: " + id +
                        " out of " + getDoorType().getValidOpenDirections().size() + " options.");
                return false; // TODO: Inform the player
            }

            opendir = getDoorType().getValidOpenDirections().get(id);
            setProcedure(Procedure.COMPLETE);
            return true;
        }

        return RotateDirection.getRotateDirection(openDirName).map(
            foundOpenDir ->
            {
                if (DoorTypeBigDoor.get().isValidOpenDirection(foundOpenDir))
                {
                    opendir = foundOpenDir;
                    // TODO: Money, canBreakBlocks.
                    setProcedure(Procedure.COMPLETE);
                    return true;
                }
                return false;
            }
        ).orElse(false);
    }

    @NotNull
    private AbstractDoorBase constructDoor()
    {
        final boolean isOpen = false;
        final boolean isLocked = false;
        final long doorUID = -1;
        final @NotNull DoorOwner owner = new DoorOwner(doorUID, player.getUUID(), player.getName(), 0);
        final @NotNull AbstractDoorBase.DoorData doorData =
            new AbstractDoorBase.DoorData(doorUID, name, cuboid.getMin(), cuboid.getMax(), engine, powerblock, world,
                                          isOpen, opendir, owner, isLocked);
        final @NotNull BigDoor door = new BigDoor(doorData);
        BigDoors.get().getMessagingInterface().broadcastMessage(door.toString());
        return door;
    }

    private boolean complete()
    {
        BigDoors.get().getMessagingInterface().broadcastMessage("COMPLETED THE CREATOR!!");

        insertDoor(constructDoor());

        // Return true even if the insertion may have failed, because at that point, there's no sense in continuing
        // the creation process.
        return true;
    }

    /**
     * Checks if the {@link ToolUser} is invalid for the current class.
     *
     * @param toolUser The {@link ToolUser} to check.
     * @return True if the class is invalid, otherwise false.
     */
    private static boolean invalidClass(final @NotNull ToolUser toolUser)
    {
        if (toolUser instanceof BigDoorCreator)
            return false;
        PLogger.get().logException(
            new IllegalArgumentException(
                "ToolUser " + toolUser.getClass().getSimpleName() + " not of type: " +
                    BigDoorCreator.class.getSimpleName()));
        // TODO: Maybe abort creator if this goes wrong?

        return true;
    }

    private enum Procedure implements IProcedure
    {
        SET_NAME(bigDoorCreator -> new StepString(bigDoorCreator::setName), Message.CREATOR_BIGDOOR_INIT),

        SET_FIRST_POS(bigDoorCreator -> new StepPLocation(bigDoorCreator::setFirstPos), Message.CREATOR_BIGDOOR_STEP1),

        SET_SECOND_POS(bigDoorCreator -> new StepPLocation(bigDoorCreator::setSecondPos),
                       Message.CREATOR_BIGDOOR_STEP2),

        SET_ENGINE_POS(bigDoorCreator -> new StepPLocation(bigDoorCreator::setEnginePos),
                       Message.CREATOR_BIGDOOR_STEP3),

        SET_POWER_BLOCK_POS(bigDoorCreator -> new StepPLocation(bigDoorCreator::setPowerBlockPos),
                            Message.CREATOR_GENERAL_SETPOWERBLOCK),

        SET_OPEN_DIR(bigDoorCreator -> new StepString(bigDoorCreator::setOpenDir), Message.CREATOR_GENERAL_SETOPENDIR,
                     Creator::getOpenDirections),

        COMPLETE(bigDoorCreator -> new StepConfirm(bigDoorCreator::complete), Message.CREATOR_GENERAL_CONFIRMPRICE,
                 creator -> Integer.toString(creator.getPrice())),
        ;

        @NotNull
        final Message message;

        //        @Getter(onMethod = @__({@Override}))
        @Getter
        @NotNull
        private static final List<Procedure> values = Collections.unmodifiableList(Arrays.asList(Procedure.values()));

        @NotNull
        final List<Function<BigDoorCreator, String>> messageVariablesRetrievers;

        @NotNull
        final Function<BigDoorCreator, Step> functionRetriever;

        Procedure(final @NotNull Function<BigDoorCreator, Step> functionRetriever,
                  final @NotNull Message message,
                  final @NotNull Function<BigDoorCreator, String>... messageVariablesRetrievers)
        {
            this.functionRetriever = functionRetriever;
            this.message = message;
            if (messageVariablesRetrievers.length != Message.getVariableCount(this.message))
            {
                VerifyError e =
                    new VerifyError("Parameter mismatch for " + name() + ". Expected: " +
                                        Message.getVariableCount(this.message) + " but received: " +
                                        messageVariablesRetrievers.length);
                PLogger.get().logError(e);
                // TODO: Throw the error as well. The PLogger needs to be able to log errors (and execptions) without
                //  dumping them in the console as well, to avoid logging them twice.
            }

            this.messageVariablesRetrievers = Collections.unmodifiableList(Arrays.asList(messageVariablesRetrievers));
        }

        /**
         * Gets the step associated with this part of the procedure.
         *
         * @param bigDoorCreator The {@link BigDoorCreator} that owns this step.
         * @return The newly-created step.
         */
        public Step getStep(final @NotNull BigDoorCreator bigDoorCreator)
        {
            return functionRetriever.apply(bigDoorCreator);
        }

        @Override
        @NotNull
        public String getMessage(final @NotNull Creator creator)
        {
            if (BigDoorCreator.invalidClass(creator))
                return "ERROR: InvalidClass!"; // TODO: Handle more gracefully.

            final @NotNull BigDoorCreator bigDoorCreator = (BigDoorCreator) creator;

            List<String> variables = new ArrayList<>();
            messageVariablesRetrievers.forEach(fun -> variables.add(fun.apply(bigDoorCreator)));

            String[] variablesArr = new String[variables.size()];
            variablesArr = variables.toArray(variablesArr);

            return BigDoors.get().getPlatform().getMessages().getString(message, variablesArr);
        }

        @NotNull
        public static Optional<Procedure> getProcedure(final int idx)
        {
            if (idx < 0 || idx >= values().length)
            {
                PLogger.get().logException(
                    new IndexOutOfBoundsException("IDX: " + idx + " is out of range [0;" + values().length + ")!!"));
                return Optional.empty();
            }
            return Optional.of(values.get(idx));
        }
    }
}
