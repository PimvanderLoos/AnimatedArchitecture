package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// TODO: Consider adding another method for PrepareStep or something. For example, the setFirstPos would prepare by
//       giving the player the creator stick, and CONFIRM_PRICE would prepare by skipping itself if the door is free.
// TODO: Look into https://projectlombok.org/features/Builder
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Step implements IStep
{
    @Getter
    private final @NotNull String name;

    private final @NotNull StepExecutor stepExecutor;

    @ToString.Exclude
    private final @NotNull Message message;

    @ToString.Exclude
    private final @NotNull List<Supplier<String>> messageVariablesRetrievers;

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
    public @NotNull Optional<StepExecutor> getStepExecutor()
    {
        return Optional.of(stepExecutor);
    }

    @Override
    public boolean skip()
    {
        return skipCondition != null && skipCondition.get();
    }

    @Override
    public @NotNull String getLocalizedMessage()
    {
        final @NotNull List<String> variables = new ArrayList<>(messageVariablesRetrievers.size());
        messageVariablesRetrievers.forEach(fun -> variables.add(fun.get()));

        String[] variablesArr = new String[variables.size()];
        variablesArr = variables.toArray(variablesArr);

        return BigDoors.get().getPlatform().getMessages().getString(message, variablesArr);
    }

    public static class Factory
    {
        private final @NotNull String name;
        private StepExecutor stepExecutor = null;
        private List<Supplier<String>> messageVariablesRetrievers = null;
        private boolean waitForUserInput = true;
        private Message message = null;
        private Supplier<Boolean> skipCondition = null;
        private boolean implicitNextStep = true;

        public Factory(final @NotNull String name)
        {
            this.name = name;
        }

        public @NotNull Factory implicitNextStep(final boolean implicitNextStep)
        {
            this.implicitNextStep = implicitNextStep;
            return this;
        }

        public @NotNull Factory stepExecutor(final @NotNull StepExecutor stepExecutor)
        {
            this.stepExecutor = stepExecutor;
            return this;
        }

        public @NotNull Factory messageVariableRetrievers(
            final @NotNull List<Supplier<String>> messageVariablesRetrievers)
        {
            this.messageVariablesRetrievers = Collections.unmodifiableList(messageVariablesRetrievers);
            return this;
        }

        public @NotNull Factory skipCondition(final @NotNull Supplier<Boolean> skipCondition)
        {
            this.skipCondition = skipCondition;
            return this;
        }

        public @NotNull Factory waitForUserInput(final boolean waitForUserInput)
        {
            this.waitForUserInput = waitForUserInput;
            return this;
        }

        public @NotNull Factory message(final @NotNull Message message)
        {
            this.message = message;
            return this;
        }

        public @NotNull Step construct()
            throws InstantiationException
        {
            if (stepExecutor == null)
                throw new InstantiationException("Trying to instantiate a Step without stepExecutor");
            if (message == null)
                throw new InstantiationException("Trying to instantiate a Step without message");

            if (messageVariablesRetrievers == null)
                messageVariablesRetrievers = Collections.emptyList();

            if (messageVariablesRetrievers.size() != Message.getVariableCount(message))
                throw new InstantiationException("Parameter mismatch for " + name + ". Expected: " +
                                                     Message.getVariableCount(message) + " but received: " +
                                                     messageVariablesRetrievers.size());

            return new Step(name, stepExecutor, message, messageVariablesRetrievers,
                            waitForUserInput, skipCondition, implicitNextStep);
        }
    }
}
