package nl.pim16aap2.bigdoors.extensions;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.data.graph.DirectedAcyclicGraph;
import nl.pim16aap2.bigdoors.doors.DoorSerializer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static nl.pim16aap2.bigdoors.extensions.DoorTypeInfo.Dependency;
import static nl.pim16aap2.bigdoors.extensions.DoorTypeInitializer.*;

class DoorTypeInitializerTest
{
    @Test
    void testPropagateLoadFailures()
    {
        final DirectedAcyclicGraph<Loadable> graph = new DirectedAcyclicGraph<>();
        final Loadable l0 = newLoadable("l0");
        final Loadable l1 = newLoadable("l1");
        final Loadable l2 = newLoadable("l2");
        final Loadable l3 = newLoadable("l3");
        final Loadable l4 = newLoadable("l4");

        graph.addConnectedNodes(l1, l0);
        graph.addConnectedNodes(l2, l1);
        graph.addConnectedNodes(l3, l2);
        graph.addConnectedNodes(l4, l2);

        l1.setLoadFailure(new LoadFailure(LoadFailureType.GENERIC_LOAD_FAILURE, "TestFailure!"));
        DoorTypeInitializer.propagateLoadFailures(graph, List.of(l0, l1, l2, l3, l4));
        Assertions.assertNull(l0.getLoadFailure());
        Assertions.assertNotNull(l1.getLoadFailure());
        Assertions.assertNotNull(l2.getLoadFailure());
        Assertions.assertNotNull(l3.getLoadFailure());
        Assertions.assertNotNull(l4.getLoadFailure());
    }

    @Test
    void testAddDependenciesToGraph()
    {
        final DirectedAcyclicGraph<Loadable> graph = newGraph();
        final Loadable l0 = newLoadable("l0");
        final Loadable l1 = newLoadable("l1");

        // Base case. No dependencies, so nothing happens.
        DoorTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.never()).addConnection(Mockito.any(), (Loadable) Mockito.any());
        Assertions.assertNull(l0.getLoadFailure());
        Assertions.assertNull(l1.getLoadFailure());

        // Add dependency for l1 on l0.
        Mockito.when(l1.getDoorTypeInfo().getDependencies()).thenReturn(List.of(new Dependency("l0", 0, 2)));

        // Ensure dependencies not being in the map result in DEPENDENCY_UNAVAILABLE.
        DoorTypeInitializer.addDependenciesToGraph(graph, Map.of("l1", l1), l1);
        Mockito.verify(graph, Mockito.never()).addConnection(Mockito.any(), (Loadable) Mockito.any());
        Assertions.assertEquals(LoadFailureType.DEPENDENCY_UNAVAILABLE, l1.getLoadFailure().loadFailuretype());

