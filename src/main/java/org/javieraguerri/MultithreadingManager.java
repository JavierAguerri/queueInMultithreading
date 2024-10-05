package org.javieraguerri;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadingManager {
    private final OrderQueue orderQueue;
    private final List<Producer> producers = new ArrayList<>();
    private final List<Consumer> consumers = new ArrayList<>();
    private final List<Thread> producerThreads = new ArrayList<>();
    private final List<Thread> consumerThreads = new ArrayList<>();
    private final AtomicInteger activeProducers = new AtomicInteger(0);
    private final AtomicInteger activeConsumers = new AtomicInteger(0);
    private final OrderFactory orderFactory = new OrderFactory();
    private final OrderProcessor orderProcessor = new OrderProcessor();
    private final int consumerLimit;
    private final int producerLimit;

    private enum ShutdownState {
        NOT_STARTED, IN_PROGRESS, COMPLETED
    }

    private volatile ShutdownState shutdownState = ShutdownState.NOT_STARTED;

    public MultithreadingManager(int maxQueueSize) {
        this(maxQueueSize, 20, 20);
    }

    public MultithreadingManager(int maxQueueSize, int consumerLimit, int producerLimit) {
        this.orderQueue = new OrderQueue(maxQueueSize);
        this.consumerLimit = consumerLimit;
        this.producerLimit = producerLimit;
    }

    public void addProducer(long delayMs) {
        if (activeProducers.get() < producerLimit) {
            activeProducers.incrementAndGet();
            Producer producer = new Producer(orderQueue, delayMs, orderFactory);
            Thread producerThread = new Thread(producer, "Producer-" + (producerThreads.size() + 1));
            producers.add(producer);
            producerThreads.add(producerThread);
            System.out.println("Created thread " + producerThread.getName());
            producerThread.start();
        } else {
            System.out.println("Can't create producer thread - reached limit: " + producerLimit);
        }
    }

    public void addConsumer(long delayMs) {
        if (activeConsumers.get() < consumerLimit) {
            activeConsumers.incrementAndGet();
            Consumer consumer = new Consumer(orderQueue, delayMs, orderProcessor);
            Thread consumerThread = new Thread(consumer, "Consumer-" + (consumerThreads.size() + 1));
            consumers.add(consumer);
            consumerThreads.add(consumerThread);
            System.out.println("Created thread " + consumerThread.getName());
            consumerThread.start();
        } else {
            System.out.println("Can't create consumer thread - reached limit: " + consumerLimit);
        }
    }

    public void removeProducer() {
        if (producers.isEmpty()) {
            System.out.println("No producers to remove.");
            return;
        }
        int index = producers.size() - 1;
        Producer producer = producers.remove(index);
        Thread producerThread = producerThreads.remove(index);
        producer.shutdown();
        producerThread.interrupt();
        activeProducers.decrementAndGet();
        try {
            producerThread.join();
            System.out.println("Removed and stopped " + producerThread.getName() + ". Active producers: " + activeProducers.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while waiting for thread " + producerThread.getName());
        }
    }

    public void removeConsumer() {
        if (consumers.isEmpty()) {
            System.out.println("No consumers to remove.");
            return;
        }
        int index = consumers.size() - 1;
        Thread consumerThread = consumerThreads.remove(index);
        consumers.remove(index);
        activeConsumers.decrementAndGet();
        consumerThread.interrupt();
        try {
            consumerThread.join();
            System.out.println("Removed and stopped " + consumerThread.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while waiting for thread " + consumerThread.getName());
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (shutdownState != ShutdownState.NOT_STARTED) {
                System.out.println("Shutdown already in progress or completed");
                return;
            }
            System.out.println("Initiating shutdown");
            shutdownState = ShutdownState.IN_PROGRESS;
        }

        orderQueue.shutdown();
        producers.forEach(Producer::shutdown);
        producerThreads.forEach(Thread::interrupt);

        waitForThreadsToFinish(producerThreads);
        waitForThreadsToFinish(consumerThreads);

        shutdownState = ShutdownState.COMPLETED;
        System.out.println("System shutdown complete");
    }

    private void waitForThreadsToFinish(List<Thread> threads) {
        threads.forEach(thread -> {
            try {
                thread.join();
                System.out.println("Thread " + thread.getName() + " has finished.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting for thread " + thread.getName());
            }
        });
    }

    public AtomicInteger getActiveProducers() {
        return activeProducers;
    }

    public AtomicInteger getActiveConsumers() {
        return activeConsumers;
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
