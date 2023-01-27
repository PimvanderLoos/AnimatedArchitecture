package nl.pim16aap2.bigdoors.movable;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Builder for {@link MovableToggleRequest} instances implemented as a guided builder.
 *
 * @author Pim
 */
public class MovableToggleRequestBuilder
{
    private final MovableToggleRequest.IFactory movableToggleRequestFactory;
    private final IMessageable messageableServer;
    private final IPPlayerFactory playerFactory;

    @Inject
    public MovableToggleRequestBuilder(
        MovableToggleRequest.IFactory movableToggleRequestFactory,
        @Named("MessageableServer") IMessageable messageableServer,
        IPPlayerFactory playerFactory)
    {
        this.movableToggleRequestFactory = movableToggleRequestFactory;
        this.messageableServer = messageableServer;
        this.playerFactory = playerFactory;
    }

    /**
     * Creates a new guided builder for a {@link MovableToggleRequest}.
     *
     * @return The first step of the guided builder.
     */
    public IBuilderMovable builder()
    {
        return new Builder(movableToggleRequestFactory, messageableServer, playerFactory);
    }

    @RequiredArgsConstructor
    public static final class Builder
        implements IBuilderMovable, IBuilderMovableActionCause, IBuilderMovableActionType, IBuilder
    {
        private final MovableToggleRequest.IFactory movableToggleRequestFactory;
        private final IMessageable messageableServer;
        private final IPPlayerFactory playerFactory;

        private MovableRetriever movableRetriever;
        private MovableActionCause movableActionCause;
        private MovableActionType movableActionType;

        private @Nullable IMessageable messageReceiver = null;
        private @Nullable IPPlayer responsible = null;
        private @Nullable Double time = null;
        private @Nullable AnimationType animationType = null;
        private boolean skipAnimation = false;

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
        public IBuilder responsible(@Nullable IPPlayer responsible)
        {
            this.responsible = responsible;
            return this;
        }

        @Override
        public IBuilder responsible(@Nullable PPlayerData playerData)
        {
            if (playerData == null)
            {
                responsible = null;
                return this;
            }
            return responsible(playerFactory.create(playerData));
        }

        @Override
        public IBuilder responsible(@Nullable MovableOwner movableOwner)
        {
            if (movableOwner == null)
            {
                responsible = null;
                return this;
            }
            return responsible(movableOwner.pPlayerData());
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
        public IBuilder movableActionType(MovableActionType movableActionType)
        {
            this.movableActionType = movableActionType;
            return this;
        }

        @Override
        @Initializer
        public IBuilderMovableActionType movableActionCause(MovableActionCause movableActionCause)
        {
            this.movableActionCause = movableActionCause;
            return this;
        }

        @Override
        @Initializer
        public IBuilderMovableActionCause movable(MovableRetriever movableRetriever)
        {
            this.movableRetriever = movableRetriever;
            return this;
        }

        @Override
        public IBuilder animationType(AnimationType animationType)
        {
            this.animationType = animationType;
            return this;
        }

        @Override
        public MovableToggleRequest build()
        {
            updateMessageReceiver();
            return movableToggleRequestFactory.create(
                movableRetriever, movableActionCause,
                Util.requireNonNull(messageReceiver, "MessageReceiver"),
                responsible, time, skipAnimation, movableActionType,
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

            if (movableActionCause == MovableActionCause.PLAYER)
                //noinspection ConstantConditions
                messageReceiver = Objects.requireNonNull(
                    responsible, "Responsible player must be set when the movable action " + "is caused by a player!");
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
         *     of the movable.
         *     <p>
         *     A value of null (default) allows the movable to figure out the time on its own.
         * @return The next step of the guided builder process.
         */
        IBuilder time(@Nullable Double time);

        /**
         * Optional: Skips the animation.
         * <p>
         * If this method is called, the animation will be skipped, and the blocks will be moved from their old
         * positions to the new ones immediately. This allows toggling movables without spawning any entities.
         * <p>
         * This is useful for when there aren't any players nearby to witness the animation anyway, as opening the
         * movable without playing the animation avoids the heavy performance impact of spawning and animating
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
         * Sets the player responsible for the toggle. When the toggle was not caused by a player this value is
         * optional.
         * <p>
         * However, this value is required if the toggle was caused by a player.
         * <p>
         * When the toggle request was not caused by a player (e.g. via redstone) and this value is not set, it will
         * default to the prime owner of the movable.
         *
         * @param responsible
         *     The player responsible for the toggle request. The responsible player's build-permissions are used for
         *     things like checking access to certain areas.
         * @return The next step of the guided builder process.
         */
        IBuilder responsible(@Nullable IPPlayer responsible);

        /**
         * See {@link #responsible(IPPlayer)}.
         */
        IBuilder responsible(@Nullable PPlayerData playerData);

        /**
         * See {@link #responsible(IPPlayer)}.
         */
        IBuilder responsible(@Nullable MovableOwner movableOwnerOwner);

        /**
         * Optional: Sets the message receiver of the toggle request. The message receiver will receive all messages
         * related to the toggle request.
         * <p>
         * When the toggle request is caused by a player, this defaults to that player. In all other situations, it
         * defaults to the server.
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
         * Constructs the new movable toggle request.
         *
         * @return The new {@link MovableToggleRequest}.
         */
        MovableToggleRequest build();
    }

    public interface IBuilderMovableActionType
    {
        /**
         * Sets the type of action to request of the movable.
         *
         * @param movableActionType
         *     The type of action to request.
         * @return The next step of the guided builder process.
         */
        IBuilder movableActionType(MovableActionType movableActionType);
    }

    public interface IBuilderMovableActionCause
    {
        /**
         * Sets what caused this toggle request.
         *
         * @param movableActionCause
         *     The cause of the toggle request.
         * @return The next step of the guided builder process.
         */
        IBuilderMovableActionType movableActionCause(MovableActionCause movableActionCause);
    }

    public interface IBuilderMovable
    {
        /**
         * Sets the target movable to toggle.
         *
         * @param movableRetriever
         *     A retriever for the movable to toggle.
         * @return The next step of the guided builder process.
         */
        IBuilderMovableActionCause movable(MovableRetriever movableRetriever);

        /**
         * Sets the target movable to toggle.
         *
         * @param movable
         *     The movable to toggle.
         * @return The next step of the guided builder process.
         */
        default IBuilderMovableActionCause movable(AbstractMovable movable)
        {
            return movable(MovableRetrieverFactory.ofMovable(movable));
        }

        /**
         * Sets the target movable to toggle.
         *
         * @param movable
         *     The movable to toggle.
         * @return The next step of the guided builder process.
         */
        default IBuilderMovableActionCause movable(CompletableFuture<Optional<AbstractMovable>> movable)
        {
            return movable(MovableRetrieverFactory.ofMovable(movable));
        }
    }
}
