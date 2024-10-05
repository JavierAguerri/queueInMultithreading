package org.javieraguerri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "multithreading.queue.size=10",
})
public class Config1MultithreadingManagerTest extends BaseMultithreadingManagerTest {

    @Test
    @Order(1)
    @DisplayName("4. Normal operation with balanced execution (no boundary limits reached)")
    public void normalOperationBalancedExecutionNoHittingBoundariesTest() throws InterruptedException {
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

}
