package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PropertyManagerSerializerTest
{
    @Mock
    private StructureType structureType;
    private PropertyManager propertyManager;

    @BeforeEach
    void setUp()
    {
        final Map<Property<?>, @Nullable Object> entries = LinkedHashMap.newLinkedHashMap(4);
        entries.put(Property.ANIMATION_SPEED_MULTIPLIER, 1.5D);
        entries.put(Property.OPEN_STATUS, null);
        entries.put(Property.ROTATION_POINT, new Vector3Di(1, 2, 3));
        entries.put(Property.REDSTONE_MODE, null);

        final Map<String, IPropertyValue<?>> propertyValueMap = entries
            .entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> PropertyManager.mapKey(entry.getKey()),
                entry -> PropertyManager.mapValue(entry.getValue())
            ));

        propertyManager = new PropertyManager(new HashMap<>(propertyValueMap));

        Mockito.when(structureType.getProperties()).thenReturn(List.copyOf(entries.keySet()));
    }


    @Test
    void testSerializationCycle()
    {
        final String serialized = PropertyManagerSerializer.serialize(propertyManager);

        Assertions.assertEquals(
            propertyManager,
            PropertyManagerSerializer.deserialize(structureType, serialized)
        );
    }

    @Test
    void testSerializeAbstractStructure()
    {
        final var structure = Mockito.mock(AbstractStructure.class);
        Mockito.when(structure.getType()).thenReturn(structureType);

        final var snapshot = propertyManager.snapshot();
        Mockito.when(structure.getPropertyManagerSnapshot()).thenReturn(snapshot);

        final String serialized = PropertyManagerSerializer.serialize(structure);
        Assertions.assertEquals(
            propertyManager,
            PropertyManagerSerializer.deserialize(structureType, serialized)
        );
    }
}
