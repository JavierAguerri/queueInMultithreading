package org.javieraguerri;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Queue;

@Component
public class OrderQueue {
    private final Queue<Order> queue = new ArrayDeque<>();
    private final int MAX_QUEUE_SIZE;
    @Getter
    private final Object lock = new Object();
    @Setter
    private boolean shutdown = false;
    @Getter
    protected volatile int totalOrdersAdded = 0;


    protected OrderQueue(@Value("${multithreading.maxQueueSize:4}") int maxQueueSize) {
        this.MAX_QUEUE_SIZE = maxQueueSize;
    }

    protected void addOrder(Order order) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == MAX_QUEUE_SIZE) {
                System.out.println(Thread.currentThread().getName() + " found the queue full and goes to sleep!");
                lock.wait();
                System.out.println(Thread.currentThread().getName() + " woke up!");
            }
            queue.add(order);
            totalOrdersAdded += 1;
            System.out.println(Thread.currentThread().getName() + " produced: " + order + " (Queue size: " + queue.size() + ")");
            lock.notifyAll();
        }
    }

    protected Order removeOrder() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                if (shutdown)
                    return null;
                System.out.println(Thread.currentThread().getName() + " found the queue empty and goes to sleep!");
                lock.wait();
                System.out.println(Thread.currentThread().getName() + " woke up!");
            }
            Order order = queue.poll();
            if (order != null)
                System.out.println(Thread.currentThread().getName() + " consumed: " + order + " (Queue size: " + queue.size() + ")");
            lock.notifyAll();
            return order;
        }
    }

    protected int currentSize() {
        synchronized (lock) {
            return queue.size();
        }
    }
}
