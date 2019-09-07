package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.events.IPCancellable;
import nl.pim16aap2.bigdoors.events.PEvent;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionEvent;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an action that is going to be applied to a door.
 *
 * @author Pim
 */
public class DoorActionEventSpigot extends Event implements PEvent, IPCancellable, DoorActionEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    /**
     * The UID of the door this action will be applied to.
     */
    private final CompletableFuture<Optional<DoorBase>> futureDoor;

    /**
     * What initiated this DoorAction event.
     */
    private final DoorActionCause cause;
    private final DoorActionType actionType;
    private final Optional<UUID> responsible;
    private boolean isCancelled = false;
    private final double time;
    private final boolean instantOpen;


    /**
     * Constructs a door action event.
     *
     * @param futureDoor  The door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param time        The number of seconds the door will take to open. Note that there are other factors that
     *                    affect the total time as well.
     * @param instantOpen If true, the door will skip the animation and open instantly.
     */
    public DoorActionEventSpigot(final @NotNull CompletableFuture<Optional<DoorBase>> futureDoor,
                                 final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                 final @Nullable UUID responsible, final double time, final boolean instantOpen)
    {
        super(true);
        this.futureDoor = futureDoor;
        this.cause = cause;
        this.actionType = actionType;
        this.responsible = Optional.ofNullable(responsible);
        this.time = time;
        this.instantOpen = instantOpen;
    }

    /**
     * Constructs a door action event.
     *
     * @param futureDoor  The door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param time        The number of seconds the door will take to open. Note that there are other factors that
     *                    affect the total time as well.
     */
    public DoorActionEventSpigot(final @NotNull CompletableFuture<Optional<DoorBase>> futureDoor,
                                 final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                 final @Nullable UUID responsible, final double time)
    {
        this(futureDoor, cause, actionType, responsible, time, false);
    }

    /**
     * Constructs a door action event.
     *
     * @param futureDoor  The door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param instantOpen If true, the door will skip the animation and open instantly.
     */
    public DoorActionEventSpigot(final @NotNull CompletableFuture<Optional<DoorBase>> futureDoor,
                                 final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                 final @Nullable UUID responsible, final boolean instantOpen)
    {
        this(futureDoor, cause, actionType, responsible, 0.0D, instantOpen);
    }

    /**
     * Constructs a door action event.
     *
     * @param futureDoor  The door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     */
    public DoorActionEventSpigot(final @NotNull CompletableFuture<Optional<DoorBase>> futureDoor,
                                 final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                 final @Nullable UUID responsible)
    {
        this(futureDoor, cause, actionType, responsible, 0.0D, false);
    }

    /**
     * Constructs a door action event.
     *
     * @param doorUID     The UID of the door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param time        The number of seconds the door will take to open. Note that there are other factors that
     *                    affect the total time as well.
     * @param instantOpen If true, the door will skip the animation and open instantly.
     */
    public DoorActionEventSpigot(final long doorUID, final @NotNull DoorActionCause cause,
                                 final @NotNull DoorActionType actionType, final @Nullable UUID responsible,
                                 final double time, final boolean instantOpen)
    {
        this(responsible == null ? DatabaseManager.get().getDoor(doorUID) :
             DatabaseManager.get().getDoor(responsible, doorUID), cause, actionType, responsible, time, instantOpen);
    }

    /**
     * Constructs a door action event.
     *
     * @param doorUID     The UID of the door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param time        The number of seconds the door will take to open. Note that there are other factors that
     *                    affect the total time as well.
     */
    public DoorActionEventSpigot(final long doorUID, final @NotNull DoorActionCause cause,
                                 final @NotNull DoorActionType actionType, final @Nullable UUID responsible,
                                 final double time)
    {
        this(doorUID, cause, actionType, responsible, time, false);
    }

    /**
     * Constructs a door action event.
     *
     * @param doorUID     The UID of the door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param instantOpen If true, the door will skip the animation and open instantly.
     */
    public DoorActionEventSpigot(final long doorUID, final @NotNull DoorActionCause cause,
                                 final @NotNull DoorActionType actionType, final @Nullable UUID responsible,
                                 final boolean instantOpen)
    {
        this(doorUID, cause, actionType, responsible, 0.0D, instantOpen);
    }

    /**
     * Constructs a door action event.
     *
     * @param doorUID     The UID of the door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     */
    public DoorActionEventSpigot(final long doorUID, final @NotNull DoorActionCause cause,
                                 final @NotNull DoorActionType actionType, final @Nullable UUID responsible)
    {
        this(doorUID, cause, actionType, responsible, 0.0D, false);
    }

    /**
     * {@inheritDoc}
     */
//    @Override
    @NotNull
    public CompletableFuture<Optional<DoorBase>> getFutureDoor()
    {
        return futureDoor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorActionCause getCause()
    {
        return cause;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UUID> getResponsible()
    {
        return responsible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorActionType getActionType()
    {
        return actionType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getInstantOpen()
    {
        return instantOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTime()
    {
        return time;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled()
    {
        return isCancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCancelled(boolean cancel)
    {
        isCancelled = cancel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
