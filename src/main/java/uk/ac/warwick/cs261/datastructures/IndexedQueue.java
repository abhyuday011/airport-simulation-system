package uk.ac.warwick.cs261.datastructures;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A generic FIFO queue that supports O(1) arbitrary element removal and membership
 * lookup by augmenting a doubly linked list with a position index.
 *
 * <p>The data structure combines two internal components whose invariants are kept in
 * strict lockstep by every mutating operation:
 * <ul>
 *   <li>A <b>doubly linked list</b> composed of {@link IndexedQueueNode} instances that
 *       maintains insertion order and enables O(1) node unlinking without a predecessor
 *       scan, since each node holds explicit references to both its successor and its
 *       predecessor.</li>
 *   <li>A <b>hash map</b> ({@code index}) that maps each live element directly to its
 *       {@link IndexedQueueNode}, enabling O(1) average lookup by value and reducing
 *       arbitrary removal from the O(n) scan of a conventional linked list to O(1)
 *       average.</li>
 * </ul>
 *
 * <p>The {@link #head} pointer always references the node that will be returned by the
 * next {@link #poll()} call, and the {@link #tail} pointer always references the node
 * most recently added by {@link #offer(Object)}. Both pointers are updated by every
 * structural modification to keep insertion and removal at either end in O(1).
 *
 * <p><b>Complexity summary:</b>
 * <table border="1">
 *   <caption>Time complexities for core operations</caption>
 *   <tr><th>Operation</th><th>Time</th></tr>
 *   <tr><td>{@link #offer(Object)}</td><td>O(1) average</td></tr>
 *   <tr><td>{@link #poll()}</td><td>O(1)</td></tr>
 *   <tr><td>{@link #peek()}</td><td>O(1)</td></tr>
 *   <tr><td>{@link #remove(Object)}</td><td>O(1) average</td></tr>
 *   <tr><td>{@link #contains(Object)}</td><td>O(1) average</td></tr>
 *   <tr><td>{@link #addAll(Collection)}</td><td>O(k) for k elements added</td></tr>
 *   <tr><td>{@link #removeAll(Collection)}</td><td>O(k) average for k elements removed</td></tr>
 *   <tr><td>{@link #retainAll(Collection)}</td><td>O(n) average</td></tr>
 * </table>
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code null} elements are not permitted; {@link #offer(Object)} throws
 *       {@link NullPointerException} on a {@code null} argument, while
 *       {@link #remove(Object)} returns {@code false} without throwing.</li>
 *   <li>Duplicate elements are not permitted; an element already present in the queue
 *       is rejected by {@link #offer(Object)}, which returns {@code false} for that
 *       element without modifying the queue.</li>
 *   <li>This class does not support concurrent access from multiple threads without
 *       external synchronisation.</li>
 * </ul>
 *
 * @param <T> the type of elements held in this queue
 */
public class IndexedQueue<T> extends AbstractQueue<T>
{
    /** Maps each live element to its {@link IndexedQueueNode} for O(1) average lookup and removal. */
    private final Map<T, IndexedQueueNode> index = new HashMap<>();

    /** The front of the queue; the node whose value will be returned by the next {@link #poll()} call, or {@code null} if the queue is empty. */
    private IndexedQueueNode head;

    /** The back of the queue; the node most recently inserted by {@link #offer(Object)}, or {@code null} if the queue is empty. */
    private IndexedQueueNode tail;

    /** The number of live elements currently in the queue; reflects the size of both {@link #index} and the linked list simultaneously. */
    private int size = 0;

    /**
     * Constructs an empty {@code IndexedQueue}.
     */
    public IndexedQueue() {}

    /**
     * Constructs an {@code IndexedQueue} pre-populated with the elements of the given
     * collection, in the order returned by the collection's iterator.
     *
     * <p>Internally this delegates to {@link #addAll(Collection)}, which calls
     * {@link #offer(Object)} for each element in iteration order. {@code null} and
     * duplicate elements in the collection are silently ignored.
     *
     * @param elements the collection whose elements are to be placed into this queue;
     *                 must not be {@code null}
     * @throws NullPointerException if {@code elements} is {@code null}
     */
    public IndexedQueue(Collection<? extends T> elements) { addAll(elements); }

    /**
     * Inserts the specified element at the tail of this queue.
     *
     * <p>A new {@link IndexedQueueNode} is created, linked as the successor of the
     * current {@link #tail}, and recorded in the {@link #index} map. If the queue was
     * empty, {@link #head} is also updated to point to the new node. The operation is
     * rejected without modifying any state if the element is already present in the
     * queue, since duplicates are not permitted.
     *
     * @param element the element to add; must not be {@code null}
     * @return {@code true} if the element was successfully added; {@code false} if it
     *         is already present in the queue
     * @throws NullPointerException if {@code element} is {@code null}
     */
    @Override
    public boolean offer(T element)
    {
        if (element == null)
            throw new NullPointerException();

        if (index.containsKey(element))
            return false;

        IndexedQueueNode oldTail = tail;
        tail = new IndexedQueueNode(element, null, oldTail);

        if (oldTail != null)
            oldTail.setNext(tail);

        if (head == null)
            head = tail;

        index.put(element, tail);
        size++;

        return true;
    }

    /**
     * Inserts all elements of the given collection at the tail of this queue, in the
     * order returned by the collection's iterator.
     *
     * <p>Each element is passed to {@link #offer(Object)} individually. {@code null}
     * elements cause a {@link NullPointerException} to be thrown immediately; elements
     * already present in the queue are silently skipped. Elements successfully added
     * before a {@code NullPointerException} is thrown remain in the queue.
     *
     * @param elements the collection of elements to add; must not be {@code null}, and
     *                 must not contain {@code null} elements
     * @return {@code true} if at least one element was added; {@code false} if every
     *         element was already present in the queue
     * @throws NullPointerException if {@code elements} is {@code null}, or if any
     *                              element within it is {@code null}
     */
    @Override
    public boolean addAll(Collection<? extends T> elements)
    {
        boolean modified = false;

        for (T element : elements)
        {
            if (offer(element))
                modified = true;
        }

        return modified;
    }

    /**
     * Retrieves, but does not remove, the element at the head of this queue.
     *
     * <p>Because {@link #head} always references the oldest node in the linked list,
     * this operation runs in O(1) regardless of queue size.
     *
     * @return the element at the head of the queue, or {@code null} if the queue is empty
     */
    @Override
    public T peek()
    {
        return head != null ? head.getValue() : null;
    }

    /**
     * Retrieves and removes the element at the head of this queue, delegating the
     * unlinking and index cleanup to {@link #removeNode(IndexedQueueNode)}.
     *
     * @return the element that was at the head of the queue, or {@code null} if the
     *         queue is empty
     */
    @Override
    public T poll()
    {
        return removeNode(head);
    }

    /**
     * Removes the specified element from the queue regardless of its position, in
     * O(1) average time.
     *
     * <p>The element's node is retrieved directly from the {@link #index} map, bypassing
     * any traversal of the linked list. The node is then unlinked via
     * {@link #removeNode(IndexedQueueNode)}. Passing {@code null} returns {@code false}
     * immediately without consulting the map.
     *
     * @param element the element to remove; passing {@code null} returns {@code false}
     *                without throwing
     * @return {@code true} if the element was present and has been removed;
     *         {@code false} if it was {@code null} or not found in the queue
     */
    @Override
    public boolean remove(Object element)
    {
        if (element == null)
            return false;

        @SuppressWarnings("unchecked")
        IndexedQueueNode node = index.get((T) element);

        return removeNode(node) != null;
    }

    /**
     * Removes from this queue all elements that are contained in the specified collection.
     *
     * <p>Each element of {@code c} is removed individually via {@link #remove(Object)}.
     * The overall cost is O(k) average, where {@code k} is the number of elements in
     * {@code c} that are actually present in the queue.
     *
     * @param c the collection of elements to remove; must not be {@code null}
     * @return {@code true} if at least one element was removed; {@code false} if the
     *         queue was unchanged
     * @throws NullPointerException if {@code c} is {@code null}
     */
    @Override
    public boolean removeAll(Collection<?> c)
    {
        boolean modified = false;

        for (Object e : c)
        {
            if (remove(e))
                modified = true;
        }

        return modified;
    }

    /**
     * Retains only the elements in this queue that are contained in the specified
     * collection, removing all others while preserving the relative insertion order of
     * the retained elements.
     *
     * <p>The current queue contents are traversed via {@link #iterator()} and any element
     * absent from {@code c} is collected into a temporary removal list. All collected
     * elements are then removed via {@link #remove(Object)}. Building the removal list
     * before performing any removals avoids concurrent-modification issues that would
     * arise from unlinking nodes during iteration. The total cost is O(n) average, where
     * {@code n} is the current queue size.
     *
     * @param c the collection of elements to retain; must not be {@code null}
     * @return {@code true} if at least one element was removed; {@code false} if the
     *         queue was unchanged
     * @throws NullPointerException if {@code c} is {@code null}
     */
    @Override
    public boolean retainAll(Collection<?> c)
    {
        boolean modified = false;

        List<T> toRemove = new ArrayList<>();
        for (T e : this)
        {
            if (!c.contains(e))
                toRemove.add(e);
        }

        for (T e : toRemove)
        {
            remove(e);
            modified = true;
        }

        return modified;
    }

    /**
     * Returns {@code true} if this queue contains the specified element.
     *
     * <p>The check is delegated to the internal {@link #index} map and therefore runs
     * in O(1) average time, in contrast to the O(n) traversal that a plain linked list
     * would require.
     *
     * @param element the element whose presence is to be tested; {@code null} returns
     *                {@code false} without throwing
     * @return {@code true} if this queue contains {@code element}; {@code false} otherwise
     */
    @Override
    public boolean contains(Object element)
    {
        return index.containsKey(element);
    }

    /**
     * Returns the number of live elements currently in this queue.
     *
     * <p>This value is maintained incrementally by {@link #offer(Object)} and
     * {@link #removeNode(IndexedQueueNode)} and always equals the number of entries
     * in both the linked list and the {@link #index} map simultaneously.
     *
     * @return the number of elements in this queue; {@code 0} if the queue is empty
     */
    @Override
    public int size() { return size; }

    /**
     * Returns an iterator over the elements in this queue in head-to-tail (insertion)
     * order.
     *
     * <p>The iterator does not support {@link Iterator#remove()} and does not detect
     * concurrent structural modifications. Modifying the queue while iterating over it
     *, by calling {@link #offer(Object)}, {@link #poll()}, or {@link #remove(Object)}
     *, may produce unpredictable results.
     *
     * @return an iterator over the elements of this queue from {@link #head} to
     *         {@link #tail} in insertion order
     */
    @Override
    public Iterator<T> iterator()
    {
        return new IndexedQueueIterator();
    }

    /**
     * Unlinks the given node from the doubly linked list, repairs its neighbours'
     * cross-references, updates {@link #head} and {@link #tail} if necessary, removes
     * the element from the {@link #index} map, and decrements {@link #size}.
     *
     * <p>The unlinking proceeds as follows:
     * <ol>
     *   <li>Retrieve the node's predecessor ({@code prev}) and successor ({@code next}).</li>
     *   <li>If a predecessor exists, redirect its {@code next} pointer to {@code next},
     *       bypassing the removed node.</li>
     *   <li>If a successor exists, redirect its {@code prev} pointer to {@code prev},
     *       bypassing the removed node.</li>
     *   <li>Null out the removed node's own {@code next} and {@code prev} references to
     *       aid garbage collection and prevent stale pointer chains.</li>
     *   <li>If the removed node was {@link #head}, advance {@link #head} to
     *       {@code next}.</li>
     *   <li>If the removed node was {@link #tail}, retract {@link #tail} to
     *       {@code prev}.</li>
     * </ol>
     *
     * <p>Passing {@code null}, which occurs when {@link #poll()} is called on an empty
     * queue, or when {@link #remove(Object)} is called with an element not in the queue
     *, is handled gracefully by returning {@code null} immediately.
     *
     * @param node the node to remove, or {@code null} to signal an empty or not-found case
     * @return the value that was stored in {@code node}, or {@code null} if {@code node}
     *         was {@code null}
     */
    private T removeNode(IndexedQueueNode node)
    {
        if (node == null)
            return null;

        IndexedQueueNode prev = node.getPrev();
        IndexedQueueNode next = node.getNext();

        if (prev != null) prev.setNext(next);
        if (next != null) next.setPrev(prev);

        node.setNext(null);
        node.setPrev(null);

        if (node == head) head = next;
        if (node == tail) tail = prev;

        T value = node.getValue();
        index.remove(value);
        size--;

        return value;
    }

    /**
     * A node in the internal doubly linked list that backs this queue.
     *
     * <p>Each node holds a single queue element together with references to its immediate
     * predecessor and successor, enabling O(1) splicing during both insertion at the tail
     * and removal at any position. All structural modifications to the list are routed
     * through {@link #removeNode(IndexedQueueNode)} and {@link #offer(Object)}, which
     * keep every node's neighbour references consistent at all times.
     */
    private class IndexedQueueNode
    {
        /** The queue element stored in this node; never {@code null} for a live node. */
        private T value;

        /** The successor node towards the tail, or {@code null} if this node is the tail. */
        private IndexedQueueNode next;

        /** The predecessor node towards the head, or {@code null} if this node is the head. */
        private IndexedQueueNode prev;

        /**
         * Constructs a new node with the given value and neighbour references.
         *
         * @param value the element to store in this node; must not be {@code null} for a
         *              live queue node
         * @param next  the node that follows this one towards the tail, or {@code null}
         *              if this node will be the tail
         * @param prev  the node that precedes this one towards the head, or {@code null}
         *              if this node will be the head
         */
        public IndexedQueueNode(T value, IndexedQueueNode next, IndexedQueueNode prev)
        {
            this.value = value;
            this.next = next;
            this.prev = prev;
        }

        /**
         * Returns the element stored in this node.
         *
         * @return the element stored in this node; never {@code null} for a live node
         */
        public T getValue() { return value; }

        /**
         * Replaces the element stored in this node.
         *
         * @param value the new element; must not be {@code null} for a live node
         */
        public void setValue(T value) { this.value = value; }

        /**
         * Returns the successor node towards the tail of the queue.
         *
         * @return the next node, or {@code null} if this is the tail node
         */
        public IndexedQueueNode getNext() { return next; }

        /**
         * Sets the successor node reference; pass {@code null} when this node becomes
         * the tail after a removal or the initial insertion into an empty queue.
         *
         * @param next the new successor node, or {@code null} if this node is the tail
         */
        public void setNext(IndexedQueueNode next) { this.next = next; }

        /**
         * Returns the predecessor node towards the head of the queue.
         *
         * @return the previous node, or {@code null} if this is the head node
         */
        public IndexedQueueNode getPrev() { return prev; }

        /**
         * Sets the predecessor node reference; pass {@code null} when this node becomes
         * the head after a removal or the initial insertion into an empty queue.
         *
         * @param prev the new predecessor node, or {@code null} if this node is the head
         */
        public void setPrev(IndexedQueueNode prev) { this.prev = prev; }
    }

    /**
     * A forward iterator that traverses the queue from {@link #head} to {@link #tail}
     * in insertion order.
     *
     * <p>The iterator captures a reference to the {@link #head} node at construction
     * time and advances its {@code current} pointer on each call to {@link #next()}.
     * It does not detect concurrent structural modifications and does not support
     * {@link #remove()}; modifying the queue during iteration may produce unpredictable
     * results.
     */
    private class IndexedQueueIterator implements Iterator<T>
    {
        /**
         * The node whose value will be returned by the next call to {@link #next()};
         * initialised to {@link #head} at construction and advanced to each node's
         * successor on every successful {@link #next()} call.
         */
        private IndexedQueueNode current = head;

        /**
         * Returns {@code true} if there are more elements to iterate over.
         *
         * @return {@code true} if {@link #next()} would return an element;
         *         {@code false} if the tail has been passed or the queue was empty at
         *         construction time
         */
        @Override
        public boolean hasNext()
        {
            return current != null;
        }

        /**
         * Returns the next element in head-to-tail order and advances the iterator to
         * the following node.
         *
         * @return the next element in insertion order
         * @throws NoSuchElementException if the iterator has no more elements
         */
        @Override
        public T next()
        {
            if (current == null)
                throw new NoSuchElementException();

            T value = current.getValue();
            current = current.getNext();
            return value;
        }
    }
}