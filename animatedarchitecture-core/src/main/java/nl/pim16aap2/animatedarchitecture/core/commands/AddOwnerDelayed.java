package nl.pim16aap2.animatedarchitecture.core.commands;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Delayed version of {@link AddOwner}.
 * <p>
 * This command is used to add a player as a co-owner to a structure.
 * <p>
 * The target player and the permission level of the new owner's ownership can be provided as delayed input.
 */
public class AddOwnerDelayed extends DelayedCommand<AddOwnerDelayed.DelayedInput>
{
    @Inject
    AddOwnerDelayed(
        Context context,
        DelayedCommandInputRequest.IFactory<DelayedInput> inputRequestFactory)
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
        ICommandSender commandSender,
        StructureRetriever structureRetriever,
        DelayedInput delayedInput)
    {
        return commandFactory.get()
            .newAddOwner(
                commandSender,
                structureRetriever,
                delayedInput.targetPlayer,
                delayedInput.targetPermissionLevel)
            .run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, StructureRetriever structureRetriever)
    {
        return localizer.getMessage("commands.add_owner.delayed.init");
    }

    /**
     * Represents the data that can be provided as delayed input for this command. See
     * {@link #runDelayed(ICommandSender, StructureRetriever)} and
     * {@link #delayedInputExecutor(ICommandSender, StructureRetriever, DelayedInput)}.
     */
    @ToString
    @EqualsAndHashCode
    @Getter
    public static final class DelayedInput
    {
        private final IPlayer targetPlayer;
        private final PermissionLevel targetPermissionLevel;

        /**
         * @param targetPlayer
         *     The target player to add to this structure as co-owner.
         *     <p>
         *     If this player is already an owner of the target door, their permission will be overridden provided that
         *     the command sender is allowed to add/remove co-owners at both the old and the new target permission
         *     level.
         * @param targetPermissionLevel
         *     The permission level of the new owner's ownership. Defaults to
         *     {@link AddOwner#DEFAULT_PERMISSION_LEVEL}.
         */
        public DelayedInput(IPlayer targetPlayer, @Nullable PermissionLevel targetPermissionLevel)
        {
            this.targetPlayer = targetPlayer;
            this.targetPermissionLevel =
                Objects.requireNonNullElse(targetPermissionLevel, AddOwner.DEFAULT_PERMISSION_LEVEL);
        }

        /**
         * See {@link #DelayedInput(IPlayer, PermissionLevel)}.
         */
        public DelayedInput(IPlayer targetPlayer)
        {
            this(targetPlayer, null);
        }
    }
}
