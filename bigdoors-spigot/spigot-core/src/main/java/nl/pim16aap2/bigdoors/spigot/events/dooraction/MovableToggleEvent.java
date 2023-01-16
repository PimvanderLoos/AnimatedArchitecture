package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableToggleEvent;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
abstract class MovableToggleEvent extends BigDoorsSpigotEvent implements IMovableToggleEvent
{
    @Getter
    private final MovableSnapshot snapshot;

    @Getter
    protected final MovableActionCause cause;

    @Getter
    protected final MovableActionType actionType;

    @Getter
    protected final IPPlayer responsible;

    @Getter
    protected final double time;

    @Getter
    protected final boolean animationSkipped;

    @Override
    public abstract @NotNull HandlerList getHandlers();
}
