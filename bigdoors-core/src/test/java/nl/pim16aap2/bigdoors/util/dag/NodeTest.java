package nl.pim16aap2.bigdoors.util.dag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class NodeTest
{

    @Test
    void testHasChildren()
    {
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());

        Assertions.assertFalse(n0.hasChildren());
        n0.addParent(n1);
        Assertions.assertFalse(n0.hasChildren());
        n1.addParent(n0);
        Assertions.assertTrue(n0.hasChildren());
    }

    @Test
    void testHasParents()
    {
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());

        Assertions.assertFalse(n0.hasParents());
        n1.addParent(n0);
        Assertions.assertFalse(n0.hasParents());
        n0.addParent(n1);
        Assertions.assertTrue(n0.hasParents());
    }

    @Test
    void testRemoveParent()
    {
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());

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
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());

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
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());
        final Node<Object> n2 = new Node<>(dag, new Object());

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
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());
        final Node<Object> n2 = new Node<>(dag, new Object());
        final Node<Object> n3 = new Node<>(dag, new Object());

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
    void testHasParent()
    {
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());
        final Node<Object> n2 = new Node<>(dag, new Object());

        n1.addParent(n0);
        Assertions.assertTrue(n1.hasParent(n0));
        Assertions.assertFalse(n1.hasParent(n2));
        Assertions.assertFalse(n0.hasParent(n1));
    }

    @Test
    void testHasChild()
    {
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());
        final Node<Object> n2 = new Node<>(dag, new Object());

        n1.addParent(n0);
        Assertions.assertTrue(n0.hasChild(n1));
        Assertions.assertFalse(n0.hasChild(n2));
        Assertions.assertFalse(n1.hasChild(n0));
    }

    @Test
    void testHasRemainingParents()
    {
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());
        final Node<Object> n2 = new Node<>(dag, new Object());
        final Node<Object> n3 = new Node<>(dag, new Object());

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
        final DirectedAcyclicGraph<Object> dag = newMockedGraph();
        final Node<Object> n0 = new Node<>(dag, new Object());
        final Node<Object> n1 = new Node<>(dag, new Object());

        n1.addParent(n0);
        n0.addParent(n1);

        // Ensure that even in case of a cyclic graph, Node#toString()
        // does not result in issues like a StackOverflow.
        Assertions.assertDoesNotThrow(n0::toString);
    }

    @SuppressWarnings("unchecked")
    private static <T> DirectedAcyclicGraph<T> newMockedGraph()
    {
        return (DirectedAcyclicGraph<T>) Mockito.mock(DirectedAcyclicGraph.class);
    }
}
