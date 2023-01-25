package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command to list a number of movables matching a single {@link MovableRetrieverFactory}. This is
 * basically only useful for String-based look-ups (as there aren't duplicate matches otherwise), but I don't judge.
 *
 * @author Pim
 */
@ToString
public class ListMovables extends BaseCommand
{
    private final MovableRetriever movableRetriever;

    @AssistedInject //
    ListMovables(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever)
    {
        super(commandSender, localizer, textFactory);
        this.movableRetriever = movableRetriever;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LIST_MOVABLES;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        final CompletableFuture<List<AbstractMovable>> movables;
        if (permissions.hasAdminPermission() || !getCommandSender().isPlayer())
            movables = movableRetriever.getMovables();
        else
            movables = movableRetriever.getMovables((IPPlayer) getCommandSender());

        return movables.thenAccept(this::sendMovableList);
    }

    private void sendMovableList(List<AbstractMovable> movables)
    {
        if (movables.isEmpty())
        {
            getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                           localizer.getMessage("commands.list_movables.error.no_movables_found"));
            return;
        }

        final StringBuilder sb = new StringBuilder(
            localizer.getMessage("commands.list_movables.movable_list_header")).append('\n');
        for (final var movable : movables)
            sb.append("  ").append(movable.getBasicInfo()).append('\n');
        getCommandSender().sendMessage(textFactory, TextType.INFO, sb.toString());
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link ListMovables} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for retrieving the information for the movables.
         *     <p>
         *     This is also the entity that will be informed about the movables that were found.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing any number of {@link AbstractMovable}s.
         * @return See {@link BaseCommand#run()}.
         */
        ListMovables newListMovables(ICommandSender commandSender, MovableRetriever movableRetriever);
    }
}
