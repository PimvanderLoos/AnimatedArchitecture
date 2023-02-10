package nl.pim16aap2.bigdoors.spigot.core.events;

import lombok.Getter;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.events.IStructureEvent;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

abstract class StructureEvent extends BigDoorsSpigotEvent implements IStructureEvent
{
    @Getter
    protected final AbstractStructure structure;

    @Getter
    protected final Optional<IPlayer> responsible;

    protected StructureEvent(AbstractStructure structure, @Nullable IPlayer responsible)
    {
        this.structure = structure;
        this.responsible = Optional.ofNullable(responsible);
    }
}
