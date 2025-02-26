package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.PropertyCannotBeEditedByUserException;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents the command that sets property values for structures.
 */
@ToString
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public class SetPropertyCommand extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_PROPERTY;

    /**
     * The property whose value to set.
     */
    private final Property<?> property;

    /**
     * The new value to set the property to.
     * <p>
     * The type of this object should match the type expected by the property (or be null).
     */
    private final @Nullable Object newValue;

    private final DatabaseManager databaseManager;

    @AssistedInject
    SetPropertyCommand(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted Property<?> property,
        @Assisted @Nullable Object newValue,
        DatabaseManager databaseManager,
        IExecutor executor,
        ILocalizer localizer,
        ITextFactory textFactory)
    {
        super(commandSender, executor, localizer, textFactory, structureRetriever, StructureAttribute.SET_PROPERTY);
        this.property = property;
        this.newValue = newValue;
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void validateInput()
    {
        if (newValue != null && !property.getType().isAssignableFrom(newValue.getClass()))
        {
            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.set_property.error.invalid_value_type"),
                TextType.ERROR));
            throw new InvalidCommandInputException(
                true,
                String.format("Value '%s' cannot be assigned to property '%s'.",
                    newValue,
                    property.getNamespacedKey().getKey()
                ));
        }

        if (property.getPropertyAccessLevel() != PropertyAccessLevel.USER_EDITABLE)
        {
            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.set_property.error.property_not_editable"),
                TextType.ERROR,
                arg -> arg.highlight(property.getNamespacedKey().getKey())
            ));
            throw new PropertyCannotBeEditedByUserException(true, property.getNamespacedKey().toString());
        }
    }

    private <T> void setProperty(Structure structure, Property<T> property, @Nullable Object object)
    {
        structure.setPropertyValue(property, property.cast(object));
    }

    @VisibleForTesting
    boolean performAction0(Structure structure)
    {
        if (!structure.hasProperty(property) && !property.canBeAdded())
        {
            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.set_property.error.property_cannot_be_added"),
                TextType.ERROR,
                arg -> arg.highlight(property.getNamespacedKey().getKey()),
                arg -> arg.highlight(localizer.getStructureType(structure))
            ));
            return false;
        }

        try
        {
            setProperty(structure, property, newValue);
        }
        catch (Exception e)
        {
            log.atWarning().withCause(e).log(
                "Failed to set value '%s' for property '%s' for structure '%s'.",
                newValue,
                property.getNamespacedKey().getKey(),
                structure.getBasicInfo()
            );

            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.base.error.generic"),
                TextType.ERROR
            ));
            return false;
        }
        return true;
    }

    CompletableFuture<?> handleActionResult(Structure structure, boolean success)
    {
        if (!success)
            return CompletableFuture.completedFuture(null);

        return databaseManager
            .syncStructureData(structure.getSnapshot())
            .thenAccept(result -> handleDatabaseActionResult(result, structure));
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        return CompletableFuture
            .supplyAsync(() -> this.performAction0(structure), executor.getVirtualExecutor())
            .thenCompose(success -> this.handleActionResult(structure, success))
            .orTimeout(10, TimeUnit.SECONDS)
            .withExceptionContext(() -> String.format(
                "Failed to set value '%s' for property '%s' for structure '%s'.",
                newValue,
                property.getNamespacedKey().getKey(),
                structure.getBasicInfo()
            ));
    }

    @Override
    public void handleDatabaseActionSuccess(Structure structure)
    {
        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.set_property.success"),
            TextType.SUCCESS,
            arg -> arg.highlight(property.getNamespacedKey().getKey())
        ));
    }

    /**
     * Factory for creating {@link SetPropertyCommand} instances.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new {@link SetPropertyCommand} instance.
         *
         * @param commandSender
         *     The {@link ICommandSender} that executed the command.
         * @param structureRetriever
         *     The {@link StructureRetriever} that retrieved the structure.
         * @param property
         *     The property whose value to set.
         * @param newValue
         *     The new value to set the property to. This should match the type expected by the property.
         *     <p>
         *     See {@link Property#getType()} for the expected type. This may be null.
         * @return The created {@link SetPropertyCommand} instance.
         */
        SetPropertyCommand newSetPropertyCommand(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            Property<?> property,
            @Nullable Object newValue
        );
    }
}
