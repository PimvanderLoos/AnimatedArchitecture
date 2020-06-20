package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleEnd;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an action that is going to be applied to a door.
 *
 * @author Pim
 */
public class DoorEventToggleEnd extends DoorToggleEvent implements IDoorEventToggleEnd
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    /**
     * Constructs a door action event.
     *
     * @param futureDoor    The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    public DoorEventToggleEnd(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                              final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                              final @Nullable IPPlayer responsible, final double time,
                              final boolean skipAnimation)
    {
        super(futureDoor, cause, actionType, responsible, time, skipAnimation);
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
    public DoorEventToggleEnd(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                              final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                              final @Nullable IPPlayer responsible, final double time)
    {
        this(futureDoor, cause, actionType, responsible, time, false);
    }

    /**
     * Constructs a door action event.
     *
     * @param futureDoor    The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    public DoorEventToggleEnd(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                              final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                              final @Nullable IPPlayer responsible, final boolean skipAnimation)
    {
        this(futureDoor, cause, actionType, responsible, 0.0D, skipAnimation);
    }

    /**
     * Constructs a door action event.
     *
     * @param futureDoor  The door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     */
    public DoorEventToggleEnd(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                              final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                              final @Nullable IPPlayer responsible)
    {
        this(futureDoor, cause, actionType, responsible, 0.0D, false);
    }

    /**
     * Constructs a door action event.
     *
     * @param doorUID       The UID of the door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    public DoorEventToggleEnd(final long doorUID, final @NotNull DoorActionCause cause,
                              final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                              final double time, final boolean skipAnimation)
    {
        this(responsible == null ? DatabaseManager.get().getDoor(doorUID) :
             DatabaseManager.get().getDoor(responsible, doorUID), cause, actionType, responsible, time, skipAnimation);
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
    public DoorEventToggleEnd(final long doorUID, final @NotNull DoorActionCause cause,
                              final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                              final double time)
    {
        this(doorUID, cause, actionType, responsible, time, false);
    }

    /**
     * Constructs a door action event.
     *
     * @param doorUID       The UID of the door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    public DoorEventToggleEnd(final long doorUID, final @NotNull DoorActionCause cause,
                              final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                              final boolean skipAnimation)
    {
        this(doorUID, cause, actionType, responsible, 0.0D, skipAnimation);
    }

    /**
     * Constructs a door action event.
     *
     * @param doorUID     The UID of the door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     */
    public DoorEventToggleEnd(final long doorUID, final @NotNull DoorActionCause cause,
                              final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible)
    {
        this(doorUID, cause, actionType, responsible, 0.0D, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
