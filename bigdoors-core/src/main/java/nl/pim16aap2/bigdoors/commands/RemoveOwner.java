package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the remove owner command. This command is used to remove owners from a door.
 *
 * @author Pim
 */
@ToString
public class RemoveOwner extends DoorTargetCommand
{
    private static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.REMOVE_OWNER;

    private final IPPlayer targetPlayer;

    protected RemoveOwner(ICommandSender commandSender, CommandContext context, DoorRetriever doorRetriever,
                          IPPlayer targetPlayer)
    {
        super(commandSender, context, doorRetriever, DoorAttribute.REMOVE_OWNER);
        this.targetPlayer = targetPlayer;
    }

    /**
     * Runs the {@link RemoveOwner} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for removing a co-owner of the door.
     * @param doorRetriever
     *     A {@link DoorRetriever} representing the {@link DoorBase} for which a co-owner is requested to be removed.
     * @param targetPlayer
     *     The co-owner that is requested to be removed.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorRetriever doorRetriever, IPPlayer targetPlayer)
    {
        return new RemoveOwner(commandSender, context, doorRetriever, targetPlayer).run();
    }

    /**
     * Executes the {@link RemoveOwner} command without a known {@link #targetPlayer}.
     * <p>
     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
     * use the  {@link RemoveOwner} command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, CommandContext, IPPlayer)}.
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param doorRetriever
     *     A {@link DoorRetriever} that references the target door.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, CommandContext context,
                                                        DoorRetriever doorRetriever)
    {
        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        final ILocalizer localizer = context.getLocalizer();
        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION, context,
                                                delayedInput -> delayedInputExecutor(commandSender, context,
                                                                                     doorRetriever, delayedInput),
                                                () -> RemoveOwner.inputRequestMessage(localizer), IPPlayer.class)
            .getCommandOutput();
    }

    /**
     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
     * ICommandSender}.
     * <p>
     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
     * informed about it.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to look for an active {@link DelayedCommandInputRequest} that can be
     *     fulfilled.
     * @param targetPlayer
     *     The target player to attempt to remove as co-owner of this door.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, CommandContext context,
                                                                 IPPlayer targetPlayer)
    {
        return context.getDelayedCommandInputManager().getInputRequest(commandSender)
                      .map(request -> request.provide(targetPlayer))
                      .orElse(CompletableFuture.completedFuture(false));
    }

    /**
     * The method that is run once delayed input is received.
     * <p>
     * It processes the new input and executes the command using the previously-provided data (see {@link
     * #runDelayed(ICommandSender, CommandContext, DoorRetriever)}).
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param doorRetriever
     *     A {@link DoorRetriever} that references the target door.
     * @param targetPlayer
     *     The target player to attempt to remove as co-owner.
     * @return See {@link BaseCommand#run()}.
     */
    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender, CommandContext context,
                                                                   DoorRetriever doorRetriever, IPPlayer targetPlayer)
    {
        return new RemoveOwner(commandSender, context, doorRetriever, targetPlayer).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static String inputRequestMessage(ILocalizer localizer)
    {
        return localizer.getMessage("commands.remove_owner.init");
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        return context.getDatabaseManager()
                      .removeOwner(door, targetPlayer, getCommandSender().getPlayer().orElse(null))
                      .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractDoor door, boolean hasBypassPermission)
    {
        final boolean bypassOwnership = !getCommandSender().isPlayer() || hasBypassPermission;

        final var doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty() && !bypassOwnership)
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.remove_owner.error.not_an_owner"));
            return false;
        }

        // Assume a permission level of 0 in case the command sender is not an owner but DOES have bypass access.
        final int ownerPermission = doorOwner.map(DoorOwner::permission).orElse(0);
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.REMOVE_OWNER))
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.remove_owner.error.not_allowed"));
            return false;
        }

        final var targetDoorOwner = door.getDoorOwner(targetPlayer);
        if (targetDoorOwner.isEmpty())
        {
            getCommandSender()
                .sendMessage(localizer.getMessage("commands.remove_owner.error.target_not_an_owner",
                                                  targetPlayer.asString(), door.getBasicInfo()));
            return false;
        }

        if (targetDoorOwner.get().permission() <= ownerPermission)
        {
            getCommandSender()
                .sendMessage(localizer.getMessage("commands.remove_owner.error.cannot_remove_higher_permission"));
            return false;
        }
        return true;
    }
}
