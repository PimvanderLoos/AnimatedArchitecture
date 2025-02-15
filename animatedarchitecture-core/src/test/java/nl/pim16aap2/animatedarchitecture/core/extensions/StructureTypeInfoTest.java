package nl.pim16aap2.animatedarchitecture.core.extensions;

import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static nl.pim16aap2.animatedarchitecture.core.extensions.StructureTypeInfo.Dependency;
import static nl.pim16aap2.animatedarchitecture.core.extensions.StructureTypeInfo.parseDependency;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureTypeInfoTest
{
    private static final String NAMESPACE = Constants.PLUGIN_NAME.toLowerCase(Locale.ROOT);

    @Test
    void testConstructor()
    {
        final String typeName = "typeName";
        final int version = 1;
        final String mainClass = "com.example.Main";
        final Path jarFile = Path.of("/does/not/exist.jar");

        Assertions.assertDoesNotThrow(() ->
            new StructureTypeInfo(NamespacedKey.of(typeName), version, mainClass, jarFile, "1.0.0", ""));

        Assertions.assertDoesNotThrow(() ->
            new StructureTypeInfo(NamespacedKey.of(typeName), version, mainClass, jarFile, "1.0.0", null));

        Assertions.assertDoesNotThrow(() ->
            new StructureTypeInfo(NamespacedKey.of(typeName), version, mainClass, jarFile, "1.0.0", "null"));

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new StructureTypeInfo(NamespacedKey.of(typeName), version, mainClass, jarFile, "1.0.0", "garbage"));

        // The name of the structure type should be lower-case.
        Assertions.assertEquals(
            "animatedarchitecture:typename",
            new StructureTypeInfo(NamespacedKey.of(typeName), version, mainClass, jarFile, "1.0.0", "").getFullKey()
        );
    }

    @Test
    void testParseValidDependencies()
    {
        final List<Dependency> dependencies =
            parseDependencies(
                "animatedarchitecture:portcullis(1;5) " +
                    "animatedarchitecture:Door(2;3) " +
                    "animatedarchitecture:my-super_special-Dependency(1;1)"
            );

        assertEquals(3, dependencies.size());

        var dep = dependencies.getFirst();
        assertEquals("animatedarchitecture:portcullis", dep.getFullKey());
        assertEquals(1, dep.minVersion());
        assertEquals(5, dep.maxVersion());

        dep = dependencies.get(1);
        assertEquals("animatedarchitecture:door", dep.getFullKey());
        assertEquals(2, dep.minVersion());
        assertEquals(3, dep.maxVersion());

        dep = dependencies.get(2);
        assertEquals("animatedarchitecture:my-super_special-dependency", dep.getFullKey());
        assertEquals(1, dep.minVersion());
        assertEquals(1, dep.maxVersion());
    }

    @Test
    void testParseValidDependency()
    {
        var dependency = parseDependency("animatedarchitecture:portcullis(1;5)");

        assertEquals("animatedarchitecture:portcullis", dependency.getFullKey());
        assertEquals(1, dependency.minVersion());
        assertEquals(5, dependency.maxVersion());
    }

    @Test
    void testParseEmptyDependency()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> parseDependency(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> parseDependency(" "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> parseDependency(null));
    }

    @Test
    void testParseInvalidDependency()
    {
        // The name of the dependency should be lower-case.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:Portcullis(1;5)")
        );

        // The namespace of the dependency should be lower-case.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("AnimatedArchitecture:portcullis(1;5)")
        );

        // The key should have both a namespace and a name.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("portcullis(1;5)")
        );

        // The name cannot be empty
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:(1;5)")
        );

        // The namespace cannot be empty
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency(":portcullis(1;5)")
        );

        // Missing closing parenthesis.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:portcullis(1;5")
        );

        // 3 version numbers instead of 2.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:portcullis(1;5;6)")
        );

        // 1 version number instead of 2.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:portcullis(1)")
        );

        // Additional characters after the closing parenthesis.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:portcullis(1;5)garbage")
        );

        // Invalid (leading) characters in the name.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency(" animatedarchitecture:portcullis(1;5)")
        );

        // Invalid (trailing) characters in the name.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:portcullis (1;5)")
        );

        // Invalid characters in the middle of the name.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:port cullis(1;5)")
        );

        // Version numbers separated by a colon instead of a semicolon.
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> parseDependency("animatedarchitecture:portcullis(1:5)")
        );
    }

    @Test
    void testValidDependencyConstructor()
    {
        final String dependencyName = "dep";

        Assertions.assertDoesNotThrow(() -> new Dependency(dependencyName, 1, 1));
        Assertions.assertDoesNotThrow(() -> new Dependency(dependencyName, 1, 2));
    }

    @Test
    void testInvalidNameDependencyConstructor()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Dependency("", 1, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Dependency(" ", 1, 1));
    }

    @Test
    void testInvalidVersionsDependencyConstructor()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Dependency("dep", 1, 0));
    }

    @Test
    void testDependencySatisfiedBy()
    {
        final String dependencyName = "structure-type";
        final var key = NamespacedKey.of(dependencyName);

        final Dependency dependency = new Dependency(key.getFullKey(), 1, 2);

        Assertions.assertTrue(dependency.satisfiedBy(structureTypeInfo(1, dependencyName)));
        Assertions.assertTrue(dependency.satisfiedBy(structureTypeInfo(2, dependencyName)));

        Assertions.assertFalse(dependency.satisfiedBy(structureTypeInfo(0, dependencyName)));
        Assertions.assertFalse(dependency.satisfiedBy(structureTypeInfo(3, dependencyName)));

        Assertions.assertFalse(dependency.satisfiedBy(structureTypeInfo(2, "another-" + dependencyName)));
    }

    @Test
    void verifyLoadedType()
    {
        final String typeName = "typename";
        final int version = 1;

        final StructureTypeInfo structureTypeInfo = new StructureTypeInfo(
            NamespacedKey.of(typeName),
            version,
            "com.example.Main",
            Path.of("/does/not/exist.jar"),
            "1.0.0",
            null
        );

        Assertions.assertDoesNotThrow(
            () -> structureTypeInfo.verifyLoadedType(mockStructureType(NAMESPACE, typeName, version))
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> structureTypeInfo.verifyLoadedType(mockStructureType(NAMESPACE + "_fail", typeName, version))
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> structureTypeInfo.verifyLoadedType(mockStructureType(NAMESPACE, typeName + "_fail", version))
        );

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> structureTypeInfo.verifyLoadedType(mockStructureType(NAMESPACE + "_fail", typeName, version + 1))
        );
    }

    /**
     * Mocks a {@link StructureType} with the given namespace, type name, and version.
     *
     * @param namespace
     *     The namespace of the structure type.
     * @param typeName
     *     The name of the structure type.
     * @param version
     *     The version of the structure type.
     * @return The mocked {@link StructureType}.
     */
    private StructureType mockStructureType(
        String namespace,
        String typeName,
        int version)
    {
        final var key = new NamespacedKey(namespace, typeName);

        final StructureType structureType = Mockito.mock(Mockito.withSettings().stubOnly());
        Mockito.when(structureType.getNamespacedKey()).thenReturn(key);
        Mockito.when(structureType.getSimpleName()).thenReturn(key.getKey());
        Mockito.when(structureType.getVersion()).thenReturn(version);
        Mockito.when(structureType.getFullKey()).thenReturn(key.getFullKey());
        Mockito.when(structureType.getFullNameWithVersion()).thenReturn(key.getFullKey() + ":" + version);
        return structureType;
    }

    /**
     * Shortcut method for {@link StructureTypeInfo#parseDependencies(String, String)}. The name of the structure type
     * is set to "TestType" (which is only used for logging).
     *
     * @param dependencies
     *     The dependencies to parse.
     * @return The parsed dependencies.
     */
    private List<Dependency> parseDependencies(@Nullable String dependencies)
    {
        return StructureTypeInfo.parseDependencies(dependencies, "TestType");
    }

    /**
     * Creates a {@link StructureTypeInfo} with the given version and name.
     * <p>
     * All other fields are set to placeholder values.
     *
     * @param version
     *     The version of the structure type.
     * @param name
     *     The name of the structure type.
     * @return The created {@link StructureTypeInfo}.
     */
    private StructureTypeInfo structureTypeInfo(int version, String name)
    {
        return new StructureTypeInfo(
            new NamespacedKey(NAMESPACE, name),
            version,
            "com.example.Main",
            Path.of("/does/not/exist.jar"),
            "1.0.0",
            null
        );
    }
}
