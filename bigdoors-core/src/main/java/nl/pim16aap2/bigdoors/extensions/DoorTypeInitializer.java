package nl.pim16aap2.bigdoors.extensions;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.dag.DirectedAcyclicGraph;
import nl.pim16aap2.bigdoors.util.dag.Node;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents an initializer for a group of {@link DoorType}s.
 * <p>
 * This class will ensure that the dependencies between {@link DoorType}s are met and then load their jars into the
 * desired {@link DoorTypeClassLoader}.,
 *
 * @author Pim
 */
@Flogger //
final class DoorTypeInitializer
{
    private final DirectedAcyclicGraph<Loadable> graph;
    private final DoorTypeClassLoader doorTypeClassLoader;

    /**
     * Instantiates this {@link DoorTypeInitializer}.
     *
     * @param doorTypeInfoList
     *     The list of {@link DoorTypeInfo}s that should be loaded.
     * @param doorTypeClassLoader
     *     The class loader in which to load the door types.
     * @param debug
     *     Whether to enable debugging. Enabling this will enable failFast mode for the graph. See {@link
     *     DirectedAcyclicGraph#DirectedAcyclicGraph(boolean)}.
     */
    DoorTypeInitializer(List<DoorTypeInfo> doorTypeInfoList, DoorTypeClassLoader doorTypeClassLoader, boolean debug)
    {
        this.doorTypeClassLoader = doorTypeClassLoader;
        this.graph = createGraph(doorTypeInfoList, debug);

        log.at(Level.FINER).log("Dependency order: %s",
                                graph.getLeafPath().stream().map(Loadable::getTypeName).toList());
    }

    /**
     * Attempts to load all door types.
     *
     * @return
     */
    public List<DoorType> loadDoorTypes()
    {
        final ArrayList<DoorType> ret = new ArrayList<>(graph.size());
        for (final Node<Loadable> node : graph)
        {
            final Loadable loadable = node.getObj();
            if (loadable.getLoadFailure() != null)
            {
                log.at(Level.WARNING).log("Failed to load door type %s, reason: %s",
                                          loadable.getDoorTypeInfo(), loadable.getLoadFailure());
                continue;
            }

            final @Nullable DoorType result = loadDoorType(loadable.getDoorTypeInfo());
            if (result == null)
            {
                loadable.setLoadFailure(new LoadFailure(LoadFailureType.GENERIC_LOAD_FAILURE,
                                                        "'An error occurred while loading this door type!'"));
                cancelAllChildren(node);
            }
            else
                ret.add(result);
        }
        ret.trimToSize();
        return ret;
    }

    private DirectedAcyclicGraph<Loadable> createGraph(List<DoorTypeInfo> doorTypeInfoList, boolean debug)
    {
        final Map<String, Loadable> loadables = new HashMap<>(doorTypeInfoList.size());
        doorTypeInfoList.forEach(info -> loadables.put(info.getTypeName(), new Loadable(info)));

        final DirectedAcyclicGraph<Loadable> graph = new DirectedAcyclicGraph<>(debug);
        loadables.values().forEach(graph::addNode);
        loadables.values().forEach(loadable -> addDependenciesToGraph(graph, loadables, loadable));
        propagateLoadFailures(graph, loadables.values());

        return graph;
    }

    /**
     * Checks for any loadables that failed to load for whatever reason and propagates this state to all its children.
     *
     * @param graph
     *     The graph to use for finding the children of the loadables that failed to load.
     * @param loadables
     *     The loadables to check.
     */
    private void propagateLoadFailures(DirectedAcyclicGraph<Loadable> graph, Collection<Loadable> loadables)
    {
        for (final Loadable loadable : loadables)
        {
            if (loadable.getLoadFailure() == null)
                continue;
            graph.getNode(loadable).ifPresent(this::cancelAllChildren);
        }
    }

    /**
     * Causes all children of a node in a graph to fail to load.
     *
     * @param node
     *     The node whose children to fail.
     */
    private void cancelAllChildren(Node<Loadable> node)
    {
        final LoadFailure loadResult =
            new LoadFailure(LoadFailureType.DEPENDENCY_UNAVAILABLE,
                            String.format("'Dependency '%s' could not be loaded!'", node.getObj().getDoorTypeInfo()));
        node.getAllChildren().forEach(child -> child.getObj().setLoadFailure(loadResult));
    }

