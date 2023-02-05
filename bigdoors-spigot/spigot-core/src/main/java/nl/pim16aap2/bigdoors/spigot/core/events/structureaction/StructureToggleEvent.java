package nl.pim16aap2.bigdoors.spigot.core.events.structureaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.events.structureaction.IStructureToggleEvent;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.spigot.core.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
abstract class StructureToggleEvent extends BigDoorsSpigotEvent implements IStructureToggleEvent
{
    @Getter
    private final StructureSnapshot snapshot;

    @Getter
    protected final StructureActionCause cause;

    @Getter
    protected final StructureActionType actionType;

    @Getter
    protected final IPPlayer responsible;

    @Getter
    protected final double time;

    @Getter
    protected final boolean animationSkipped;

    @Override
    public abstract @NotNull HandlerList getHandlers();
}
