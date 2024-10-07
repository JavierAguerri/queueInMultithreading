package org.javieraguerri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigDefaultManagerTest extends BaseMultithreadingManagerTest {

    @Value("${multithreading.maxQueueSize}")
    private int MAX_QUEUE_SIZE;

    @Test
    @Order(1)
    @DisplayName("1. Processing from an empty queue")
    public void processingFromEmptyQueueTest() throws InterruptedException {
        long consumerDelayMs = 100L;
        int consumers = 6;

        IntStream.range(0, 6).forEach(i -> manager.addConsumer(consumerDelayMs));
        Thread.sleep(2000);

        assertEquals(0, manager.getOrderQueueSize(), "Queue should be empty.");
        assertEquals(consumers, manager.getActiveConsumers(), "Consumers should be active.");
        manager.shutdown();
        assertEquals(0, manager.getActiveConsumers(), "Consumers should have terminated after shutdown.");
        assertEquals(0, manager.getTotalOrdersAdded(), "No orders should have been created.");
        assertEquals(0, manager.getTotalOrdersProcessed(), "No orders should have been processed.");
    }

    @Test
    @Order(2)
    @DisplayName("2. Force the upper boundary (queue full)")
    public void producersBlockedWhenQueueFullTest() throws InterruptedException {
        long producerDelayMs = 100L;
        int producers = 6;
        IntStream.range(0, producers).forEach(i -> manager.addProducer(producerDelayMs));
        Thread.sleep(2000);

        assertEquals(MAX_QUEUE_SIZE, manager.getOrderQueueSize(), "Queue should be full.");
        assertEquals(producers, manager.getActiveProducers(), "Producers should be active.");
        manager.shutdown();
        assertEquals(0, manager.getActiveProducers(), "Producers should have terminated after shutdown.");
        assertEquals(MAX_QUEUE_SIZE, manager.getTotalOrdersAdded(),
                "Total orders created ("+manager.getTotalOrdersAdded()+") should equal the queue's maximum capacity ("+MAX_QUEUE_SIZE+").");
        assertEquals(0, manager.getTotalOrdersProcessed(), "No orders should have been processed.");
    }

    @Test
    @Order(3)
    @DisplayName("3. Start with processing from an empty queue and then continue normally")
    public void processingFromEmptyQueueThenContinueTest() throws InterruptedException {
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

        int totalOrdersCreated = manager.getTotalOrdersAdded();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed,
                "All orders created (" + totalOrdersCreated + ") should have been processed (" + totalOrdersProcessed + ").");
        assertEquals(0, manager.getOrderQueueSize(),
                "Order queue " + manager.getOrderQueueSize() + " should be empty at the end.");
    }

    @Test
    @Order(5)
    @DisplayName("5. Gently hitting boundaries (without forcing them)")
    public void gentlyHittingBoundariesTest() {
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
        int totalOrdersCreated = manager.getTotalOrdersAdded();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed,
                "All orders created (" + totalOrdersCreated + ") should have been processed (" + totalOrdersProcessed + ").");
        assertEquals(0, manager.getOrderQueueSize(),
                "Order queue " + manager.getOrderQueueSize() + " should be empty at the end.");
    }

    @Test
    @Order(6)
    @DisplayName("6. Burst of placing orders and burst of processing")
    public void burstsTest() throws InterruptedException {
        long delayMs = 100L;
        int producers = 8;
        int consumers = 8;

        IntStream.range(0, producers).forEach(i -> manager.addProducer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, producers).forEach(i -> manager.removeProducer());
        IntStream.range(0, consumers).forEach(i -> manager.addConsumer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, consumers).forEach(i -> manager.removeConsumer());
        IntStream.range(0, producers).forEach(i -> manager.addProducer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, producers).forEach(i -> manager.removeProducer());
        IntStream.range(0, consumers).forEach(i -> manager.addConsumer(delayMs));
        Thread.sleep(3000);
        IntStream.range(0, consumers).forEach(i -> manager.removeConsumer());

        manager.shutdown();
        assertEquals(0, manager.getActiveProducers(), "Producers should have terminated after shutdown.");
        assertEquals(0, manager.getActiveConsumers(), "Consumers should have terminated after shutdown.");
        int totalOrdersCreated = manager.getTotalOrdersAdded();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed,
                "All orders created (" + totalOrdersCreated + ") should have been processed (" + totalOrdersProcessed + ").");
        assertEquals(0, manager.getOrderQueueSize(),
                "Order queue " + manager.getOrderQueueSize() + " should be empty at the end.");
    }

    @Test
    @Order(7)
    @DisplayName("7. Long execution with mixed operations")
    public void longExecutionTest() throws InterruptedException {
        int producers = 8;
        int consumers = 8;

        IntStream.range(0, producers).forEach(i -> manager.addProducer(getRandomDelay()));
        IntStream.range(0, consumers).forEach(i -> manager.addConsumer(getRandomDelay()));
        Thread.sleep(60000);

        assertEquals(producers, manager.getActiveProducers(), "Producers should be active.");
        assertEquals(consumers, manager.getActiveConsumers(), "Consumers should be active.");
        manager.shutdown();
        assertEquals(0, manager.getActiveProducers(), "Producers should have terminated after shutdown.");
        assertEquals(0, manager.getActiveConsumers(), "Consumers should have terminated after shutdown.");

        int totalOrdersCreated = manager.getTotalOrdersAdded();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed,
                "All orders created (" + totalOrdersCreated + ") should have been processed (" + totalOrdersProcessed + ").");
        assertEquals(0, manager.getOrderQueueSize(),
                "Order queue " + manager.getOrderQueueSize() + " should be empty at the end.");
    }

    @Test
    @Order(9)
    @DisplayName("9. Activate shutdown repeatedly")
    public void activateShutdownRepeatedlyTest() throws InterruptedException {
        IntStream.range(0, 10).forEach(i -> manager.addProducer(100));
        IntStream.range(0, 10).forEach(i -> manager.addConsumer(100));
        Thread.sleep(4000);

        IntStream.range(0, 5).forEach(i -> manager.shutdown());

        int totalOrdersCreated = manager.getTotalOrdersAdded();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed,
                "All orders created (" + totalOrdersCreated + ") should have been processed (" + totalOrdersProcessed + ").");
        assertEquals(0, manager.getOrderQueueSize(),
                "Order queue " + manager.getOrderQueueSize() + " should be empty at the end.");
    }
}
