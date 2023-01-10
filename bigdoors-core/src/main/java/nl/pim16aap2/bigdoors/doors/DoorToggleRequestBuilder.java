package nl.pim16aap2.bigdoors.doors;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Builder for {@link DoorToggleRequest} instances implemented as a guided builder.
 *
 * @author Pim
 */
public class DoorToggleRequestBuilder
{
    private final DoorToggleRequest.IFactory doorToggleRequestFactory;
    private final IMessageable messageableServer;
    private final IPPlayerFactory playerFactory;

    @Inject
    public DoorToggleRequestBuilder(
        DoorToggleRequest.IFactory doorToggleRequestFactory,
        @Named("MessageableServer") IMessageable messageableServer,
        IPPlayerFactory playerFactory)
    {
        this.doorToggleRequestFactory = doorToggleRequestFactory;
        this.messageableServer = messageableServer;
        this.playerFactory = playerFactory;
    }

    /**
     * Creates a new guided builder for a {@link DoorToggleRequest}.
     *
     * @return The first step of the guided builder.
     */
    public IBuilderDoor builder()
    {
        return new Builder(doorToggleRequestFactory, messageableServer, playerFactory);
    }

    @RequiredArgsConstructor
    public static final class Builder
        implements IBuilderDoor, IBuilderDoorActionCause, IBuilderDoorActionType, IBuilder
    {
        private final DoorToggleRequest.IFactory doorToggleRequestFactory;
        private final IMessageable messageableServer;
        private final IPPlayerFactory playerFactory;

        private DoorRetriever doorRetriever;
        private DoorActionCause doorActionCause;
        private DoorActionType doorActionType;

        private @Nullable IMessageable messageReceiver = null;
        private @Nullable IPPlayer responsible = null;
        private @Nullable Double time = null;
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
        public IBuilder responsible(@Nullable DoorOwner doorOwner)
        {
            if (doorOwner == null)
            {
                responsible = null;
                return this;
            }
            return responsible(doorOwner.pPlayerData());
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
        public IBuilder doorActionType(DoorActionType doorActionType)
        {
            this.doorActionType = doorActionType;
            return this;
        }

        @Override
        @Initializer
        public IBuilderDoorActionType doorActionCause(DoorActionCause doorActionCause)
        {
            this.doorActionCause = doorActionCause;
            return this;
        }

        @Override
        @Initializer
        public IBuilderDoorActionCause door(DoorRetriever doorRetriever)
        {
            this.doorRetriever = doorRetriever;
            return this;
        }

        @Override
        public DoorToggleRequest build()
        {
            updateMessageReceiver();
            return doorToggleRequestFactory.create(doorRetriever, doorActionCause,
                                                   Util.requireNonNull(messageReceiver, "MessageReceiver"),
                                                   responsible, time, skipAnimation, doorActionType);
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

            if (doorActionCause == DoorActionCause.PLAYER)
                //noinspection ConstantConditions
                messageReceiver = Objects.requireNonNull(
                    responsible, "Responsible player must be set when the door action " + "is caused by a player!");
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
         *     of the door.
         *     <p>
         *     A value of null (default) allows the door to figure out the time on its own.
         * @return The next step of the guided builder process.
         */
        IBuilder time(@Nullable Double time);

        /**
         * Optional: Skips the animation.
         * <p>
         * If this method is called, the animation will be skipped, and the blocks will be moved from their old
         * positions to the new ones immediately. This allows toggling doors without spawning any entities.
         * <p>
         * This is useful for when there aren't any players nearby to witness the animation anyway, as opening the door
         * without playing the animation avoids the heavy performance impact of spawning and animating entities.
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
         * default to the prime owner of the door.
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
        IBuilder responsible(@Nullable DoorOwner doorOwner);

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
         * Constructs the new door toggle request.
         *
         * @return The new {@link DoorToggleRequest}.
         */
        DoorToggleRequest build();
    }

    public interface IBuilderDoorActionType
    {
        /**
         * Sets the type of action to request of the door.
         *
         * @param doorActionType
         *     The type of action to request.
         * @return The next step of the guided builder process.
         */
        IBuilder doorActionType(DoorActionType doorActionType);
    }

    public interface IBuilderDoorActionCause
    {
        /**
         * Sets what caused this toggle request.
         *
         * @param doorActionCause
         *     The cause of the toggle request.
         * @return The next step of the guided builder process.
         */
        IBuilderDoorActionType doorActionCause(DoorActionCause doorActionCause);
    }

    public interface IBuilderDoor
    {
        /**
         * Sets the target door to toggle.
         *
         * @param doorRetriever
         *     A retriever for the door to toggle.
         * @return The next step of the guided builder process.
         */
        IBuilderDoorActionCause door(DoorRetriever doorRetriever);

        /**
         * Sets the target door to toggle.
         *
         * @param door
         *     The door to toggle.
         * @return The next step of the guided builder process.
         */
        default IBuilderDoorActionCause door(AbstractDoor door)
        {
            return door(DoorRetrieverFactory.ofDoor(door));
        }

        /**
         * Sets the target door to toggle.
         *
         * @param door
         *     The door to toggle.
         * @return The next step of the guided builder process.
         */
        default IBuilderDoorActionCause door(CompletableFuture<Optional<AbstractDoor>> door)
        {
            return door(DoorRetrieverFactory.ofDoor(door));
        }
    }
}
