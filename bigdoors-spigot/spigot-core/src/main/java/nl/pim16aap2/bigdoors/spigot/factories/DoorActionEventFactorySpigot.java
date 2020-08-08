package nl.pim16aap2.bigdoors.spigot.factories;

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
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

public class DoorActionEventFactorySpigot implements IDoorActionEventFactory
{
    @Override
    @NotNull
    public IDoorEventTogglePrepare createPrepareEvent(final @NotNull AbstractDoorBase door,
                                                      final @NotNull DoorActionCause cause,
                                                      final @NotNull DoorActionType actionType,
                                                      final @NotNull IPPlayer responsible, final double time,
                                                      final boolean skipAnimation,
                                                      final @NotNull IVector3DiConst newMinimum,
                                                      final @NotNull IVector3DiConst newMaximum)
    {
        return new DoorEventTogglePrepare(door, cause, actionType, responsible, time, skipAnimation, newMinimum,
                                          newMaximum);
    }

    @Override
    @NotNull
    public IDoorEventToggleStart createStartEvent(final @NotNull AbstractDoorBase door,
                                                  final @NotNull DoorActionCause cause,
                                                  final @NotNull DoorActionType actionType,
                                                  final @NotNull IPPlayer responsible, final double time,
                                                  final boolean skipAnimation,
                                                  final @NotNull IVector3DiConst newMinimum,
                                                  final @NotNull IVector3DiConst newMaximum)

    {
        return new DoorEventToggleStart(door, cause, actionType, responsible, time, skipAnimation, newMinimum,
                                        newMaximum);
    }

    @Override
    @NotNull
    public IDoorEventToggleEnd createEndEvent(final @NotNull AbstractDoorBase door,
                                              final @NotNull DoorActionCause cause,
                                              final @NotNull DoorActionType actionType,
                                              final @NotNull IPPlayer responsible, final double time,
                                              final boolean skipAnimation)
    {
        return new DoorEventToggleEnd(door, cause, actionType, responsible, time, skipAnimation);
    }
}
