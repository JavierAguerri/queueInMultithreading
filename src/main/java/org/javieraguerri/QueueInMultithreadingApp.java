package org.javieraguerri;

import java.util.stream.IntStream;

public class QueueInMultithreadingApp {
    private static final int MAX_QUEUE_SIZE = 4;
    private static final int NUMBER_OF_PRODUCERS = 3;
    private static final int NUMBER_OF_CONSUMERS = 3;
    private static final long RUN_DURATION_MS = 15000;

    public static void main(String[] args) {
        MultithreadingManager manager = new MultithreadingManager(MAX_QUEUE_SIZE);

        IntStream.range(0, NUMBER_OF_PRODUCERS)
                .forEach(i -> manager.addProducer(getRandomDelay()));

        IntStream.range(0, NUMBER_OF_CONSUMERS)
                .forEach(i -> manager.addConsumer(getRandomDelay()));

        try {
            Thread.sleep(RUN_DURATION_MS / 2);
            manager.removeProducer();
            manager.removeConsumer();
            Thread.sleep(RUN_DURATION_MS / 2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        manager.shutdown();

        int totalCreated = manager.getTotalOrdersCreated();
        int totalProcessed = manager.getTotalOrdersProcessed();

        System.out.println("Total orders created: " + totalCreated);
        System.out.println("Total orders processed: " + totalProcessed);

        if (totalCreated == totalProcessed) {
            System.out.println("All orders were processed successfully.");
        } else {
            System.out.println("Mismatch in orders created and processed!");
        }
    }

    private static long getRandomDelay() {
        return 100L + (long) (Math.random() * 3901);
    }
}
