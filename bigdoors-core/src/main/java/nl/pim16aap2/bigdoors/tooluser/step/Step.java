package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// TODO: Consider adding another method for PrepareStep or something. For example, the setFirstPos would prepare by
//       giving the player the creator stick, and CONFIRM_PRICE would prepare by skipping itself if the door is free.
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Step implements IStep
{
    @Getter
    private final String name;

    private final StepExecutor stepExecutor;

    @ToString.Exclude
    private final String messageKey;

    @ToString.Exclude
    private final List<Supplier<String>> messageVariablesRetrievers;

    private final boolean waitForUserInput;

    @ToString.Exclude
    private final @Nullable Supplier<Boolean> skipCondition;

    @Getter
    private final boolean implicitNextStep;

    @Override
    public boolean waitForUserInput()
    {
        return waitForUserInput;
    }

    @Override
    public Optional<StepExecutor> getStepExecutor()
    {
        return Optional.of(stepExecutor);
    }

    @Override
    public boolean skip()
    {
        return skipCondition != null && skipCondition.get();
    }

    @Override
    public String getLocalizedMessage()
    {
        final List<String> variables = new ArrayList<>(messageVariablesRetrievers.size());
        messageVariablesRetrievers.forEach(fun -> variables.add(fun.get()));

        Object[] variablesArr = new String[variables.size()];
        variablesArr = variables.toArray(variablesArr);

        return BigDoors.get().getLocalizer().getMessage(messageKey, variablesArr);
    }

    public static class Factory
    {
        private final String name;
        private @Nullable StepExecutor stepExecutor = null;
        private @Nullable List<Supplier<String>> messageVariablesRetrievers = null;
        private boolean waitForUserInput = true;
        private @Nullable String messageKey = null;
        private @Nullable Supplier<Boolean> skipCondition = null;
        private boolean implicitNextStep = true;

        public Factory(String name)
        {
            this.name = name;
        }

        public Factory implicitNextStep(boolean implicitNextStep)
        {
            this.implicitNextStep = implicitNextStep;
            return this;
        }

        public Factory stepExecutor(StepExecutor stepExecutor)
        {
            this.stepExecutor = stepExecutor;
            return this;
        }

        public Factory messageVariableRetriever(Supplier<String> messageVariablesRetriever)
        {
            messageVariablesRetrievers = List.of(messageVariablesRetriever);
            return this;
        }

        public Factory messageVariableRetrievers(List<Supplier<String>> messageVariablesRetrievers)
        {
            this.messageVariablesRetrievers = Collections.unmodifiableList(messageVariablesRetrievers);
            return this;
        }

        public Factory skipCondition(Supplier<Boolean> skipCondition)
        {
            this.skipCondition = skipCondition;
            return this;
        }

        public Factory waitForUserInput(boolean waitForUserInput)
        {
            this.waitForUserInput = waitForUserInput;
            return this;
        }

        public Factory messageKey(String messageKey)
        {
            this.messageKey = messageKey;
            return this;
        }

        public Step construct()
            throws InstantiationException
        {
            if (stepExecutor == null)
                throw new InstantiationException("Trying to instantiate a Step without stepExecutor");
            if (messageKey == null)
                throw new InstantiationException("Trying to instantiate a Step without message");

            if (messageVariablesRetrievers == null)
                messageVariablesRetrievers = Collections.emptyList();

            return new Step(name, stepExecutor, messageKey, messageVariablesRetrievers,
                            waitForUserInput, skipCondition, implicitNextStep);
        }
    }
}
