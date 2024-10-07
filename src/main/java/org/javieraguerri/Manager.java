package org.javieraguerri;

import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class Manager {
    private final OrderQueue orderQueue;
    private final OrderFactory orderFactory;
    private final OrderProcessor orderProcessor;
    private final List<Producer> producers = new CopyOnWriteArrayList<>();
    private final List<Consumer> consumers = new CopyOnWriteArrayList<>();
    @Getter
    protected int totalOrdersAdded = 0;
    @Value("${multithreading.consumer.limit:20}")
    private int consumerLimit;

    @Value("${multithreading.producer.limit:20}")
    private int producerLimit;

    public Manager(OrderQueue orderQueue,
                   OrderFactory orderFactory,
                   OrderProcessor orderProcessor) {
        this.orderQueue = orderQueue;
        this.orderFactory = orderFactory;
        this.orderProcessor = orderProcessor;
    }

    public void addProducer(long delayMs) {
        if (producers.size() < producerLimit) {
            Producer producer = new Producer(orderQueue, delayMs, orderFactory, producers.size());
            producers.add(producer);
        } else
            System.out.println("Can't create producer - reached limit: " + producerLimit);
    }

    public void removeProducer() {
        if (!producers.isEmpty())
            totalOrdersAdded += producers.remove(producers.size() - 1).kill();
    }

    public void addConsumer(long delayMs) {
        if (consumers.size() < consumerLimit) {
            Consumer consumer = new Consumer(orderQueue, delayMs, orderProcessor, consumers.size());
            consumers.add(consumer);
        } else
            System.out.println("Can't create consumer - reached limit: " + consumerLimit);
    }

    public void removeConsumer() {
        if (!consumers.isEmpty()) {
            consumers.remove(consumers.size() - 1).kill();
        }
    }

    @SneakyThrows
    public void shutdown() {
        System.out.println("Initiating shutdown");
        orderQueue.setShutdown(true);
        producers.forEach(p -> {
            p.kill();
            producers.remove(p);
        });
        totalOrdersAdded = orderQueue.getTotalOrdersAdded();
        synchronized (orderQueue.getLock()) {
            orderQueue.getLock().notifyAll(); // Wake up asleep consumers
        }
        consumers.forEach(c -> {
            c.shutdown();
            consumers.remove(c);
        });
        System.out.println("System shutdown complete");
    }

    public int getActiveProducers() {
        return producers.size();
    }

    public int getActiveConsumers() {
        return consumers.size();
    }

    public int getOrderQueueSize() {
        return orderQueue.currentSize();
    }

    public int getTotalOrdersProcessed() {
        return orderProcessor.getTotalOrdersProcessed();
    }

}
