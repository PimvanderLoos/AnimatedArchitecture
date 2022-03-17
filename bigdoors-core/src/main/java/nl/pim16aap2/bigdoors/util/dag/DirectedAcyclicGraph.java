package nl.pim16aap2.bigdoors.util.dag;

import nl.pim16aap2.bigdoors.util.Util;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Represents a Directed Acyclic Graph.
 *
 * @param <T>
 *     The type of data to store in the nodes.
 * @author Pim
 */
public final class DirectedAcyclicGraph<T> implements Iterable<Node<T>>
{
    /**
     * The leaves, or end nodes in the graph.
     * <p>
     * These are nodes that have exactly 0 parents.
     */
    private final Set<Node<T>> leaves = new LinkedHashSet<>();

    /**
     * Map of all stored values with their respective nodes.
     */
    private final Map<T, Node<T>> nodes = new HashMap<>();

    private int size = 0;
    private int modCount = 0;

    /**
     * Cached version of the leaf path.
     * <p>
     * Finding this path is an expensive operation, so this is cached where possible.
     */
    private @Nullable Set<Node<T>> leafPath = null;

    private final boolean failFast;

    /**
     * Constructs a new Directed Acyclic Graph.
     *
     * @param failFast
     *     Whether to fail 'fast'. This will cause the cyclic nature of this graph to be checked every time a connection
     *     is added. This has the advantage of failing immediately when trying to add a cyclic dependency, at the cost
     *     of running the expensive cyclic dependency check operation for every change.
     *     <p>
     *     Unlike the name might imply, disabling this is going to be much faster overall, as it'll only verify the
     *     acyclic constraint when needed (e.g. for iterating) rather than for every insertion.
     */
    public DirectedAcyclicGraph(boolean failFast)
    {
        this.failFast = failFast;
    }

    /**
     * See {@link DirectedAcyclicGraph#DirectedAcyclicGraph(boolean)} with `failFast = false`.
     */
    public DirectedAcyclicGraph()
    {
        this(false);
    }

    /**
     * Creates a new node on the graph if no node exists for the given value.
     * <p>
     * Use {@link #addConnection(Object, Object)} to add connections between nodes.
     *
     * @param val
     *     The value of the node.
     */
    public Node<T> addNode(T val)
    {
        if (nodes.containsKey(val))
            return Util.requireNonNull(nodes.get(val), "Node");

        final Node<T> node = new Node<>(val);
        nodes.put(val, node);
        ++modCount;
        ++size;
        this.leaves.add(node);
        return node;
    }

    /**
     * Creates two new nodes (if no nodes exist for the given values) and creates a connection between them.
     * <p>
     * See {@link #addNode(Object)} and {@link #addConnection(Object, Object)}.
     *
     * @param child
     *     The child node to add to the graph.
     * @param parent
     *     The parent node to add to the graph.
     */
    public void addConnectedNodes(T child, T parent)
    {
        final Node<T> childNode = addNode(child);
        final Node<T> parentNode = addNode(parent);
        addConnection0(childNode, parentNode);
    }

    /**
     * Gets the current number of nodes in this graph.
     *
     * @return The size of this graph.
     */
    public int size()
    {
        return size;
    }

    private @Nullable Node<T> remove0(T val)
    {
        final @Nullable Node<T> removed = nodes.remove(val);
        if (removed == null)
            return null;

        --size;
        ++modCount;
        removed.clearRelations();

        return removed;
    }

    /**
     * Removes a value from this graph.
     *
     * @param val
     *     The value to remove.
     * @return The value if it was removed, otherwise null.
     */
    public @Nullable T remove(T val)
    {
        final @Nullable Node<T> removed = remove0(val);
        return removed == null ? null : removed.getObj();
    }

    /**
     * Removes a node from this graph.
     *
     * @param node
     *     The node to remove.
     * @return The node if it was removed, otherwise null.
     */
    public @Nullable Node<T> remove(Node<T> node)
    {
        return remove0(node.getObj());
    }

    /**
     * Removes multiple values from this graph.
     *
     * @param values
     *     A collection of values.
     */
    public void removeAll(Collection<T> values)
    {
        values.forEach(this::remove0);
    }

    /**
     * Removes multiple nodes from this graph.
     *
     * @param values
     *     A collection of nodes.
     */
    public void removeAllNodes(Collection<Node<T>> values)
    {
        values.forEach(this::remove);
    }

