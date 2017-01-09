/*
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @modules java.base/java.util.concurrent:open
 * @run testng WhiteBox
 * @summary White box tests of implementation details
 */

import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadLocalRandom;
import static java.util.stream.Collectors.toList;
import java.util.function.Consumer;
import java.util.function.Function;

@Test
public class WhiteBox {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final VarHandle HEAD, TAIL, ITEM, NEXT;

    public WhiteBox() throws ReflectiveOperationException {
        Class<?> qClass = LinkedTransferQueue.class;
        Class<?> nodeClass = Class.forName(qClass.getName() + "$Node");
        MethodHandles.Lookup lookup
            = MethodHandles.privateLookupIn(qClass, MethodHandles.lookup());
        HEAD = lookup.findVarHandle(qClass, "head", nodeClass);
        TAIL = lookup.findVarHandle(qClass, "tail", nodeClass);
        NEXT = lookup.findVarHandle(nodeClass, "next", nodeClass);
        ITEM = lookup.findVarHandle(nodeClass, "item", Object.class);
    }

    Object head(LinkedTransferQueue q) { return HEAD.getVolatile(q); }
    Object tail(LinkedTransferQueue q) { return TAIL.getVolatile(q); }
    Object item(Object node)           { return ITEM.getVolatile(node); }
    Object next(Object node)           { return NEXT.getVolatile(node); }

    int nodeCount(LinkedTransferQueue q) {
        int i = 0;
        for (Object p = head(q); p != null; ) {
            i++;
            if (p == (p = next(p))) p = head(q);
        }
        return i;
    }

    int tailCount(LinkedTransferQueue q) {
        int i = 0;
        for (Object p = tail(q); p != null; ) {
            i++;
            if (p == (p = next(p))) p = head(q);
        }
        return i;
    }

    Object findNode(LinkedTransferQueue q, Object e) {
        for (Object p = head(q); p != null; ) {
            if (item(p) != null && e.equals(item(p)))
                return p;
            if (p == (p = next(p))) p = head(q);
        }
        throw new AssertionError("not found");
    }

    Iterator iteratorAt(LinkedTransferQueue q, Object e) {
        for (Iterator it = q.iterator(); it.hasNext(); )
            if (it.next().equals(e))
                return it;
        throw new AssertionError("not found");
    }

    void assertIsSelfLinked(Object node) {
        assertSame(next(node), node);
        assertNull(item(node));
    }
    void assertIsNotSelfLinked(Object node) {
        assertNotSame(node, next(node));
    }

    @Test
    public void addRemove() {
        LinkedTransferQueue q = new LinkedTransferQueue();
        assertInvariants(q);
        assertNull(head(q));
        assertEquals(nodeCount(q), 0);
        q.add(1);
        assertEquals(nodeCount(q), 1);
        assertInvariants(q);
        q.remove(1);
        assertEquals(nodeCount(q), 1);
        assertInvariants(q);
    }

