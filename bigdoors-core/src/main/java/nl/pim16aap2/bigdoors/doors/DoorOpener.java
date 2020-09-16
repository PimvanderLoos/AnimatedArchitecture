package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the class that can open, close, and toggle doors.
 *
 * @author Pim
 */
public final class DoorOpener
{
    private static final DoorOpener instance = new DoorOpener();

    private DoorOpener()
    {
    }

    /**
     * Gets the instance of the {@link DoorOpener} if it exists.
     *
     * @return The instance of the {@link DoorOpener}.
     */
    public static @NotNull DoorOpener get()
    {
        return instance;
    }

    /**
     * Toggles, opens, or closes a door. Can be called (a)synchronously.
     *
     * @param futureDoor      The door to toggle.
     * @param cause           What caused this action.
     * @param messageReceiver Who will receive any messages that have to be sent.
     * @param responsible     Who is responsible for this door. Either the player who directly toggled it (via a command
     *                        or the GUI), or the original creator when this data is not available.
     * @param time            The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum
     *                        speed is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation   If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param doorActionType  Whether the door should be toggled, opened, or closed.
     * @return The future result of the toggle (will be available before the door starts its animation).
     */
    public @NotNull CompletableFuture<DoorToggleResult> animateDoorAsync(
        final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor, final @NotNull DoorActionCause cause,
        final @NotNull IMessageable messageReceiver, @Nullable IPPlayer responsible, final double time,
        final boolean skipAnimation, final @NotNull DoorActionType doorActionType)
    {
        return futureDoor.thenCompose(
            doorOpt ->
            {
                if (!doorOpt.isPresent())
                {
                    PLogger.get().logThrowable(new NullPointerException("Received empty Optional in toggle request!"));
                    return CompletableFuture.completedFuture(DoorToggleResult.ERROR);
                }
                final @NotNull IPPlayer finalResponsible = responsible == null ?
                                                           doorOpt.get().getPrimeOwner().getPlayer() : responsible;

                return animateDoor(doorOpt.get(), cause, messageReceiver, finalResponsible,
                                   time, skipAnimation, doorActionType);
            });
    }

    /**
     * Toggles, opens, or closes a door. Can be called (a)synchronously.
     *
     * @param futureDoor     The door to toggle.
     * @param cause          What caused this action.
     * @param responsible    Who is responsible for this door. Either the player who directly toggled it (via a command
     *                       or the GUI), or the original creator when this data is not available.
     * @param time           The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum speed
     *                       is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation  If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param doorActionType Whether the door should be toggled, opened, or closed.
     * @return The future result of the toggle (will be available before the door starts its animation).
     */
    public @NotNull CompletableFuture<DoorToggleResult> animateDoorAsync(
        final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor, final @NotNull DoorActionCause cause,
        final @Nullable IPPlayer responsible, final double time, final boolean skipAnimation,
        final @NotNull DoorActionType doorActionType)
    {
        final @NotNull IMessageable messageReceiver = getMessageReceiver(responsible, cause);
        return animateDoorAsync(futureDoor, cause, messageReceiver, responsible, time, skipAnimation, doorActionType);
    }

    /**
     * Figures out who should receive all communications about potential issues etc. If there is no responsible player,
     * or if the player did not cause it, that will be the server. Otherwise, it'll be the player themselves.
     *
     * @param responsible The player responsible for this door.
     * @param cause       The cause of the toggle.
     * @return The object that will receive all future messages regarding this toggle.
     */
    private @NotNull IMessageable getMessageReceiver(final @Nullable IPPlayer responsible,
                                                     final @NotNull DoorActionCause cause)
    {
        return responsible == null || cause != DoorActionCause.PLAYER ?
               BigDoors.get().getPlatform().getMessageableServer() : responsible;
    }

    /**
     * Toggles, opens, or closes a door. Can be called (a)synchronously.
     *
     * @param door           The door to toggle.
     * @param cause          What caused this action.
     * @param responsible    Who is responsible for this door. Either the player who directly toggled it (via a command
     *                       or the GUI), or the original creator when this data is not available.
     * @param time           The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum speed
     *                       is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation  If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param doorActionType Whether the door should be toggled, opened, or closed.
     * @return The result of the toggle.
     */
    public @NotNull CompletableFuture<DoorToggleResult> animateDoorAsync(final @NotNull AbstractDoorBase door,
                                                                         final @NotNull DoorActionCause cause,
                                                                         final @Nullable IPPlayer responsible,
                                                                         final double time,
                                                                         final boolean skipAnimation,
                                                                         final @NotNull DoorActionType doorActionType)
    {
        final @NotNull IMessageable messageReceiver = getMessageReceiver(responsible, cause);
        return animateDoorAsync(door, cause, messageReceiver, responsible, time, skipAnimation, doorActionType);
    }

    /**
     * Toggles, opens, or closes a door. Can be called (a)synchronously.
     *
     * @param door            The door to toggle.
     * @param cause           What caused this action.
     * @param messageReceiver The object that will receive any messages that may be sent.
     * @param responsible     Who is responsible for this door. Either the player who directly toggled it (via a command
     *                        or the GUI), or the original creator when this data is not available.
     * @param time            The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum
     *                        speed is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation   If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param doorActionType  Whether the door should be toggled, opened, or closed.
     * @return The result of the toggle.
     */
    public @NotNull CompletableFuture<DoorToggleResult> animateDoorAsync(final @NotNull AbstractDoorBase door,
                                                                         final @NotNull DoorActionCause cause,
                                                                         final @NotNull IMessageable messageReceiver,
                                                                         @Nullable IPPlayer responsible,
                                                                         final double time,
                                                                         final boolean skipAnimation,
                                                                         final @NotNull DoorActionType doorActionType)
    {
        if (responsible == null)
            responsible = door.getPrimeOwner().getPlayer();

        return animateDoor(door, cause, messageReceiver, responsible, time, skipAnimation, doorActionType);
    }

    /**
     * Initiates a door animation on the main thread. May be called from any thread.
     *
     * @param door            The door.
     * @param cause           What caused this action.
     * @param messageReceiver Who will receive any messages that have to be sent.
     * @param responsible     Who is responsible for this door. Either the player who directly toggled it (via a command
     *                        or the GUI), or the original creator when this data is not available.
     * @param time            The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum
     *                        speed is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation   If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param doorActionType  Whether the door should be toggled, opened, or closed.
     * @return The result of the animation attempt.
     */
    private @NotNull CompletableFuture<DoorToggleResult> animateDoor(final @NotNull AbstractDoorBase door,
                                                                     final @NotNull DoorActionCause cause,
                                                                     final @NotNull IMessageable messageReceiver,
                                                                     final @NotNull IPPlayer responsible,
                                                                     final double time, boolean skipAnimation,
                                                                     final @NotNull DoorActionType doorActionType)
    {
        if (!BigDoors.get().getPlatform().isMainThread(Thread.currentThread().getId()))
            return CompletableFuture.completedFuture(door.toggle(cause, messageReceiver, responsible,
                                                                 time, skipAnimation, doorActionType));

        final @NotNull IPExecutor<DoorToggleResult> pExecutor = BigDoors.get().getPlatform().newPExecutor();
        return pExecutor.supplyOnMainThread(() -> door.toggle(cause, messageReceiver, responsible,
                                                              time, skipAnimation, doorActionType));
    }
}
