package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.Nullable;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to create new movables.
 *
 * @author Pim
 */
@ToString
public class NewMovable extends BaseCommand
{
    private final MovableType movableType;
    private final @Nullable String movableName;
    private final ToolUserManager toolUserManager;
    private final Provider<Creator.Context> creatorContextProvider;

    @AssistedInject //
    NewMovable(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableType movableType, @Assisted @Nullable String movableName, ToolUserManager toolUserManager,
        Provider<Creator.Context> creatorContextProvider)
    {
        super(commandSender, localizer, textFactory);
        this.movableType = movableType;
        this.movableName = movableName;
        this.toolUserManager = toolUserManager;
        this.creatorContextProvider = creatorContextProvider;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.NEW_MOVABLE;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        toolUserManager.startToolUser(movableType.getCreator(creatorContextProvider.get(),
                                                             (IPPlayer) getCommandSender(), movableName),
                                      Constants.MOVABLE_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link NewMovable} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for creating a new movable.
         * @param movableType
         *     The type of movable that will be created.
         * @param movableName
         *     The name of the movable, if it has been specified already.
         *     <p>
         *     When this is null, the creator will start at the first step (specifying the name). If it has been
         *     specified, this step will be skipped.
         * @return See {@link BaseCommand#run()}.
         */
        NewMovable newNewMovable(ICommandSender commandSender, MovableType movableType, @Nullable String movableName);

        /**
         * See {@link #newNewMovable(ICommandSender, MovableType, String)}.
         */
        default NewMovable newNewMovable(ICommandSender commandSender, MovableType movableType)
        {
            return newNewMovable(commandSender, movableType, null);
        }
    }
}