        // Ensure dependencies with versions outside the specified range result int DEPENDENCY_UNSUPPORTED_VERSION.
        Mockito.when(l0.getDoorTypeInfo().getVersion()).thenReturn(4);
        DoorTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.never()).addConnection(Mockito.any(), (Loadable) Mockito.any());
        Assertions.assertEquals(LoadFailureType.DEPENDENCY_UNSUPPORTED_VERSION, l1.getLoadFailure().loadFailuretype());

        // Ensure that a valid dependency setup results in a dependency being added to the graph.
        Mockito.when(l0.getDoorTypeInfo().getVersion()).thenReturn(2);
        DoorTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.times(1)).addConnection(Mockito.any(), (Loadable) Mockito.any());
    }

    @SneakyThrows
    @Test
    void testLoadDoorType()
    {
        final Path badPath = Path.of("path/to/something/else.txt");
        final Path goodPath = Path.of("path/to/jar/to/load.jar");
        final String goodClassName = "Proper DoorType class that can be loaded fine";
        final String middleClassName = "Bad DoorType class that has a broken serializer";
        final String badClassName = "Bad DoorType class that does not conform to the required format";

        final DoorType goodType = Mockito.mock(DoorType.class);
        //noinspection unchecked
        Mockito.when(goodType.getDoorSerializer()).thenReturn(Mockito.mock(DoorSerializer.class));
        Mockito.when(goodType.getSimpleName()).thenReturn("good class!");

        final DoorType middleType = Mockito.mock(DoorType.class);
        Mockito.when(middleType.getDoorSerializer()).thenThrow(IllegalStateException.class);

        final IDoorTypeClassLoader doorTypeClassLoader = Mockito.mock(IDoorTypeClassLoader.class);
        Mockito.when(doorTypeClassLoader.loadDoorTypeClass(Mockito.anyString())).thenAnswer(
            invocation -> switch (invocation.getArgument(0, String.class))
                {
                    case goodClassName -> goodType;
                    case middleClassName -> middleType;
                    case badClassName -> throw new NoSuchMethodException();
                    default -> throw new IllegalArgumentException();
                });
        Mockito.when(doorTypeClassLoader.loadJar(Mockito.any())).thenAnswer(
            invocation -> goodPath.equals(invocation.getArgument(0, Path.class)));

        final DoorTypeInfo info = Mockito.mock(DoorTypeInfo.class);
        Mockito.when(info.getJarFile()).thenReturn(badPath);

        final DoorTypeInitializer initializer = new DoorTypeInitializer(List.of(), doorTypeClassLoader, false);
        Assertions.assertNull(initializer.loadDoorType(info));

        Mockito.when(info.getJarFile()).thenReturn(goodPath);
        Mockito.when(info.getMainClass()).thenReturn(middleClassName);
        Assertions.assertNull(initializer.loadDoorType(info));

        Mockito.when(info.getMainClass()).thenReturn(badClassName);
        Assertions.assertNull(initializer.loadDoorType(info));

        Mockito.when(info.getMainClass()).thenReturn(goodClassName);
        Assertions.assertEquals(goodType, initializer.loadDoorType(info));
    }

    @SneakyThrows
    @Test
    void testLoadDoorTypes()
    {
        final IDoorTypeClassLoader doorTypeClassLoader = Mockito.mock(IDoorTypeClassLoader.class);
        Mockito.when(doorTypeClassLoader.loadJar(Mockito.any())).thenReturn(true);

        final DoorType dt0 = newDoorType();
        final DoorType dt1 = newDoorType();
        final DoorType dt2 = newDoorType();
        final DoorType dt3 = newDoorType();
        final DoorType dt4 = newDoorType();
        final DoorType dt5 = newDoorType();

        Mockito.when(doorTypeClassLoader.loadDoorTypeClass(Mockito.anyString()))
               .thenAnswer(invocation -> switch (invocation.getArgument(0, String.class))
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
        final DoorTypeInfo i0 = newDoorTypeInfo("i0");

        // Loads fine
        final DoorTypeInfo i1 = newDoorTypeInfo("i1");
        Mockito.when(i1.getDependencies()).thenReturn(List.of(new Dependency("i0", 0, 0)));

        // Won't load, as it depends on i1[10,20], while i1 has version = 0.
        final DoorTypeInfo i2 = newDoorTypeInfo("i2");
        Mockito.when(i2.getDependencies()).thenReturn(List.of(new Dependency("i1", 10, 20)));

        // Loads fine
        final DoorTypeInfo i3 = newDoorTypeInfo("i3");
        Mockito.when(i3.getDependencies()).thenReturn(List.of(new Dependency("i1", 0, 0)));

        // Won't load, as it depends on i2, which didn't load
        final DoorTypeInfo i4 = newDoorTypeInfo("i4");
        Mockito.when(i4.getDependencies()).thenReturn(List.of(new Dependency("i2", 0, 0)));

        // Won't load, as it's dependency does not exist.
        final DoorTypeInfo i5 = newDoorTypeInfo("i5");
        Mockito.when(i5.getDependencies()).thenReturn(List.of(new Dependency("DoesNotExist", 0, 0)));

        // Won't load, as the DoorTypeClassLoader will throw a NoSuchMethodException.
        final DoorTypeInfo i6 = newDoorTypeInfo("i6");

        final DoorTypeInitializer doorTypeInitializer = new DoorTypeInitializer(List.of(i0, i1, i2, i3, i4, i5, i6),
                                                                                doorTypeClassLoader, false);

        Assertions.assertEquals(List.of(dt0, dt1, dt3), doorTypeInitializer.loadDoorTypes());
    }

    @SuppressWarnings("unchecked")//
    private static DirectedAcyclicGraph<Loadable> newGraph()
    {
        return Mockito.mock(DirectedAcyclicGraph.class);
    }

    private static DoorType newDoorType()
    {
        final DoorType doorType = Mockito.mock(DoorType.class);
        Mockito.when(doorType.getSimpleName()).thenReturn("");
        return doorType;
    }

    private static Loadable newLoadable(String name)
    {
        return new Loadable(newDoorTypeInfo(name));
    }

    private static DoorTypeInfo newDoorTypeInfo(String name)
    {
        final DoorTypeInfo info = Mockito.mock(DoorTypeInfo.class);
        Mockito.when(info.getTypeName()).thenReturn(name);
        Mockito.when(info.getMainClass()).thenReturn(name);
        Mockito.when(info.getDependencies()).thenReturn(Collections.emptyList());
        Mockito.when(info.getVersion()).thenReturn(0);
        return info;
    }
}
