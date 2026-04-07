package uk.ac.warwick.cs261;

import com.google.common.collect.testing.QueueTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestQueueGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.TestSuite;
import uk.ac.warwick.cs261.datastructures.IndexedPriorityQueue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.Assert.*;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    IndexedPriorityQueueTest.IndexedPriorityQueueSuite.class,
    IndexedPriorityQueueTest.HeapInvariantTests.class
})
public class IndexedPriorityQueueTest
{
    // Guava contract tests, covers offer/add, poll/remove(), peek/element(),
    // remove(Object), contains, size, iterator, addAll, removeAll, retainAll,
    // null rejection, and duplicate rejection. order() sorts ascending so
    // Guava's KNOWN_ORDER peek/poll tests verify the minimum is always at the head.
    public static class IndexedPriorityQueueSuite
    {
        public static TestSuite suite()
        {
            return QueueTestSuiteBuilder.using(new TestQueueGenerator<String>()
                {
                    @Override
                    public Queue<String> create(Object... elements)
                    {
                        IndexedPriorityQueue<String> pq = new IndexedPriorityQueue<>();
                        for (Object e : elements)
                            pq.offer((String) e);
                        return pq;
                    }

                    // Sorted order: alpha < beta < delta < epsilon < gamma.
                    @Override
                    public SampleElements<String> samples()
                    {
                        return new SampleElements<>("alpha", "beta", "delta", "epsilon", "gamma");
                    }

                    @Override
                    public String[] createArray(int length)
                    {
                        return new String[length];
                    }

                    // Min-heap, so dequeue order is ascending.
                    @Override
                    public Iterable<String> order(List<String> insertionOrder)
                    {
                        List<String> sorted = new ArrayList<>(insertionOrder);
                        java.util.Collections.sort(sorted);
                        return sorted;
                    }
                })
                .named("IndexedPriorityQueue")
                .withFeatures(
                    CollectionFeature.SUPPORTS_ADD,
                    CollectionFeature.SUPPORTS_REMOVE,
                    CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                    CollectionSize.ANY
                )
                .createTestSuite();
        }
    }

    // Hand-written tests for gaps Guava does not cover:
    // - full sorted drain (Guava only polls the first element at fixed sizes)
    // - heap validity after remove(Object) (Guava only checks the element is absent)
    // - peek after removal (Guava checks peek at construction, not after remove)
    // - addAll bulk heapify drain order (Guava only checks element presence)
    // - interleaved offer/poll (Guava uses fixed-size snapshots)
    // - re-insertion of a removed element (Guava never re-inserts)
    // - offer after full drain (Guava never offers again after draining to empty)
    // - scale (Guava fixes sizes at 0, 1, 3)
    public static class HeapInvariantTests
    {
        private <T extends Comparable<T>> List<T> drainToList(IndexedPriorityQueue<T> pq)
        {
            List<T> result = new ArrayList<>();

            T item = pq.poll();

            while (item != null)
            {
                result.add(item);
                item = pq.poll();
            }

            return result;
        }

        private <T extends Comparable<T>> void assertSortedAscending(List<T> items)
        {
            for (int i = 1; i < items.size(); i++)
            {
                assertTrue(
                    "Expected non-decreasing at index " + i +
                    " but got " + items.get(i - 1) + " then " + items.get(i),
                    items.get(i - 1).compareTo(items.get(i)) <= 0
                );
            }
        }