    /**
     * Traversal actions that visit every node and do nothing, but
     * have side effect of squeezing out dead nodes.
     */
    @DataProvider
    public Object[][] traversalActions() {
        return List.<Consumer<LinkedTransferQueue>>of(
            q -> q.forEach(e -> {}),
            q -> assertFalse(q.contains(new Object())),
            q -> assertFalse(q.remove(new Object())),
            q -> q.spliterator().forEachRemaining(e -> {}),
            q -> q.stream().collect(toList()),
            q -> assertFalse(q.removeIf(e -> false)),
            q -> assertFalse(q.removeAll(List.of())))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "traversalActions")
    public void traversalOperationsCollapseLeadingNodes(
        Consumer<LinkedTransferQueue> traversalAction) {
        LinkedTransferQueue q = new LinkedTransferQueue();
        Object oldHead;
        int n = 2 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) q.add(i);
        assertEquals(nodeCount(q), n);
        oldHead = head(q);
        assertEquals(q.poll(), 0);
        assertEquals(nodeCount(q), n);
        assertSame(head(q), oldHead);
        traversalAction.accept(q);
        assertInvariants(q);
        assertEquals(nodeCount(q), n - 1);
        assertIsSelfLinked(oldHead);
    }

    @Test(dataProvider = "traversalActions")
    public void traversalOperationsCollapseInteriorNodes(
        Consumer<LinkedTransferQueue> traversalAction) {
        LinkedTransferQueue q = new LinkedTransferQueue();
        int n = 6;
        for (int i = 0; i < n; i++) q.add(i);

        // We must be quite devious to reliably create an interior dead node
        Object p0 = findNode(q, 0);
        Object p1 = findNode(q, 1);
        Object p2 = findNode(q, 2);
        Object p3 = findNode(q, 3);
        Object p4 = findNode(q, 4);
        Object p5 = findNode(q, 5);

        Iterator it1 = iteratorAt(q, 1);
        Iterator it2 = iteratorAt(q, 2);

        it2.remove(); // causes it2's ancestor to advance to 1
        assertSame(next(p1), p3);
        assertSame(next(p2), p3);
        assertNull(item(p2));
        it1.remove(); // removes it2's ancestor
        assertSame(next(p0), p3);
        assertSame(next(p1), p3);
        assertSame(next(p2), p3);
        assertNull(item(p1));
        assertEquals(it2.next(), 3);
        it2.remove(); // it2's ancestor can't unlink

        assertSame(next(p0), p3); // p3 is now interior dead node
        assertSame(next(p1), p4); // it2 uselessly CASed p1.next
        assertSame(next(p2), p3);
        assertSame(next(p3), p4);
        assertInvariants(q);

        int c = nodeCount(q);
        traversalAction.accept(q);
        assertEquals(nodeCount(q), c - 1);

        assertSame(next(p0), p4);
        assertSame(next(p1), p4);
        assertSame(next(p2), p3);
        assertSame(next(p3), p4);
        assertInvariants(q);

        // trailing nodes are not unlinked
        Iterator it5 = iteratorAt(q, 5); it5.remove();
        traversalAction.accept(q);
        assertSame(next(p4), p5);
        assertNull(next(p5));
        assertEquals(nodeCount(q), c - 1);
    }

    /**
     * Traversal actions that remove every element, and are also
     * expected to squeeze out dead nodes.
     */
    @DataProvider
    public Object[][] bulkRemovalActions() {
        return List.<Consumer<LinkedTransferQueue>>of(
            q -> q.clear(),
            q -> assertTrue(q.removeIf(e -> true)),
            q -> assertTrue(q.retainAll(List.of())))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "bulkRemovalActions")
    public void bulkRemovalOperationsCollapseNodes(
        Consumer<LinkedTransferQueue> bulkRemovalAction) {
        LinkedTransferQueue q = new LinkedTransferQueue();
        int n = 1 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) q.add(i);
        bulkRemovalAction.accept(q);
        assertEquals(nodeCount(q), 1);
        assertInvariants(q);
    }

    /**
     * Actions that remove the first element, and are expected to
     * leave at most one slack dead node at head.
     */
    @DataProvider
    public Object[][] pollActions() {
        return List.<Consumer<LinkedTransferQueue>>of(
            q -> assertNotNull(q.poll()),
            q -> assertNotNull(q.remove()))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "pollActions")
    public void pollActionsOneNodeSlack(
        Consumer<LinkedTransferQueue> pollAction) {
        LinkedTransferQueue q = new LinkedTransferQueue();
        int n = 1 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) q.add(i);
        assertEquals(nodeCount(q), n);
        for (int i = 0; i < n; i++) {
            int c = nodeCount(q);
            boolean slack = item(head(q)) == null;
            if (slack) assertNotNull(item(next(head(q))));
            pollAction.accept(q);
            assertEquals(nodeCount(q), q.isEmpty() ? 1 : c - (slack ? 2 : 0));
        }
        assertInvariants(q);
    }

    /**
     * Actions that append an element, and are expected to
     * leave at most one slack node at tail.
     */
    @DataProvider
    public Object[][] addActions() {
        return List.<Consumer<LinkedTransferQueue>>of(
            q -> q.add(1),
            q -> q.offer(1))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "addActions")
    public void addActionsOneNodeSlack(
        Consumer<LinkedTransferQueue> addAction) {
        LinkedTransferQueue q = new LinkedTransferQueue();
        int n = 1 + rnd.nextInt(9);
        for (int i = 0; i < n; i++) {
            int c = tailCount(q);
            assertTrue(c <= 2); // tail slack at most 1
            if (i < 2) assertEquals(c, 0);
            addAction.accept(q);
            // tail slack toggles between 0 and 1
            if (i > 0) assertEquals(Math.abs(tailCount(q) - c), 1);
        }
        assertInvariants(q);
    }

    /** Checks conditions which should always be true. */
    void assertInvariants(LinkedTransferQueue q) {
        // Unlike CLQ, head and tail are initially null
        if (head(q) != null) {
            // tail is only initialized when second element added!
            //assertNotNull(tail(q));
            // head is never self-linked (but tail may!)
            for (Object h; next(h = head(q)) == h; )
                assertNotSame(h, head(q)); // must be update race
        }
    }
}
