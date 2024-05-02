package nl.pim16aap2.animatedarchitecture.core.managers;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Locale;

class StructureTypeManagerTest
{
    private final StructureType type0 = newMockedStructureType("TestType0");
    private final StructureType type1 = newMockedStructureType("TestType1");
    private final StructureType type2 = newMockedStructureType("TestType2");
    private final StructureType type3 = newMockedStructureType("TestType3");

    @Test
    void testRegistry()
    {
        final var manager = new StructureTypeManager(Mockito.mock(), Mockito.mock());
        manager.register(type0, true);
        manager.register(type1);
        manager.register(type2, false);

        Assertions.assertTrue(manager.isRegistered(type0));
        Assertions.assertTrue(manager.isRegistered(type1));
        Assertions.assertTrue(manager.isRegistered(type2));
        Assertions.assertFalse(manager.isRegistered(type3));

        Assertions.assertTrue(manager.isStructureTypeEnabled(type0));
        Assertions.assertTrue(manager.isStructureTypeEnabled(type1));
        Assertions.assertFalse(manager.isStructureTypeEnabled(type2));
        Assertions.assertFalse(manager.isStructureTypeEnabled(type3));
    }

    @Test
    void testUpdateEnabledStatus()
    {
        final var manager = new StructureTypeManager(Mockito.mock(), Mockito.mock());

        manager.register(type0);
        Assertions.assertTrue(manager.isStructureTypeEnabled(type0));
        Assertions.assertEquals(1, manager.getEnabledStructureTypes().size());
        Assertions.assertEquals(0, manager.getDisabledStructureTypes().size());

        manager.setEnabledState(type0, false);
        Assertions.assertFalse(manager.isStructureTypeEnabled(type0));
        Assertions.assertEquals(0, manager.getEnabledStructureTypes().size());
        Assertions.assertEquals(1, manager.getDisabledStructureTypes().size());

        manager.setEnabledState(type0, true);
        Assertions.assertTrue(manager.isStructureTypeEnabled(type0));
        Assertions.assertEquals(1, manager.getEnabledStructureTypes().size());
        Assertions.assertEquals(0, manager.getDisabledStructureTypes().size());

        Assertions.assertEquals(1, manager.getRegisteredStructureTypes().size());
    }

    private static StructureType newMockedStructureType(String simpleName)
    {
        final String simpleName0 = simpleName.toLowerCase(Locale.ENGLISH);
        final String pluginName = "AnimatedArchitecture";
        final String fullName = String.format("%s:%s", pluginName, simpleName0).toLowerCase(Locale.ENGLISH);
        final int version = 1;

        final var structureType = Mockito.mock(StructureType.class);

        Mockito.when(structureType.getPluginName()).thenReturn(pluginName);
        Mockito.when(structureType.getSimpleName()).thenReturn(simpleName0);
        Mockito.when(structureType.getVersion()).thenReturn(version);
        Mockito.when(structureType.getLocalizationKey()).thenReturn("localization.key." + simpleName0);
        Mockito.when(structureType.getFullName()).thenReturn(fullName);
        Mockito.when(structureType.getFullNameWithVersion()).thenReturn(fullName + ":" + version);
        Mockito.when(structureType.getValidMovementDirections()).thenReturn(Collections.emptySet());

        Mockito.when(structureType.toString()).thenReturn(
            "MockedStructureType[@" + Integer.toHexString(structureType.hashCode()) + "] " + fullName + ":" + version);

        return structureType;
    }
}
