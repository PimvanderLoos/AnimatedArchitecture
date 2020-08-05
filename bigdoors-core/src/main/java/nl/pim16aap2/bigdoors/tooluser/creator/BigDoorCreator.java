package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.step.StepConfirm;
import nl.pim16aap2.bigdoors.tooluser.step.StepPLocation;
import nl.pim16aap2.bigdoors.tooluser.step.StepString;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BigDoorCreator extends Creator<BigDoorCreator>
{
    private static final List<Step<BigDoorCreator>> procedure =
        Collections.unmodifiableList(Arrays.stream(Procedure.values())
                                           .map(Procedure::getStep)
                                           .collect(Collectors.toList()));

    private static final DoorType type = DoorTypeBigDoor.get();

    public BigDoorCreator(final @NotNull IPPlayer player)
    {
        super(player);
        setProcedure(Procedure.SET_NAME);
    }

    public BigDoorCreator(final @NotNull IPPlayer player, final @NotNull String name)
    {
        this(player);
        setName(name);
    }

    @Override
    @NotNull
    protected DoorType getDoorType()
    {
        return type;
    }

    private void setProcedure(final @NotNull Procedure nextStep, final String... values)
    {
        System.out.print("Setting stepIDX (" + stepIDX + ") to: " + nextStep.ordinal());
        stepIDX = nextStep.ordinal();
        Message message = nextStep.getMessage();
        if (message != Message.EMPTY)
            player.sendMessage(messages.getString(nextStep.getMessage(), values));
        else
            BigDoors.get().getMessagingInterface().broadcastMessage("EMPTY MESSAGE: STEP: " + nextStep.name());
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
        System.out.println("Setting open dir: " + openDirName);

        return RotateDirection.getRotateDirection(openDirName).map(
            foundOpenDir ->
            {
                if (DoorTypeBigDoor.get().isValidOpenDirection(foundOpenDir))
                {
                    opendir = foundOpenDir;
                    // TODO: Money, canBreakBlocks.
                    setProcedure(Procedure.COMPLETE, cuboid.getVolume().toString());
                    return true;
                }
                return false;
            }
        ).orElse(false);
    }

    private boolean complete()
    {
        BigDoors.get().getMessagingInterface().broadcastMessage("COMPLETED THE CREATOR!!");

        final long doorUID = -1;
        final @NotNull DoorOwner owner = new DoorOwner(doorUID, player.getUUID(), player.getName(), 0);
        final @NotNull AbstractDoorBase.DoorData doorData =
            new AbstractDoorBase.DoorData(doorUID, name, cuboid.getMin(), cuboid.getMax(), engine, powerblock, world,
                                          true, opendir, owner, false);
        BigDoor door = new BigDoor(doorData, -1, -1, PBlockFace.NONE);
        BigDoors.get().getMessagingInterface().broadcastMessage(door.toString());

        return true;
    }

    @Override
    protected Message getStepMessage(final @NotNull Step<BigDoorCreator> step)
    {
        return Procedure.getProcedure(step).map(Procedure::getMessage).orElse(Message.EMPTY);
    }

    @Override
    public List<Step<BigDoorCreator>> getProcedure()
    {
        return procedure;
    }

    // TODO: Rename this, it is not a procedure, but a definition of steps.
    // TODO: Use an interface.
    private enum Procedure
    {
        SET_NAME(new StepString<>(BigDoorCreator::setName), Message.CREATOR_BIGDOOR_INIT),
        SET_FIRST_POS(new StepPLocation<>(BigDoorCreator::setFirstPos), Message.CREATOR_BIGDOOR_STEP1),
        SET_SECOND_POS(new StepPLocation<>(BigDoorCreator::setSecondPos), Message.CREATOR_BIGDOOR_STEP2),
        SET_ENGINE_POS(new StepPLocation<>(BigDoorCreator::setEnginePos), Message.CREATOR_BIGDOOR_STEP3),
        SET_POWER_BLOCK_POS(new StepPLocation<>(BigDoorCreator::setPowerBlockPos),
                            Message.CREATOR_GENERAL_SETPOWERBLOCK),
        SET_OPEN_DIR(new StepString<>(BigDoorCreator::setOpenDir), Message.CREATOR_GENERAL_SETOPENDIR),
        COMPLETE(new StepConfirm<>(BigDoorCreator::complete), Message.CREATOR_GENERAL_CONFIRMPRICE);

        @Getter
        @NotNull
        final Step<BigDoorCreator> step;

        @Getter
        @NotNull
        final Message message;

        @Getter
        @NotNull
        private static final List<Procedure> values = Collections.unmodifiableList(Arrays.asList(Procedure.values()));

        @NotNull
        private static final Map<Step<BigDoorCreator>, Procedure> procedureMap;

        static
        {
            Map<Step<BigDoorCreator>, Procedure> procedureMapTmp = new HashMap<>(values.size());
            values.forEach(procedure -> procedureMapTmp.put(procedure.step, procedure));
            procedureMap = Collections.unmodifiableMap(procedureMapTmp);
        }

        Procedure(final @NotNull Step<BigDoorCreator> step, final @NotNull Message message)
        {
            this.step = step;
            this.message = message;
        }

        @NotNull
        Optional<Procedure> next()
        {
            final int nextIDX = ordinal() + 1;
            if (nextIDX >= values().length)
                return Optional.empty();
            return Optional.of(values.get(nextIDX));
        }

        static Optional<Procedure> getProcedure(final @NotNull Step<BigDoorCreator> step)
        {
            return Optional.ofNullable(procedureMap.get(step));
        }
    }
}
