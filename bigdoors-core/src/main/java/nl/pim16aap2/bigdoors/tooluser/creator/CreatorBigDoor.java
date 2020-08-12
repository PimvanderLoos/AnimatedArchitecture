package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.tooluser.IStep;
import nl.pim16aap2.bigdoors.tooluser.Procedure;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorBoolean;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorString;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CreatorBigDoor extends Creator
{
    @NotNull
    private static final DoorType doorType = DoorTypeBigDoor.get();

    public CreatorBigDoor(final @NotNull IPPlayer player, final @Nullable String name)
    {
        super(player);
        if (name != null)
            setName(name);
        prepareCurrentStep();
    }

    public CreatorBigDoor(final @NotNull IPPlayer player)
    {
        this(player, null);
    }

    @Override
    @NotNull
    protected Procedure<?> getProcedure()
    {
        return new ProcedureBigDoor();
    }

    @Override
    @NotNull
    protected DoorType getDoorType()
    {
        return doorType;
    }

    private boolean setName(final @NotNull String str)
    {
        name = str;
        procedure.skipToStep(Step.SET_FIRST_POS);
        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_BIGDOOR_STICKLORE, Message.CREATOR_BIGDOOR_INIT);
        return true;
    }

    private boolean setEnginePos(final @NotNull IPLocationConst loc)
    {
        if (!verifyWorldMatch(loc))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final @NotNull Vector3Di pos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (!cuboid.isPosInsideCuboid(pos))
        {
            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_INVALIDROTATIONPOINT));
            return false;
        }

        engine = pos;
        procedure.goToNextStep();
        return true;
    }

    private boolean setPowerBlockPos(final @NotNull IPLocationConst loc)
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
        powerblock = pos;

        procedure.goToNextStep();
        removeTool();
        return true;
    }

    private boolean setOpenDir(final @NotNull String str)
    {
        return parseOpenDirection(str).map(
            foundOpenDir ->
            {
                opendir = foundOpenDir;
                procedure.skipToStep(getPrice().isPresent() ?
                                     Step.CONFIRM_PRICE : Step.COMPLETE_PROCESS);
                return true;
            }).orElse(false);
    }

    @Override
    @NotNull
    protected AbstractDoorBase constructDoor()
    {
        final @NotNull BigDoor door = new BigDoor(constructDoorData());
        BigDoors.get().getMessagingInterface().broadcastMessage(door.toString());
        return door;
    }

    /**
     * Checks if the {@link ToolUser} is invalid for the current class.
     *
     * @param toolUser The {@link ToolUser} to check.
     * @return True if the class is invalid, otherwise false.
     */
    private static boolean invalidClass(final @NotNull ToolUser toolUser)
    {
        if (toolUser instanceof CreatorBigDoor)
            return false;
        PLogger.get().logException(
            new IllegalArgumentException(
                "ToolUser " + toolUser.getClass().getSimpleName() + " not of type: " +
                    CreatorBigDoor.class.getSimpleName()));
        // TODO: Maybe abort creator if this goes wrong?
        //       Maybe move this to Creator? As non-static.

        return true;
    }

    private class ProcedureBigDoor extends Procedure<CreatorBigDoor>
    {
        public ProcedureBigDoor()
        {
            super(CreatorBigDoor.this, Step.getSteps());
        }

//        @Override
//        public boolean applyStepExecutor(@NotNull Object obj)
//        {
//            return currentStep.getStepExecutor(toolUser).map(stepExecutor -> stepExecutor.apply(obj)).orElse(false);
//        }
//
//
//        @Override
//        public int indexOf(final @NotNull IStep step)
//        {
//            if (!(step instanceof Step))
//                return -1;
//            return ((Step) step).ordinal();
//        }
//
//        @Override
//        public IStep goToNextStep(final @NotNull IStep currentStep)
//        {
//            // TODO: Check within bounds.
//            return getSteps().get(indexOf(currentStep) + 1);
//        }
    }

    private enum Step implements IStep
    {
        SET_NAME(true, creatorBigDoor -> new StepExecutorString(creatorBigDoor::setName),
                 Message.CREATOR_GENERAL_GIVENAME),

        SET_FIRST_POS(true, creatorBigDoor -> new StepExecutorPLocation(creatorBigDoor::setFirstPos),
                      Message.CREATOR_BIGDOOR_STEP1),

        SET_SECOND_POS(true, creatorBigDoor -> new StepExecutorPLocation(creatorBigDoor::setSecondPos),
                       Message.CREATOR_BIGDOOR_STEP2),

        SET_ENGINE_POS(true, creatorBigDoor -> new StepExecutorPLocation(creatorBigDoor::setEnginePos),
                       Message.CREATOR_BIGDOOR_STEP3),

        SET_POWER_BLOCK_POS(true, creatorBigDoor -> new StepExecutorPLocation(creatorBigDoor::setPowerBlockPos),
                            Message.CREATOR_GENERAL_SETPOWERBLOCK),

        SET_OPEN_DIR(true, creatorBigDoor -> new StepExecutorString(creatorBigDoor::setOpenDir),
                     Message.CREATOR_GENERAL_SETOPENDIR,
                     Creator::getOpenDirections),

        CONFIRM_PRICE(true, creatorBigDoor -> new StepExecutorBoolean(creatorBigDoor::confirmPrice),
                      Message.CREATOR_GENERAL_CONFIRMPRICE,
                      creator -> String.format("%.2f", creator.getPrice().orElse(0))),

        COMPLETE_PROCESS(false, creatorBigDoor -> new StepExecutorVoid(creatorBigDoor::completeCreationProcess),
                         Message.CREATOR_BIGDOOR_SUCCESS),
        ;

        @NotNull
        final Message message;

        @Getter
        @NotNull
        private static final List<IStep> steps = Collections.unmodifiableList(Arrays.asList(Step.values()));

//        @NotNull
//        final Function<CreatorBigDoor, IStep> nextStepRetriever;

        final boolean waitForUserInput;

        @NotNull
        final List<Function<CreatorBigDoor, String>> messageVariablesRetrievers;

        @NotNull
        final Function<CreatorBigDoor, StepExecutor> stepExecutorRetriever;

        Step(final boolean waitForUserInput,
             final @NotNull Function<CreatorBigDoor, StepExecutor> stepExecutorRetriever,
//             final @NotNull Function<CreatorBigDoor, IStep> nextStepRetriever,
             final @NotNull Message message,
             final @NotNull Function<CreatorBigDoor, String>... messageVariablesRetrievers)
        {
            this.waitForUserInput = waitForUserInput;
            this.stepExecutorRetriever = stepExecutorRetriever;
//            this.nextStepRetriever = nextStepRetriever;
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

        @Override
        @NotNull
        public List<String> populateVariables(final @NotNull ToolUser toolUser)
        {
            if (CreatorBigDoor.invalidClass(toolUser))
                return new ArrayList<>(); // TODO: Handle more gracefully.

            final @NotNull CreatorBigDoor creatorBigDoor = (CreatorBigDoor) toolUser;
            List<String> variables = new ArrayList<>(messageVariablesRetrievers.size());
            messageVariablesRetrievers.forEach(fun -> variables.add(fun.apply(creatorBigDoor)));
            return variables;
        }

        @Override
        @NotNull
        public Message getMessage()
        {
            return message;
        }

        @Override
        public boolean waitForUserInput()
        {
            return false;
        }

        @Override
        @NotNull
        public Optional<StepExecutor> getStepExecutor(final @NotNull ToolUser toolUser)
        {
            if (CreatorBigDoor.invalidClass(toolUser))
                return Optional.empty(); // TODO: Handle more descriptively.
            return Optional.of(stepExecutorRetriever.apply((CreatorBigDoor) toolUser));
        }
    }
}
