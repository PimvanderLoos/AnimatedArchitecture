package nl.pim16aap2.bigdoors.util.dag;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a node in a graph.
 *
 * @param <T>
 *     The type of data stored in this node.
 * @author Pim
 */
@Getter(AccessLevel.PACKAGE)
public final class Node<T>
{
    private final List<Node<T>> parents = new ArrayList<>();
    private final List<Node<T>> children = new ArrayList<>();
    private final T obj;

    Node(T obj)
    {
        this.obj = obj;
    }

    /**
     * Checks if this node has any parents.
     *
     * @return True if this node has 1 or more parents.
     */
    public boolean hasParents()
    {
        return !parents.isEmpty();
    }

    /**
     * Recursively gets all the children of this node.
     *
     * @return All the children of this node as well as the all the children of each of those children etc.
     */
    public Set<Node<T>> getAllChildren()
    {
        final Set<Node<T>> ret = new LinkedHashSet<>(children);
        children.forEach(child -> ret.addAll(child.getAllChildren()));
        return ret;
    }

    /**
     * Adds a new parent to this node.
     *
     * @param parent
     *     The new parent node of this node.
     */
    void addParent(Node<T> parent)
    {
        this.parents.add(parent);
    }

    /**
     * Adds a new child to this node.
     *
     * @param child
     *     The new child node of this node.
     */
    void addChild(Node<T> child)
    {
        this.children.add(child);
    }

    /**
     * Removes a parent to this node.
     *
     * @param parent
     *     The parent node to remove.
     */
    void removeParent(Node<T> parent)
    {
        this.parents.remove(parent);
    }

    /**
     * Removes a child to this node.
     *
     * @param child
     *     The child node to remove.
     */
    void removeChild(Node<T> child)
    {
        this.children.remove(child);
    }

    /**
     * Checks if this node would have any remaining parents when excluding a provided set of other nodes.
     *
     * @param exclude
     *     The nodes to exclude when looking at the parents.
     * @return True if this node has 1 or more parents that do not occur in the excluded set.
     */
    boolean hasRemainingParents(Collection<Node<T>> exclude)
    {
        if (parents.isEmpty())
            return false;

        for (final Node<T> parent : parents)
            if (!exclude.contains(parent))
                return true;
        return false;
    }

    @Override
    public String toString()
    {
        return String.format("Node[Obj: '%s', parents: %s, children: %s]", obj, parents, children);
    }
}