    /**
     * Gets the path from the leaves (i.e. nodes without parents) down to the base of the graph.
     * <p>
     * This means that for an entry `x` in position n in this list, `x` might have one or more dependencies on any
     * entries with `index < n`, but not on any entries with `index > n`.
     *
     * @return The leaf path.
     *
     * @throws IllegalStateException
     *     if the acyclic constraint of this graph has been violated.
     */
    public List<T> getLeafPath()
    {
        return Arrays.asList(createLeafArray(getLeafNodePath()));
    }

    /**
     * Adds a connection between a child node and a parent node.
     *
     * @param child
     *     The child node. This is where the connection is coming from.
     * @param parent
     *     The parent node. This is the target of the connection.
     */
    public void addConnection(T child, T parent)
    {
        final Node<T> childNode = Util.requireNonNull(nodes.get(child), "Child Node");
        final Node<T> parentNode = Util.requireNonNull(nodes.get(parent), "Parent Node");
        addConnection0(childNode, parentNode);
    }

    private void addConnection0(Node<T> child, Node<T> parent)
    {
        ++modCount;

        if (!child.hasParents())
            this.leaves.remove(child);
        child.addParent(parent);

        if (failFast)
        {
            try
            {
                this.leafPath = verifyAcyclic();
            }
            catch (IllegalStateException e)
            {
                throw new IllegalArgumentException(
                    "Failed to add connection between child '" + child + "' and parent: '" + parent + "'", e);
            }
        }
        else
        {
            this.leafPath = null;
        }
    }

    /**
     * Checks if there are any cyclic dependencies in the current graph.
     *
     * @return The set of nodes ordered by removal order. A node is removed when it has no parents left when excluding
     * the previously-removed nodes.
     *
     * @throws IllegalStateException
     *     if a cyclic dependency is found.
     */
    Set<Node<T>> verifyAcyclic()
    {
        if (!nodes.isEmpty() && leaves.isEmpty())
            throw new IllegalStateException("Graph has no leaves and must therefore be cyclic!");

        final LinkedList<Node<T>> leafQueue = new LinkedList<>(leaves);
        final Set<Node<T>> removed = new LinkedHashSet<>(nodes.size());

        while (!leafQueue.isEmpty())
        {
            final Node<T> node = leafQueue.removeFirst();
            removed.add(node);
            for (final Node<T> child : node.getChildren())
                if (!removed.contains(child) && !child.hasRemainingParents(removed))
                    leafQueue.addLast(child);
        }
        if (removed.size() < nodes.size())
        {
            final Set<Node<T>> remaining = new HashSet<>(nodes.values());
            remaining.removeAll(removed);
            throw new IllegalStateException("Found acyclic dependency in graph: " + remaining);
        }

        return removed;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException
     *     if the acyclic constraint of this graph has been violated.
     */
    @Override
    public Iterator<Node<T>> iterator()
    {
        return new DAGIterator(getLeafNodePath());
    }

    Set<Node<T>> getLeafNodePath()
    {
        return leafPath != null ? leafPath : (leafPath = verifyAcyclic());
    }

    @SuppressWarnings("unchecked")
    static <T> T[] createLeafArray(Collection<Node<T>> leafPathCol)
    {
        final T[] leafPath = (T[]) new Object[leafPathCol.size()];
        int idx = 0;
        for (final Node<T> leaf : leafPathCol)
            leafPath[idx++] = leaf.getObj();
        return leafPath;
    }

    /**
     * Simple iterator that iterates over the leaf path of nodes in the graph.
     *
     * @author Pim
     */
    public class DAGIterator implements Iterator<Node<T>>
    {
        private final int expectedModCount = modCount;
        private final Node<T>[] leafPath;
        int cursor = 0;

        DAGIterator(Collection<Node<T>> leafPath)
        {
            //noinspection unchecked
            this.leafPath = leafPath.toArray(new Node[0]);
        }

        @Override
        public boolean hasNext()
        {
            return cursor < size;
        }

        @Override
        public Node<T> next()
        {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            final int next = cursor + 1;
            if (next >= size)
                throw new NoSuchElementException();
            cursor = next;
            return leafPath[cursor];
        }
    }
}
