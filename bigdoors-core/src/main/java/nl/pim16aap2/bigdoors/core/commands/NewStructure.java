package nl.pim16aap2.bigdoors.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.structures.StructureType;
import nl.pim16aap2.bigdoors.core.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.core.util.Constants;
import org.jetbrains.annotations.Nullable;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to create new structures.
 *
 * @author Pim
 */
@ToString
public class NewStructure extends BaseCommand
{
    private final StructureType structureType;
    private final @Nullable String structureName;
    private final ToolUserManager toolUserManager;
    private final Provider<Creator.Context> creatorContextProvider;

    @AssistedInject //
    NewStructure(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureType structureType, @Assisted @Nullable String structureName,
        ToolUserManager toolUserManager,
        Provider<Creator.Context> creatorContextProvider)
    {
        super(commandSender, localizer, textFactory);
        this.structureType = structureType;
        this.structureName = structureName;
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
        return CommandDefinition.NEW_STRUCTURE;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        toolUserManager.startToolUser(structureType.getCreator(creatorContextProvider.get(),
                                                               (IPlayer) getCommandSender(), structureName),
                                      Constants.STRUCTURE_CREATOR_TIME_LIMIT);
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link NewStructure} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for creating a new structure.
         * @param structureType
         *     The type of structure that will be created.
         * @param structureName
         *     The name of the structure, if it has been specified already.
         *     <p>
         *     When this is null, the creator will start at the first step (specifying the name). If it has been
         *     specified, this step will be skipped.
         * @return See {@link BaseCommand#run()}.
         */
        NewStructure newNewStructure(
            ICommandSender commandSender, StructureType structureType, @Nullable String structureName);

        /**
         * See {@link #newNewStructure(ICommandSender, StructureType, String)}.
         */
        default NewStructure newNewStructure(ICommandSender commandSender, StructureType structureType)
        {
            return newNewStructure(commandSender, structureType, null);
        }
    }
}
