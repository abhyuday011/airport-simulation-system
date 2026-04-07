package uk.ac.warwick.cs261;

import com.google.common.collect.testing.QueueTestSuiteBuilder;
import com.google.common.collect.testing.SampleElements;
import com.google.common.collect.testing.TestQueueGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;

import junit.framework.TestSuite;
import uk.ac.warwick.cs261.datastructures.IndexedQueue;

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
    IndexedQueueTest.IndexedQueueSuite.class,
    IndexedQueueTest.FifoAndStructuralTests.class
})
public class IndexedQueueTest
{
    // Guava contract tests, covers offer/add, poll/remove(), peek/element(),
    // remove(Object), contains, size, iterator ordering + NoSuchElementException,
    // addAll, removeAll, retainAll, null rejection, and duplicate rejection.
    public static class IndexedQueueSuite
    {
        public static TestSuite suite()
        {
            return QueueTestSuiteBuilder.using(new TestQueueGenerator<String>()
                {
                    @Override
                    public Queue<String> create(Object... elements)
                    {
                        IndexedQueue<String> queue = new IndexedQueue<>();
                        for (Object e : elements)
                            queue.offer((String) e);
                        return queue;
                    }

                    @Override
                    public SampleElements<String> samples()
                    {
                        return new SampleElements<>("alpha", "beta", "gamma", "delta", "epsilon");
                    }

                    @Override
                    public String[] createArray(int length)
                    {
                        return new String[length];
                    }

                    // IndexedQueue is FIFO so insertion order == dequeue order.
                    @Override
                    public Iterable<String> order(List<String> insertionOrder)
                    {
                        return insertionOrder;
                    }
                })
                .named("IndexedQueue")
                .withFeatures(
                    CollectionFeature.SUPPORTS_ADD,
                    CollectionFeature.SUPPORTS_REMOVE,
                    CollectionFeature.KNOWN_ORDER,
                    CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
                    CollectionSize.ANY
                )
                .createTestSuite();
        }
    }

    // Hand-written tests for gaps Guava does not cover:
    // - drain order after pointer splicing (Guava only checks the removed element is absent)
    // - tail pointer update after tail removal (Guava never offers after a tail removal)
    // - index cleanup allowing re-insertion (Guava never re-inserts a removed element)
    // - head/tail reset after a full drain (Guava never offers again after draining to empty)
    // - interleaved offer/poll cycles (Guava uses fixed-size snapshots)
    // - addAll drain order and duplicate skipping (Guava only checks element presence)
    // - scale (Guava fixes sizes at 0, 1, 3)
    public static class FifoAndStructuralTests
    {
        private <T> List<T> drainToList(IndexedQueue<T> q)
        {
            List<T> result = new ArrayList<>();

            T item = q.poll();
            
            while (item != null)
            {
                result.add(item);
                item = q.poll();
            }

            return result;
        }

        @Test
        public void removeInterior_survivorsDrainInOrder()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("a"); 
            q.offer("b"); 
            q.offer("c"); 
            q.offer("d"); 
            q.offer("e");

            q.remove("c");

            assertEquals(Arrays.asList("a", "b", "d", "e"), drainToList(q));
        }

        @Test
        public void removeHead_remainderDrainsInOrder()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("a"); 
            q.offer("b"); 
            q.offer("c");

            q.remove("a");

            assertEquals(Arrays.asList("b", "c"), drainToList(q));
        }

        // Tail pointer must be updated so the next offer appends correctly.
        @Test
        public void removeTail_offerAppendsCorrectly()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("a"); 
            q.offer("b"); 
            q.offer("c");

            q.remove("c");
            q.offer("d");

            assertEquals(Arrays.asList("a", "b", "d"), drainToList(q));
        }

        // The element must be re-offerable and land at the tail.
        @Test
        public void reinsertAfterRemoval_appearsAtTail()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("a"); 
            q.offer("b"); 
            q.offer("c");

            q.remove("b");
            assertTrue(q.offer("b"));

            assertEquals(Arrays.asList("a", "c", "b"), drainToList(q));
        }

        // Head and tail must be reset so the next offer starts a fresh list.
        @Test
        public void offerAfterFullDrain_works()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("x"); 
            q.offer("y");

            q.poll(); 
            q.poll();

            q.offer("new");

            assertEquals("new", q.peek());
            assertEquals(1, q.size());
            assertEquals(Arrays.asList("new"), drainToList(q));
        }

        @Test
        public void interleavedOfferAndPoll_maintainsFifo()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("a");

            q.offer("b");
            assertEquals("a", q.poll());

            q.offer("c");
            assertEquals("b", q.poll());

            q.offer("d");
            assertEquals("c", q.poll());

            assertEquals("d", q.poll());
            assertNull(q.poll());
        }

        @Test
        public void addAll_appendsInOrder()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("a"); 
            q.offer("b");
            q.addAll(Arrays.asList("c", "d", "e"));

            assertEquals(Arrays.asList("a", "b", "c", "d", "e"), drainToList(q));
        }

        @Test
        public void addAll_skipsDuplicates()
        {
            IndexedQueue<String> q = new IndexedQueue<>();

            q.offer("a"); 
            q.offer("b");
            q.addAll(Arrays.asList("b", "c", "a", "d"));

            assertEquals(Arrays.asList("a", "b", "c", "d"), drainToList(q));
        }

        @Test
        public void largeQueue_drainsInInsertionOrder()
        {
            IndexedQueue<Integer> q = new IndexedQueue<>();

            for (int i = 0; i < 1000; i++) 
                q.offer(i);

            List<Integer> result = drainToList(q);

            assertEquals(1000, result.size());

            for (int i = 0; i < 1000; i++)
                assertEquals(Integer.valueOf(i), result.get(i));
        }

        @Test
        public void largeRemovals_survivorsDrainInOrder()
        {
            IndexedQueue<Integer> q = new IndexedQueue<>();

            for (int i = 0; i < 100; i++) 
                q.offer(i);

            for (int i = 0; i < 100; i += 2) 
                q.remove(i); // remove all evens

            List<Integer> result = drainToList(q);

            assertEquals(50, result.size());

            for (int i = 0; i < 50; i++)
                assertEquals(Integer.valueOf(i * 2 + 1), result.get(i));
        }
    }
}