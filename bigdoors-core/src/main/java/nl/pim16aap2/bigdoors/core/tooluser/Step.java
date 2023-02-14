package nl.pim16aap2.bigdoors.core.tooluser;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Represents a step in a {@link Procedure}.
 * <p>
 * The step can be configured with messages to send to the user as preparation as well as {@link StepExecutor}s to
 * process the user input.
 */
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Step
{
    @ToString.Exclude
    private final ILocalizer localizer;

    /**
     * The name of this step.
     */
    @Getter
    private final String name;

    private final StepExecutor stepExecutor;

    @ToString.Exclude
    private final String messageKey;

    /**
     * The action to be taken to prepare this step, if any.
     */
    @ToString.Exclude
    @Getter
    private final @Nullable Runnable stepPreparation;

    @ToString.Exclude
    private final List<Supplier<String>> messageVariablesRetrievers;

    private final boolean waitForUserInput;

    @ToString.Exclude
    private final @Nullable Supplier<Boolean> skipCondition;

    /**
     * Checks if this step should 'automatically' proceed to the next step if the result of running the
     * {@link #getStepExecutor()} is true.
     * <p>
     * See {@link StepExecutor#apply(Object)}.
     *
     * @return True if the successful execution of this step's executor should cause it to go to the next step
     * automatically.
     */
    @Getter
    private final boolean implicitNextStep;

    /**
     * Checks if this type of Step waits for user input or not.
     * <p>
     * Most steps will usually wait for user input (e.g. providing a name, or location, etc.). However, some steps can
     * be executed immediately. One such example is a finishing step, that runs after all other steps have been
     * completed and processes the final result of all the steps that have been executed.
     *
     * @return True if this type of step waits for user input.
     */
    public boolean waitForUserInput()
    {
        return waitForUserInput;
    }

    /**
     * @return The {@link StepExecutor} for the current step.
     */
    public Optional<StepExecutor> getStepExecutor()
    {
        return Optional.of(stepExecutor);
    }

    /**
     * Checks if this step should be skipped based on certain criteria defined by the implementation.
     *
     * @return True if this step should be skipped.
     */
    public boolean skip()
    {
        return skipCondition != null && skipCondition.get();
    }

    /**
     * @return The localized {@link String} that belongs to the current step.
     */
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
         * See {@link Step#isImplicitNextStep()}.
         */
        public Factory implicitNextStep(boolean implicitNextStep)
        {
            this.implicitNextStep = implicitNextStep;
            return this;
        }

        /**
         * See {@link Step#getStepPreparation()}.
         */
        public Factory stepPreparation(Runnable prepareStep)
        {
            this.stepPreparation = prepareStep;
            return this;
        }

        /**
         * See {@link Step#getStepExecutor()}.
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
         * Sets the implementation of {@link Step#skip()}.
         * <p>
         * This can be used to disable certain steps based on arbitrary conditions (e.g. configuration settings).
         */
        public Factory skipCondition(Supplier<Boolean> skipCondition)
        {
            this.skipCondition = skipCondition;
            return this;
        }

        /**
         * See {@link Step#waitForUserInput()}.
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
