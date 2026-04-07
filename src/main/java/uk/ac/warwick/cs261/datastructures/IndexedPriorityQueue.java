package uk.ac.warwick.cs261.datastructures;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A generic min-priority queue that supports O(log n) arbitrary element removal and
 * O(1) membership lookup by augmenting a standard binary heap with a position index.
 *
 * <p>The data structure combines two internal components whose invariants are kept in
 * strict lockstep by every mutating operation:
 * <ul>
 *   <li>A <b>min-heap</b> backed by an {@link ArrayList}, storing live elements at
 *       indices {@code 0} through {@code size - 1} and satisfying the property that every
 *       parent is less than or equal to both of its children under
 *       {@link Comparator#naturalOrder()}. The smallest element therefore always resides
 *       at index {@code 0} and is returned in O(1) by {@link #peek()}.</li>
 *   <li>A <b>hash map</b> ({@code index}) that records each live element's current
 *       position within the heap array. Because every {@link #swap(int, int)} call
 *       updates both structures simultaneously, the map always reflects the heap's true
 *       layout, enabling O(1) average lookup via {@link #contains(Object)} and reducing
 *       arbitrary removal from the O(n) scan of a plain heap to O(log n).</li>
 * </ul>
 *
 * <p>When multiple elements are added at once via {@link #addAll(Collection)}, all
 * eligible elements are placed into the heap array first and then a single bottom-up
 * {@link #heapify()} pass restores the heap invariant in O(n) time. This is
 * asymptotically superior to the O(n log n) cost of inserting each element individually
 * via {@link #offer(Object)}.
 *
 * <p>Out-of-bounds child slots encountered during sift operations are represented as
 * {@code null} and handled transparently by {@link #nullsLastComparator}, which places
 * {@code null} after all real elements so that leaf nodes are never mistakenly promoted.
 *
 * <p><b>Complexity summary:</b>
 * <table border="1">
 *   <caption>Time complexities for core operations</caption>
 *   <tr><th>Operation</th><th>Time</th></tr>
 *   <tr><td>{@link #offer(Object)}</td><td>O(log n) average</td></tr>
 *   <tr><td>{@link #poll()}</td><td>O(log n)</td></tr>
 *   <tr><td>{@link #peek()}</td><td>O(1)</td></tr>
 *   <tr><td>{@link #remove(Object)}</td><td>O(log n) average</td></tr>
 *   <tr><td>{@link #contains(Object)}</td><td>O(1) average</td></tr>
 *   <tr><td>{@link #addAll(Collection)}</td><td>O(n) for bulk insert</td></tr>
 *   <tr><td>{@link #removeAll(Collection)}</td><td>O(k log(n)) average for k elements removed</td></tr>
 *   <tr><td>{@link #retainAll(Collection)}</td><td>O((n - k) + k log(n)) worst case</td></tr>
 * </table>
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>{@code null} elements are not permitted; {@link #offer(Object)} and
 *       {@link #addAll(Collection)} both throw {@link NullPointerException} on a
 *       {@code null} element.</li>
 *   <li>Duplicate elements are not permitted; an element already present in the queue
 *       is silently skipped by {@link #offer(Object)} and {@link #addAll(Collection)},
 *       both of which return {@code false}/{@code false} for that element.</li>
 *   <li>Elements must implement {@link Comparable}; ordering follows
 *       {@link Comparator#naturalOrder()}, so the globally smallest element is always at
 *       the head of the queue.</li>
 *   <li>This queue is <b>not stable</b>: elements with equal natural ordering have no
 *       guaranteed relative retrieval sequence.</li>
 *   <li>This class does not support concurrent access from multiple threads without
 *       external synchronisation.</li>
 * </ul>
 *
 * @param <T> the type of elements held in this queue; must implement {@link Comparable}
 */
public class IndexedPriorityQueue<T extends Comparable<T>> extends AbstractQueue<T>
{
    /**
     * Comparator used throughout heap sift operations to treat out-of-bounds child slots
     *, represented as {@code null}, as larger than any real element, ensuring that
     * {@link #downheap(int)} never promotes a phantom leaf node above a real one.
     */
    private final Comparator<T> nullsLastComparator = Comparator.nullsLast(Comparator.naturalOrder());

    /**
     * The heap array backing this priority queue. Elements at indices {@code 0} through
     * {@code size - 1} are live heap entries satisfying the min-heap invariant; slots at
     * indices {@code size} and beyond may hold {@code null} tombstones left by previous
     * removals and are never examined by heap operations.
     */
    private final List<T> minHeap = new ArrayList<>();

    /**
     * Maps each live element to its current index in {@link #minHeap}, enabling O(1)
     * average membership tests and O(log n) arbitrary removal. Every {@link #swap(int, int)}
     * call updates this map so that it always reflects the heap's true layout.
     */
    private final Map<T, Integer> index = new HashMap<>();

    /** The number of live elements currently occupying indices {@code 0} through {@code size - 1} of {@link #minHeap}. */
    private int size = 0;

    /**
     * Constructs an empty {@code IndexedPriorityQueue}.
     */
    public IndexedPriorityQueue() {}

    /**
     * Constructs an {@code IndexedPriorityQueue} pre-populated with the elements of the
     * given collection, restoring the heap invariant in a single O(n) pass.
     *
     * <p>Internally this delegates to {@link #addAll(Collection)}, which places all
     * eligible elements into the heap array and then calls {@link #heapify()} once.
     * {@code null} and duplicate elements in the collection are silently ignored.
     *
     * @param elements the collection whose elements are to be placed into this queue;
     *                 must not be {@code null}
     * @throws NullPointerException if {@code elements} is {@code null}
     */
    public IndexedPriorityQueue(Collection<? extends T> elements) { addAll(elements); }

    /**
     * Inserts the specified element into this priority queue, maintaining the min-heap
     * invariant by sifting the new element upward from the tail of the heap.
     *
     * <p>The element is appended at the next available slot (index {@code size}),
     * reusing an existing {@code null} tombstone slot in the backing list if one is
     * available, or growing the list otherwise. {@link #upheap(int)} is then called to
     * bubble the element up until neither it nor any ancestor violates the min-heap
     * invariant.
     *
     * <p>If {@code size} is currently less than {@link List#size() minHeap.size()}, the
     * slot at index {@code size} is overwritten rather than appended, avoiding unnecessary
     * list growth caused by tombstone slots left by earlier removals.
     *
     * @param element the element to add; must not be {@code null}
     * @return {@code true} if the element was successfully inserted; {@code false} if
     *         it is already present in the queue
     * @throws NullPointerException if {@code element} is {@code null}
     */
    @Override
    public boolean offer(T element)
    {
        if (element == null)
            throw new NullPointerException();

        if (index.containsKey(element))
            return false;

        if (size < minHeap.size())
            minHeap.set(size, element);
        else
            minHeap.add(element);

        index.put(element, size);
        size++;

        upheap(size - 1);

        return true;
    }

    /**
     * Retrieves, but does not remove, the minimum element in this priority queue.
     *
     * <p>The minimum element always resides at index {@code 0} of {@link #minHeap} as a
     * consequence of the min-heap invariant; this method therefore runs in O(1).
     *
     * @return the minimum element according to {@link Comparator#naturalOrder()}, or
     *         {@code null} if this queue is empty
     */
    @Override
    public T peek()
    {
        return size != 0 ? minHeap.get(0) : null;
    }

    /**
     * Retrieves and removes the minimum element from this priority queue, restoring the
     * min-heap invariant by sifting the promoted tail element downward from the root.
     *
     * <p>The removal proceeds in four steps:
     * <ol>
     *   <li>Record the root element (index {@code 0}) as the return value.</li>
     *   <li>Swap the root with the last live element at index {@code size - 1}.</li>
     *   <li>Clear the vacated slot (now at index {@code size - 1}) by writing {@code null}
     *       and removing the root element from the {@link #index} map.</li>
     *   <li>Call {@link #downheap(int)} at index {@code 0} to sink the promoted element
     *       into its correct heap position.</li>
     * </ol>
     *
     * @return the minimum element in the queue, or {@code null} if the queue is empty
     */
    @Override
    public T poll()
    {
        if (size == 0)
            return null;

        int last_index = size - 1;
        T head = minHeap.get(0);

        swap(0, last_index);
        minHeap.set(last_index, null);
        index.remove(head);
        size--;

        downheap(0);

        return head;
    }

    /**
     * Adds all elements of the given collection to this queue in a single O(n) bulk
     * operation by deferring heap-invariant restoration to a single {@link #heapify()}
     * call after all insertions.
     *
     * <p>Each element is first checked for {@code null} and duplicate status. Eligible
     * elements are placed into the heap array directly, reusing tombstone slots where
     * available, and recorded in the {@link #index} map. Once all elements have been
     * staged, {@link #heapify()} performs a bottom-up sift pass over every internal node
     * in O(n) total time. This is more efficient than calling {@link #offer(Object)} for
     * each element individually, which would cost O(n log n).
     *
     * <p>{@code null} elements cause a {@link NullPointerException} to be thrown
     * immediately; any elements already staged at that point remain in the queue.
     * Duplicate elements are silently skipped and do not count towards the return value.
     *
     * @param elements the collection of elements to add; must not be {@code null}, and
     *                 must not contain {@code null} elements
     * @return {@code true} if at least one element was added to the queue;
     *         {@code false} if every element was a duplicate and nothing changed
     * @throws NullPointerException if {@code elements} is {@code null}, or if any
     *                              element within it is {@code null}
     */
    @Override
    public boolean addAll(Collection<? extends T> elements)
    {
        boolean modified = false;

        for (T element : elements)
        {
            if (element == null)
                throw new NullPointerException();

            if (index.containsKey(element))
                continue;

            if (size < minHeap.size())
                minHeap.set(size, element);
            else
                minHeap.add(element);

            index.put(element, size);
            size++;
            modified = true;
        }

        if (modified)
            heapify();

        return modified;
    }

    /**
     * Removes the specified element from the queue regardless of its current heap
     * position, restoring the min-heap invariant in O(log n) average time.
     *
     * <p>The removal proceeds as follows:
     * <ol>
     *   <li>Look up the element's current heap index via the {@link #index} map in O(1)
     *       average time. If the element is absent, return {@code false} immediately.</li>
     *   <li>Swap the target element with the last live element at index {@code size - 1},
     *       then write {@code null} into that last slot and remove the target from the
     *       {@link #index} map.</li>
     *   <li>Apply both {@link #downheap(int)} and {@link #upheap(int)} at the index
     *       previously occupied by the target. Both directions are necessary because the
     *       replacement element, formerly the last live node, may be either smaller or
     *       larger than the removed element's original neighbours; exactly one of the two
     *       sift calls will perform work while the other exits immediately.</li>
     * </ol>
     *
     * <p>Passing {@code null} or an element not present in the queue returns {@code false}
     * without modifying any state.
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
        Integer element_index = index.get((T) element);

        if (element_index == null)
            return false;

        int last_index = size - 1;

        swap(element_index, last_index);
        minHeap.set(last_index, null);
        index.remove(element);
        size--;

        downheap(element_index);
        upheap(element_index);

        return true;
    }

    /**
     * Removes from this queue all elements that are contained in the specified collection.
     *
     * <p>Each element of {@code c} is removed individually via {@link #remove(Object)}.
     * The overall cost is O(k log n), where {@code k} is the number of elements in
     * {@code c} that are present in the queue and {@code n} is the queue size at the
     * start of the operation.
     *
     * @param c the collection of elements to remove; must not be {@code null}
     * @return {@code true} if at least one element was removed; {@code false} otherwise
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
     * collection, removing all others.
     *
     * <p>The current queue contents are traversed via {@link #iterator()} and any element
     * absent from {@code c} is collected into a removal list. All collected elements are
     * then removed via {@link #remove(Object)}. Building the removal list first avoids
     * concurrent-modification issues that would arise from removing elements during
     * iteration.
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
     * <p>The check is delegated to the internal {@link #index} map and therefore runs in
     * O(1) average time, in contrast to the O(n) scan that a plain heap would require.
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
     * Returns the number of live elements currently in this priority queue.
     *
     * <p>This value reflects only the elements occupying indices {@code 0} through
     * {@code size - 1} of the backing {@link #minHeap} list; {@code null} tombstone
     * slots beyond that range are not counted.
     *
     * @return the number of elements in this queue; {@code 0} if the queue is empty
     */
    @Override
    public int size() { return size; }

    /**
     * Returns an iterator over the live elements of this queue in heap-array order.
     *
     * <p>The traversal visits elements at indices {@code 0} through {@code size - 1} of
     * the backing array. This order reflects the internal heap layout and is <em>not</em>
     * sorted by priority; callers that require elements in ascending order must drain the
     * queue via repeated {@link #poll()} calls instead. The iterator does not support
     * {@link Iterator#remove()} and does not detect concurrent structural modifications;
     * mutating the queue during iteration may produce unpredictable results.
     *
     * @return an iterator over the live elements of this queue in internal heap-array order
     */
    @Override
    public Iterator<T> iterator()
    {
        return new IndexedPriorityQueueIterator();
    }

    /**
     * Restores the min-heap invariant across the entire heap array in O(n) time using a
     * bottom-up sift-down pass.
     *
     * <p>All internal (non-leaf) nodes, from the last one at index {@code (size / 2) - 1}
     * up to the root at index {@code 0}, are processed in reverse order. Applying
     * {@link #downheap(int)} at each internal node guarantees that every subtree satisfies
     * the heap property before its parent is visited, bounding the total work to O(n)
     * rather than the O(n log n) that repeated {@link #upheap(int)} calls would require.
     * This method is called exclusively by {@link #addAll(Collection)} after a bulk insert.
     */
    private void heapify()
    {
        for (int i = (size / 2) - 1; i >= 0; i--)
            downheap(i);
    }

    /**
     * Sifts the element at the given heap index downward until the min-heap invariant is
     * restored in the subtree rooted at that index.
     *
     * <p>The algorithm identifies the smaller of the two children using
     * {@link #nullsLastComparator}, which treats out-of-bounds child slots (represented as
     * {@code null}) as larger than any real element, effectively making leaf nodes always
     * win the boundary check. If the minimum child is smaller than the element at
     * {@code current_index}, the two are swapped via {@link #swap(int, int)}, which keeps
     * the {@link #index} map in sync, and the method recurses on the child's index.
     * If both children are {@code null} or the current element is already no larger than
     * its smallest child, the invariant is satisfied and the recursion terminates.
     *
     * @param current_index the heap index of the element to sift downward; values outside
     *                      {@code [0, size)} are silently ignored
     */
    private void downheap(int current_index)
    {
        if (current_index < 0 || current_index >= size)
            return;

        T current = minHeap.get(current_index);

        int left_child_index  = (current_index * 2) + 1;
        int right_child_index = (current_index * 2) + 2;

        T left_child  = left_child_index  < size ? minHeap.get(left_child_index)  : null;
        T right_child = right_child_index < size ? minHeap.get(right_child_index) : null;

        int child_comparison = nullsLastComparator.compare(left_child, right_child);
        int min_child_index  = child_comparison <= 0 ? left_child_index : right_child_index;
        T   min_child        = min_child_index  < size ? minHeap.get(min_child_index) : null;

        if (nullsLastComparator.compare(current, min_child) > 0)
        {
            swap(current_index, min_child_index);
            downheap(min_child_index);
        }
    }

    /**
     * Sifts the element at the given heap index upward until the min-heap invariant is
     * restored on the path from that index to the root.
     *
     * <p>The parent of the node at index {@code i} is located at index
     * {@code (i - 1) / 2}. If the current element is smaller than its parent under
     * {@link #nullsLastComparator}, the two are swapped via {@link #swap(int, int)} and
     * the method recurses on the parent's index. The recursion terminates when
     * {@code current_index} reaches {@code 0} (the root has no parent) or when the
     * current element is greater than or equal to its parent.
     *
     * @param current_index the heap index of the element to sift upward; values of
     *                      {@code 0} or less are silently ignored since the root has no
     *                      parent to compare against; values at or beyond {@code size}
     *                      are also silently ignored
     */
    private void upheap(int current_index)
    {
        if (current_index <= 0 || current_index >= size)
            return;

        int parent_index = (current_index - 1) / 2;

        T current = minHeap.get(current_index);
        T parent  = minHeap.get(parent_index);

        if (nullsLastComparator.compare(parent, current) > 0)
        {
            swap(parent_index, current_index);
            upheap(parent_index);
        }
    }

    /**
     * Swaps the elements at the two specified heap indices and updates the {@link #index}
     * map to reflect their new positions.
     *
     * <p>Both the {@link #minHeap} list and the {@link #index} map are updated in the
     * same call so that the two structures are never transiently inconsistent. Every
     * heap mutation, {@link #upheap(int)}, {@link #downheap(int)}, {@link #poll()}, and
     * {@link #remove(Object)}, routes all element movement through this method to
     * maintain that invariant.
     *
     * @param a the index of the first element; must be a valid index in {@link #minHeap}
     * @param b the index of the second element; must be a valid index in {@link #minHeap}
     */
    private void swap(int a, int b)
    {
        T objectA = minHeap.get(a);
        T objectB = minHeap.get(b);

        minHeap.set(a, objectB);
        minHeap.set(b, objectA);

        index.put(objectA, b);
        index.put(objectB, a);
    }

    /**
     * A forward iterator that traverses the live elements of the heap in backing-array order.
     *
     * <p>Traversal visits indices {@code 0} through {@code size - 1} of {@link #minHeap};
     * this order reflects the internal heap layout and is <em>not</em> sorted by priority.
     * The iterator does not support {@link Iterator#remove()} and does not detect concurrent
     * structural modifications; mutating the queue during iteration may produce
     * unpredictable results.
     */
    private class IndexedPriorityQueueIterator implements Iterator<T>
    {
        /**
         * The heap index of the element to be returned by the next call to {@link #next()};
         * initialised to {@code 0} and incremented on each successful {@link #next()} call.
         */
        private int currentIndex = 0;

        /**
         * Returns {@code true} if there are more live elements remaining in the traversal.
         *
         * @return {@code true} if {@link #next()} would return an element;
         *         {@code false} if {@code currentIndex} has reached {@code size}
         */
        @Override
        public boolean hasNext() { return currentIndex < size; }

        /**
         * Returns the next element in heap-array order and advances the iterator by one
         * position.
         *
         * @return the live heap element at the current traversal index
         * @throws NoSuchElementException if all live elements have already been visited
         */
        @Override
        public T next()
        {
            if (!hasNext())
                throw new NoSuchElementException();

            return minHeap.get(currentIndex++);
        }
    }
}