package nl.pim16aap2.animatedarchitecture.core.extensions;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.data.graph.DirectedAcyclicGraph;
import nl.pim16aap2.animatedarchitecture.core.data.graph.Node;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an initializer for a group of {@link StructureType}s.
 * <p>
 * This class will ensure that the dependencies between {@link StructureType}s are met and then load their jars into the
 * desired {@link StructureTypeClassLoader}.,
 */
@Flogger
final class StructureTypeInitializer
{
    private final DirectedAcyclicGraph<Loadable> graph;
    private final IStructureTypeClassLoader structureTypeClassLoader;

    /**
     * Instantiates this {@link StructureTypeInitializer}.
     *
     * @param structureTypeInfos
     *     The list of {@link StructureTypeInfo}s that should be loaded.
     * @param structureTypeClassLoader
     *     The class loader in which to load the structure types.
     * @param debug
     *     Whether to enable debugging. Enabling this will enable failFast mode for the graph. See
     *     {@link DirectedAcyclicGraph#DirectedAcyclicGraph(boolean)}.
     */
    StructureTypeInitializer(
        List<StructureTypeInfo> structureTypeInfos,
        IStructureTypeClassLoader structureTypeClassLoader,
        boolean debug)
    {
        this.structureTypeClassLoader = structureTypeClassLoader;
        this.graph = createGraph(structureTypeInfos, debug);

        log.atFiner().log("Dependency order: %s", graph.getLeafPath().stream().map(Loadable::getFullKey).toList());
    }

    /**
     * Attempts to load all structure types.
     *
     * @return A list of all successfully-loaded structure types.
     */
    public List<StructureType> loadStructureTypes()
    {
        final ArrayList<StructureType> ret = new ArrayList<>(graph.size());
        for (final Node<Loadable> node : graph)
        {
            final Loadable loadable = node.getObj();
            if (loadable.getLoadFailure() != null)
            {
                log.atWarning().log(
                    "Failed to load structure type %s, reason: %s",
                    loadable.getStructureTypeInfo(),
                    loadable.getLoadFailure()
                );
                continue;
            }

            final @Nullable StructureType result = loadStructureType(loadable.getStructureTypeInfo());
            if (result == null)
            {
                loadable.setLoadFailure(new LoadFailure(
                    LoadFailureType.GENERIC_LOAD_FAILURE,
                    "'An error occurred while loading this structure type!'")
                );
                cancelAllChildren(node);
            }
            else
                ret.add(result);
        }
        ret.trimToSize();
        return ret;
    }

    DirectedAcyclicGraph<Loadable> createGraph(List<StructureTypeInfo> structureTypeInfos, boolean debug)
    {
        final Map<String, Loadable> loadables = new HashMap<>(MathUtil.ceil(1.25 * structureTypeInfos.size()));
        structureTypeInfos.forEach(info -> loadables.put(info.getFullKey(), new Loadable(info)));

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
            graph.getNode(loadable).ifPresent(StructureTypeInitializer::cancelAllChildren);
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
            new LoadFailure(
                LoadFailureType.DEPENDENCY_UNAVAILABLE,
                String.format(
                    "'Dependency '%s' could not be loaded!'",
                    node.getObj().getStructureTypeInfo())
            );
        node.getAllChildren().forEach(child -> child.getObj().setLoadFailure(loadResult));
    }

