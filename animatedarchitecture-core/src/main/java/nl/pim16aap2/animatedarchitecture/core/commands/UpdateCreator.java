package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a command that is used to update active {@link Creator} processes with arbitrary data in out-of-procedure
 * order.
 * <p>
 * The exact effects of this command depend on the specific state of the creator process and its defined settings. As
 * such, it is not recommended to give users free access to this command.
 * <p>
 * See {@link Creator#update(String, Object)}.
 */
public final class UpdateCreator extends BaseCommand
{
    private final String stepName;
    private final @Nullable Object stepValue;
    private final ToolUserManager toolUserManager;

    @AssistedInject UpdateCreator(
        @Assisted ICommandSender commandSender,
        @Assisted String stepName,
        @Assisted @Nullable Object stepValue,
        ToolUserManager toolUserManager,
        ILocalizer localizer,
        ITextFactory textFactory)
    {
        super(commandSender, localizer, textFactory);
        this.stepName = stepName;
        this.stepValue = stepValue;
        this.toolUserManager = toolUserManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.UPDATE_CREATOR;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        final @Nullable var toolUser =
            toolUserManager.getToolUser(((IPlayer) getCommandSender()).getUUID()).orElse(null);
        if (!(toolUser instanceof Creator creator))
        {
            getCommandSender().sendError(
                textFactory, localizer.getMessage("commands.update_creator.error.no_creator_process"));
            return CompletableFuture.completedFuture(null);
        }

        creator.update(stepName, stepValue);
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    public interface IFactory
    {
        /**
         * Creates (but does not execute) a new UpdateCreator to update an existing {@link Creator} process.
         *
         * @param commandSender
         *     The command sender whose creator process to update, if they have any active processes.
         * @param stepName
         *     The name of the step in the creator process to update.
         * @param stepValue
         *     The optional value to provide to the step.
         * @return The new command.
         */
        UpdateCreator newUpdateCreator(ICommandSender commandSender, String stepName, @Nullable Object stepValue);
    }
}
