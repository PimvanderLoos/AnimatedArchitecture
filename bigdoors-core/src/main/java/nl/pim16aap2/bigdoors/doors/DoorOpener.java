package nl.pim16aap2.bigdoors.doors;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
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
    @NotNull
    public static DoorOpener get()
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
    @NotNull
    public CompletableFuture<DoorToggleResult> animateDoorAsync(
        final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor, final @NotNull DoorActionCause cause,
        final @NotNull IMessageable messageReceiver, final @NotNull IPPlayer responsible, final double time,
        final boolean skipAnimation, final @NotNull DoorActionType doorActionType)
    {
        return futureDoor.thenApply(
            optionalDoor -> optionalDoor.map(
                door -> animateDoorSendToMainThread(door, cause, messageReceiver, responsible, time, skipAnimation,
                                                    doorActionType))
                                        .orElse(DoorToggleResult.ERROR));
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
    @NotNull
    public DoorToggleResult animateDoorAsync(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                             final @Nullable IPPlayer responsible, final double time,
                                             final boolean skipAnimation, final @NotNull DoorActionType doorActionType)
    {
        IMessageable messageReceiver = responsible == null || cause != DoorActionCause.PLAYER ?
                                       BigDoors.get().getPlatform().getMessageableServer() : responsible;
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
    @SneakyThrows // FIXME: DO NOT DO THIS!
    @NotNull
    public DoorToggleResult animateDoorAsync(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                             final @NotNull IMessageable messageReceiver,
                                             final @Nullable IPPlayer responsible, final double time,
                                             final boolean skipAnimation, final @NotNull DoorActionType doorActionType)
    {
        if (responsible == null)
            return DatabaseManager.get().getPrimeOwner(door.getDoorUID()).thenApply(
                (optionalDoorOwner) ->
                {
                    if (!optionalDoorOwner.isPresent())
                    {
                        PLogger.get().logMessage("Failed to obtain prime owner of door: " + door.getDoorUID());
                        return DoorToggleResult.ERROR;
                    }
                    return animateDoorSendToMainThread(door, cause, messageReceiver,
                                                       optionalDoorOwner.get().getPlayer(), time, skipAnimation,
                                                       doorActionType);
                }).get(); // FIXME: NO!
        else
            return animateDoorSendToMainThread(door, cause, messageReceiver, responsible, time, skipAnimation,
                                               doorActionType);
    }

    /**
     * Attempts to toggle, open, or close a door. Must only be called from the main thread!
     *
     * @param door            The door.
     * @param cause           What caused this action.
     * @param messageReceiver Who will receive any messages that have to be sent.
     * @param responsible     Who is responsible for this door. Either the player who directly toggled it (via a command
     *                        or the GUI), or the original creator when this data is not available.
     * @param time            The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum
     *                        speed is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation   If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @return The result of the attempt.
     */
    @NotNull
    private DoorToggleResult animateDoorSync(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                             final @NotNull IMessageable messageReceiver,
                                             final @NotNull IPPlayer responsible, final double time,
                                             boolean skipAnimation, final @NotNull DoorActionType doorActionType)
    {
        if (!BigDoors.get().getPlatform().isMainThread(Thread.currentThread().getId()))
        {
            PLogger.get()
                   .logException(new IllegalThreadStateException("Doors can only be animated on the main thread!"));
            return DoorToggleResult.ERROR;
        }

        return door.toggle(cause, messageReceiver, responsible, time, skipAnimation, doorActionType);
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
    @SneakyThrows // FIXME: DO NOT DO THIS!
    private DoorToggleResult animateDoorSendToMainThread(final @NotNull AbstractDoorBase door,
                                                         final @NotNull DoorActionCause cause,
                                                         final @NotNull IMessageable messageReceiver,
                                                         final @NotNull IPPlayer responsible,
                                                         final double time, boolean skipAnimation,
                                                         final @NotNull DoorActionType doorActionType)
    {
        IPExecutor<DoorToggleResult> mainThreadExecutor = BigDoors.get().getPlatform().newPExecutor();

        DoorToggleResult result = mainThreadExecutor
            .supplyOnMainThread(() -> animateDoorSync(door, cause, messageReceiver, responsible, time,
                                                      skipAnimation, doorActionType)).get(); // FIXME: NO!
        return result;
    }
}
