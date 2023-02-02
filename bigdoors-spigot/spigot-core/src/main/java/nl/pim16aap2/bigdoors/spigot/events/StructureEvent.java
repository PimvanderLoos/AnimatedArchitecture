package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IStructureEvent;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

abstract class StructureEvent extends BigDoorsSpigotEvent implements IStructureEvent
{
    @Getter
    protected final AbstractStructure structure;

    @Getter
    protected final Optional<IPPlayer> responsible;

    protected StructureEvent(AbstractStructure structure, @Nullable IPPlayer responsible)
    {
        this.structure = structure;
        this.responsible = Optional.ofNullable(responsible);
    }
}
