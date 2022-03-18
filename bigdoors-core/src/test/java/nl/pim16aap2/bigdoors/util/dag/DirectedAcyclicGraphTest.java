package nl.pim16aap2.bigdoors.util.dag;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;

class DirectedAcyclicGraphTest
{
    private static final TestClass o0 = new TestClass("o0");
    private static final TestClass o1 = new TestClass("o1");
    private static final TestClass o2 = new TestClass("o2");
    private static final TestClass o3 = new TestClass("o3");
    private static final TestClass o4 = new TestClass("o4");

    @Test
    void testFailFast()
    {
        final DirectedAcyclicGraph<Object> failFast = new DirectedAcyclicGraph<>(true);
        failFast.addConnectedNodes(o0, o1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> failFast.addConnection(o1, o0));


        final DirectedAcyclicGraph<Object> failSlow = new DirectedAcyclicGraph<>(false);
        failSlow.addConnectedNodes(o0, o1);
        Assertions.assertDoesNotThrow(failSlow::iterator);

        failSlow.addConnectedNodes(o1, o0);
        Assertions.assertThrows(IllegalStateException.class, failSlow::iterator);
    }

    @Test
    void testGetNode()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        final Node<Object> n0 = graph.addNode(o0);
        Assertions.assertNotNull(n0);
        Assertions.assertEquals(n0, graph.getNode(o0).orElse(null));
        Assertions.assertTrue(graph.getNode(o1).isEmpty());
    }

    @Test
    void testGetChildren()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        graph.addConnectedNodes(o1, o0);
        graph.addConnectedNodes(o2, o1);
        graph.addConnectedNodes(o3, o1);

        Assertions.assertEquals(List.of(o1, o2, o3), graph.getAllChildren(o0));
    }

    @Test
    void testAvoidDuplicates()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        graph.addConnectedNodes(o0, o1);
        graph.addConnectedNodes(o0, o1);
        graph.addConnectedNodes(o1, o0);
        graph.addNode(o0);
        Assertions.assertEquals(2, graph.size());
    }

    @Test
    void testModCount()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        graph.addNode(o0);
        Assertions.assertEquals(1, graph.getModCount());

        graph.addConnectedNodes(o0, o1); // modCount += 2; +1 for addNode(o1) and +1 for connection.
        Assertions.assertEquals(3, graph.getModCount());
        graph.addConnectedNodes(o0, o1); // no change, as nothing happened.
        Assertions.assertEquals(3, graph.getModCount());
        graph.addConnection(o1, o0);
        graph.addConnection(o1, o0);
        Assertions.assertEquals(4, graph.getModCount());

        graph.remove(o0);
        Assertions.assertEquals(5, graph.getModCount());
    }

    @Test
    void testRemoval()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        final Node<Object> node0 = graph.addNode(o0);
        final Node<Object> node1 = graph.addNode(o1);
        graph.addNode(o2);
        graph.addNode(o3);

        graph.addConnection(o0, o1);
        graph.addConnection(o1, o0);
        graph.addConnection(o1, o2);
        graph.addConnection(o3, o0);

        Assertions.assertEquals(1, graph.getLeaves().size()); // Only o2 is a leaf node
        graph.removeAll(List.of(o0));
        Assertions.assertEquals(2, graph.getLeaves().size()); // o3 is now a leaf nodes as well

        Assertions.assertEquals(3, graph.size());
        Assertions.assertFalse(node1.hasChildren());
        Assertions.assertFalse(node1.hasParent(node0));
        Assertions.assertTrue(node1.hasParents());

        graph.removeAllNodes(List.of(node1));
        Assertions.assertEquals(2, graph.getLeaves().size());
        Assertions.assertEquals(2, graph.size());

        Assertions.assertNull(graph.remove(new Node<>(graph, new Object())));
    }

    @Test
    void testRemoveAllNodes()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        final Node<Object> node2 = graph.addNode(o2);
        graph.addConnectedNodes(o1, o0);
        graph.addConnectedNodes(o2, o1);
        graph.addConnectedNodes(o3, o2);
        graph.addConnectedNodes(o4, o2);

        Assertions.assertEquals(5, graph.size());
        graph.removeAllNodes(node2.getAllChildren());
        Assertions.assertEquals(3, graph.size());
    }

    @Test
    void testGetAllChildren()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        final Node<Object> node0 = graph.addNode(o0);
        final Node<Object> node1 = graph.addNode(o1);
        final Node<Object> node2 = graph.addNode(o2);
        final Node<Object> node3 = graph.addNode(o3);
        graph.addConnection(o0, o1);
        graph.addConnection(o1, o0);
        graph.addConnection(o2, o1);
        graph.addConnection(o3, o2);

        Assertions.assertThrows(IllegalStateException.class, node0::getAllChildren);

        graph.removeConnection(o0, o1);
        Assertions.assertEquals(List.of(node2, node3), new ArrayList<>(node1.getAllChildren()));
    }

    @Test
    void testAvoidSelfReference()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        Assertions.assertThrows(IllegalArgumentException.class, () -> graph.addConnectedNodes(o0, o0));
    }

    @Test
    void testVerifyAcyclic()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        Assertions.assertDoesNotThrow(graph::verifyAcyclic);
        graph.addConnectedNodes(o1, o0);
        Assertions.assertDoesNotThrow(graph::verifyAcyclic);
        graph.addConnectedNodes(o0, o1);
        Assertions.assertThrows(IllegalStateException.class, graph::verifyAcyclic);

        graph.clear();
        // Big circle!
        graph.addConnectedNodes(o1, o0);
        graph.addConnectedNodes(o2, o1);
        graph.addConnectedNodes(o3, o2);
        graph.addConnectedNodes(o0, o3);
        Assertions.assertThrows(IllegalStateException.class, graph::verifyAcyclic);

        graph.addConnectedNodes(o1, o4);
        Assertions.assertThrows(IllegalStateException.class, graph::verifyAcyclic);

        graph.removeConnection(o1, o0);
        Assertions.assertDoesNotThrow(graph::verifyAcyclic);
    }

    @Test
    void testLeafPath()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();
        graph.addConnectedNodes(o1, o0);
        graph.addConnectedNodes(o2, o1);
        graph.addConnectedNodes(o3, o2);
        graph.addConnectedNodes(o4, o2);
        Assertions.assertEquals(List.of(o0, o1, o2, o3, o4), graph.getLeafPath());

        graph.clear();
        graph.addNode(o0);
        graph.addConnectedNodes(o2, o1);
        graph.addConnectedNodes(o3, o2);
        graph.addNode(o4);
        Assertions.assertEquals(List.of(o0, o1, o4, o2, o3), graph.getLeafPath());
    }

    @Test
    void testIterator()
    {
        final DirectedAcyclicGraph<Object> graph = new DirectedAcyclicGraph<>();

        var it = graph.iterator();
        Assertions.assertFalse(it.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, it::next);

        graph.addConnectedNodes(o1, o0);
        graph.addConnectedNodes(o2, o1);
        graph.addConnectedNodes(o3, o2);

        it = graph.iterator();
        graph.addConnectedNodes(o4, o3);
        Assertions.assertThrows(ConcurrentModificationException.class, it::next);

        it = graph.iterator();
        for (final Node<Object> node : graph.getLeafNodePath())
        {
            Assertions.assertTrue(it.hasNext());
            Assertions.assertEquals(node, it.next());
        }
        Assertions.assertFalse(it.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, it::next);
    }

    @AllArgsConstructor
    private static final class TestClass
    {
        private final String name;

        @Override
        public String toString()
        {
            return name;
        }
    }
}