    /**
     * Adds the dependencies of a loadable to the graph.
     *
     * @param graph
     *     The graph to add the dependencies to.
     * @param loadables
     *     The loadables to use for finding the dependencies.
     *     <p>
     *     Every dependency of {@code loadable} needs to be present in {@code loadables}. If at least one dependency is
     *     not present, {@code loadable} will be marked as failed to load with a
     *     {@link LoadFailureType#DEPENDENCY_UNAVAILABLE} failure.
     * @param loadable
     *     The loadable whose dependencies to add.
     */
    static void addDependenciesToGraph(
        DirectedAcyclicGraph<Loadable> graph,
        Map<String, Loadable> loadables,
        Loadable loadable)
    {
        final StructureTypeInfo info = loadable.getStructureTypeInfo();
        final List<StructureTypeInfo.Dependency> dependencies = info.getDependencies();

        if (dependencies.isEmpty())
            return;

        for (final StructureTypeInfo.Dependency dependency : dependencies)
        {
            final @Nullable Loadable parent = loadables.get(dependency.getFullKey());
            if (parent == null)
                loadable.setLoadFailure(
                    new LoadFailure(
                        LoadFailureType.DEPENDENCY_UNAVAILABLE,
                        String.format(
                            "'Could not find dependency '%s' for type: '%s''",
                            dependency.getFullKey(),
                            loadable))
                );
            else if (!dependency.satisfiedBy(parent.getStructureTypeInfo()))
                loadable.setLoadFailure(
                    new LoadFailure(
                        LoadFailureType.DEPENDENCY_UNSUPPORTED_VERSION,
                        String.format(
                            "'Version %d does not satisfy dependency %s for type: '%s''",
                            parent.getStructureTypeInfo().getVersion(),
                            dependency,
                            loadable))
                );
            else
                graph.addConnection(loadable, parent);
        }
    }

    /**
     * Attempts to load a {@link StructureTypeInfo} from its {@link StructureTypeInfo#getMainClass()}.
     *
     * @param structureTypeInfo
     *     The {@link StructureTypeInfo} to load.
     * @return The {@link StructureType} that resulted from loading the {@link StructureTypeInfo}, if possible.
     */
    @Nullable
    StructureType loadStructureType(StructureTypeInfo structureTypeInfo)
    {
        if (!structureTypeClassLoader.loadJar(structureTypeInfo.getJarFile()))
        {
            log.atSevere().log(
                "Failed to load file: '%s'! This type ('%s') will not be loaded!",
                structureTypeInfo.getJarFile(),
                structureTypeInfo.getFullKey()
            );
            return null;
        }

        final StructureType structureType;
        try
        {
            structureType = structureTypeClassLoader.loadStructureTypeClass(structureTypeInfo.getMainClass());
            postLoadValidation(structureTypeInfo, structureType);
        }
        catch (NoSuchMethodException e)
        {
            log.atSevere().withCause(e).log("Failed to load invalid extension: %s", structureTypeInfo);
            return null;
        }
        catch (Exception | Error e)
        {
            log.atSevere().withCause(e).log("Failed to load extension: %s", structureTypeInfo);
            return null;
        }

        log.atFine().log(
            "Loaded AnimatedArchitecture extension: %s",
            StringUtil.capitalizeFirstLetter(structureType.getSimpleName())
        );
        return structureType;
    }

    /**
     * Performs post-load validation on a {@link StructureType}.
     *
     * @param structureTypeInfo
     *     The {@link StructureTypeInfo} to use for validation.
     * @param structureType
     *     The {@link StructureType} to validate.
     * @throws Exception
     *     If the validation fails.
     */
    static void postLoadValidation(StructureTypeInfo structureTypeInfo, StructureType structureType)
        throws Exception
    {
        structureTypeInfo.verifyLoadedType(structureType);
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
     * Wrapper class for {@link StructureTypeInfo} with a hashCode and equals method that only uses
     * {@link StructureTypeInfo#getFullKey()}.
     */
    @ToString
    static class Loadable implements IKeyed
    {
        @Getter
        private final StructureTypeInfo structureTypeInfo;

        @Getter
        private @Nullable LoadFailure loadFailure = null;

        Loadable(StructureTypeInfo structureTypeInfo)
        {
            this.structureTypeInfo = structureTypeInfo;
        }

        @Override
        public NamespacedKey getNamespacedKey()
        {
            return structureTypeInfo.getNamespacedKey();
        }

        public void setLoadFailure(LoadFailure loadFailure)
        {
            log.atFiner().log("Failed to load %s: %s", this, loadFailure);
            this.loadFailure = loadFailure;
        }

        @Override
        public int hashCode()
        {
            return getFullKey().hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof Loadable other && getFullKey().equals(other.getFullKey());
        }
    }
}
