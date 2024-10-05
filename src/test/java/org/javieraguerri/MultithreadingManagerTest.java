package org.javieraguerri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

public class MultithreadingManagerTest {

    private static long getRandomDelay() {
        // Generate a random delay between 100 and 4000 milliseconds
        return 100L + (long) (Math.random() * 3901);
    }

    @Test
    @DisplayName("1. Processing from an empty queue")
    public void testProcessingFromEmptyQueue() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);
        long consumerDelayMs = 100L;
        int numberOfConsumers = 6;
        for (int i = 0; i < numberOfConsumers; i++) {
            manager.addConsumer(consumerDelayMs);
        }
        Thread.sleep(2000);

        assertEquals(manager.getOrderQueueSize(), 0, "Queue should be empty.");

        for (Thread consumerThread : manager.getConsumerThreads()) {
            assertTrue(consumerThread.isAlive(), "Consumer thread should be alive.");
        }

        manager.shutdown();

        for (Thread consumerThread : manager.getConsumerThreads()) {
            assertFalse(consumerThread.isAlive(), "Consumer thread should have terminated after shutdown.");
        }
        int totalOrdersCreated = manager.getTotalOrdersCreated();
        assertEquals(0, totalOrdersCreated, "No orders should have been created.");

        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(0, totalOrdersProcessed, "No orders should have been processed.");
    }

    @Test
    @DisplayName("2. Force the upper boundary (queue full)")
    public void testProducersBlockedWhenQueueFull() throws InterruptedException {
        int MAX_QUEUE_SIZE = 4;
        MultithreadingManager manager = new MultithreadingManager(MAX_QUEUE_SIZE);
        long producerDelayMs = 100L;
        int numberOfProducers = 6;
        for (int i = 0; i < numberOfProducers; i++) {
            manager.addProducer(producerDelayMs);
        }
        Thread.sleep(2000);

        assertEquals(manager.getOrderQueueSize(), MAX_QUEUE_SIZE, "Queue should be full.");

        for (Thread producerThread : manager.getProducerThreads()) {
            assertTrue(producerThread.isAlive(), "Producer thread should be alive.");
        }

        manager.shutdown();

        for (Thread producerThread : manager.getProducerThreads()) {
            assertFalse(producerThread.isAlive(), "Producer thread should have terminated after shutdown.");
        }

        int totalOrdersCreated = manager.getTotalOrdersCreated();
        assertEquals(MAX_QUEUE_SIZE, totalOrdersCreated, "Total orders created should equal the queue's maximum capacity.");

        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(0, totalOrdersProcessed, "No orders should have been processed.");
    }

    @Test
    @DisplayName("3. Start with processing from an empty queue and then continue normally")
    public void testProcessingFromEmptyQueueThenContinue() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);

        long consumerDelayMs = 500L;
        manager.addConsumer(consumerDelayMs);
        Thread.sleep(1000); // give time to consumer to get the lock

        long producerDelayMs = 500L;
        manager.addProducer(producerDelayMs);
        manager.addConsumer(consumerDelayMs);
        manager.addProducer(producerDelayMs);
        manager.addConsumer(consumerDelayMs);
        manager.addProducer(producerDelayMs);
        Thread.sleep(15000);
        manager.shutdown();

        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }

    @Test
    @DisplayName("4. Normal operation with balanced execution (no boundary limits reached)")
    public void testNormalOperationBalancedExecutionNoHittingBoundaries() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(10);

        manager.addProducer(500L);
        manager.addProducer(500L);
        Thread.sleep(600);
        manager.removeProducer();
        manager.addConsumer(500L);

        Thread.sleep(15000);
        manager.shutdown();
        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }

    @Test
    @DisplayName("5. Gently hitting boundaries (without forcing them)")
    public void testGentlyHittingBoundaries() {
        int MAX_QUEUE_SIZE = 4;
        MultithreadingManager manager = new MultithreadingManager(MAX_QUEUE_SIZE);

        manager.addProducer(100L);
        await().atMost(2, TimeUnit.SECONDS).until(() -> manager.getOrderQueueSize() >= MAX_QUEUE_SIZE);
        manager.removeProducer();
        manager.addConsumer(100L);
        await().atMost(2, TimeUnit.SECONDS).until(() -> manager.getOrderQueueSize() <= 0);
        manager.removeConsumer();
        manager.addProducer(100L);
        await().atMost(2, TimeUnit.SECONDS).until(() -> manager.getOrderQueueSize() >= MAX_QUEUE_SIZE);
        manager.removeProducer();
        manager.addConsumer(100L);
        await().atMost(2, TimeUnit.SECONDS).until(() -> manager.getOrderQueueSize() <= 0);
        manager.removeConsumer();
        manager.addProducer(100L);
        await().atMost(2, TimeUnit.SECONDS).until(() -> manager.getOrderQueueSize() >= MAX_QUEUE_SIZE);
        manager.removeProducer();
        manager.addConsumer(100L);
        await().atMost(2, TimeUnit.SECONDS).until(() -> manager.getOrderQueueSize() == 0);
        manager.removeConsumer();

        manager.shutdown();
        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }

    @Test
    @DisplayName("6. Burst of placing orders and burst of processing")
    public void testBursts() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);
        long delayMs = 100L;
        int numberOfProducers = 8;
        int numberOfConsumers = 8;

        for (int i = 0; i < numberOfProducers; i++) {
            manager.addProducer(delayMs);
        }
        Thread.sleep(3000);
        for (int i = 0; i < numberOfProducers; i++) {
            manager.removeProducer();
        }
        for (int i = 0; i < numberOfConsumers; i++) {
            manager.addConsumer(delayMs);
        }
        Thread.sleep(3000);
        for (int i = 0; i < numberOfConsumers; i++) {
            manager.removeConsumer();
        }
        for (int i = 0; i < numberOfProducers; i++) {
            manager.addProducer(delayMs);
        }
        Thread.sleep(3000);
        for (int i = 0; i < numberOfProducers; i++) {
            manager.removeProducer();
        }
        for (int i = 0; i < numberOfConsumers; i++) {
            manager.addConsumer(delayMs);
        }
        Thread.sleep(3000);
        for (int i = 0; i < numberOfConsumers; i++) {
            manager.removeConsumer();
        }
        manager.shutdown();
        for (Thread producerThread : manager.getProducerThreads()) {
            assertFalse(producerThread.isAlive(), "Producer thread should have terminated after shutdown.");
        }
        for (Thread consumerThread : manager.getConsumerThreads()) {
            assertFalse(consumerThread.isAlive(), "Consumer thread should have terminated after shutdown.");
        }

        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }

    @Test
    @DisplayName("7. Long execution with mixed operations")
    public void testLongExecution() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(6);
        int numberOfProducers = 8;
        for (int i = 0; i < numberOfProducers; i++) {
            manager.addProducer(getRandomDelay());
        }
        int numberOfConsumers = 8;
        for (int i = 0; i < numberOfConsumers; i++) {
            manager.addConsumer(getRandomDelay());
        }
        Thread.sleep(60000);

        for (Thread producerThread : manager.getProducerThreads()) {
            assertTrue(producerThread.isAlive(), "Producer thread should be alive.");
        }
        for (Thread consumerThread : manager.getConsumerThreads()) {
            assertTrue(consumerThread.isAlive(), "Consumer thread should be alive.");
        }
        manager.shutdown();
        for (Thread producerThread : manager.getProducerThreads()) {
            assertFalse(producerThread.isAlive(), "Producer thread should have terminated after shutdown.");
        }
        for (Thread consumerThread : manager.getConsumerThreads()) {
            assertFalse(consumerThread.isAlive(), "Consumer thread should have terminated after shutdown.");
        }

        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }






}
