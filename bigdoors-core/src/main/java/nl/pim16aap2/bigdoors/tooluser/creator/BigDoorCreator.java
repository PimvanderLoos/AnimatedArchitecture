package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.step.StepString;
import nl.pim16aap2.bigdoors.tooluser.step.StepVector3Di;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
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

    private static final List<Step<BigDoorCreator>> procedure =
        Collections.unmodifiableList(Arrays.stream(Procedure.values())
                                           .map(Procedure::getStep)
                                           .collect(Collectors.toList()));

    private void setProcedure(final @NotNull Procedure nextStep, final String... values)
    {
        stepIDX = nextStep.ordinal();
        player.sendMessage(messages.getString(nextStep.getMessage(), values));
    }

    private boolean setName(final @NotNull String str)
    {
        name = str;
        setProcedure(Procedure.SET_FIRST_POS);
        return true;
    }

    private boolean setFirstPos(final @NotNull IVector3DiConst pos)
    {
        setProcedure(Procedure.SET_SECOND_POS);
        return true;
    }

    private boolean setSecondPos(final @NotNull IVector3DiConst pos)
    {
        setProcedure(Procedure.SET_ENGINE_POS);
        return true;
    }

    private boolean setEnginePos(final @NotNull IVector3DiConst pos)
    {
        setProcedure(Procedure.SET_POWER_BLOCK_POS);
        return true;
    }

    private boolean setPowerBlockPos(final @NotNull IVector3DiConst pos)
    {
        setProcedure(Procedure.SET_OPEN_DIR);
        return true;
    }

    private boolean setOpenDir(final @NotNull String str)
    {
        BigDoors.get().getMessagingInterface().broadcastMessage("COMPLETED THE CREATOR!!");
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
        SET_FIRST_POS(new StepVector3Di<>(BigDoorCreator::setFirstPos), Message.CREATOR_BIGDOOR_STEP1),
        SET_SECOND_POS(new StepVector3Di<>(BigDoorCreator::setSecondPos), Message.CREATOR_BIGDOOR_STEP2),
        SET_ENGINE_POS(new StepVector3Di<>(BigDoorCreator::setEnginePos), Message.CREATOR_BIGDOOR_STEP3),
        SET_POWER_BLOCK_POS(new StepVector3Di<>(BigDoorCreator::setPowerBlockPos), Message.EMPTY),
        SET_OPEN_DIR(new StepString<>(BigDoorCreator::setOpenDir), Message.EMPTY),
        ;

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
