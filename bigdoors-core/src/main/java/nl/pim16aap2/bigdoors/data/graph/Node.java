package nl.pim16aap2.bigdoors.data.graph;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collection;
import java.util.LinkedHashSet;
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
    private final Set<Node<T>> parents = new LinkedHashSet<>();
    private final Set<Node<T>> children = new LinkedHashSet<>();
    private final DirectedAcyclicGraph<T> owner;

    @Getter
    private final T obj;

    Node(DirectedAcyclicGraph<T> owner, T obj)
    {
        this.owner = owner;
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
     * Checks if this node has any children.
     *
     * @return True if this node has 1 or more children.
     */
    public boolean hasChildren()
    {
        return !children.isEmpty();
    }

    /**
     * Recursively gets all the children of this node.
     *
     * @return All the children of this node as well as the all the children of each of those children etc.
     */
    public Set<Node<T>> getAllChildren()
    {
        owner.getLeafNodePath();
        final Set<Node<T>> ret = new LinkedHashSet<>(children);
        children.forEach(child -> ret.addAll(child.getAllChildren0()));
        return ret;
    }

    private Set<Node<T>> getAllChildren0()
    {
        final Set<Node<T>> ret = new LinkedHashSet<>(children);
        children.forEach(child -> ret.addAll(child.getAllChildren()));
        return ret;
    }

    /**
     * Clears all relations between this node and any other nodes.
     */
    void clearRelations()
    {
        children.forEach(child -> child.removeParent0(this));
        children.clear();

        parents.forEach(parent -> parent.removeChild0(this));
        parents.clear();
    }

    /**
     * Checks if another node 'test' is a parent of this node.
     *
     * @param test
     *     The node to check.
     * @return True if the test node is a parent of this node.
     */
    boolean hasParent(Node<T> test)
    {
        return parents.contains(test);
    }

    /**
     * Checks if another node 'test' is a child of this node.
     *
     * @param test
     *     The node to check.
     * @return True if the test node is a child of this node.
     */
    boolean hasChild(Node<T> test)
    {
        return children.contains(test);
    }

    /**
     * Adds a new parent to this node.
     *
     * @param parent
     *     The new parent node of this node.
     */
    void addParent(Node<T> parent)
    {
        this.addParent0(parent);
        parent.addChild0(this);
    }

    private void addParent0(Node<T> parent)
    {
        if (this.equals(parent))
            throw new IllegalArgumentException("Cannot make a node a parent of itself!");
        this.parents.add(parent);
    }

    private void addChild0(Node<T> child)
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
        this.removeParent0(parent);
        parent.removeChild0(this);
    }

    private void removeParent0(Node<T> parent)
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
        this.removeChild0(child);
        child.removeParent0(this);
    }

    private void removeChild0(Node<T> child)
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
        return String.format("Node[Obj: '%s', parents: %d, children: %d]", obj, parents.size(), children.size());
    }
}
