package nl.pim16aap2.animatedarchitecture.core.managers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.structures.types.bigdoor.StructureTypeBigDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.clock.StructureTypeClock;
import nl.pim16aap2.animatedarchitecture.core.structures.types.drawbridge.StructureTypeDrawbridge;
import nl.pim16aap2.animatedarchitecture.core.structures.types.flag.StructureTypeFlag;
import nl.pim16aap2.animatedarchitecture.core.structures.types.garagedoor.StructureTypeGarageDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.portcullis.StructureTypePortcullis;
import nl.pim16aap2.animatedarchitecture.core.structures.types.revolvingdoor.StructureTypeRevolvingDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.slidingdoor.StructureTypeSlidingDoor;
import nl.pim16aap2.animatedarchitecture.core.structures.types.windmill.StructureTypeWindmill;

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
