package nl.pim16aap2.bigdoors.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IMessageable;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.events.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.StructureActionType;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureAttribute;
import nl.pim16aap2.bigdoors.core.structures.StructureToggleRequestBuilder;
import nl.pim16aap2.bigdoors.core.text.TextType;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetriever;
import org.jetbrains.annotations.Nullable;

import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command that toggles a structure.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Toggle extends BaseCommand
{
    public static final StructureActionType DEFAULT_STRUCTURE_ACTION_TYPE = StructureActionType.TOGGLE;
    public static final AnimationType DEFAULT_ANIMATION_TYPE = AnimationType.MOVE_BLOCKS;

    private final StructureToggleRequestBuilder structureToggleRequestBuilder;
    private final StructureRetriever[] structureRetrievers;
    private final IMessageable messageableServer;
    private final StructureActionType structureActionType;
    private final AnimationType animationType;
    private final @Nullable Double time;
    private final boolean preventPerpetualMovement;

    @AssistedInject //
    Toggle(
        ILocalizer localizer,
        ITextFactory textFactory,
        @Named("MessageableServer") IMessageable messageableServer,
        StructureToggleRequestBuilder structureToggleRequestBuilder,
        @Assisted ICommandSender commandSender,
        @Assisted StructureActionType structureActionType,
        @Assisted AnimationType animationType,
        @Assisted @Nullable Double time,
        @Assisted boolean preventPerpetualMovement,
        @Assisted StructureRetriever... structureRetrievers)
    {
        super(commandSender, localizer, textFactory);
        this.structureActionType = structureActionType;
        this.animationType = animationType;
        this.time = time;
        this.preventPerpetualMovement = preventPerpetualMovement;
        this.structureToggleRequestBuilder = structureToggleRequestBuilder;
        this.structureRetrievers = structureRetrievers;
        this.messageableServer = messageableServer;
    }

    @Override
    protected boolean validInput()
    {
        if (structureRetrievers.length > 0)
            return true;

        getCommandSender().sendMessage(textFactory, TextType.ERROR,
                                       localizer.getMessage("commands.toggle.error.not_enough_structures"));

        if (animationType == AnimationType.PREVIEW)
            return getCommandSender() instanceof IPlayer player && player.isOnline();

        return false;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.TOGGLE;
    }

    /**
     * Checks if the provided {@link AbstractStructure} can be toggled with the action provided by
     * {@link #structureActionType}.
     * <p>
     * For example, if the action is {@link StructureActionType#CLOSE} and the structure is already closed, the action
     * is not possible.
     *
     * @param structure
     *     The structure for which to check whether it can be toggled.
     * @return True if the toggle action is possible, otherwise false.
     */
    protected final boolean canToggle(AbstractStructure structure)
    {
        return switch (structureActionType)
            {
                case TOGGLE -> true;
                case OPEN -> structure.isCloseable();
                case CLOSE -> structure.isOpenable();
            };
    }

    private void toggleStructure(AbstractStructure structure, StructureActionCause cause, boolean hasBypassPermission)
    {
        if (!hasAccessToAttribute(structure, StructureAttribute.TOGGLE, hasBypassPermission))
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.toggle.error.no_access",
                                                  localizer.getStructureType(structure), structure.getBasicInfo()));
            log.atFine()
               .log("%s has no access for command %s for structure %s!", getCommandSender(), this, structure);
            return;
        }
        if (!canToggle(structure))
        {
            getCommandSender()
                .sendMessage(textFactory, TextType.ERROR,
                             localizer.getMessage("commands.toggle.error.cannot_toggle",
                                                  localizer.getStructureType(structure), structure.getBasicInfo()));
            log.atFiner()
               .log("Blocked action for command %s for structure %s by %s", this, structure, getCommandSender());
            return;
        }

        final Optional<IPlayer> playerOpt = getCommandSender().getPlayer();
        structureToggleRequestBuilder
            .builder()
            .structure(structure)
            .structureActionCause(cause)
            .structureActionType(structureActionType)
            .animationType(animationType)
            .responsible(playerOpt.orElse(null))
            .messageReceiver(playerOpt.isPresent() ? playerOpt.get() : messageableServer)
            .time(time)
            .preventPerpetualMovement(preventPerpetualMovement)
            .build().execute();
    }

    private CompletableFuture<Void> handleStructureRequest(
        StructureRetriever structureRetriever, StructureActionCause cause, boolean hasBypassPermission)
    {
        return getStructure(structureRetriever)
            .thenAccept(structureOpt ->
                            structureOpt.ifPresent(
                                structure -> toggleStructure(structure, cause, hasBypassPermission)));
    }

    @Override
    protected final CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        final StructureActionCause actionCause =
            getCommandSender().isPlayer() ? StructureActionCause.PLAYER : StructureActionCause.SERVER;

        final CompletableFuture<?>[] actions = new CompletableFuture[structureRetrievers.length];
        for (int idx = 0; idx < actions.length; ++idx)
            actions[idx] = handleStructureRequest(structureRetrievers[idx], actionCause,
                                                  permissions.hasAdminPermission());

        return CompletableFuture.allOf(actions);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Toggle} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} to hold responsible for the toggle action.
         * @param actionType
         *     The type of action to apply.
         *     <p>
         *     For example, when {@link StructureActionType#OPEN} is used, the structure can only be toggled if it is
         *     possible to open it (in most cases that would mean that it is currently closed).
         *     <p>
         *     {@link StructureActionType#TOGGLE}, however, is possible regardless of its current open/close status.
         * @param animationType
         *     The type of animation to use.
         * @param speedMultiplier
         *     The speed multiplier to apply to the animation.
         * @param preventPerpetualMovement
         *     True to prevent perpetual movement. When perpetual movement is requested but denied via this setting, the
         *     animation will still be time-limited.
         * @param structureRetrievers
         *     The structure(s) to toggle.
         * @return See {@link BaseCommand#run()}.
         */
        Toggle newToggle(
            ICommandSender commandSender, StructureActionType actionType, AnimationType animationType,
            @Nullable Double speedMultiplier, boolean preventPerpetualMovement,
            StructureRetriever... structureRetrievers);

        /**
         * See
         * {@link #newToggle(ICommandSender, StructureActionType, AnimationType, Double, boolean,
         * StructureRetriever...)}.
         * <p>
         * Defaults to null for the speed multiplier and true for preventPerpetualMovement.
         */
        default Toggle newToggle(
            ICommandSender commandSender, StructureActionType actionType, AnimationType animationType,
            StructureRetriever... structureRetrievers)
        {
            return newToggle(
                commandSender, actionType, animationType, null, true, structureRetrievers);
        }

        /**
         * See
         * {@link #newToggle(ICommandSender, StructureActionType, AnimationType, Double, boolean,
         * StructureRetriever...)}.
         * <p>
         * Defaults to null for the speed multiplier, to {@link Toggle#DEFAULT_STRUCTURE_ACTION_TYPE} for the structure
         * action type, to {@link Toggle#DEFAULT_ANIMATION_TYPE} for the animation type, and to true for
         * preventPerpetualMovement.
         */
        default Toggle newToggle(ICommandSender commandSender, StructureRetriever... structureRetrievers)
        {
            return newToggle(
                commandSender, Toggle.DEFAULT_STRUCTURE_ACTION_TYPE, Toggle.DEFAULT_ANIMATION_TYPE,
                null, true, structureRetrievers);
        }
    }
}
