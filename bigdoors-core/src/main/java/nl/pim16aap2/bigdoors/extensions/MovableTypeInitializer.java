package nl.pim16aap2.bigdoors.extensions;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.data.graph.DirectedAcyclicGraph;
import nl.pim16aap2.bigdoors.data.graph.Node;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents an initializer for a group of {@link MovableType}s.
 * <p>
 * This class will ensure that the dependencies between {@link MovableType}s are met and then load their jars into the
 * desired {@link MovableTypeClassLoader}.,
 *
 * @author Pim
 */
@Flogger //
final class MovableTypeInitializer
{
    private final DirectedAcyclicGraph<Loadable> graph;
    private final IMovableTypeClassLoader movableTypeClassLoader;

    /**
     * Instantiates this {@link MovableTypeInitializer}.
     *
     * @param movableTypeInfos
     *     The list of {@link MovableTypeInfo}s that should be loaded.
     * @param movableTypeClassLoader
     *     The class loader in which to load the movable types.
     * @param debug
     *     Whether to enable debugging. Enabling this will enable failFast mode for the graph. See
     *     {@link DirectedAcyclicGraph#DirectedAcyclicGraph(boolean)}.
     */
    MovableTypeInitializer(
        List<MovableTypeInfo> movableTypeInfos, IMovableTypeClassLoader movableTypeClassLoader, boolean debug)
    {
        this.movableTypeClassLoader = movableTypeClassLoader;
        this.graph = createGraph(movableTypeInfos, debug);

        log.at(Level.FINER).log("Dependency order: %s",
                                graph.getLeafPath().stream().map(Loadable::getTypeName).toList());
    }

    /**
     * Attempts to load all movable types.
     *
     * @return A list of all successfully-loaded movable types.
     */
    public List<MovableType> loadMovableTypes()
    {
        final ArrayList<MovableType> ret = new ArrayList<>(graph.size());
        for (final Node<Loadable> node : graph)
        {
            final Loadable loadable = node.getObj();
            if (loadable.getLoadFailure() != null)
            {
                log.at(Level.WARNING).log("Failed to load movable type %s, reason: %s",
                                          loadable.getMovableTypeInfo(), loadable.getLoadFailure());
                continue;
            }

            final @Nullable MovableType result = loadMovableType(loadable.getMovableTypeInfo());
            if (result == null)
            {
                loadable.setLoadFailure(new LoadFailure(LoadFailureType.GENERIC_LOAD_FAILURE,
                                                        "'An error occurred while loading this movable type!'"));
                cancelAllChildren(node);
            }
            else
                ret.add(result);
        }
        ret.trimToSize();
        return ret;
    }

    DirectedAcyclicGraph<Loadable> createGraph(List<MovableTypeInfo> movableTypeInfos, boolean debug)
    {
        final Map<String, Loadable> loadables = new HashMap<>(movableTypeInfos.size());
        movableTypeInfos.forEach(info -> loadables.put(info.getTypeName(), new Loadable(info)));

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
    static void propagateLoadFailures(DirectedAcyclicGraph<Loadable> graph, Collection<Loadable> loadables)
    {
        for (final Loadable loadable : loadables)
        {
            if (loadable.getLoadFailure() == null)
                continue;
            graph.getNode(loadable).ifPresent(MovableTypeInitializer::cancelAllChildren);
        }
    }

    /**
     * Causes all children of a node in a graph to fail to load.
     *
     * @param node
     *     The node whose children to fail.
     */
    static void cancelAllChildren(Node<Loadable> node)
    {
        final LoadFailure loadResult =
            new LoadFailure(LoadFailureType.DEPENDENCY_UNAVAILABLE,
                            String.format("'Dependency '%s' could not be loaded!'",
                                          node.getObj().getMovableTypeInfo()));
        node.getAllChildren().forEach(child -> child.getObj().setLoadFailure(loadResult));
    }

    static void addDependenciesToGraph(
        DirectedAcyclicGraph<Loadable> graph, Map<String, Loadable> loadables,
        Loadable loadable)
    {
        final MovableTypeInfo info = loadable.getMovableTypeInfo();
        final List<MovableTypeInfo.Dependency> dependencies = info.getDependencies();
        if (dependencies.isEmpty())
            return;

        for (final MovableTypeInfo.Dependency dependency : dependencies)
        {
            final @Nullable Loadable parent = loadables.get(dependency.dependencyName());
            if (parent == null)
                loadable.setLoadFailure(
                    new LoadFailure(LoadFailureType.DEPENDENCY_UNAVAILABLE,
                                    String.format("'Could not find dependency '%s' for type: '%s''",
                                                  dependency.dependencyName(), loadable)));
            else if (!dependency.satisfiedBy(parent.getMovableTypeInfo()))
                loadable.setLoadFailure(
                    new LoadFailure(LoadFailureType.DEPENDENCY_UNSUPPORTED_VERSION,
                                    String.format("'Version %d does not satisfy dependency %s for type: '%s''",
                                                  parent.getMovableTypeInfo().getVersion(), dependency, loadable)));
            else
                graph.addConnection(loadable, parent);
        }
    }

    /**
     * Attempts to load a {@link MovableTypeInfo} from its {@link MovableTypeInfo#getMainClass()}.
     *
     * @param movableTypeInfo
     *     The {@link MovableTypeInfo} to load.
     * @return The {@link MovableType} that resulted from loading the {@link MovableTypeInfo}, if possible.
     */
    @Nullable MovableType loadMovableType(MovableTypeInfo movableTypeInfo)
    {
        if (!movableTypeClassLoader.loadJar(movableTypeInfo.getJarFile()))
        {
            log.at(Level.SEVERE).log("Failed to load file: '%s'! This type ('%s') will not be loaded!",
                                     movableTypeInfo.getJarFile(), movableTypeInfo.getTypeName());
            return null;
        }

        final MovableType movableType;
        try
        {
            movableType = movableTypeClassLoader.loadMovableTypeClass(movableTypeInfo.getMainClass());
            // Retrieve the serializer to ensure that it could be initialized successfully.
            movableType.getMovableSerializer();
        }
        catch (NoSuchMethodException e)
        {
            log.at(Level.SEVERE).log("Failed to load invalid extension: %s", movableTypeInfo);
            return null;
        }
        catch (Exception | Error e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to load extension: %s", movableTypeInfo);
            return null;
        }

        log.at(Level.FINE)
           .log("Loaded BigDoors extension: %s", Util.capitalizeFirstLetter(movableType.getSimpleName()));
        return movableType;
    }

    record LoadFailure(LoadFailureType loadFailuretype, String message)
    {
    }

    enum LoadFailureType
    {
        DEPENDENCY_UNSUPPORTED_VERSION,
        DEPENDENCY_UNAVAILABLE,
        GENERIC_LOAD_FAILURE
    }

    /**
     * Wrapper class for {@link MovableTypeInfo} with a hashCode and equals method that only uses
     * {@link MovableTypeInfo#getTypeName()}.
     */
    @ToString
    static class Loadable
    {
        @Getter
        private final MovableTypeInfo movableTypeInfo;

        @Getter
        private @Nullable LoadFailure loadFailure = null;

        Loadable(MovableTypeInfo movableTypeInfo)
        {
            this.movableTypeInfo = movableTypeInfo;
        }

        public String getTypeName()
        {
            return movableTypeInfo.getTypeName();
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
