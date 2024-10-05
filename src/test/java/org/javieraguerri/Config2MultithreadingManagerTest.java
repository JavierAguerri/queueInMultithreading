package org.javieraguerri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
    @Order(8)
    @DisplayName("8. Add and remove producers and consumers beyond the limits")
    public void producerConsumerLimitsTest() throws InterruptedException {
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
}
