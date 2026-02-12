package nl.pim16aap2.animatedarchitecture.core.managers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.structures.bigdoor.StructureTypeBigDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.clock.StructureTypeClock;
import nl.pim16aap2.animatedarchitecture.core.structures.drawbridge.StructureTypeDrawbridge;
import nl.pim16aap2.animatedarchitecture.core.structures.flag.StructureTypeFlag;
import nl.pim16aap2.animatedarchitecture.core.structures.garagedoor.StructureTypeGarageDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.portcullis.StructureTypePortcullis;
import nl.pim16aap2.animatedarchitecture.core.structures.revolvingdoor.StructureTypeRevolvingDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.slidingdoor.StructureTypeSlidingDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.windmill.StructureTypeWindmill;

import java.util.List;

/**
 * Registers all built-in structure types.
 */
@Singleton
public final class BuiltinStructureTypeRegistrar extends Restartable
{
    private final StructureTypeManager structureTypeManager;

    @Inject
    public BuiltinStructureTypeRegistrar(
        RestartableHolder holder,
        StructureTypeManager structureTypeManager)
    {
        super(holder);
        this.structureTypeManager = structureTypeManager;
    }

    @Override
    public void initialize()
    {
        structureTypeManager.register(List.of(
            StructureTypeBigDoor.get(),
            StructureTypeDrawbridge.get(),
            StructureTypeWindmill.get(),
            StructureTypeClock.get(),
            StructureTypeFlag.get(),
            StructureTypeGarageDoor.get(),
            StructureTypePortcullis.get(),
            StructureTypeRevolvingDoor.get(),
            StructureTypeSlidingDoor.get()
        ));
    }
}
