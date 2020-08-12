//package nl.pim16aap2.bigdoors.tooluser.creator;
//
//import lombok.Getter;
//import nl.pim16aap2.bigdoors.BigDoors;
//import nl.pim16aap2.bigdoors.api.IPLocationConst;
//import nl.pim16aap2.bigdoors.api.IPPlayer;
//import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
//import nl.pim16aap2.bigdoors.doors.Elevator;
//import nl.pim16aap2.bigdoors.doortypes.DoorType;
//import nl.pim16aap2.bigdoors.doortypes.DoorTypeElevator;
//import nl.pim16aap2.bigdoors.tooluser.step.IStep;
//import nl.pim16aap2.bigdoors.tooluser.ToolUser;
//import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
//import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorBoolean;
//import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorPLocation;
//import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorString;
//import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutorVoid;
//import nl.pim16aap2.bigdoors.util.PLogger;
//import nl.pim16aap2.bigdoors.util.RotateDirection;
//import nl.pim16aap2.bigdoors.util.messages.Message;
//import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//import java.util.function.Function;
//
//public class CreatorElevator extends Creator
//{
//    @NotNull
//    private static final DoorType doorType = DoorTypeElevator.get();
//
//    public CreatorElevator(final @NotNull IPPlayer player, final @Nullable String name)
//    {
//        super(player);
//        if (name == null)
//            stepIDX = Step.SET_NAME.ordinal();
//        else
//            setName(name);
//        prepareNextStep();
//    }
//
//    public CreatorElevator(final @NotNull IPPlayer player)
//    {
//        this(player, null);
//    }
//
//    @Override
//    @NotNull
//    protected DoorType getDoorType()
//    {
//        return doorType;
//    }
//
//    @Override
//    @NotNull
//    public String getStepMessage(final @NotNull StepExecutor stepExecutor)
//    {
//        return Step.getProcedure(procedure.indexOf(stepExecutor)).map(
//            procedure -> procedure.getMessage(this))
//                   .orElse("ERROR: Failed to find procedure from idx!");
//    }
//
//    @Override
//    @NotNull
//    protected List<StepExecutor> constructProcedure()
//    {
//        final @NotNull List<StepExecutor> procedure = new ArrayList<>(Step.getValues().size());
//        Step.getValues().forEach(proc -> procedure.add(proc.getStep(this)));
//        return procedure;
//    }
//
//    private boolean setName(final @NotNull String str)
//    {
//        name = str;
//        stepIDX = Step.SET_FIRST_POS.ordinal();
//        giveTool(Message.CREATOR_GENERAL_STICKNAME, Message.CREATOR_BIGDOOR_STICKLORE, Message.CREATOR_BIGDOOR_INIT);
//        return true;
//    }
//
//    private boolean setEnginePos(final @NotNull IPLocationConst loc)
//    {
//        if (!verifyWorldMatch(loc))
//            return false;
//
//        if (!playerHasAccessToLocation(loc))
//            return false;
//
//        final @NotNull Vector3Di pos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
//        if (!cuboid.isPosInsideCuboid(pos))
//        {
//            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_INVALIDROTATIONPOINT));
//            return false;
//        }
//
//        engine = pos;
//        stepIDX = Step.SET_POWER_BLOCK_POS.ordinal();
//        return true;
//    }
//
//    private boolean setPowerBlockPos(final @NotNull IPLocationConst loc)
//    {
//        if (!loc.getWorld().getUID().equals(world.getUID()))
//            return false;
//
//        if (!playerHasAccessToLocation(loc))
//            return false;
//
//        final @NotNull Vector3Di pos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
//        if (cuboid.isPosInsideCuboid(pos))
//        {
//            player.sendMessage(messages.getString(Message.CREATOR_GENERAL_POWERBLOCKINSIDEDOOR));
//            return false;
//        }
//        powerblock = pos;
//
//        stepIDX = Step.SET_OPEN_DIR.ordinal();
//        removeTool();
//        return true;
//    }
//
//    private boolean setOpenDir(final @NotNull String str)
//    {
//        return parseOpenDirection(str).map(
//            foundOpenDir ->
//            {
//                opendir = foundOpenDir;
//                stepIDX = getPrice().isPresent() ? Step.CONFIRM_PRICE.ordinal() :
//                          Step.COMPLETE_PROCESS.ordinal();
//                return true;
//            }).orElse(false);
//    }
//
//    @Override
//    @NotNull
//    protected AbstractDoorBase constructDoor()
//    {
//        final @NotNull AbstractDoorBase.DoorData doorData = constructDoorData();
//        final int blocksToMove;
//        if (opendir == RotateDirection.NORTH || opendir == RotateDirection.SOUTH)
//            blocksToMove = doorData.getMax().getZ() - doorData.getMin().getZ() + 1;
//        else
//            blocksToMove = doorData.getMax().getX() - doorData.getMin().getX() + 1;
//
//        final @NotNull Elevator door = new Elevator(doorData, blocksToMove);
//        BigDoors.get().getMessagingInterface().broadcastMessage(door.toString());
//        return door;
//    }
//
//    /**
//     * Checks if the {@link ToolUser} is invalid for the current class.
//     *
//     * @param toolUser The {@link ToolUser} to check.
//     * @return True if the class is invalid, otherwise false.
//     */
//    private static boolean invalidClass(final @NotNull ToolUser toolUser)
//    {
//        if (toolUser instanceof CreatorElevator)
//            return false;
//        PLogger.get().logException(
//            new IllegalArgumentException(
//                "ToolUser " + toolUser.getClass().getSimpleName() + " not of type: " +
//                    CreatorElevator.class.getSimpleName()));
//        // TODO: Maybe abort creator if this goes wrong?
//        //       Maybe move this to Creator? As non-static.
//
//        return true;
//    }
//
//    private enum Step implements IStep
//    {
//        SET_NAME(creatorElevator -> new StepExecutorString(creatorElevator::setName), Message.CREATOR_GENERAL_GIVENAME),
//
//        SET_FIRST_POS(creatorElevator -> new StepExecutorPLocation(creatorElevator::setFirstPos),
//                      Message.CREATOR_BIGDOOR_STEP1),
//
//        SET_SECOND_POS(creatorElevator -> new StepExecutorPLocation(creatorElevator::setSecondPos),
//                       Message.CREATOR_BIGDOOR_STEP2),
//
//        SET_POWER_BLOCK_POS(creatorElevator -> new StepExecutorPLocation(creatorElevator::setPowerBlockPos),
//                            Message.CREATOR_GENERAL_SETPOWERBLOCK),
//
//        SET_OPEN_DIR(creatorElevator -> new StepExecutorString(creatorElevator::setOpenDir),
//                     Message.CREATOR_GENERAL_SETOPENDIR,
//                     Creator::getOpenDirections),
//
//        CONFIRM_PRICE(creatorElevator -> new StepExecutorBoolean(creatorElevator::confirmPrice),
//                      Message.CREATOR_GENERAL_CONFIRMPRICE,
//                      creator -> String.format("%.2f", creator.getPrice().orElse(0))),
//
//        COMPLETE_PROCESS(creatorElevator -> new StepExecutorVoid(creatorElevator::completeCreationProcess),
//                         Message.CREATOR_BIGDOOR_SUCCESS),
//        ;
//
//        @NotNull
//        final Message message;
//
//        @Getter
//        @NotNull
//        private static final List<Step> values = Collections.unmodifiableList(Arrays.asList(Step.values()));
//
//        @NotNull
//        final List<Function<CreatorElevator, String>> messageVariablesRetrievers;
//
//        @NotNull
//        final Function<CreatorElevator, StepExecutor> functionRetriever;
//
//        Step(final @NotNull Function<CreatorElevator, StepExecutor> functionRetriever,
//             final @NotNull Message message,
//             final @NotNull Function<CreatorElevator, String>... messageVariablesRetrievers)
//        {
//            this.functionRetriever = functionRetriever;
//            this.message = message;
//            if (messageVariablesRetrievers.length != Message.getVariableCount(this.message))
//            {
//                VerifyError e =
//                    new VerifyError("Parameter mismatch for " + name() + ". Expected: " +
//                                        Message.getVariableCount(this.message) + " but received: " +
//                                        messageVariablesRetrievers.length);
//                PLogger.get().logError(e);
//                // TODO: Throw the error as well. The PLogger needs to be able to log errors (and execptions) without
//                //  dumping them in the console as well, to avoid logging them twice.
//            }
//
//            this.messageVariablesRetrievers = Collections.unmodifiableList(Arrays.asList(messageVariablesRetrievers));
//        }
//
//        /**
//         * Gets the step associated with this part of the procedure.
//         *
//         * @param creatorElevator The {@link CreatorElevator} that owns this step.
//         * @return The newly-created step.
//         */
//        @NotNull
//        public StepExecutor getStep(final @NotNull CreatorElevator creatorElevator)
//        {
//            return functionRetriever.apply(creatorElevator);
//        }
//
//        @Override
//        public <T extends ToolUser> List<String> populateVariables(final @NotNull T toolUser)
//        {
//            if (CreatorElevator.invalidClass(toolUser))
//                return new ArrayList<>(); // TODO: Handle more gracefully.
//
//            List<String> variables = new ArrayList<>(getValues().size());
//            final @NotNull CreatorElevator creatorElevator = (CreatorElevator) toolUser;
//            messageVariablesRetrievers.forEach(fun -> variables.add(fun.apply(creatorElevator)));
//            return variables;
//        }
//
//        @Override
//        @NotNull
//        public Message getMessage()
//        {
//            return message;
//        }
//
//        @NotNull
//        public static Optional<Step> getProcedure(final int idx)
//        {
//            if (idx < 0 || idx >= values().length)
//            {
//                PLogger.get().logException(
//                    new IndexOutOfBoundsException("IDX: " + idx + " is out of range [0;" + values().length + ")!!"));
//                return Optional.empty();
//            }
//            return Optional.of(values.get(idx));
//        }
//    }
//}
