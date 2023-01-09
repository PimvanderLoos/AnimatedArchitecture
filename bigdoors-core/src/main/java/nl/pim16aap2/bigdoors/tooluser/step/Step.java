package nl.pim16aap2.bigdoors.tooluser.step;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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
    private final ILocalizer localizer;

    @Getter
    private final String name;

    private final StepExecutor stepExecutor;

    @ToString.Exclude
    private final String messageKey;

    @ToString.Exclude
    @Getter
    private final @Nullable Runnable stepPreparation;

    @ToString.Exclude
    private final List<Supplier<String>> messageVariablesRetrievers;

    @ToString.Exclude
    private final Supplier<List<String>> flatMessageVariablesRetrievers;

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
        variables.addAll(flatMessageVariablesRetrievers.get());

        Object[] variablesArr = new String[variables.size()];
        variablesArr = variables.toArray(variablesArr);

        return localizer.getMessage(messageKey, variablesArr);
    }

    public static class Factory
    {
        private final ILocalizer localizer;
        private final String name;
        private @Nullable StepExecutor stepExecutor = null;
        private @Nullable List<Supplier<String>> messageVariablesRetrievers = null;
        private @Nullable Supplier<List<String>> flatMessageVariablesRetrievers = null;
        private @Nullable Runnable stepPreparation;
        private boolean waitForUserInput = true;
        private @Nullable String messageKey = null;
        private @Nullable Supplier<Boolean> skipCondition = null;
        private boolean implicitNextStep = true;

        /**
         * @deprecated Prefer instantiation using {@link Step.Factory.IFactory} instead.
         */
        @VisibleForTesting
        @AssistedInject
        @Deprecated
        public Factory(ILocalizer localizer, @Assisted String name)
        {
            this.localizer = localizer;
            this.name = name;
        }

        public Factory implicitNextStep(boolean implicitNextStep)
        {
            this.implicitNextStep = implicitNextStep;
            return this;
        }

        public Factory stepPreparation(Runnable prepareStep)
        {
            this.stepPreparation = prepareStep;
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

        public Factory messageVariableRetrievers(Supplier<List<String>> messageVariablesRetrievers)
        {
            flatMessageVariablesRetrievers = messageVariablesRetrievers;
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
            if (flatMessageVariablesRetrievers == null)
                flatMessageVariablesRetrievers = Collections::emptyList;

            return new Step(localizer, name, stepExecutor, messageKey, stepPreparation, messageVariablesRetrievers,
                            flatMessageVariablesRetrievers, waitForUserInput, skipCondition, implicitNextStep);
        }

        /**
         * Nested factory used to create new {@link Step.Factory} instances.
         * <p>
         * It is preferred to use this over the direct constructor as this ensures that all required dependencies are
         * included.
         */
        @AssistedFactory
        public interface IFactory
        {
            /**
             * Creates a new {@link Step.Factory}.
             *
             * @param stepName
             *     The name of the step to be created.
             * @return The new factory.
             */
            Step.Factory stepName(String stepName);
        }
    }
}
