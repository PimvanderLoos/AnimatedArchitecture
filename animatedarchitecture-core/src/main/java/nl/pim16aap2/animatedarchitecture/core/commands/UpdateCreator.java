package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
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
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public final class UpdateCreator extends BaseCommand
{
    private final String stepName;

    private final @Nullable Object stepValue;

    @ToString.Exclude
    private final ToolUserManager toolUserManager;

    @AssistedInject
    UpdateCreator(
        @Assisted ICommandSender commandSender,
        @Assisted String stepName,
        @Assisted @Nullable Object stepValue,
        IExecutor executor,
        ToolUserManager toolUserManager,
        ILocalizer localizer,
        ITextFactory textFactory)
    {
        super(commandSender, executor, localizer, textFactory);
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
                textFactory,
                localizer.getMessage("commands.update_creator.error.no_creator_process")
            );
            throw new CommandExecutionException(true, "Could not find a creator process for the player.");
        }

        return creator
            .update(stepName, stepValue)
            .withExceptionContext(() -> String.format(
                "Updating creator process for player %s with step name %s and value %s",
                getCommandSender(),
                stepName,
                stepValue)
            );
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
