package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.util.Constants;
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

    AddOwner(ICommandSender commandSender, DoorRetriever doorRetriever, IPPlayer targetPlayer,
             int targetPermissionLevel)
    {
        super(commandSender, doorRetriever, DoorAttribute.ADD_OWNER);
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel;
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

        getCommandSender().sendMessage(BigDoors.get().getLocalizer()
                                               .getMessage("commands.add_owner.error.invalid_target_permission",
                                                           targetPermissionLevel));
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        return BigDoors.get().getDatabaseManager().addOwner(door, targetPlayer, targetPermissionLevel,
                                                            getCommandSender().getPlayer().orElse(null))
                       .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(AbstractDoor door, boolean hasBypassPermission)
    {
        final int existingPermission = door.getDoorOwner(targetPlayer).map(DoorOwner::permission)
                                           .orElse(Integer.MAX_VALUE);

        final var localizer = BigDoors.get().getLocalizer();
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

    /**
     * Runs the {@link AddOwner} command.
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param doorRetriever
     *     A {@link DoorRetriever} that references the target door.
     * @param targetPlayer
     *     The target player to add to this door as co-owner.
     *     <p>
     *     If this player is already an owner of the target door, their permission will be overridden provided that the
     *     command sender is allowed to add/remove co-owners at both the old and the new target permission level.
     * @param targetPermissionLevel
     *     The permission level of the new owner's ownership. 1 = admin, 2 = user.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, DoorRetriever doorRetriever,
                                                 IPPlayer targetPlayer, int targetPermissionLevel)
    {
        return new AddOwner(commandSender, doorRetriever, targetPlayer, targetPermissionLevel).run();
    }

    /**
     * See {@link #run(ICommandSender, DoorRetriever, IPPlayer, int)}.
     * <p>
     * {@link #DEFAULT_PERMISSION_LEVEL} is used as permission level.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, DoorRetriever doorRetriever,
                                                 IPPlayer targetPlayer)
    {
        return run(commandSender, doorRetriever, targetPlayer, DEFAULT_PERMISSION_LEVEL);
    }

    /**
     * Executes the {@link AddOwner} command without a known {@link #targetPlayer} or {@link #targetPermissionLevel}.
     * <p>
     * These missing values will be retrieved using a {@link DelayedCommandInputRequest}. The player will be asked to
     * use the {@link AddOwner} command (again, if needed) to supply the missing data.
     * <p>
     * These missing data can be supplied using {@link #provideDelayedInput(ICommandSender, IPPlayer, int)}.
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param doorRetriever
     *     A {@link DoorRetriever} that references the target door.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> runDelayed(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        final int commandTimeout = Constants.COMMAND_WAITER_TIMEOUT;
        return new DelayedCommandInputRequest<>(commandTimeout, commandSender, COMMAND_DEFINITION,
                                                delayedInput -> delayedInputExecutor(commandSender,
                                                                                     doorRetriever,
                                                                                     delayedInput),
                                                AddOwner::inputRequestMessage, DelayedInput.class).getCommandOutput();
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
     *     The target player to add to this door as co-owner.
     *     <p>
     *     If this player is already an owner of the target door, their permission will be overridden provided that the
     *     command sender is allowed to add/remove co-owners at both the old and the new target permission level.
     * @param targetPermissionLevel
     *     The permission level of the new owner's ownership. 1 = admin, 2 = user.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPPlayer targetPlayer,
                                                                 int targetPermissionLevel)
    {
        return BigDoors.get().getDelayedCommandInputManager().getInputRequest(commandSender)
                       .map(request -> request.provide(new DelayedInput(targetPlayer, targetPermissionLevel)))
                       .orElse(CompletableFuture.completedFuture(false));
    }

    /**
     * See {@link #provideDelayedInput(ICommandSender, IPPlayer, int)}.
     * <p>
     * {@link #DEFAULT_PERMISSION_LEVEL} is used as permission level.
     */
    public static CompletableFuture<Boolean> provideDelayedInput(ICommandSender commandSender, IPPlayer targetPlayer)
    {
        return provideDelayedInput(commandSender, targetPlayer, DEFAULT_PERMISSION_LEVEL);
    }

    /**
     * The method that is run once delayed input is received.
     * <p>
     * It processes the new input and executes the command using the previously-provided data (see {@link
     * #runDelayed(ICommandSender, DoorRetriever)}).
     *
     * @param commandSender
     *     The entity that sent the command and is held responsible (i.e. permissions, communication) for its
     *     execution.
     * @param doorRetriever
     *     A {@link DoorRetriever} that references the target door.
     * @param delayedInput
     *     The delayed input that was retrieved.
     * @return See {@link BaseCommand#run()}.
     */
    private static CompletableFuture<Boolean> delayedInputExecutor(ICommandSender commandSender,
                                                                   DoorRetriever doorRetriever,
                                                                   DelayedInput delayedInput)
    {
        return new AddOwner(commandSender, doorRetriever, delayedInput.targetPlayer(),
                            delayedInput.permission()).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static String inputRequestMessage()
    {
        return BigDoors.get().getLocalizer().getMessage("commands.add_owner.init");
    }

    /**
     * Represents the data that can be provided as delayed input for this command. See {@link
     * #runDelayed(ICommandSender, DoorRetriever)} and {@link #delayedInputExecutor(ICommandSender, DoorRetriever,
     * DelayedInput)}.
     */
    private record DelayedInput(IPPlayer targetPlayer, int permission)
    {
    }
}
