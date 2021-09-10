package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that adds co-owners to a given door.
 *
 * @author Pim
 */
@ToString
public class AddOwner extends DoorTargetCommand
{
    private static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.ADD_OWNER;

    /**
     * The default value to use for {@link #targetPermissionLevel} when none is specified.
     */
    protected static final int DEFAULT_PERMISSION_LEVEL = 2;

    /**
     * The target player that will be added to the {@link #doorRetriever} as co-owner.
     * <p>
     * If this player is already an owner of the target door, their permission will be overridden provided that the
     * command sender is allowed to add/remove co-owners at both the old and the new target permission level.
     */
    private final IPPlayer targetPlayer;

    /**
     * The permission level of the new owner's ownership. 1 = admin, 2 = user.
     */
    private final int targetPermissionLevel;

    private final DatabaseManager databaseManager;

    @AssistedInject //
    AddOwner(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
             @Assisted DoorRetriever.AbstractRetriever doorRetriever, @Assisted IPPlayer targetPlayer,
             @Assisted int targetPermissionLevel, DatabaseManager databaseManager, CompletableFutureHandler handler)
    {
        super(commandSender, logger, localizer, doorRetriever, DoorAttribute.ADD_OWNER, handler);
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel;
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected boolean validInput()
    {
        if (targetPermissionLevel == 1 || targetPermissionLevel == 2)
            return true;

        getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.invalid_target_permission",
                                                            targetPermissionLevel));
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        return databaseManager.addOwner(door, targetPlayer, targetPermissionLevel,
                                        getCommandSender().getPlayer().orElse(null))
                              .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractDoor door, boolean hasBypassPermission)
    {
        final int existingPermission = door.getDoorOwner(targetPlayer).map(DoorOwner::permission)
                                           .orElse(Integer.MAX_VALUE);

        if (!getCommandSender().isPlayer() || hasBypassPermission)
        {
            if (existingPermission == 0)
            {
                getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.targeting_prime_owner"));
                return false;
            }
            return true;
        }

        final var doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty())
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.not_an_owner"));
            return false;
        }

        final int ownerPermission = doorOwner.get().permission();
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.ADD_OWNER))
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.not_allowed"));
            return false;
        }

        if (ownerPermission >= targetPermissionLevel)
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.cannot_assign_below_self"));
            return false;
        }

        if (existingPermission <= ownerPermission || existingPermission == targetPermissionLevel)
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.add_owner.error.target_already_owner",
                                                                targetPlayer.asString()));
            return false;
        }

        return true;
    }

    // TODO: Reimplement this
