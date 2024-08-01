package nl.pim16aap2.animatedarchitecture.core.extensions;

import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.data.graph.DirectedAcyclicGraph;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSerializer;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class StructureTypeInitializerTest
{
    private static final String NAMESPACE = Constants.PLUGIN_NAME.toLowerCase(Locale.ROOT);

    @Test
    void testPropagateLoadFailures()
    {
        final DirectedAcyclicGraph<StructureTypeInitializer.Loadable> graph = new DirectedAcyclicGraph<>();
        final StructureTypeInitializer.Loadable l0 = newLoadable("l0");
        final StructureTypeInitializer.Loadable l1 = newLoadable("l1");
        final StructureTypeInitializer.Loadable l2 = newLoadable("l2");
        final StructureTypeInitializer.Loadable l3 = newLoadable("l3");
        final StructureTypeInitializer.Loadable l4 = newLoadable("l4");

        graph.addConnectedNodes(l1, l0);
        graph.addConnectedNodes(l2, l1);
        graph.addConnectedNodes(l3, l2);
        graph.addConnectedNodes(l4, l2);

        l1.setLoadFailure(new StructureTypeInitializer.LoadFailure(
            StructureTypeInitializer.LoadFailureType.GENERIC_LOAD_FAILURE,
            "TestFailure!")
        );
        StructureTypeInitializer.propagateLoadFailures(graph, List.of(l0, l1, l2, l3, l4));
        Assertions.assertNull(l0.getLoadFailure());
        Assertions.assertNotNull(l1.getLoadFailure());
        Assertions.assertNotNull(l2.getLoadFailure());
        Assertions.assertNotNull(l3.getLoadFailure());
        Assertions.assertNotNull(l4.getLoadFailure());
    }

    @Test
    void testAddDependenciesToGraph()
    {
        final DirectedAcyclicGraph<StructureTypeInitializer.Loadable> graph = newGraph();
        final StructureTypeInitializer.Loadable l0 = newLoadable("l0");
        final StructureTypeInitializer.Loadable l1 = newLoadable("l1");

        // Base case. No dependencies, so nothing happens.
        StructureTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.never())
            .addConnection(Mockito.any(), (StructureTypeInitializer.Loadable) Mockito.any());
        Assertions.assertNull(l0.getLoadFailure());
        Assertions.assertNull(l1.getLoadFailure());

        // Add dependency for l1 on l0.
        Mockito.when(l1.getStructureTypeInfo().getDependencies())
            .thenReturn(List.of(new StructureTypeInfo.Dependency("l0", 0, 2)));

        // Ensure dependencies not being in the map result in DEPENDENCY_UNAVAILABLE.
        StructureTypeInitializer.addDependenciesToGraph(graph, Map.of("l1", l1), l1);
        Mockito.verify(graph, Mockito.never())
            .addConnection(Mockito.any(), (StructureTypeInitializer.Loadable) Mockito.any());
        Assertions.assertEquals(
            StructureTypeInitializer.LoadFailureType.DEPENDENCY_UNAVAILABLE,
            l1.getLoadFailure().loadFailuretype());

        // Ensure dependencies with versions outside the specified range result int DEPENDENCY_UNSUPPORTED_VERSION.
        Mockito.when(l0.getStructureTypeInfo().getVersion()).thenReturn(4);
        StructureTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.never())
            .addConnection(Mockito.any(), (StructureTypeInitializer.Loadable) Mockito.any());
        Assertions.assertEquals(
            StructureTypeInitializer.LoadFailureType.DEPENDENCY_UNSUPPORTED_VERSION,
            l1.getLoadFailure().loadFailuretype());

        // Ensure that a valid dependency setup results in a dependency being added to the graph.
        Mockito.when(l0.getStructureTypeInfo().getVersion()).thenReturn(2);
        StructureTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.times(1))
            .addConnection(Mockito.any(), (StructureTypeInitializer.Loadable) Mockito.any());
    }

    @Test
    void testLoadStructureType()
        throws Exception
    {
        final Path badPath = Path.of("path/to/something/else.txt");
        final Path goodPath = Path.of("path/to/jar/to/load.jar");
        final String goodClassName = "Proper StructureType class that can be loaded fine";
        final String middleClassName = "Bad StructureType class that has a broken serializer";
        final String badClassName = "Bad StructureType class that does not conform to the required format";

        final StructureType goodType = Mockito.mock(StructureType.class);
        //noinspection unchecked
        Mockito.when(goodType.getStructureSerializer()).thenReturn(Mockito.mock(StructureSerializer.class));
        Mockito.when(goodType.getSimpleName()).thenReturn("good class!");

        final StructureType middleType = Mockito.mock(StructureType.class);
        Mockito.when(middleType.getStructureSerializer()).thenThrow(IllegalStateException.class);

        final IStructureTypeClassLoader structureTypeClassLoader = Mockito.mock(IStructureTypeClassLoader.class);
        Mockito.when(structureTypeClassLoader.loadStructureTypeClass(Mockito.anyString()))
            .thenAnswer(invocation ->
                switch (invocation.getArgument(0, String.class))
                {
                    case goodClassName -> goodType;
                    case middleClassName -> middleType;
                    case badClassName -> throw new NoSuchMethodException();
                    default -> throw new IllegalArgumentException();
                });
        Mockito.when(structureTypeClassLoader.loadJar(Mockito.any()))
            .thenAnswer(invocation -> goodPath.equals(invocation.getArgument(0, Path.class)));

        final StructureTypeInfo info = Mockito.mock(StructureTypeInfo.class);
        Mockito.when(info.getJarFile()).thenReturn(badPath);

        final StructureTypeInitializer initializer =
            new StructureTypeInitializer(List.of(), structureTypeClassLoader, false);

        Assertions.assertNull(initializer.loadStructureType(info));

        Mockito.when(info.getJarFile()).thenReturn(goodPath);
        Mockito.when(info.getMainClass()).thenReturn(middleClassName);
        Assertions.assertNull(initializer.loadStructureType(info));

        Mockito.when(info.getMainClass()).thenReturn(badClassName);
        Assertions.assertNull(initializer.loadStructureType(info));

        Mockito.when(info.getMainClass()).thenReturn(goodClassName);
        Assertions.assertEquals(goodType, initializer.loadStructureType(info));
    }

    @Test
    void testLoadStructureTypes()
        throws Exception
    {
        final IStructureTypeClassLoader structureTypeClassLoader = Mockito.mock(IStructureTypeClassLoader.class);
        Mockito.when(structureTypeClassLoader.loadJar(Mockito.any())).thenReturn(true);

        final StructureType dt0 = newStructureType();
        final StructureType dt1 = newStructureType();
        final StructureType dt2 = newStructureType();
        final StructureType dt3 = newStructureType();
        final StructureType dt4 = newStructureType();
        final StructureType dt5 = newStructureType();

        Mockito.when(structureTypeClassLoader.loadStructureTypeClass(Mockito.anyString()))
            .thenAnswer(invocation ->
                switch (invocation.getArgument(0, String.class))
                {
                    case "i0" -> dt0;
                    case "i1" -> dt1;
                    case "i2" -> dt2;
                    case "i3" -> dt3;
                    case "i4" -> dt4;
                    case "i5" -> dt5;
                    case "i6" -> throw new NoSuchMethodException();
                    default -> throw new IllegalArgumentException(invocation.getArgument(0, String.class));
                });

        // Loads fine
        final StructureTypeInfo i0 = newStructureTypeInfo("i0");

        // Loads fine
        final StructureTypeInfo i1 = newStructureTypeInfo("i1");
        Mockito.when(i1.getDependencies()).thenReturn(List.of(new StructureTypeInfo.Dependency("i0", 0, 0)));

        // Won't load, as it depends on i1[10,20], while i1 has version = 0.
        final StructureTypeInfo i2 = newStructureTypeInfo("i2");
        Mockito.when(i2.getDependencies()).thenReturn(List.of(new StructureTypeInfo.Dependency("i1", 10, 20)));

        // Loads fine
        final StructureTypeInfo i3 = newStructureTypeInfo("i3");
        Mockito.when(i3.getDependencies()).thenReturn(List.of(new StructureTypeInfo.Dependency("i1", 0, 0)));

        // Won't load, as it depends on i2, which didn't load
        final StructureTypeInfo i4 = newStructureTypeInfo("i4");
        Mockito.when(i4.getDependencies()).thenReturn(List.of(new StructureTypeInfo.Dependency("i2", 0, 0)));

        // Won't load, as it's dependency does not exist.
        final StructureTypeInfo i5 = newStructureTypeInfo("i5");
        Mockito.when(i5.getDependencies()).thenReturn(List.of(new StructureTypeInfo.Dependency("DoesNotExist", 0, 0)));

        // Won't load, as the StructureTypeClassLoader will throw a NoSuchMethodException.
        final StructureTypeInfo i6 = newStructureTypeInfo("i6");

        final StructureTypeInitializer structureTypeInitializer = new StructureTypeInitializer(
            List.of(i0, i1, i2, i3, i4, i5, i6),
            structureTypeClassLoader,
            false
        );

        Assertions.assertEquals(List.of(dt0, dt1, dt3), structureTypeInitializer.loadStructureTypes());
    }

    private static DirectedAcyclicGraph<StructureTypeInitializer.Loadable> newGraph()
    {
        return Mockito.mock();
    }

    private static StructureType newStructureType()
    {
        final StructureType structureType = Mockito.mock();
        Mockito.when(structureType.getSimpleName()).thenReturn("");
        return structureType;
    }

    private static StructureTypeInitializer.Loadable newLoadable(String name)
    {
        return new StructureTypeInitializer.Loadable(newStructureTypeInfo(name));
    }

    private static StructureTypeInfo newStructureTypeInfo(String name)
    {
        final var key = new NamespacedKey(NAMESPACE, name);

        final StructureTypeInfo info = Mockito.mock();
        Mockito.when(info.getNamespacedKey()).thenReturn(key);
        Mockito.when(info.getFullKey()).thenReturn(key.getFullKey());
        Mockito.when(info.getMainClass()).thenReturn(name);
        Mockito.when(info.getDependencies()).thenReturn(Collections.emptyList());
        Mockito.when(info.getVersion()).thenReturn(0);
        return info;
    }
}
