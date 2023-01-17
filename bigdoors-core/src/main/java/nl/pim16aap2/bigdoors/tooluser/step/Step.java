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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

// TODO: Consider adding another method for PrepareStep or something. For example, the setFirstPos would prepare by
//       giving the player the creator stick, and CONFIRM_PRICE would prepare by skipping itself if the movable is free.
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Step implements IStep
{
    @ToString.Exclude
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
        return localizer.getMessage(messageKey, messageVariablesRetrievers.stream().map(Supplier::get).toArray());
    }

    /**
     * Factory class for new {@link Step} objects.
     */
    public static class Factory
    {
        private final ILocalizer localizer;
        private final String name;
        private @Nullable StepExecutor stepExecutor = null;
        private @Nullable List<Supplier<String>> messageVariablesRetrievers = null;
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

        /**
         * See {@link IStep#isImplicitNextStep()}.
         */
        public Factory implicitNextStep(boolean implicitNextStep)
        {
            this.implicitNextStep = implicitNextStep;
            return this;
        }

        /**
         * See {@link IStep#getStepPreparation()}.
         */
        public Factory stepPreparation(Runnable prepareStep)
        {
            this.stepPreparation = prepareStep;
            return this;
        }

        /**
         * See {@link IStep#getStepExecutor()}.
         */
        public Factory stepExecutor(StepExecutor stepExecutor)
        {
            this.stepExecutor = stepExecutor;
            return this;
        }

        /**
         * Sets the key of the localized message for this step.
         */
        public Factory messageKey(String messageKey)
        {
            this.messageKey = messageKey;
            return this;
        }

        /**
         * Provides the variables for the placeholder(s) in the localized messages for this step.
         */
        @SafeVarargs
        public final Factory messageVariableRetrievers(Supplier<String>... messageVariablesRetriever)
        {
            messageVariablesRetrievers = List.of(messageVariablesRetriever);
            return this;
        }

        /**
         * Sets the implementation of {@link IStep#skip()}.
         * <p>
         * This can be used to disable certain steps based on arbitrary conditions (e.g. configuration settings).
         */
        public Factory skipCondition(Supplier<Boolean> skipCondition)
        {
            this.skipCondition = skipCondition;
            return this;
        }

        /**
         * See {@link IStep#waitForUserInput()}.
         */
        public Factory waitForUserInput(boolean waitForUserInput)
        {
            this.waitForUserInput = waitForUserInput;
            return this;
        }

        /**
         * Creates the new {@link Step} object using the provided values.
         *
         * @return The new Step object.
         *
         * @throws InstantiationException
         *     When this method is called but neither the step executor nor the message key is set.
         */
        public Step construct()
            throws InstantiationException
        {
            if (stepExecutor == null)
                throw new InstantiationException("Trying to instantiate a Step without stepExecutor");
            if (messageKey == null)
                throw new InstantiationException("Trying to instantiate a Step without message");

            if (messageVariablesRetrievers == null)
                messageVariablesRetrievers = Collections.emptyList();

            return new Step(localizer, name, stepExecutor, messageKey, stepPreparation, messageVariablesRetrievers,
                            waitForUserInput, skipCondition, implicitNextStep);
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
