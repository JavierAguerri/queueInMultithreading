package org.javieraguerri;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "multithreading.queue.size=10",
})
public class Config1ManagerTest extends BaseMultithreadingManagerTest {

    @Test
    @DisplayName("4. Normal operation with balanced execution (no boundary limits reached)")
    public void normalOperationBalancedExecutionNoHittingBoundariesTest() throws InterruptedException {
        manager.addProducer(500L);
        manager.addProducer(500L);
        Thread.sleep(600);
        manager.removeProducer();
        manager.addConsumer(500L);

        Thread.sleep(8000);
        manager.shutdown();
        int totalOrdersCreated = manager.getTotalOrdersAdded();
        int totalOrdersProcessed = manager.getTotalOrdersProcessed();
        assertEquals(totalOrdersCreated, totalOrdersProcessed,
                "All orders created (" + totalOrdersCreated + ") should have been processed (" + totalOrdersProcessed + ").");
        assertEquals(0, manager.getOrderQueueSize(),
                "Order queue " + manager.getOrderQueueSize() + " should be empty at the end.");
    }

}
