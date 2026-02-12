package nl.pim16aap2.animatedarchitecture.core.managers;

import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltinStructureTypeRegistrarTest
{
    @Test
    void initialize_shouldRegisterAllBuiltInStructureTypes()
    {
        // setup
        final StructureTypeManager structureTypeManager =
            new StructureTypeManager(
                Mockito.mock(DebuggableRegistry.class),
                Mockito.mock(LocalizationManager.class)
            );
        final BuiltinStructureTypeRegistrar registrar =
            new BuiltinStructureTypeRegistrar(new RestartableHolder(), structureTypeManager);

        // execute
        registrar.initialize();

        // verify
        final Set<String> registeredKeys = structureTypeManager.getRegisteredStructureTypes()
            .stream()
            .map(type -> type.getFullKey())
            .collect(Collectors.toSet());

        assertThat(registeredKeys).containsExactlyInAnyOrder(
            "bigdoor",
            "clock",
            "drawbridge",
            "flag",
            "garagedoor",
            "portcullis",
            "revolvingdoor",
            "slidingdoor",
            "windmill"
        );
        assertThat(registeredKeys).allMatch(key -> !key.contains(":"));
    }
}
