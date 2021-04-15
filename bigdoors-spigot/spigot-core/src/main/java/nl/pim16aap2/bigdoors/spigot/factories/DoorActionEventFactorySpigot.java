package nl.pim16aap2.bigdoors.spigot.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleEnd;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleStart;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleStart;
import nl.pim16aap2.bigdoors.util.CuboidConst;

public class DoorActionEventFactorySpigot implements IDoorActionEventFactory
{
    @Override
    public @NonNull IDoorEventTogglePrepare createPrepareEvent(final @NonNull AbstractDoorBase door,
                                                               final @NonNull DoorActionCause cause,
                                                               final @NonNull DoorActionType actionType,
                                                               final @NonNull IPPlayer responsible, final double time,
                                                               final boolean skipAnimation,
                                                               final @NonNull CuboidConst newCuboid)
    {
        return new DoorEventTogglePrepare(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public @NonNull IDoorEventToggleStart createStartEvent(final @NonNull AbstractDoorBase door,
                                                           final @NonNull DoorActionCause cause,
                                                           final @NonNull DoorActionType actionType,
                                                           final @NonNull IPPlayer responsible, final double time,
                                                           final boolean skipAnimation,
                                                           final @NonNull CuboidConst newCuboid)

    {
        return new DoorEventToggleStart(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public @NonNull IDoorEventToggleEnd createEndEvent(final @NonNull AbstractDoorBase door,
                                                       final @NonNull DoorActionCause cause,
                                                       final @NonNull DoorActionType actionType,
                                                       final @NonNull IPPlayer responsible, final double time,
                                                       final boolean skipAnimation)
    {
        return new DoorEventToggleEnd(door, cause, actionType, responsible, time, skipAnimation);
    }
}
