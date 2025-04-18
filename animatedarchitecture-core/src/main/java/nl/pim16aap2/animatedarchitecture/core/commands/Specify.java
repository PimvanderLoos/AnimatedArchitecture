package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.util.delayedinput.DelayedInputRequest;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command to specify a structure for the {@link StructureSpecificationManager}.
 * <p>
 * For example, when a player wants to perform an action on a structure, but multiple structures fit the criteria. The
 * Specify command is then used to specify which structure the player wants to perform the action on.
 * <p>
 * The specify command takes a String as input, which is then used to specify the structure. See
 * {@link StructureSpecificationManager#handleInput(IPlayer, String)}.
 */
@ToString(callSuper = true)
public class Specify extends BaseCommand
{
    private final String input;

    @ToString.Exclude
    private final StructureSpecificationManager structureSpecificationManager;

    @AssistedInject
    Specify(
        @Assisted ICommandSender commandSender,
        @Assisted String input,
        IExecutor executor,
        StructureSpecificationManager structureSpecificationManager)
    {
        super(commandSender, executor);
        this.input = input;
        this.structureSpecificationManager = structureSpecificationManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.SPECIFY;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<?> executeCommand(@Nullable PermissionsStatus permissions)
    {
        if (!structureSpecificationManager.handleInput((IPlayer) getCommandSender(), input))
            getCommandSender().sendError("commands.base.error.no_pending_process");
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Specify} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible specifying a structure.
         * @param data
         *     The data that specifies a structure based on the {@link DelayedInputRequest} for the command sender as
         *     registered by the {@link StructureSpecificationManager}.
         * @return See {@link BaseCommand#run()}.
         */
        Specify newSpecify(ICommandSender commandSender, String data);
    }
}
