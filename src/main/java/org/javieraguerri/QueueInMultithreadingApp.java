package org.javieraguerri;

public class QueueInMultithreadingApp {
    private static final int MAX_QUEUE_SIZE = 4;
    private static final int NUMBER_OF_PRODUCERS = 3;
    private static final int NUMBER_OF_CONSUMERS = 3;
    private static final long RUN_DURATION_MS = 15000;

    public static void main(String[] args) {
        MultithreadingManager manager = new MultithreadingManager(MAX_QUEUE_SIZE);

        // Adding producers with random delays
        for (int i = 0; i < NUMBER_OF_PRODUCERS; i++) {
            long delayMs = getRandomDelay();
            manager.addProducer(delayMs);
        }

        // Adding consumers with random delays
        for (int i = 0; i < NUMBER_OF_CONSUMERS; i++) {
            long delayMs = getRandomDelay();
            manager.addConsumer(delayMs);
        }

        // Simulate runtime behavior and remove a producer and consumer
        try {
            Thread.sleep(RUN_DURATION_MS / 2);
            manager.removeProducer();
            manager.removeConsumer();
            Thread.sleep(RUN_DURATION_MS / 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        manager.shutdown();

        // Print total orders created and processed
        int totalCreated = manager.getTotalOrdersCreated();
        int totalProcessed = manager.getTotalOrdersProcessed();

        System.out.println("Total orders created: " + totalCreated);
        System.out.println("Total orders processed: " + totalProcessed);

        // Assert that all orders created were processed
        if (totalCreated == totalProcessed) {
            System.out.println("All orders were processed successfully.");
        } else {
            System.out.println("Mismatch in orders created and processed!");
        }
    }

    private static long getRandomDelay() {
        // Generate a random delay between 100 and 4000 milliseconds
        return 100L + (long) (Math.random() * 3901);
    }
}
