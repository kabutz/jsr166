/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain. Use, modify, and
 * redistribute this code in any way without acknowledgement.
 */

package java.util.concurrent;
import java.util.concurrent.atomic.*;

/**
 * A linked list node supporting atomic operations on both item and
 * next fields, Used by non-blocking linked-list based classes.
 * @since 1.5
 * @author Doug Lea
 */

final class AtomicLinkedNode {
    private volatile Object item;
    private volatile AtomicLinkedNode next;

    private static final 
        AtomicReferenceFieldUpdater<AtomicLinkedNode, AtomicLinkedNode> 
        nextUpdater =
        AtomicReferenceFieldUpdater.newUpdater
        (AtomicLinkedNode.class, AtomicLinkedNode.class, "next");
    private static final 
        AtomicReferenceFieldUpdater<AtomicLinkedNode, Object> 
        itemUpdater =
        AtomicReferenceFieldUpdater.newUpdater
        (AtomicLinkedNode.class, Object.class, "item");

    AtomicLinkedNode(Object x) { item = x; }

    AtomicLinkedNode(Object x, AtomicLinkedNode n) { item = x; next = n; }

    Object getItem() {
        return item;
    }

    boolean casItem(Object cmp, Object val) {
        return itemUpdater.compareAndSet(this, cmp, val);
    }

    void setItem(Object val) {
        itemUpdater.set(this, val);
    }

    AtomicLinkedNode getNext() {
        return next;
    }

    boolean casNext(AtomicLinkedNode cmp, AtomicLinkedNode val) {
        return nextUpdater.compareAndSet(this, cmp, val);
    }

    void setNext(AtomicLinkedNode val) {
        nextUpdater.set(this, val);
    }

}
