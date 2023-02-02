package nl.pim16aap2.bigdoors.commands;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AddOwnerDelayed extends DelayedCommand<AddOwnerDelayed.DelayedInput>
{
    @Inject AddOwnerDelayed(
        Context context, DelayedCommandInputRequest.IFactory<DelayedInput> inputRequestFactory)
    {
        super(context, inputRequestFactory, DelayedInput.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return AddOwner.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender, MovableRetriever movableRetriever, DelayedInput delayedInput)
    {
        return commandFactory.get().newAddOwner(commandSender, movableRetriever, delayedInput.targetPlayer,
                                                delayedInput.targetPermissionLevel).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, MovableRetriever movableRetriever)
    {
        return localizer.getMessage("commands.add_owner.delayed.init");
    }

    /**
     * Represents the data that can be provided as delayed input for this command. See
     * {@link #runDelayed(ICommandSender, MovableRetriever)} and
     * {@link #delayedInputExecutor(ICommandSender, MovableRetriever, DelayedInput)}.
     */
    @ToString
    @EqualsAndHashCode
    @Getter
    public static final class DelayedInput
    {
        private final IPPlayer targetPlayer;
        private final PermissionLevel targetPermissionLevel;

        /**
         * @param targetPlayer
         *     The target player to add to this movable as co-owner.
         *     <p>
         *     If this player is already an owner of the target door, their permission will be overridden provided that
         *     the command sender is allowed to add/remove co-owners at both the old and the new target permission
         *     level.
         * @param targetPermissionLevel
         *     The permission level of the new owner's ownership. Defaults to
         *     {@link AddOwner#DEFAULT_PERMISSION_LEVEL}.
         */
        public DelayedInput(IPPlayer targetPlayer, @Nullable PermissionLevel targetPermissionLevel)
        {
            this.targetPlayer = targetPlayer;
            this.targetPermissionLevel =
                Objects.requireNonNullElse(targetPermissionLevel, AddOwner.DEFAULT_PERMISSION_LEVEL);
        }

        /**
         * See {@link #DelayedInput(IPPlayer, PermissionLevel)}.
         */
        public DelayedInput(IPPlayer targetPlayer)
        {
            this(targetPlayer, null);
        }
    }
}