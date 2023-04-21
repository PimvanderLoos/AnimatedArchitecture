package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Builder for {@link StructureAnimationRequest} instances implemented as a guided builder.
 * <p>
 * You can obtain an instance of this class using
 * {@link IAnimatedArchitecturePlatform#getStructureAnimationRequestBuilder()}.
 * <p>
 * Example usage:
 * <pre>{@code
 * structureAnimationRequestBuilder
 *     .builder()
 *     .structure(structure) // Structures or their retrievers both work
 *     .structureActionCause(StructureActionCause.PLUGIN)
 *     .structureActionType(StructureActionType.TOGGLE)
 *     .responsible(player)
 *     .messageReceiver(player)
 *     .build()
 *     .execute()
 *     .thenAccept(structureToggleResult -> ...)
 *     .exceptionally(throwable -> ...);
 * }</pre>
 * <p>
 * The builder is implemented as a guided builder, which means that you can only call methods in a specific order until
 * all required data is available. After that, there are additional options to specify optional data, such as the time
 * the animation should take or the animation type.
 * <p>
 * One thing to note is that when the animation cause is set to {@link StructureActionCause#PLAYER}, the request
 * requires the responsible player to be set using the otherwise-optional {@link IBuilder#responsible(IPlayer)} method.
 * <p>
 * Even when providing a responsible player, the builder allows you to specify a message receiver. This is useful when a
 * player is responsible for the animation, but any messages should be sent elsewhere. For example, you can ensure all
 * messages are only sent to the server console by using {@link IBuilder#messageReceiverServer()}.
 */
public class StructureAnimationRequestBuilder
{
    private final StructureAnimationRequest.IFactory structureToggleRequestFactory;
    private final IMessageable messageableServer;
    private final IPlayerFactory playerFactory;
    private final IConfig config;

    @Inject
    public StructureAnimationRequestBuilder(
        StructureAnimationRequest.IFactory structureToggleRequestFactory,
        @Named("MessageableServer") IMessageable messageableServer,
        IPlayerFactory playerFactory, IConfig config)
    {
        this.structureToggleRequestFactory = structureToggleRequestFactory;
        this.messageableServer = messageableServer;
        this.playerFactory = playerFactory;
        this.config = config;
    }

    /**
     * Creates a new guided builder for a {@link StructureAnimationRequest}.
     *
     * @return The first step of the guided builder.
     */
    public IBuilderStructure builder()
    {
        return new Builder(structureToggleRequestFactory, messageableServer, playerFactory, config);
    }

    @RequiredArgsConstructor
    public static final class Builder
        implements IBuilderStructure, IBuilderStructureActionCause, IBuilderStructureActionType, IBuilder
    {
        private final StructureAnimationRequest.IFactory structureToggleRequestFactory;
        private final IMessageable messageableServer;
        private final IPlayerFactory playerFactory;
        private final IConfig config;

        private StructureRetriever structureRetriever;
        private StructureActionCause structureActionCause;
        private StructureActionType structureActionType;

        private @Nullable IMessageable messageReceiver = null;
        private @Nullable IPlayer responsible = null;
        private @Nullable Double time = null;
        private @Nullable AnimationType animationType = null;
        private boolean skipAnimation = false;
        private boolean preventPerpetualMovement = false;

        @Override
        public IBuilder time(@Nullable Double time)
        {
            this.time = time;
            return this;
        }

        @Override
        public IBuilder skipAnimation(boolean skip)
        {
            skipAnimation = skip;
            return this;
        }

        @Override
        public IBuilder preventPerpetualMovement(boolean preventPerpetualMovement)
        {
            this.preventPerpetualMovement = preventPerpetualMovement;
            return this;
        }

        @Override
        public IBuilder responsible(@Nullable IPlayer responsible)
        {
            this.responsible = responsible;
            return this;
        }

        @Override
        public IBuilder responsible(@Nullable PlayerData playerData)
        {
            if (playerData == null)
            {
                responsible = null;
                return this;
            }
            return responsible(playerFactory.create(playerData));
        }

        @Override
        public IBuilder responsible(@Nullable StructureOwner structureOwner)
        {
            if (structureOwner == null)
            {
                responsible = null;
                return this;
            }
            return responsible(structureOwner.playerData());
        }

        @Override
        public IBuilder messageReceiver(IMessageable messageReceiver)
        {
            this.messageReceiver = messageReceiver;
            return this;
        }

        @Override
        public IBuilder messageReceiverServer()
        {
            return messageReceiver(messageableServer);
        }

        @Override
        @Initializer
        public IBuilder structureActionType(StructureActionType structureActionType)
        {
            this.structureActionType = structureActionType;
            return this;
        }

        @Override
        @Initializer
        public IBuilderStructureActionType structureActionCause(StructureActionCause structureActionCause)
        {
            this.structureActionCause = structureActionCause;
            return this;
        }

        @Override
        @Initializer
        public IBuilderStructureActionCause structure(StructureRetriever structureRetriever)
        {
            this.structureRetriever = structureRetriever;
            return this;
        }

        @Override
        public IBuilder animationType(AnimationType animationType)
        {
            this.animationType = animationType;
            return this;
        }

        private void verify()
        {
            if (structureActionCause == StructureActionCause.REDSTONE && !config.isRedstoneEnabled())
                throw new IllegalStateException("Trying to execute redstone toggle while redstone is disabled!");
            if (structureActionCause == StructureActionCause.PLAYER && responsible == null)
                throw new IllegalStateException("Trying to execute player toggle without a responsible player!");
        }

        @Override
        public StructureAnimationRequest build()
        {
            verify();
            updateMessageReceiver();

            return structureToggleRequestFactory.create(
                structureRetriever, structureActionCause,
                Util.requireNonNull(messageReceiver, "MessageReceiver"),
                responsible, time, skipAnimation, preventPerpetualMovement, structureActionType,
                Objects.requireNonNullElse(animationType, AnimationType.MOVE_BLOCKS));
        }

        /**
         * Figures out who should receive all communications about potential issues etc. If there is no responsible
         * player, or if the player did not cause it, that will be the server. Otherwise, it'll be the player
         * themselves.
         */
        private void updateMessageReceiver()
        {
            // If the message receiver was set explicitly, we don't want to override it.
            if (messageReceiver != null)
                return;

            if (structureActionCause == StructureActionCause.PLAYER)
                //noinspection ConstantConditions
                messageReceiver = Objects.requireNonNull(
                    responsible,
                    "Responsible player must be set when the structure action is caused by a player!");
            else
                messageReceiver = messageableServer;
        }
    }

    public interface IBuilder
    {
        /**
         * Optional: Sets the time value of the toggle request.
         *
         * @param time
         *     The duration (in seconds) of the toggle animation. Because the maximum speed of the animated blocks is
         *     limited, there is a lower bound limit of this value. The exact limit depends on the type and dimensions
         *     of the structure.
         *     <p>
         *     A value of null (default) allows the structure to figure out the time on its own.
         * @return The next step of the guided builder process.
         */
        IBuilder time(@Nullable Double time);

        /**
         * Optional: Skips the animation.
         * <p>
         * If this method is called, the animation will be skipped, and the blocks will be moved from their old
         * positions to the new ones immediately. This allows toggling structures without spawning any entities.
         * <p>
         * This is useful for when there aren't any players nearby to witness the animation anyway, as opening the
         * structure without playing the animation avoids the heavy performance impact of spawning and animating
         * entities.
         *
         * @return The next step of the guided builder process.
         */
        IBuilder skipAnimation(boolean skip);

        /**
         * See {@link #skipAnimation(boolean)}.
         */
        default IBuilder skipAnimation()
        {
            return skipAnimation(true);
        }

        /**
         * Sets if perpetual movement should be blocked. When perpetual movement is requested but denied via this
         * setting, the animation will still be time-limited.
         *
         * @param preventPerpetualMovement
         *     True to prevent any perpetual movement.
         * @return The next step of the guided builder process.
         */
        IBuilder preventPerpetualMovement(boolean preventPerpetualMovement);

        /**
         * Prevents perpetual movement. When perpetual movement is requested but denied via this setting, the animation
         * will still be time-limited.
         *
         * @return The next step of the guided builder process.
         */
        default IBuilder preventPerpetualMovement()
        {
            return preventPerpetualMovement(true);
        }

        /**
         * Sets the player responsible for the toggle. When the toggle was not caused by a player this value is
         * optional.
         * <p>
         * However, this value is required if the toggle was caused by a player.
         * <p>
         * When the toggle request was not caused by a player (e.g. via redstone) and this value is not set, it will
         * default to the prime owner of the structure.
         *
         * @param responsible
         *     The player responsible for the toggle request. The responsible player's build-permissions are used for
         *     things like checking access to certain areas.
         * @return The next step of the guided builder process.
         */
        IBuilder responsible(@Nullable IPlayer responsible);

        /**
         * See {@link #responsible(IPlayer)}.
         */
        IBuilder responsible(@Nullable PlayerData playerData);

        /**
         * See {@link #responsible(IPlayer)}.
         */
        IBuilder responsible(@Nullable StructureOwner structureOwnerOwner);

        /**
         * Optional: Sets the message receiver of the toggle request. The message receiver will receive all messages
         * related to the toggle request.
         * <p>
         * When the toggle request is caused by a player, this defaults to that player. In all other situations, it
         * defaults to the server (See {@link IAnimatedArchitecturePlatform#getServer()}).
         *
         * @param messageReceiver
         *     The message receiver for all messages related to the toggle request.
         * @return The next step of the guided builder process.
         */
        IBuilder messageReceiver(IMessageable messageReceiver);

        /**
         * See {@link #messageReceiver(IMessageable)}.
         * <p>
         * Sets the server to be the message receiver.
         */
        IBuilder messageReceiverServer();

        /**
         * Sets the animation type of the animation. Defaults to {@link AnimationType#MOVE_BLOCKS}.
         *
         * @param animationType
         *     The animation type to apply.
         * @return The next step of the guided builder process.
         */
        IBuilder animationType(AnimationType animationType);

        /**
         * Constructs the new structure toggle request.
         *
         * @return The new {@link StructureAnimationRequest}.
         */
        StructureAnimationRequest build();
    }

    public interface IBuilderStructureActionType
    {
        /**
         * Sets the type of action to request of the structure.
         *
         * @param structureActionType
         *     The type of action to request.
         * @return The next step of the guided builder process.
         */
        IBuilder structureActionType(StructureActionType structureActionType);
    }

    public interface IBuilderStructureActionCause
    {
        /**
         * Sets what caused this toggle request.
         *
         * @param structureActionCause
         *     The cause of the toggle request.
         * @return The next step of the guided builder process.
         */
        IBuilderStructureActionType structureActionCause(StructureActionCause structureActionCause);
    }

    public interface IBuilderStructure
    {
        /**
         * Sets the target structure to toggle.
         *
         * @param structureRetriever
         *     A retriever for the structure to toggle.
         * @return The next step of the guided builder process.
         */
        IBuilderStructureActionCause structure(StructureRetriever structureRetriever);

        /**
         * Sets the target structure to toggle.
         *
         * @param structure
         *     The structure to toggle.
         * @return The next step of the guided builder process.
         */
        default IBuilderStructureActionCause structure(AbstractStructure structure)
        {
            return structure(StructureRetrieverFactory.ofStructure(structure));
        }

        /**
         * Sets the target structure to toggle.
         *
         * @param structure
         *     The structure to toggle.
         * @return The next step of the guided builder process.
         */
        default IBuilderStructureActionCause structure(CompletableFuture<Optional<AbstractStructure>> structure)
        {
            return structure(StructureRetrieverFactory.ofStructure(structure));
        }
    }
}
