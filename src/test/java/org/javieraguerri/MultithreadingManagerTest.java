package org.javieraguerri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;


import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultithreadingManagerTest {

    private static long getRandomDelay() {
        return 100L + (long) (Math.random() * 3901);
    }

    @Test
    @Order(1)
    @DisplayName("1. Processing from an empty queue")
    public void testProcessingFromEmptyQueue() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);
        long consumerDelayMs = 100L;
        IntStream.range(0, 6).forEach(i -> manager.addConsumer(consumerDelayMs));
        Thread.sleep(2000);

        assertEquals(0, manager.getOrderQueueSize(), "Queue should be empty.");
        manager.getConsumerThreads().forEach(thread -> assertTrue(thread.isAlive(), "Consumer thread should be alive."));
        manager.shutdown();
        manager.getConsumerThreads().forEach(thread -> assertFalse(thread.isAlive(), "Consumer thread should have terminated after shutdown."));
        assertEquals(0, manager.getTotalOrdersCreated(), "No orders should have been created.");
        assertEquals(0, manager.getTotalOrdersProcessed(), "No orders should have been processed.");
    }

    @Test
    @Order(2)
    @DisplayName("2. Force the upper boundary (queue full)")
    public void testProducersBlockedWhenQueueFull() throws InterruptedException {
        int MAX_QUEUE_SIZE = 4;
        MultithreadingManager manager = new MultithreadingManager(MAX_QUEUE_SIZE);
        long producerDelayMs = 100L;
        IntStream.range(0, 6).forEach(i -> manager.addProducer(producerDelayMs));
        Thread.sleep(2000);

        assertEquals(MAX_QUEUE_SIZE, manager.getOrderQueueSize(), "Queue should be full.");
        manager.getProducerThreads().forEach(thread -> assertTrue(thread.isAlive(), "Producer thread should be alive."));
        manager.shutdown();
        manager.getProducerThreads().forEach(thread -> assertFalse(thread.isAlive(), "Producer thread should have terminated after shutdown."));
        assertEquals(MAX_QUEUE_SIZE, manager.getTotalOrdersCreated(), "Total orders created should equal the queue's maximum capacity.");
        assertEquals(0, manager.getTotalOrdersProcessed(), "No orders should have been processed.");
    }

    @Test
    @Order(3)
    @DisplayName("3. Start with processing from an empty queue and then continue normally")
    public void testProcessingFromEmptyQueueThenContinue() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);

        long consumerDelayMs = 500L;
        manager.addConsumer(consumerDelayMs);
        Thread.sleep(1000);

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
    @Order(4)
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
    @Order(5)
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
    @Order(6)
    @DisplayName("6. Burst of placing orders and burst of processing")
    public void testBursts() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);
        long delayMs = 100L;

        IntStream.range(0, 8).forEach(i -> manager.addProducer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, 8).forEach(i -> manager.removeProducer());
        IntStream.range(0, 8).forEach(i -> manager.addConsumer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, 8).forEach(i -> manager.removeConsumer());
        IntStream.range(0, 8).forEach(i -> manager.addProducer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, 8).forEach(i -> manager.removeProducer());
        IntStream.range(0, 8).forEach(i -> manager.addConsumer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, 8).forEach(i -> manager.removeConsumer());

        manager.shutdown();
        manager.getProducerThreads().forEach(thread -> assertFalse(thread.isAlive(), "Producer thread should have terminated after shutdown."));
        manager.getConsumerThreads().forEach(thread -> assertFalse(thread.isAlive(), "Consumer thread should have terminated after shutdown."));

        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }

    @Test
    @Order(7)
    @DisplayName("7. Long execution with mixed operations")
    public void testLongExecution() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);

        IntStream.range(0, 8).forEach(i -> manager.addProducer(getRandomDelay()));
        IntStream.range(0, 8).forEach(i -> manager.addConsumer(getRandomDelay()));
        Thread.sleep(60000);

        manager.getProducerThreads().forEach(thread -> assertTrue(thread.isAlive(), "Producer thread should be alive."));
        manager.getConsumerThreads().forEach(thread -> assertTrue(thread.isAlive(), "Consumer thread should be alive."));
        manager.shutdown();
        manager.getProducerThreads().forEach(thread -> assertFalse(thread.isAlive(), "Producer thread should have terminated after shutdown."));
        manager.getConsumerThreads().forEach(thread -> assertFalse(thread.isAlive(), "Consumer thread should have terminated after shutdown."));

        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }

    @Test
    @Order(8)
    @DisplayName("8. Add and remove producers and consumers beyond the limits")
    public void testProducerConsumerLimits() throws InterruptedException {
        int consumerLimit = 4;
        int producerLimit = 4;
        MultithreadingManager manager = new MultithreadingManager(4, consumerLimit, producerLimit);

        IntStream.range(0, 10).forEach(i -> manager.addProducer(100));
        IntStream.range(0, 10).forEach(i -> manager.addConsumer(100));
        Thread.sleep(3000);
        assertEquals(producerLimit, manager.getActiveProducers().get());
        assertEquals(consumerLimit, manager.getActiveConsumers().get());

        IntStream.range(0, 10).forEach(i -> manager.removeProducer());
        while (manager.getOrderQueueSize() > 0) {
            Thread.sleep(100);
        }
        IntStream.range(0, 10).forEach(i -> manager.removeConsumer());
        Thread.sleep(3000);
        assertEquals(0, manager.getActiveProducers().get());
        assertEquals(0, manager.getActiveConsumers().get());

        manager.shutdown();
        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }

    @Test
    @Order(9)
    @DisplayName("9. Activate shutdown repeatedly")
    public void testActivateShutdownRepeatedly() throws InterruptedException {
        MultithreadingManager manager = new MultithreadingManager(4);
        IntStream.range(0, 10).forEach(i -> manager.addProducer(100));
        IntStream.range(0, 10).forEach(i -> manager.addConsumer(100));
        Thread.sleep(4000);

        IntStream.range(0, 5).forEach(i -> manager.shutdown());

        int totalOrdersCreated = manager.getTotalOrdersCreated();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed, "All orders created should have been processed.");
        assertEquals(0, manager.getOrderQueueSize(), "Order queue should be empty at the end.");
    }
}
