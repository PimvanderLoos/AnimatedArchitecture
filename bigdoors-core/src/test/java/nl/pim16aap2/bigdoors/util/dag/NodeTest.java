package nl.pim16aap2.bigdoors.util.dag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class NodeTest
{
    @Test
    void testHasChildren()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());

        Assertions.assertFalse(n0.hasChildren());
        n0.addParent(n1);
        Assertions.assertFalse(n0.hasChildren());
        n1.addParent(n0);
        Assertions.assertTrue(n0.hasChildren());
    }

    @Test
    void testHasParents()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());

        Assertions.assertFalse(n0.hasParents());
        n1.addParent(n0);
        Assertions.assertFalse(n0.hasParents());
        n0.addParent(n1);
        Assertions.assertTrue(n0.hasParents());
    }

    @Test
    void testRemoveParent()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());

        n0.addParent(n1);
        n1.addParent(n0);
        n0.removeParent(n1);

        Assertions.assertFalse(n0.hasParents());
        Assertions.assertTrue(n0.hasChildren());

        Assertions.assertTrue(n1.hasParents());
        Assertions.assertFalse(n1.hasChildren());
    }

    @Test
    void testRemoveChild()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());

        n0.addParent(n1);
        n1.addParent(n0);
        n1.removeChild(n0);

        Assertions.assertFalse(n0.hasParents());
        Assertions.assertTrue(n0.hasChildren());

        Assertions.assertTrue(n1.hasParents());
        Assertions.assertFalse(n1.hasChildren());
    }

    @Test
    void testClearRelations()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());
        final Node<Object> n2 = new Node<>(new Object());

        n0.addParent(n1);
        n1.addParent(n0);
        n2.addParent(n1);

        n0.clearRelations();

        Assertions.assertFalse(n0.hasChildren());
        Assertions.assertFalse(n0.hasParents());

        Assertions.assertFalse(n1.hasParents());
        Assertions.assertTrue(n1.hasChildren());

        Assertions.assertTrue(n2.hasParents());
        Assertions.assertFalse(n2.hasChildren());
    }

    @Test
    void testGetChildren()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());
        final Node<Object> n2 = new Node<>(new Object());
        final Node<Object> n3 = new Node<>(new Object());

        n2.addParent(n1);
        n3.addParent(n1);
        n1.addParent(n0);

        // Use list to ensure order is compared as well.
        List<Node<Object>> children = new ArrayList<>(n3.getAllChildren());
        Assertions.assertEquals(Collections.emptyList(), children);

        children = new ArrayList<>(n0.getAllChildren());
        Assertions.assertEquals(List.of(n1, n2, n3), children);
    }

    @Test
    void testHasRemainingParents()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());
        final Node<Object> n2 = new Node<>(new Object());
        final Node<Object> n3 = new Node<>(new Object());

        n2.addParent(n0);
        n2.addParent(n1);

        Assertions.assertTrue(n2.hasRemainingParents(List.of()));
        Assertions.assertTrue(n2.hasRemainingParents(List.of(n3)));
        Assertions.assertTrue(n2.hasRemainingParents(List.of(n0)));
        Assertions.assertTrue(n2.hasRemainingParents(List.of(n1)));
        Assertions.assertTrue(n2.hasRemainingParents(List.of(n1, n3)));
        Assertions.assertFalse(n2.hasRemainingParents(List.of(n0, n1)));
    }

    @Test
    void testToString()
    {
        final Node<Object> n0 = new Node<>(new Object());
        final Node<Object> n1 = new Node<>(new Object());

        n1.addParent(n0);
        n0.addParent(n1);

        // Ensure that even in case of a cyclic graph, Node#toString()
        // does not result in issues like a StackOverflow.
        Assertions.assertDoesNotThrow(n0::toString);
    }
}
