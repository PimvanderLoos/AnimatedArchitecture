package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening direction of structures.
 *
 * @author Pim
 */
@ToString
public class SetOpenDirection extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_DIRECTION;

    private final MovementDirection movementDirection;

    @AssistedInject //
    SetOpenDirection(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever, @Assisted MovementDirection movementDirection)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.OPEN_DIRECTION);
        this.movementDirection = movementDirection;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final var desc = getRetrievedStructureDescription();
        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.set_open_direction.success"), TextType.SUCCESS,
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id())));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        if (!structure.getType().isValidOpenDirection(movementDirection))
        {
            getCommandSender().sendMessage(textFactory.newText().append(
                localizer.getMessage("commands.set_open_direction.error.invalid_rotation"), TextType.ERROR,
                arg -> arg.highlight(localizer.getMessage(movementDirection.getLocalizationKey())),
                arg -> arg.highlight(localizer.getStructureType(structure)),
                arg -> arg.highlight(structure.getBasicInfo())));
            return CompletableFuture.completedFuture(null);
        }

        structure.setOpenDir(movementDirection);
        return structure.syncData().thenAccept(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetOpenDirection} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open direction of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} for which the open
         *     direction will be modified.
         * @param movementDirection
         *     The new movement direction.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenDirection newSetOpenDirection(
            ICommandSender commandSender, StructureRetriever structureRetriever, MovementDirection movementDirection);
    }
}
