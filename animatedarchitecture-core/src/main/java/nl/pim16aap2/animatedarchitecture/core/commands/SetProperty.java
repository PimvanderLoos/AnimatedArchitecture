package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CannotAddPropertyException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.PropertyCannotBeEditedByUserException;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
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
public class SetProperty extends StructureTargetCommand
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
    SetProperty(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted Property<?> property,
        @Assisted @Nullable Object newValue,
        DatabaseManager databaseManager,
        IExecutor executor)
    {
        super(commandSender, executor, structureRetriever, StructureAttribute.SET_PROPERTY);
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
            getCommandSender().sendError("commands.set_property.error.invalid_value_type");

            throw new InvalidCommandInputException(
                true,
                String.format("Value '%s' cannot be assigned to property '%s'.",
                    newValue,
                    property.getNamespacedKey()
                ));
        }

        if (property.getPropertyAccessLevel() != PropertyAccessLevel.USER_EDITABLE)
        {
            getCommandSender().sendError(
                "commands.set_property.error.property_not_editable",
                arg -> arg.highlight(property.getNamespacedKey().getKey())
            );
            throw new PropertyCannotBeEditedByUserException(true, property.getNamespacedKey().toString());
        }
    }

    private <T> void setProperty(Structure structure, Property<T> property, @Nullable Object object)
    {
        structure.setPropertyValue(property, property.cast(object));
    }

    @VisibleForTesting
    void performAction0(Structure structure)
    {
        if (!structure.hasProperty(property) && !property.canBeAdded())
        {
            getCommandSender().sendError(
                "commands.set_property.error.property_cannot_be_added",
                arg -> arg.highlight(property.getNamespacedKey().getKey()),
                arg -> arg.localizedHighlight(structure)
            );
            throw new CannotAddPropertyException(true, property.getNamespacedKey().toString());
        }

        try
        {
            setProperty(structure, property, newValue);
        }
        catch (Exception exception)
        {
            getCommandSender().sendError("commands.base.error.generic");

            throw new CommandExecutionException(
                true,
                String.format(
                    "Failed to set value '%s' for property '%s' for structure '%s'.",
                    newValue,
                    property.getNamespacedKey().getKey(),
                    structure.getBasicInfo()
                ),
                exception
            );
        }
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        return CompletableFuture
            .runAsync(() -> this.performAction0(structure), executor.getVirtualExecutor())
            .thenCompose(__ -> databaseManager.syncStructureData(structure.getSnapshot()))
            .thenAccept(result -> handleDatabaseActionResult(result, structure))
            .orTimeout(10, TimeUnit.SECONDS)
            .withExceptionContext(() -> String.format(
                "Set value '%s' for property '%s' for structure '%s'.",
                newValue,
                property.getNamespacedKey().getKey(),
                structure.getBasicInfo()
            ));
    }

    @Override
    public void handleDatabaseActionSuccess(Structure structure)
    {
        getCommandSender().sendSuccess(
            "commands.set_property.success",
            arg -> arg.highlight(property.getNamespacedKey().getKey())
        );
    }

    /**
     * Factory for creating {@link SetProperty} instances.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new {@link SetProperty} instance.
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
         * @return The created {@link SetProperty} instance.
         */
        SetProperty newSetProperty(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            Property<?> property,
            @Nullable Object newValue
        );
    }
}
