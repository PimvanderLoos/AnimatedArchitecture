package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import lombok.Value;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that adds co-owners to a given door.
 *
 * @author Pim
 */
@ToString
public class AddOwner extends DoorTargetCommand
{
    /**
     * The default value to use for {@link #targetPermissionLevel} when none is specified.
     */
    protected static final int DEFAULT_PERMISSION_LEVEL = 2;

    private static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.ADD_OWNER;

    /**
     * The target player that will be added to the {@link #doorRetriever} as co-owner.
     * <p>
     * If this player is already an owner of the target door, their permission will be overridden provided that the
     * command sender is allowed to add/remove co-owners at both the old and the new target permission level.
     */
    private final @NotNull IPPlayer targetPlayer;

    /**
     * The permission level of the new owner's ownership. 1 = admin, 2 = user.
     */
    private final int targetPermissionLevel;

    AddOwner(final @NotNull ICommandSender commandSender, final @NotNull DoorRetriever doorRetriever,
             final @NotNull IPPlayer targetPlayer, final int targetPermissionLevel)
    {
        super(commandSender, doorRetriever, DoorAttribute.ADD_OWNER);
        this.targetPlayer = targetPlayer;
        this.targetPermissionLevel = targetPermissionLevel;
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected boolean validInput()
    {
        if (targetPermissionLevel == 1 || targetPermissionLevel == 2)
            return true;

        //TODO: Localization
        getCommandSender().sendMessage("Target permission level " + targetPermissionLevel + " is invalid! " +
                                           "It must be either 1 (admin) or 2 (user).");
        return false;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> performAction(final @NotNull AbstractDoorBase door)
    {
        return BigDoors.get().getDatabaseManager().addOwner(door, targetPlayer, targetPermissionLevel,
                                                            getCommandSender().getPlayer().orElse(null))
                       .thenApply(this::handleDatabaseActionResult);
    }

    @Override
    protected boolean isAllowed(final @NotNull AbstractDoorBase door, final boolean hasBypassPermission)
    {
        final int existingPermission = door.getDoorOwner(targetPlayer).map(DoorOwner::permission)
                                           .orElse(Integer.MAX_VALUE);

        if (!getCommandSender().isPlayer() || hasBypassPermission)
        {
            if (existingPermission == 0)
            {
                // TODO: Localization
                getCommandSender().sendMessage("You cannot change the permission level of this player!");
                return false;
            }
            return true;
        }

        val doorOwner = getCommandSender().getPlayer().flatMap(door::getDoorOwner);
        if (doorOwner.isEmpty())
        {
            // TODO: Localization
            getCommandSender().sendMessage("You are not an owner of this door!");
            return false;
        }

        final int ownerPermission = doorOwner.get().permission();
        if (ownerPermission > DoorAttribute.getPermissionLevel(DoorAttribute.ADD_OWNER))
        {
            // TODO: Localization
            getCommandSender().sendMessage("Your are not allowed to add co-owners to this door!");
            return false;
        }

        if (ownerPermission >= targetPermissionLevel)
        {
            // TODO: Localization
            getCommandSender().sendMessage("You cannot only add co-owners with a higher permission level!");
            return false;
        }

        if (existingPermission <= ownerPermission)
        {
            // TODO: Localization
            getCommandSender().sendMessage(
                targetPlayer.asString() + " is already a (co-)owner of this door with a lower permission level!");
            return false;
        }
        if (existingPermission == targetPermissionLevel)
        {
            // TODO: Localization
            getCommandSender().sendMessage(
                targetPlayer.asString() + " is already a (co-)owner of this door the same permission level!");
            return false;
        }

        return true;
    }

    /**
     * Runs the {@link AddOwner} command.
     *
     * @param commandSender         The entity that sent the command and is held responsible (i.e. permissions,
     *                              communication) for its execution.
     * @param doorRetriever         A {@link DoorRetriever} that references the target door.
     * @param targetPlayer          The target player to add to this door as co-owner.
     *                              <p>
     *                              If this player is already an owner of the target door, their permission will be
     *                              overridden provided that the command sender is allowed to add/remove co-owners at
     *                              both the old and the new target permission level.
     * @param targetPermissionLevel The permission level of the new owner's ownership. 1 = admin, 2 = user.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever doorRetriever,
                                                          final @NotNull IPPlayer targetPlayer,
                                                          final int targetPermissionLevel)
    {
        return new AddOwner(commandSender, doorRetriever, targetPlayer, targetPermissionLevel).run();
    }

    /**
     * See {@link #run(ICommandSender, DoorRetriever, IPPlayer, int)}.
     * <p>
     * {@link #DEFAULT_PERMISSION_LEVEL} is used as permission level.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever doorRetriever,
                                                          final @NotNull IPPlayer targetPlayer)
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
     * @param commandSender The entity that sent the command and is held responsible (i.e. permissions, communication)
     *                      for its execution.
     * @param doorRetriever A {@link DoorRetriever} that references the target door.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> runDelayed(final @NotNull ICommandSender commandSender,
                                                                 final @NotNull DoorRetriever doorRetriever)
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
     * @param commandSender         The {@link ICommandSender} for which to look for an active {@link
     *                              DelayedCommandInputRequest} that can be fulfilled.
     * @param targetPlayer          The target player to add to this door as co-owner.
     *                              <p>
     *                              If this player is already an owner of the target door, their permission will be
     *                              overridden provided that the command sender is allowed to add/remove co-owners at
     *                              both the old and the new target permission level.
     * @param targetPermissionLevel The permission level of the new owner's ownership. 1 = admin, 2 = user.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> provideDelayedInput(final @NotNull ICommandSender commandSender,
                                                                          final @NotNull IPPlayer targetPlayer,
                                                                          final int targetPermissionLevel)
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
    public static @NotNull CompletableFuture<Boolean> provideDelayedInput(final @NotNull ICommandSender commandSender,
                                                                          final @NotNull IPPlayer targetPlayer)
    {
        return provideDelayedInput(commandSender, targetPlayer, DEFAULT_PERMISSION_LEVEL);
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
     * @param delayedInput  The delayed input that was retrieved.
     * @return See {@link BaseCommand#run()}.
     */
    private static @NotNull CompletableFuture<Boolean> delayedInputExecutor(final @NotNull ICommandSender commandSender,
                                                                            final @NotNull DoorRetriever doorRetriever,
                                                                            final @NotNull DelayedInput delayedInput)
    {
        return new AddOwner(commandSender, doorRetriever, delayedInput.getTargetPlayer(),
                            delayedInput.getPermission()).run();
    }

    /**
     * Retrieves the message that will be sent to the command sender after initialization of a delayed input request.
     *
     * @return The init message for the delayed input request.
     */
    private static @NotNull String inputRequestMessage()
    {
        return BigDoors.get().getPlatform().getMessages().getString(Message.COMMAND_ADDOWNER_INIT);
    }

    /**
     * Represents the data that can be provided as delayed input for this command. See {@link
     * #runDelayed(ICommandSender, DoorRetriever)} and {@link #delayedInputExecutor(ICommandSender, DoorRetriever,
     * DelayedInput)}.
     */
    @Value
    private static class DelayedInput
    {
        @NotNull IPPlayer targetPlayer;
        int permission;
    }
}
