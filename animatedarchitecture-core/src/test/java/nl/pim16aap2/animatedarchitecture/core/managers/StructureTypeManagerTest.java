package nl.pim16aap2.animatedarchitecture.core.managers;

import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StructureTypeManagerTest
{
    private static final List<StructureType> DISCOVERED_STRUCTURE_TYPES = discoverStructureTypes();

    @InjectMocks
    private StructureTypeManager manager;

    @Mock
    private DebuggableRegistry debuggableRegistry;

    @BeforeEach
    void setUp()
    {
        verify(debuggableRegistry).registerDebuggable(manager);
    }

    @AfterEach
    void tearDown()
    {
        verifyNoMoreInteractions(debuggableRegistry);
    }

    @ParameterizedTest
    @FieldSource("DISCOVERED_STRUCTURE_TYPES")
    void getFromKey_shouldReturnCorrectType(StructureType type)
    {
        // execute & verify
        assertThat(manager.getFromKey(type.getKey())).hasValue(type);
    }

    @Test
    void getFromKey_shouldReturnEmptyOptionalForNullKey()
    {
        // execute & verify
        assertThat(manager.getFromKey(null)).isEmpty();
    }

    @Test
    void getFromKey_shouldReturnEmptyOptionalForUnknownKey()
    {
        // execute & verify
        assertThat(manager.getFromKey("unknownKey")).isEmpty();
    }

    @Test
    void getEnabledStructureTypes_shouldReturnAllTypesByDefault()
    {
        // execute & verify
        assertThat(manager.getEnabledStructureTypes()).containsExactlyInAnyOrderElementsOf(DISCOVERED_STRUCTURE_TYPES);
    }

    @Test
    void updateEnabledStatusForStructureTypes_shouldUpdateEnabledTypesBasedOnPredicate()
    {
        // setup
        StructureType typeToEnable = DISCOVERED_STRUCTURE_TYPES.getFirst();

        // execute
        manager.updateEnabledStatusForStructureTypes(type -> type.equals(typeToEnable));

        // verify
        assertThat(manager.getEnabledStructureTypes()).containsExactly(typeToEnable);
    }

    @Test
    void structureTypeManager_shouldHaveAllDiscoveredTypesRegistered()
    {
        // execute & verify
        assertThat(manager)
            .extracting("registeredStructureTypes", InstanceOfAssertFactories.LIST)
            .containsExactlyInAnyOrderElementsOf(DISCOVERED_STRUCTURE_TYPES);
    }

    @Test
    void verifyDiscoveredStructureTypeCount()
    {
        // execute & verify
        assertThat(DISCOVERED_STRUCTURE_TYPES).size().isGreaterThanOrEqualTo(9);
    }

    private static List<StructureType> discoverStructureTypes()
    {
        try
        {
            return discoverStructureTypes0();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to discover structure types", e);
        }
    }

    private static List<StructureType> discoverStructureTypes0()
        throws Exception
    {
        final String packageName = "nl.pim16aap2.animatedarchitecture.core.structures.types";
        final String path = packageName.replace('.', '/');

        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Enumeration<URL> resources = classLoader.getResources(path);

        if (!resources.hasMoreElements())
            throw new IllegalStateException("Package not found: " + packageName);

        return Collections.list(resources).stream()
            .filter(resource -> !resource.getPath().contains("test-classes"))
            .flatMap(resource ->
            {
                try
                {
                    File directory = new File(resource.toURI());
                    return findClassFiles(directory)
                        .map(file -> resolveStructureType(file, directory, packageName));
                }
                catch (URISyntaxException e)
                {
                    throw new RuntimeException("Invalid URI: " + resource, e);
                }
            })
            .filter(Objects::nonNull)
            .toList();
    }

    private static @Nullable StructureType resolveStructureType(File file, File baseDir, String packageName)
    {
        String relativePath = baseDir.toURI().relativize(file.toURI()).getPath();
        String className = packageName + "." +
            relativePath.replace('/', '.').replace(".class", "");
        try
        {
            Class<?> clazz = Class.forName(className);
            if (StructureType.class.isAssignableFrom(clazz) && !clazz.equals(StructureType.class))
            {
                Method getMethod = clazz.getDeclaredMethod("get");
                return (StructureType) getMethod.invoke(null);
            }
            return null;
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException("Failed to get instance: " + className, e);
        }
    }


    private static Stream<File> findClassFiles(File directory)
    {
        return streamFilesInDirectory(directory)
            .flatMap(file ->
            {
                if (file.isDirectory())
                {
                    return findClassFiles(file);
                }
                else if (file.getName().endsWith(".class"))
                {
                    return Stream.of(file);
                }
                return Stream.empty();
            });
    }

    @SuppressWarnings("DataFlowIssue")
    private static Stream<File> streamFilesInDirectory(File directory)
    {
        final File[] files = directory.listFiles();
        if (files == null)
        {
            return Stream.empty();
        }

        return Arrays.stream(files);
    }
}
