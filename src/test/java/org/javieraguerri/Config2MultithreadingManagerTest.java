package org.javieraguerri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "multithreading.producer.limit=4",
        "multithreading.consumer.limit=4"
})
@TestPropertySource(locations = "classpath:application.properties")
public class Config2MultithreadingManagerTest extends BaseMultithreadingManagerTest {

    @Value("${multithreading.producer.limit}")
    private int producerLimit;
    @Value("${multithreading.consumer.limit}")
    private int consumerLimit;

    @Test
    @DisplayName("8. Add and remove producers and consumers beyond the limits")
    public void producerConsumerLimitsTest() throws InterruptedException {
        IntStream.range(0, 10).forEach(i -> manager.addProducer(100));
        IntStream.range(0, 10).forEach(i -> manager.addConsumer(100));
        Thread.sleep(3000);
        assertEquals(producerLimit, manager.getActiveProducers());
        assertEquals(consumerLimit, manager.getActiveConsumers());

        IntStream.range(0, 10).forEach(i -> manager.removeProducer());
        while (manager.getOrderQueueSize() > 0) {
            Thread.sleep(100);
        }
        IntStream.range(0, 10).forEach(i -> manager.removeConsumer());
        Thread.sleep(3000);
        assertEquals(0, manager.getActiveProducers());
        assertEquals(0, manager.getActiveConsumers());

        manager.shutdown();
        int totalOrdersCreated = manager.getTotalOrdersAdded();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed,
                "All orders created (" + totalOrdersCreated + ") should have been processed (" + totalOrdersProcessed + ").");
        assertEquals(0, manager.getOrderQueueSize(),
                "Order queue " + manager.getOrderQueueSize() + " should be empty at the end.");
    }
}
