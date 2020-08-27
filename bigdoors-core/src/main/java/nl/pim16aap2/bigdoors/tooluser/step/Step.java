package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Step<T extends ToolUser> implements IStep
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final String name;
    @NotNull
    private final StepExecutor stepExecutor;

    //    @Getter(onMethod = @__({@Override}))
    @NotNull
    private final Message message;
    @NotNull
    private final List<Supplier<String>> messageVariablesRetrievers;

    private final boolean waitForUserInput;

    @Nullable
    private final Supplier<Boolean> skipCondition;


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

    public static class Factory<T extends ToolUser>
    {
        @NotNull
        private final String name;
        private StepExecutor stepExecutor = null;
        private List<Supplier<String>> messageVariablesRetrievers = null;
        private boolean waitForUserInput = true;
        private Message message = null;
        private Supplier<Boolean> skipCondition = null;

        public Factory(final @NotNull String name)
        {
            this.name = name;
        }

        public Factory<T> stepExecutor(final @NotNull StepExecutor stepExecutor)
        {
            this.stepExecutor = stepExecutor;
            return this;
        }

        public Factory<T> messageVariableRetrievers(
            final @NotNull List<Supplier<String>> messageVariablesRetrievers)
        {
            this.messageVariablesRetrievers = Collections.unmodifiableList(messageVariablesRetrievers);
            return this;
        }

        public Factory<T> skipCondition(final @NotNull Supplier<Boolean> skipCondition)
        {
            this.skipCondition = skipCondition;
            return this;
        }

        public Factory<T> waitForUserInput(final boolean waitForUserInput)
        {
            this.waitForUserInput = waitForUserInput;
            return this;
        }

        public Factory<T> message(final @NotNull Message message)
        {
            this.message = message;
            return this;
        }

        public Step<T> construct()
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

            return new Step<>(name, stepExecutor, message, messageVariablesRetrievers, waitForUserInput, skipCondition);
        }
    }
}
