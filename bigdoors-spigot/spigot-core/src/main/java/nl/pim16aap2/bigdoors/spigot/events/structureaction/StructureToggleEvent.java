package nl.pim16aap2.bigdoors.spigot.events.structureaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.structureaction.IStructureToggleEvent;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.structures.StructureSnapshot;
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
