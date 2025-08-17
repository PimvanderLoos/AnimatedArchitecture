package nl.pim16aap2.animatedarchitecture.core.structures;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the principal way to submit a request to animate a structure.
 * <p>
 * To submit a new request, first create a new instance using
 * {@link IAnimatedArchitecturePlatform#getStructureAnimationRequestBuilder()}.
 * <p>
 * Once the new request has been constructed, it can be submitted using {@link #execute()}.
 */
@Getter
@ToString
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
public class StructureAnimationRequest
{
    @Getter
    private final StructureRetriever structureRetriever;
    @Getter
    private final StructureActionCause cause;
    @Getter
    private final IMessageable messageReceiver;
    @Getter
    private final @Nullable IPlayer responsible;
    @Getter
    private final @Nullable Double time;
    @Getter
    private final boolean skipAnimation;
    @Getter
    private final boolean preventPerpetualMovement;
    @Getter
    private final StructureActionType actionType;
    @Getter
    private final AnimationType animationType;

    @ToString.Exclude
    private final ILocalizer localizer;

    @ToString.Exclude
    private final ITextFactory textFactory;

    @ToString.Exclude
    private final StructureActivityManager structureActivityManager;

    @ToString.Exclude
    private final IPlayerFactory playerFactory;

    @ToString.Exclude
    private final IExecutor executor;

    @AssistedInject
    public StructureAnimationRequest(
        @Assisted StructureRetriever structureRetriever,
        @Assisted StructureActionCause cause,
        @Assisted IMessageable messageReceiver,
        @Assisted @Nullable IPlayer responsible,
        @Assisted @Nullable Double time,
        @Assisted("skipAnimation") boolean skipAnimation,
        @Assisted("preventPerpetualMovement") boolean preventPerpetualMovement,
        @Assisted StructureActionType actionType,
        @Assisted AnimationType animationType,
        ILocalizer localizer,
        ITextFactory textFactory,
        StructureActivityManager structureActivityManager,
        IPlayerFactory playerFactory,
        IExecutor executor)
    {
        this.structureRetriever = structureRetriever;
        this.cause = cause;
        this.messageReceiver = messageReceiver;
        this.responsible = responsible;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.preventPerpetualMovement = preventPerpetualMovement;
        this.actionType = actionType;
        this.animationType = animationType;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.structureActivityManager = structureActivityManager;
        this.playerFactory = playerFactory;
        this.executor = executor;
    }

    /**
     * Executes the toggle request.
     *
     * @return The result of the request.
     */
    public CompletableFuture<StructureToggleResult> execute()
    {
        log.atDebug().log("Executing toggle request: %s", this);
        return structureRetriever
            .getStructure()
            .thenCompose(this::execute)
            .withExceptionContext(() -> "Execute structure animation request: " + this);
    }

    private CompletableFuture<StructureToggleResult> execute(Optional<Structure> structureOpt)
    {
        if (structureOpt.isEmpty())
        {
            log.atInfo().log("Toggle failure (no structure found): %s", this);
            return CompletableFuture.completedFuture(StructureToggleResult.ERROR);
        }
        final Structure structure = structureOpt.get();
        final IPlayer actualResponsible = getActualResponsible(structure);
        verifyValidity(actualResponsible);

        if (!isValidActionType(structure))
            return CompletableFuture.completedFuture(StructureToggleResult.MISSING_REQUIRED_PROPERTY_OPEN_STATUS);

        return structure.toggle(this, actualResponsible);
    }

    /**
     * Verifies that the selected action type is valid for the provided structure.
     * <p>
     * If the action type is invalid, an error message will be sent to the message receiver.
     * <p>
     * The action is invalid if the structure does not have an open status, but the action type is open or close.
     *
     * @param structure
     *     The structure for which to verify the action type.
     * @return True if the action type is valid, false otherwise.
     */
    boolean isValidActionType(Structure structure)
    {
        if (actionType == StructureActionType.TOGGLE)
            return true;

        if (structure.hasProperty(Property.OPEN_STATUS))
            return true;

        final String errorKey = actionType == StructureActionType.OPEN
            ? "structure_action.open.error.type_has_no_open_status"
            : "structure_action.close.error.type_has_no_open_status";

        messageReceiver.sendError(
            errorKey,
            arg -> arg.localizedHighlight(structure.getType()),
            arg -> arg.highlight(structure.getName())
        );

        return false;
    }

    private void verifyValidity(IPlayer actualResponsible)
    {
        if (this.animationType == AnimationType.PREVIEW && !actualResponsible.isOnline())
            throw new IllegalStateException("Trying to show preview to offline player: " + actualResponsible);
    }

    /**
     * Gets the player responsible for this toggle. When {@link #responsible} is provided, this will be the responsible
     * player.
     * <p>
     * If {@link #responsible} is null, the prime owner will be used as responsible player.
     *
     * @param structure
     *     The structure for which to find the responsible player.
     * @return The player responsible for toggling the structure.
     */
    private IPlayer getActualResponsible(Structure structure)
    {
        if (responsible != null)
            return responsible;
        return playerFactory.create(structure.getPrimeOwner().playerData());
    }

    /**
     * The factory class for {@link StructureAnimationRequest} instances.
     */
    @AssistedFactory
    public interface IFactory
    {
        /**
         * Creates a new {@link StructureAnimationRequest}.
         * <p>
         * Once created, use {@link StructureAnimationRequest#execute()} to submit the request.
         *
         * @param structureRetriever
         *     A retriever for the structure for which this toggle request will be created.
         *     <p>
         *     Note that the retriever may only specify a single structure. See
         *     {@link StructureRetriever#getStructure()}.
         * @param cause
         *     The cause of the movement.
         * @param messageReceiver
         *     The receiver for all messages related to the toggle. When no player is available, this should usually be
         *     the server (See {@link IAnimatedArchitecturePlatform#getServer()}).
         * @param time
         *     The duration of the animation in seconds. May be null to use the default time.
         * @param skipAnimation
         *     True to skip the animation and move the blocks to their new locations immediately.
         * @param preventPerpetualMovement
         *     True to prevent perpetual movement. When perpetual movement is requested but denied via this setting, the
         *     animation will still be time-limited.
         * @param responsible
         *     The player responsible for the movement.
         * @param animationType
         *     The type of animation to apply.
         * @param actionType
         *     The type of movement to apply.
         * @return The new {@link AnimationRequestData}.
         */
        StructureAnimationRequest create(
            StructureRetriever structureRetriever,
            StructureActionCause cause,
            IMessageable messageReceiver,
            @Nullable IPlayer responsible,
            @Nullable Double time,
            @Assisted("skipAnimation") boolean skipAnimation,
            @Assisted("preventPerpetualMovement") boolean preventPerpetualMovement,
            StructureActionType actionType,
            AnimationType animationType);
    }
}