    private void addDependenciesToGraph(DirectedAcyclicGraph<Loadable> graph, Map<String, Loadable> loadables,
                                        Loadable loadable)
    {
        final DoorTypeInfo info = loadable.getDoorTypeInfo();
        final List<DoorTypeInfo.Dependency> dependencies = info.getDependencies();
        if (dependencies.isEmpty())
            return;

        for (final DoorTypeInfo.Dependency dependency : dependencies)
        {
            final @Nullable Loadable parent = loadables.get(dependency.dependencyName());
            if (parent == null)
                loadable.setLoadFailure(
                    new LoadFailure(LoadFailureType.DEPENDENCY_UNAVAILABLE,
                                    String.format("'Could not find dependency '%s' for type: '%s''",
                                                  dependency.dependencyName(), loadable)));
            else if (!dependency.satisfiedBy(parent.getDoorTypeInfo()))
                loadable.setLoadFailure(
                    new LoadFailure(LoadFailureType.DEPENDENCY_UNSUPPORTED_VERSION,
                                    String.format("'Version %d does not satisfy dependency %s for type: '%s''",
                                                  parent.getDoorTypeInfo().getVersion(), dependency, loadable)));
            else
                graph.addConnection(loadable, parent);
        }
    }

    /**
     * Attempts to load a jar.
     *
     * @param file
     *     The jar file.
     * @return True if the jar loaded successfully.
     */
    private boolean loadJar(Path file)
    {
        try
        {
            doorTypeClassLoader.addURL(file.toUri().toURL());
        }
        catch (Exception e)
        {
            log.at(Level.FINE).withCause(e).log();
            return false;
        }
        return true;
    }

    /**
     * Attempts to load a {@link DoorTypeInfo} from its {@link DoorTypeInfo#getMainClass()}.
     *
     * @param DoorTypeInfo
     *     The {@link DoorTypeInfo} to load.
     * @return The {@link DoorType} that resulted from loading the {@link DoorTypeInfo}, if possible.
     */
    private @Nullable DoorType loadDoorType(DoorTypeInfo DoorTypeInfo)
    {
        if (!loadJar(DoorTypeInfo.getJarFile()))
        {
            log.at(Level.WARNING).log("Failed to load file: '%s'! This type ('%s') will not be loaded!",
                                      DoorTypeInfo.getJarFile(), DoorTypeInfo.getTypeName());
            return null;
        }

        final DoorType doorType;
        try
        {
            final Class<?> typeClass = doorTypeClassLoader.loadClass(DoorTypeInfo.getMainClass());
            final Method getter = typeClass.getDeclaredMethod("get");
            doorType = (DoorType) getter.invoke(null);
            // Retrieve the serializer to ensure that it could be initialized successfully.
            doorType.getDoorSerializer();
        }
        catch (Exception | Error e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to load extension: %s", DoorTypeInfo.getTypeName());
            return null;
        }

        log.at(Level.FINE).log("Loaded BigDoors extension: %s", Util.capitalizeFirstLetter(doorType.getSimpleName()));
        return doorType;
    }

    private record LoadFailure(LoadFailureType loadFailuretype, String message)
    {
    }

    private enum LoadFailureType
    {
        DEPENDENCY_UNSUPPORTED_VERSION,
        DEPENDENCY_UNAVAILABLE,
        GENERIC_LOAD_FAILURE
    }

    /**
     * Wrapper class for {@link DoorTypeInfo} with a hashCode and equals method that only uses {@link
     * DoorTypeInfo#getTypeName()}.
     */
    @ToString
    static final class Loadable
    {
        @Getter
        private final DoorTypeInfo doorTypeInfo;

        @Getter
        private @Nullable LoadFailure loadFailure = null;

        Loadable(DoorTypeInfo doorTypeInfo)
        {
            this.doorTypeInfo = doorTypeInfo;
        }

        public String getTypeName()
        {
            return doorTypeInfo.getTypeName();
        }

        public void setLoadFailure(LoadFailure loadFailure)
        {
            log.at(Level.FINER).log("Failed to load %s: %s", this, loadFailure);
            this.loadFailure = loadFailure;
        }

        @Override
        public int hashCode()
        {
            return getTypeName().hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof Loadable other && getTypeName().equals(other.getTypeName());
        }
    }
}
