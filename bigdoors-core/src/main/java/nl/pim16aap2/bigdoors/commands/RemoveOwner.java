package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
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
    private final DatabaseManager databaseManager;

    @AssistedInject
    public RemoveOwner(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
                       @Assisted DoorRetriever.AbstractRetriever doorRetriever, @Assisted IPPlayer targetPlayer,
                       DatabaseManager databaseManager)
    {
        super(commandSender, logger, localizer, doorRetriever, DoorAttribute.REMOVE_OWNER);
        this.targetPlayer = targetPlayer;
        this.databaseManager = databaseManager;
    }

    // TODO: Re-implement
//    /**
//     * Executes the {@link RemoveOwner} command without a known {@link #targetPlayer}.
//     * <p>
//     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
//     * use the  {@link RemoveOwner} command (again, if needed) to supply the missing data.
//     * <p>
//     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, IPLogger, ILocalizer, IPPlayer)}.
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetriever} that references the target door.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, IPLogger logger,
//                                                        ILocalizer localizer,
//                                                        DoorRetriever.AbstractRetriever doorRetriever)
//    {
//        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
//        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION, logger, localizer,
//                                                delayedInput -> delayedInputExecutor(commandSender, logger, localizer,
//                                                                                     doorRetriever, delayedInput),
//                                                () -> RemoveOwner.inputRequestMessage(localizer), IPPlayer.class)
//            .getCommandOutput();
//    }
//
//    /**
//     * Provides the delayed input if there is currently an active {@link DelayedCommandInputRequest} for the {@link
//     * ICommandSender}.
//     * <p>
//     * If no active {@link DelayedCommandInputRequest} can be found for the command sender, the command sender will be
//     * informed about it.
//     *
//     * @param commandSender
//     *     The {@link ICommandSender} for which to look for an active {@link DelayedCommandInputRequest} that can be
//     *     fulfilled.
//     * @param targetPlayer
//     *     The target player to attempt to remove as co-owner of this door.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPLogger logger,
//                                                                 ILocalizer localizer,
//                                                                 IPPlayer targetPlayer)
//    {
//        return delayedCommandInputManager().getInputRequest(commandSender)
//                                           .map(request -> request.provide(targetPlayer))
//                                           .orElse(CompletableFuture.completedFuture(false));
//    }
//
//    /**
//     * The method that is run once delayed input is received.
//     * <p>
//     * It processes the new input and executes the command using the previously-provided data (see {@link
//     * #runDelayed(ICommandSender, IPLogger, ILocalizer, DoorRetriever.AbstractRetriever)}).
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetriever} that references the target door.
//     * @param targetPlayer
//     *     The target player to attempt to remove as co-owner.
//     * @return See {@link BaseCommand#run()}.
//     */
//    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender, IPLogger logger,
//                                                                   ILocalizer localizer,
//                                                                   DoorRetriever.AbstractRetriever doorRetriever,
//                                                                   IPPlayer targetPlayer)
//    {
//        return new RemoveOwner(commandSender, logger, localizer, doorRetriever, targetPlayer).run();
//    }
//
//    /**
//     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
//     *
//     * @return The init message for the delayed input request.
//     */
//    private static String inputRequestMessage(ILocalizer localizer)
//    {
//        return localizer.getMessage("commands.remove_owner.init");
//    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        return databaseManager.removeOwner(door, targetPlayer, getCommandSender().getPlayer().orElse(null))
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

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link RemoveOwner} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for removing a co-owner of the door.
         * @param doorRetriever
         *     A {@link DoorRetriever} representing the {@link DoorBase} for which a co-owner is requested to be
         *     removed.
         * @param targetPlayer
         *     The co-owner that is requested to be removed.
         * @return See {@link BaseCommand#run()}.
         */
        RemoveOwner newRemoveOwner(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever,
                                   IPPlayer targetPlayer);
    }
}