        @Test
        public void arbitraryInsertion_fullDrainIsSorted()
        {
            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>();

            int[] items = {7, 3, 9, 1, 5, 8, 2, 6, 4, 0};

            for (int v : items)
                pq.offer(v);

            assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), drainToList(pq));
        }

        // Exercises both upheap and downheap paths in remove(Object).
        @Test
        public void removeInterior_drainRemainsOrdered()
        {
            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>();

            int[] items = {5, 3, 8, 1, 9, 2, 6, 4, 7};

            for (int v : items)
                pq.offer(v);

            pq.remove(5); // interior, needs both upheap and downheap
            pq.remove(1); // root removal via remove(Object)
            pq.remove(9); // leaf/max, replacement must sift up

            List<Integer> result = drainToList(pq);

            assertSortedAscending(result);
            assertFalse(result.contains(5));
            assertFalse(result.contains(1));
            assertFalse(result.contains(9));
        }

        @Test
        public void multipleRemovals_peekReturnsCurrentMin()
        {
            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>();

            int[] items = {6, 2, 8, 1, 4, 10, 3, 7, 5, 9};

            for (int v : items)
                pq.offer(v);

            pq.remove(2);
            assertEquals(Integer.valueOf(1), pq.peek());

            pq.remove(1);
            assertEquals(Integer.valueOf(3), pq.peek());

            pq.remove(6);
            assertEquals(Integer.valueOf(3), pq.peek());
        }

        // Confirms the O(n) bulk heapify produces the same sorted drain as individual offers.
        @Test
        public void addAll_drainIsSorted()
        {
            List<Integer> values = Arrays.asList(7, 3, 9, 1, 5, 8, 2, 6, 4, 0);

            IndexedPriorityQueue<Integer> pqBulk = new IndexedPriorityQueue<>(values);
            IndexedPriorityQueue<Integer> pqOne  = new IndexedPriorityQueue<>();
            
            for (int v : values) 
                pqOne.offer(v);

            assertEquals(drainToList(pqOne), drainToList(pqBulk));
        }

        @Test
        public void addAll_ontoPopulatedQueue_drainIsSorted()
        {
            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>();

            pq.offer(5); 
            pq.offer(10); 
            pq.offer(15);
            pq.addAll(Arrays.asList(3, 7, 12, 1, 20));

            List<Integer> result = drainToList(pq);
            assertSortedAscending(result);
            assertEquals(8, result.size());
        }

        @Test
        public void interleavedOfferAndPoll_alwaysYieldsMin()
        {
            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>();
            pq.offer(10); 
            pq.offer(20); 
            pq.offer(5);

            assertEquals(Integer.valueOf(5),  pq.poll());

            pq.offer(3);
            assertEquals(Integer.valueOf(3),  pq.poll());

            pq.offer(8);
            assertEquals(Integer.valueOf(8),  pq.poll());
            assertEquals(Integer.valueOf(10), pq.poll());

            pq.offer(1);
            assertEquals(Integer.valueOf(1),  pq.poll());
            assertEquals(Integer.valueOf(20), pq.poll());

            assertNull(pq.poll());
        }

        // The element must be re-offerable and sift to the correct heap position.
        @Test
        public void reinsertAfterRemoval_heapRemainsOrdered()
        {
            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>();

            pq.offer(3); 
            pq.offer(1); 
            pq.offer(5); 
            pq.offer(2); 
            pq.offer(4);

            pq.remove(1);
            assertTrue(pq.offer(1));

            assertEquals(Integer.valueOf(1), pq.peek());
            assertSortedAscending(drainToList(pq));
        }

        // Internal size and index must reset so subsequent offers work correctly.
        @Test
        public void offerAfterFullDrain_works()
        {
            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>();
            pq.offer(2); 
            pq.offer(1); 
            pq.offer(3);

            pq.poll(); 
            pq.poll(); 
            pq.poll();

            pq.offer(7); 
            pq.offer(4);

            assertEquals(Integer.valueOf(4), pq.peek());
            assertEquals(Arrays.asList(4, 7), drainToList(pq));
        }

        @Test
        public void largeShuffledInsert_drainIsSorted()
        {
            List<Integer> values = new ArrayList<>();

            for (int i = 0; i < 1000; i++) 
                values.add(i);
            
            java.util.Collections.shuffle(values, new java.util.Random(42));

            IndexedPriorityQueue<Integer> pq = new IndexedPriorityQueue<>(values);
            List<Integer> result = drainToList(pq);

            assertEquals(1000, result.size());
            assertSortedAscending(result);
        }
    }
}