//    /**
//     * Creates (but does not execute!) a new {@link AddOwner} command.
//     *
//     * @param commandSender
//     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
//     *     execution.
//     * @param doorRetriever
//     *     A {@link DoorRetriever} that references the target door.
//     * @param targetPlayer
//     *     The target player to add to this door as co-owner.
//     *     <p>
//     *     If this player is already an owner of the target door, their permission will be overridden provided that the
//     *     command sender is allowed to add/remove co-owners at both the old and the new target permission level.
//     * @param targetPermissionLevel
//     *     The permission level of the new owner's ownership. 1 = admin, 2 = user.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> run(ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
//                                                 DoorRetriever.AbstractRetriever doorRetriever, IPPlayer targetPlayer,
//                                                 int targetPermissionLevel)
//    {
//        return new AddOwner(commandSender, logger, localizer, doorRetriever, targetPlayer, targetPermissionLevel).run();
//    }
//
//    /**
//     * See {@link #run(ICommandSender, IPLogger, ILocalizer, DoorRetriever.AbstractRetriever, IPPlayer, int)}.
//     * <p>
//     * {@link #DEFAULT_PERMISSION_LEVEL} is used as permission level.
//     */
//    public static CompletableFuture<Boolean> run(ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
//                                                 DoorRetriever.AbstractRetriever doorRetriever, IPPlayer targetPlayer)
//    {
//        return run(commandSender, logger, localizer, doorRetriever, targetPlayer, DEFAULT_PERMISSION_LEVEL);
//    }
//
//    /**
//     * Executes the {@link AddOwner} command without a known {@link #targetPlayer} or {@link #targetPermissionLevel}.
//     * <p>
//     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
//     * use the {@link AddOwner} command (again, if needed) to supply the missing data.
//     * <p>
//     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, IPLogger, ILocalizer, IPPlayer,
//     * int)}.
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
//        final ILocalizer localizer = logger, localizer.getLocalizer();
//        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION, logger, localizer,
//                                                delayedInput -> delayedInputExecutor(commandSender, logger, localizer,
//                                                                                     doorRetriever, delayedInput),
//                                                () -> AddOwner.inputRequestMessage(localizer), DelayedInput.class)
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
//     *     The target player to add to this door as co-owner.
//     *     <p>
//     *     If this player is already an owner of the target door, their permission will be overridden provided that the
//     *     command sender is allowed to add/remove co-owners at both the old and the new target permission level.
//     * @param targetPermissionLevel
//     *     The permission level of the new owner's ownership. 1 = admin, 2 = user.
//     * @return See {@link BaseCommand#run()}.
//     */
//    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPLogger logger,
//                                                                 ILocalizer localizer,
//                                                                 IPPlayer targetPlayer, int targetPermissionLevel)
//    {
//        return delayedCommandInputManager().getInputRequest(commandSender)
//                                           .map(request -> request.provide(
//                                               new DelayedInput(targetPlayer, targetPermissionLevel)))
//                                           .orElse(CompletableFuture.completedFuture(false));
//    }
//
//    /**
//     * See {@link #provideDelayedInput(ICommandSender, IPLogger, ILocalizer, IPPlayer, int)}.
//     * <p>
//     * {@link #DEFAULT_PERMISSION_LEVEL} is used as permission level.
//     */
//    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPLogger logger,
//                                                                 ILocalizer localizer,
//                                                                 IPPlayer targetPlayer)
//    {
//        return provideDelayedInput(commandSender, logger, localizer, targetPlayer, DEFAULT_PERMISSION_LEVEL);
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
//     * @param delayedInput
//     *     The delayed input that was retrieved.
//     * @return See {@link BaseCommand#run()}.
//     */
//    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender, IPLogger logger,
//                                                                   ILocalizer localizer,
//                                                                   DoorRetriever.AbstractRetriever doorRetriever,
//                                                                   DelayedInput delayedInput)
//    {
//        return new AddOwner(commandSender, logger, localizer, doorRetriever, delayedInput.targetPlayer(),
//                            delayedInput.permission()).run();
//    }
//
//    /**
//     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
//     *
//     * @return The init message for the delayed input request.
//     */
//    private static String inputRequestMessage(ILocalizer localizer)
//    {
//        return localizer.getMessage("commands.add_owner.init");
//    }
//
//    /**
//     * Represents the data that can be provided as delayed input for this command. See {@link
//     * #runDelayed(ICommandSender, IPLogger, ILocalizer, DoorRetriever.AbstractRetriever)} and {@link
//     * #delayedInputExecutor(ICommandSender, IPLogger, ILocalizer, DoorRetriever.AbstractRetriever, DelayedInput)}.
//     */
//    private record DelayedInput(IPPlayer targetPlayer, int permission)
//    {
//    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link AddOwner} command.
         *
         * @param commandSender
         *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
         *     execution.
         * @param doorRetriever
         *     A {@link DoorRetriever} that references the target door.
         * @param targetPlayer
         *     The target player to add to this door as co-owner.
         *     <p>
         *     If this player is already an owner of the target door, their permission will be overridden provided that
         *     the command sender is allowed to add/remove co-owners at both the old and the new target permission
         *     level.
         * @param targetPermissionLevel
         *     The permission level of the new owner's ownership. 1 = admin, 2 = user.
         * @return See {@link BaseCommand#run()}.
         */
        AddOwner newAddOwner(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever,
                             IPPlayer targetPlayer, int targetPermissionLevel);

        /**
         * See {@link #newAddOwner(ICommandSender, DoorRetriever.AbstractRetriever, IPPlayer, int)}.
         * <p>
         * The default permission node defined by {@link AddOwner#DEFAULT_PERMISSION_LEVEL} is used.
         */
        default AddOwner newAddOwner(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever,
                                     IPPlayer targetPlayer)
        {
            return newAddOwner(commandSender, doorRetriever, targetPlayer, AddOwner.DEFAULT_PERMISSION_LEVEL);
        }
    }
}
