package nl.pim16aap2.bigdoors.extensions;

import nl.pim16aap2.bigdoors.data.graph.DirectedAcyclicGraph;
import nl.pim16aap2.bigdoors.movable.MovableSerializer;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static nl.pim16aap2.bigdoors.extensions.MovableTypeInfo.Dependency;
import static nl.pim16aap2.bigdoors.extensions.MovableTypeInitializer.*;

class MovableTypeInitializerTest
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
        MovableTypeInitializer.propagateLoadFailures(graph, List.of(l0, l1, l2, l3, l4));
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
        MovableTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.never()).addConnection(Mockito.any(), (Loadable) Mockito.any());
        Assertions.assertNull(l0.getLoadFailure());
        Assertions.assertNull(l1.getLoadFailure());

        // Add dependency for l1 on l0.
        Mockito.when(l1.getMovableTypeInfo().getDependencies()).thenReturn(List.of(new Dependency("l0", 0, 2)));

        // Ensure dependencies not being in the map result in DEPENDENCY_UNAVAILABLE.
        MovableTypeInitializer.addDependenciesToGraph(graph, Map.of("l1", l1), l1);
        Mockito.verify(graph, Mockito.never()).addConnection(Mockito.any(), (Loadable) Mockito.any());
        Assertions.assertEquals(LoadFailureType.DEPENDENCY_UNAVAILABLE, l1.getLoadFailure().loadFailuretype());

        // Ensure dependencies with versions outside the specified range result int DEPENDENCY_UNSUPPORTED_VERSION.
        Mockito.when(l0.getMovableTypeInfo().getVersion()).thenReturn(4);
        MovableTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.never()).addConnection(Mockito.any(), (Loadable) Mockito.any());
        Assertions.assertEquals(LoadFailureType.DEPENDENCY_UNSUPPORTED_VERSION, l1.getLoadFailure().loadFailuretype());

        // Ensure that a valid dependency setup results in a dependency being added to the graph.
        Mockito.when(l0.getMovableTypeInfo().getVersion()).thenReturn(2);
        MovableTypeInitializer.addDependenciesToGraph(graph, Map.of("l0", l0, "l1", l1), l1);
        Mockito.verify(graph, Mockito.times(1)).addConnection(Mockito.any(), (Loadable) Mockito.any());
    }

    @Test
    void testLoadMovableType()
        throws Exception
    {
        final Path badPath = Path.of("path/to/something/else.txt");
        final Path goodPath = Path.of("path/to/jar/to/load.jar");
        final String goodClassName = "Proper MovableType class that can be loaded fine";
        final String middleClassName = "Bad MovableType class that has a broken serializer";
        final String badClassName = "Bad MovableType class that does not conform to the required format";

        final MovableType goodType = Mockito.mock(MovableType.class);
        //noinspection unchecked
        Mockito.when(goodType.getMovableSerializer()).thenReturn(Mockito.mock(MovableSerializer.class));
        Mockito.when(goodType.getSimpleName()).thenReturn("good class!");

        final MovableType middleType = Mockito.mock(MovableType.class);
        Mockito.when(middleType.getMovableSerializer()).thenThrow(IllegalStateException.class);

        final IMovableTypeClassLoader movableTypeClassLoader = Mockito.mock(IMovableTypeClassLoader.class);
        Mockito.when(movableTypeClassLoader.loadMovableTypeClass(Mockito.anyString())).thenAnswer(
            invocation -> switch (invocation.getArgument(0, String.class))
                {
                    case goodClassName -> goodType;
                    case middleClassName -> middleType;
                    case badClassName -> throw new NoSuchMethodException();
                    default -> throw new IllegalArgumentException();
                });
        Mockito.when(movableTypeClassLoader.loadJar(Mockito.any())).thenAnswer(
            invocation -> goodPath.equals(invocation.getArgument(0, Path.class)));

        final MovableTypeInfo info = Mockito.mock(MovableTypeInfo.class);
        Mockito.when(info.getJarFile()).thenReturn(badPath);

        final MovableTypeInitializer initializer = new MovableTypeInitializer(List.of(), movableTypeClassLoader, false);
        Assertions.assertNull(initializer.loadMovableType(info));

        Mockito.when(info.getJarFile()).thenReturn(goodPath);
        Mockito.when(info.getMainClass()).thenReturn(middleClassName);
        Assertions.assertNull(initializer.loadMovableType(info));

        Mockito.when(info.getMainClass()).thenReturn(badClassName);
        Assertions.assertNull(initializer.loadMovableType(info));

        Mockito.when(info.getMainClass()).thenReturn(goodClassName);
        Assertions.assertEquals(goodType, initializer.loadMovableType(info));
    }

    @Test
    void testLoadMovableTypes()
        throws Exception
    {
        final IMovableTypeClassLoader movableTypeClassLoader = Mockito.mock(IMovableTypeClassLoader.class);
        Mockito.when(movableTypeClassLoader.loadJar(Mockito.any())).thenReturn(true);

        final MovableType dt0 = newMovableType();
        final MovableType dt1 = newMovableType();
        final MovableType dt2 = newMovableType();
        final MovableType dt3 = newMovableType();
        final MovableType dt4 = newMovableType();
        final MovableType dt5 = newMovableType();

        Mockito.when(movableTypeClassLoader.loadMovableTypeClass(Mockito.anyString()))
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
        final MovableTypeInfo i0 = newMovableTypeInfo("i0");

        // Loads fine
        final MovableTypeInfo i1 = newMovableTypeInfo("i1");
        Mockito.when(i1.getDependencies()).thenReturn(List.of(new Dependency("i0", 0, 0)));

        // Won't load, as it depends on i1[10,20], while i1 has version = 0.
        final MovableTypeInfo i2 = newMovableTypeInfo("i2");
        Mockito.when(i2.getDependencies()).thenReturn(List.of(new Dependency("i1", 10, 20)));

        // Loads fine
        final MovableTypeInfo i3 = newMovableTypeInfo("i3");
        Mockito.when(i3.getDependencies()).thenReturn(List.of(new Dependency("i1", 0, 0)));

        // Won't load, as it depends on i2, which didn't load
        final MovableTypeInfo i4 = newMovableTypeInfo("i4");
        Mockito.when(i4.getDependencies()).thenReturn(List.of(new Dependency("i2", 0, 0)));

        // Won't load, as it's dependency does not exist.
        final MovableTypeInfo i5 = newMovableTypeInfo("i5");
        Mockito.when(i5.getDependencies()).thenReturn(List.of(new Dependency("DoesNotExist", 0, 0)));

        // Won't load, as the MovableTypeClassLoader will throw a NoSuchMethodException.
        final MovableTypeInfo i6 = newMovableTypeInfo("i6");

        final MovableTypeInitializer movableTypeInitializer = new MovableTypeInitializer(
            List.of(i0, i1, i2, i3, i4, i5, i6),
            movableTypeClassLoader, false);

        Assertions.assertEquals(List.of(dt0, dt1, dt3), movableTypeInitializer.loadMovableTypes());
    }

    @SuppressWarnings("unchecked")//
    private static DirectedAcyclicGraph<Loadable> newGraph()
    {
        return Mockito.mock(DirectedAcyclicGraph.class);
    }

    private static MovableType newMovableType()
    {
        final MovableType movableType = Mockito.mock(MovableType.class);
        Mockito.when(movableType.getSimpleName()).thenReturn("");
        return movableType;
    }

    private static Loadable newLoadable(String name)
    {
        return new Loadable(newMovableTypeInfo(name));
    }

    private static MovableTypeInfo newMovableTypeInfo(String name)
    {
        final MovableTypeInfo info = Mockito.mock(MovableTypeInfo.class);
        Mockito.when(info.getTypeName()).thenReturn(name);
        Mockito.when(info.getMainClass()).thenReturn(name);
        Mockito.when(info.getDependencies()).thenReturn(Collections.emptyList());
        Mockito.when(info.getVersion()).thenReturn(0);
        return info;
    }
}
