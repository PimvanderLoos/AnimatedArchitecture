package nl.pim16aap2.animatedarchitecture.core.tooluser;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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
public final class Step
{
    private final PersonalizedLocalizer localizer;

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
     * The name of the property to be used when reporting the result of this step to the user.
     * <p>
     * The value that will be reported is retrieved using the supplier set via {@link #propertyValueSupplier}.
     * <p>
     * Set to null to disable reporting this step.
     */
    private final @Nullable String propertyName;

    /**
     * The supplier for the value of the property that was modified by this step when reporting the result of this step
     * to the user.
     * <p>
     * The name of the value is retrieved using {@link #propertyName}.
     * <p>
     * Set to null to disable reporting this step.
     */
    private final @Nullable Supplier<?> propertyValueSupplier;

    /**
     * Sets whether this step can be updated after it has been set.
     * <p>
     * This is used for the review step, where the user can review the selected settings and revisit those that they
     * would like to change.
     * <p>
     * This setting has no effect when either {@link #propertyValueSupplier} or {@link #propertyName} is null.
     */
    private final boolean updatable;

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
    public StepExecutor getStepExecutor()
    {
        return stepExecutor;
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
    public Text getLocalizedMessage(IMessageable messageable)
    {
        return textSupplier.apply(messageable.newText());
    }

    /**
     * Creates the property information for this step using {@link #propertyName} and {@link #propertyValueSupplier}.
     * <p>
     * If either the property name or the associated value supplier is null, this method will return an empty optional.
     * <p>
     * If {@link #updatable} is true and the supplied value is not null, the text will contain clickable text to take
     * the user back to this step.
     *
     * @param text
     *     The text to append the property information to.
     */
    public void appendPropertyText(Text text)
    {
        if (propertyName == null || propertyValueSupplier == null)
            return;

        final @Nullable Object value = propertyValueSupplier.get();

        final Text.ArgumentCreator argument;
        if (updatable && value != null)
            argument = arg -> arg.clickable(
                value,
                "/AnimatedArchitecture UpdateCreator " + this.getName(),
                localizer.getMessage("creator.base.property.info.clickable_message"));
        else
            argument = arg -> arg.highlight(value);

        text.append(propertyName, TextType.INFO, argument).append('\n');
    }

    /**
     * Factory class for new {@link Step} objects.
     */
    public static final class Factory
    {
        private final PersonalizedLocalizer localizer;
        private final String name;
        private @Nullable StepExecutor stepExecutor = null;
        private @Nullable Runnable stepPreparation;
        private boolean waitForUserInput = true;
        private @Nullable Supplier<Boolean> skipCondition = null;
        private boolean implicitNextStep = true;
        private @Nullable Function<Text, Text> textSupplier;
        private @Nullable String propertyName;
        private @Nullable Supplier<?> propertyValueSupplier;
        private boolean updatable;

        @VisibleForTesting
        @AssistedInject
        public Factory(@Assisted PersonalizedLocalizer localizer, @Assisted String name)
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
         * Sets the name of the property to be used when reporting the result of this step to the user.
         * <p>
         * The value that will be reported is retrieved using the supplier set via
         * {@link #propertyValueSupplier(Supplier)}.
         * <p>
         * Set to null to disable reporting this step.
         *
         * @param name
         *     The localized name.
         * @return The current Factory.
         */
        public Factory propertyName(@Nullable String name)
        {
            this.propertyName = name;
            return this;
        }

        /**
         * Sets the supplier for the value of the property that was modified by this step when reporting the result of
         * this step to the user.
         * <p>
         * The name of the value is retrieved using {@link #propertyName(String)}.
         * <p>
         * Set to null to disable reporting this step.
         *
         * @param propertyValueSupplier
         *     The supplier for the field.
         * @return The current Factory.
         */
        public Factory propertyValueSupplier(@Nullable Supplier<?> propertyValueSupplier)
        {
            this.propertyValueSupplier = propertyValueSupplier;
            return this;
        }

        /**
         * Sets whether this step can be updated after it has been set.
         * <p>
         * This is used for the review step, where the user can review the selected settings and revisit those that they
         * would like to change.
         * <p>
         * This setting has no effect when either {@link #propertyValueSupplier(Supplier)} or
         * {@link #propertyName(String)} is null.
         *
         * @param updatable
         *     True if this step can be updated.
         * @return The current Factory.
         */
        public Factory updatable(boolean updatable)
        {
            this.updatable = updatable;
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
                localizer,
                name,
                stepExecutor,
                stepPreparation,
                textSupplier,
                waitForUserInput,
                skipCondition,
                implicitNextStep,
                propertyName,
                propertyValueSupplier,
                updatable);
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
             * @param localizer
             *     The {@link PersonalizedLocalizer} that will be used to create messages.
             * @param stepName
             *     The name of the step to be created.
             * @return The new factory.
             */
            Step.Factory stepName(PersonalizedLocalizer localizer, String stepName);
        }
    }
}
