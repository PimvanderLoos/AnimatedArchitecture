package nl.pim16aap2.animatedarchitecture.core.tooluser;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Optional;
import java.util.function.Function;
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
    /**
     * The name of this step.
     */
    @Getter
    private final String name;

    private final StepExecutor stepExecutor;

    /**
     * The action to be taken to prepare this step, if any.
     */
    @ToString.Exclude
    @Getter
    private final @Nullable Runnable stepPreparation;

    @ToString.Exclude
    private final Function<Text, Text> textSupplier;

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
    public Text getLocalizedMessage(ITextFactory textFactory)
    {
        return textSupplier.apply(textFactory.newText());
    }

    /**
     * Factory class for new {@link Step} objects.
     */
    public static class Factory
    {
        private final ILocalizer localizer;
        private final String name;
        private @Nullable StepExecutor stepExecutor = null;
        private @Nullable Runnable stepPreparation;
        private boolean waitForUserInput = true;
        private @Nullable Supplier<Boolean> skipCondition = null;
        private boolean implicitNextStep = true;
        private @Nullable Function<Text, Text> textSupplier;

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
         *
         * @return The current Factory.
         */
        public Factory implicitNextStep(boolean implicitNextStep)
        {
            this.implicitNextStep = implicitNextStep;
            return this;
        }

        /**
         * See {@link Step#getStepPreparation()}.
         *
         * @return The current Factory.
         */
        public Factory stepPreparation(Runnable prepareStep)
        {
            this.stepPreparation = prepareStep;
            return this;
        }

        /**
         * See {@link Step#getStepExecutor()}.
         *
         * @return The current Factory.
         */
        public Factory stepExecutor(StepExecutor stepExecutor)
        {
            this.stepExecutor = stepExecutor;
            return this;
        }

        /**
         * Sets the key of the localized message for this step.
         * <p>
         * This is a shortcut for setting a text supplier with a string localized via the localizer.
         *
         * @return The current Factory.
         */
        public Factory messageKey(String messageKey)
        {
            this.textSupplier = text -> text.append(localizer.getMessage(messageKey), TextType.INFO);
            return this;
        }

        /**
         * Sets the supplier for the Text object that is used to provide instructions for this step.
         * <p>
         * The input of the function is always a new and empty {@link Text} object.
         *
         * @param textSupplier
         *     The function that supplies an instruction Text.
         * @return The current Factory.
         */
        public Factory textSupplier(Function<Text, Text> textSupplier)
        {
            this.textSupplier = textSupplier;
            return this;
        }

        /**
         * Sets the implementation of {@link Step#skip()}.
         * <p>
         * This can be used to disable certain steps based on arbitrary conditions (e.g. configuration settings).
         *
         * @return The current Factory.
         */
        public Factory skipCondition(Supplier<Boolean> skipCondition)
        {
            this.skipCondition = skipCondition;
            return this;
        }

        /**
         * See {@link Step#waitForUserInput()}.
         *
         * @return The current Factory.
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
            if (textSupplier == null)
                throw new InstantiationException("Trying to instantiate a Step without text supplier");

            return new Step(
                name,
                stepExecutor,
                stepPreparation,
                textSupplier,
                waitForUserInput,
                skipCondition,
                implicitNextStep);
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
