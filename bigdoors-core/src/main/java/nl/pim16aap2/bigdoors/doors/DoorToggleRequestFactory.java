package nl.pim16aap2.bigdoors.doors;

import lombok.RequiredArgsConstructor;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Factory for {@link DoorToggleRequest} instances using a guided builder.
 *
 * @author Pim
 */
public class DoorToggleRequestFactory
{
    private final DoorToggleRequest.Factory doorToggleRequestFactory;
    private final IMessageable messageableServer;

    @Inject //
    DoorToggleRequestFactory(DoorToggleRequest.Factory doorToggleRequestFactory,
                             @Named("MessageableServer") IMessageable messageableServer)
    {
        this.doorToggleRequestFactory = doorToggleRequestFactory;
        this.messageableServer = messageableServer;
    }

    /**
     * Creates a new guided builder for a {@link DoorToggleRequest}.
     *
     * @return The first step of the guided builder.
     */
    public IBuilderDoor build()
    {
        return new Builder(doorToggleRequestFactory, messageableServer);
    }

    @RequiredArgsConstructor
    private static final class Builder
        implements IBuilderDoor, IBuilderDoorActionCause, IBuilderDoorActionType, IBuilder
    {
        private final DoorToggleRequest.Factory doorToggleRequestFactory;
        private final IMessageable messageableServer;

        private DoorRetriever.AbstractRetriever doorRetriever;
        private DoorActionCause doorActionCause;
        private DoorActionType doorActionType;

        private @Nullable IMessageable messageReceiver = null;
        private @Nullable IPPlayer responsible = null;
        private double time = 0D;
        private boolean skipAnimation = false;

        @Override
        public IBuilder time(double time)
        {
            this.time = time;
            return this;
        }

        @Override
        public IBuilder skipAnimation()
        {
            skipAnimation = true;
            return this;
        }

        @Override
        public IBuilder responsible(IPPlayer responsible)
        {
            this.responsible = responsible;
            return this;
        }

        @Override
        public IBuilder messageReceiver(IMessageable messageReceiver)
        {
            this.messageReceiver = messageReceiver;
            return this;
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
        public IBuilderDoorActionCause door(DoorRetriever.AbstractRetriever doorRetriever)
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
         *     A value of 0 (default) allows the mover to figure out the time on its own.
         * @return The next step of the guided builder process.
         */
        IBuilder time(double time);

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
        IBuilder skipAnimation();

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
        IBuilder responsible(IPPlayer responsible);

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
        IBuilderDoorActionCause door(DoorRetriever.AbstractRetriever doorRetriever);

        /**
         * Sets the target door to toggle.
         *
         * @param door
         *     The door to toggle.
         * @return The next step of the guided builder process.
         */
        default IBuilderDoorActionCause door(AbstractDoor door)
        {
            return door(DoorRetriever.ofDoor(door));
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
            return door(DoorRetriever.ofDoor(door));
        }
    }
}
