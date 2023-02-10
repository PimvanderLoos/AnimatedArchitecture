package nl.pim16aap2.bigdoors.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.text.TextType;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command to list a number of structures matching a single {@link StructureRetrieverFactory}. This is
 * basically only useful for String-based look-ups (as there aren't duplicate matches otherwise), but I don't judge.
 *
 * @author Pim
 */
@ToString
public class ListStructures extends BaseCommand
{
    private final StructureRetriever structureRetriever;

    @AssistedInject //
    ListStructures(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever)
    {
        super(commandSender, localizer, textFactory);
        this.structureRetriever = structureRetriever;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LIST_STRUCTURES;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        final CompletableFuture<List<AbstractStructure>> structures;
        if (permissions.hasAdminPermission() || !getCommandSender().isPlayer())
            structures = structureRetriever.getStructures();
        else
            structures = structureRetriever.getStructures((IPlayer) getCommandSender());

        return structures.thenAccept(this::sendStructureList);
    }

    private void sendStructureList(List<AbstractStructure> structures)
    {
        if (structures.isEmpty())
        {
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.list_structures.error.no_structures_found"));
            return;
        }

        final StringBuilder sb = new StringBuilder(
            localizer.getMessage("commands.list_structures.structure_list_header")).append('\n');
        for (final var structure : structures)
            sb.append("  ").append(structure.getBasicInfo()).append('\n');
        getCommandSender().sendMessage(textFactory, TextType.INFO, sb.toString());
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link ListStructures} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for retrieving the information for the structures.
         *     <p>
         *     This is also the entity that will be informed about the structures that were found.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing any number of {@link AbstractStructure}s.
         * @return See {@link BaseCommand#run()}.
         */
        ListStructures newListStructures(ICommandSender commandSender, StructureRetriever structureRetriever);
    }
}
