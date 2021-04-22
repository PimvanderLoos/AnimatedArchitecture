package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.messages.Message;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the remove owner command. This command is used to remove owners from a door.
 *
 * @author Pim
 */
@ToString
public class RemoveOwner extends DoorTargetCommand
{
    private final @NonNull IPPlayer targetPlayer;

    private static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.REMOVE_OWNER;

    protected RemoveOwner(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever,
                          final @NonNull IPPlayer targetPlayer)
    {
        super(commandSender, doorRetriever);
        this.targetPlayer = targetPlayer;
    }

    /**
     * Runs the {@link RemoveOwner} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for removing a co-owner of the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link AbstractDoorBase} for which a co-owner is
     *                      requested to be removed.
     * @param targetPlayer  The co-owner that is requested to be removed.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorRetriever doorRetriever,
                                                          final @NonNull IPPlayer targetPlayer)
    {
        return new RemoveOwner(commandSender, doorRetriever, targetPlayer).run();
    }

    /**
     * Executes the remove owner command without a known {@link #targetPlayer}.
     * <p>
     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
     * use the RemoveOwner command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, IPPlayer}.
     *
     * @param commandSender The entity that sent the command and is held responsible (i.e. permissions, communication)
     *                      for its execution.
     * @param doorRetriever A {@link DoorRetriever} that references the target door.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> runDelayed(final @NonNull ICommandSender commandSender,
                                                                 final @NonNull DoorRetriever doorRetriever)
    {
        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION,
                                                delayedInput -> delayedInputExecutor(commandSender,
                                                                                     doorRetriever,
                                                                                     delayedInput),
                                                RemoveOwner::inputRequestMessage, IPPlayer.class).getCommandOutput();
    }

    /**
     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
     * ICommandSender}.
     * <p>
     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
     * informed about it.
     *
     * @param commandSender The {@link ICommandSender} for which to look for an active {@link
     *                      DelayedCommandInputRequest} that can be fulfilled.
     * @param targetPlayer  The target player to attempt to remove as co-owner of this door.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> provideDelayedInput(final @NonNull ICommandSender commandSender,
                                                                          final @NonNull IPPlayer targetPlayer)
    {
        return BigDoors.get().getDelayedCommandInputManager().getInputRequest(commandSender)
                       .map(request -> request.provide(targetPlayer))
                       .orElse(CompletableFuture.completedFuture(false));
    }

    /**
     * The method that is run once delayed input is received.
     * <p>
     * It processes the new input and executes the command using the previously-provided data (see {@link
     * #runDelayed(ICommandSender, DoorRetriever)}).
     *
     * @param commandSender The entity that sent the command and is held responsible (i.e. permissions, communication)
     *                      for its execution.
     * @param doorRetriever A {@link DoorRetriever} that references the target door.
     * @param targetPlayer  The target player to attempt to remove as co-owner.
     * @return See {@link BaseCommand#run()}.
     */
    private static @NonNull CompletableFuture<Boolean> delayedInputExecutor(final @NonNull ICommandSender commandSender,
                                                                            final @NonNull DoorRetriever doorRetriever,
                                                                            final @NonNull IPPlayer targetPlayer)
    {
        return new RemoveOwner(commandSender, doorRetriever, targetPlayer).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static @NonNull String inputRequestMessage()
    {
        return BigDoors.get().getPlatform().getMessages().getString(Message.COMMAND_REMOVEOWNER_INIT);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        return BigDoors.get().getDatabaseManager()
                       .removeOwner(door, targetPlayer, getCommandSender().getPlayer().orElse(null))
                       .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean hasBypassPermission)
    {
        final boolean bypassOwnership = !getCommandSender().isPlayer() || hasBypassPermission;

        val doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty() && !bypassOwnership)
        {
            // TODO: Localization
            getCommandSender().sendMessage("You are not an owner of this door!");
            return false;
        }

        // Assume a permission level of 0 in case the command sender is not an owner but DOES have bypass access.
        final int ownerPermission = doorOwner.map(DoorOwner::getPermission).orElse(0);
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.REMOVE_OWNER))
        {
            // TODO: Localization
            getCommandSender().sendMessage("Your are not allowed to remove co-owners from this door!");
            return false;
        }

        val targetDoorOwner = door.getDoorOwner(targetPlayer);
        if (targetDoorOwner.isEmpty())
        {
            // TODO: Localization
            getCommandSender()
                .sendMessage(targetPlayer.asString() + " is not a (co-)owner of door " + door.getBasicInfo());
            return false;
        }

        if (targetDoorOwner.get().getPermission() <= ownerPermission)
        {
            // TODO: Localization
            getCommandSender()
                .sendMessage("You can only remove (co)owners with a higher permission level than yourself! ");
            return false;
        }
        return true;
    }
}
