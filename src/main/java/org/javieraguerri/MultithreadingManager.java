package org.javieraguerri;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadingManager {
    private final OrderQueue orderQueue;
    private final List<Producer> producers;
    private final List<Consumer> consumers;
    private final List<Thread> producerThreads;
    private final List<Thread> consumerThreads;
    private final AtomicInteger activeProducers;
    private final OrderFactory orderFactory;
    private final OrderProcessor orderProcessor;

    public MultithreadingManager(int maxQueueSize) {
        this.orderQueue = new OrderQueue(maxQueueSize);
        this.producers = new ArrayList<>();
        this.consumers = new ArrayList<>();
        this.producerThreads = new ArrayList<>();
        this.consumerThreads = new ArrayList<>();
        this.activeProducers = new AtomicInteger(0);
        this.orderFactory = new OrderFactory();
        this.orderProcessor = new OrderProcessor();
    }

    public void addProducer(long delayMs) {
        activeProducers.incrementAndGet();
        Producer producer = new Producer(orderQueue, delayMs, this, orderFactory);
        Thread producerThread = new Thread(producer, "Producer-" + (producerThreads.size() + 1));
        producers.add(producer);
        producerThreads.add(producerThread);
        System.out.println("Created thread " + producerThread.getName());
        producerThread.start();
    }

    public void addConsumer(long delayMs) {
        Consumer consumer = new Consumer(orderQueue, delayMs, orderProcessor);
        Thread consumerThread = new Thread(consumer, "Consumer-" + (consumerThreads.size() + 1));
        consumers.add(consumer);
        consumerThreads.add(consumerThread);
        System.out.println("Created thread " + consumerThread.getName());
        consumerThread.start();
    }


    public void removeProducer() {
        if (producers.isEmpty()) {
            System.out.println("No producers to remove.");
            return;
        }
        // Remove the last added producer
        int index = producers.size() - 1;
        Producer producer = producers.remove(index);
        Thread producerThread = producerThreads.remove(index);
        producer.shutdown();
        producerThread.interrupt(); // Interrupt the producer thread to wake it up
        try {
            producerThread.join();
            System.out.println("Removed and stopped " + producerThread.getName());
        } catch (InterruptedException e) {
            producerThread.interrupt();
            System.out.println("Interrupted while waiting for thread " + producerThread.getName());
        }
    }


    public void removeConsumer() {
        if (consumers.isEmpty()) {
            System.out.println("No consumers to remove.");
            return;
        }
        // Remove the last added consumer
        int index = consumers.size() - 1;
        consumers.remove(index);
        Thread consumerThread = consumerThreads.remove(index);
        // Optionally interrupt the consumer if needed
        consumerThread.interrupt();
        try {
            consumerThread.join();
            System.out.println("Removed and stopped " + consumerThread.getName());
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for thread " + consumerThread.getName());
        }
    }

    public void shutdown() {
        System.out.println("Initiating shutdown");

        // Shutdown the order queue first
        orderQueue.shutdown();

        // Shutdown all producers
        for (Producer producer : producers) {
            producer.shutdown();
        }

        // Wait for all producers to finish
        waitForThreadsToFinish(producerThreads);

        // Wait for all consumers to finish
        waitForThreadsToFinish(consumerThreads);

        System.out.println("System shutdown complete");
    }

    private void waitForThreadsToFinish(List<Thread> threads) {
        for (Thread thread : threads) {
            try {
                thread.join();
                System.out.println("Thread " + thread.getName() + " has finished.");
            } catch (InterruptedException e) {
                System.out.println("Interrupted while waiting for thread " + thread.getName());
            }
        }
    }

    // Getter for activeProducers
    public AtomicInteger getActiveProducers() {
        return activeProducers;
    }

    public List<Thread> getProducerThreads() {
        return producerThreads;
    }

    public List<Thread> getConsumerThreads() {
        return consumerThreads;
    }

    public int getOrderQueueSize() {
        return orderQueue.currentSize();
    }

    public int getTotalOrdersCreated() {
        return orderFactory.getTotalOrdersCreated();
    }
    public int getTotalOrdersProcessed() {
        return orderProcessor.getTotalOrdersProcessed();
    }

}
