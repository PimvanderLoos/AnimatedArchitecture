package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureToggleEvent;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event where a structure is toggled.
 * <p>
 * The subclasses of this class define the different stages of the toggle event.
 * <p>
 * The preparation stage can be cancelled, while the start and end stages cannot.
 */
@AllArgsConstructor
abstract class StructureToggleEvent extends AnimatedArchitectureSpigotEvent implements IStructureToggleEvent
{
    @Getter
    private final StructureSnapshot snapshot;

    @Getter
    protected final StructureActionCause cause;

    @Getter
    protected final StructureActionType actionType;

    @Getter
    protected final IPlayer responsible;

    @Getter
    protected final double time;

    @Getter
    protected final boolean animationSkipped;

    @Override
    public abstract @NotNull HandlerList getHandlers();
}
