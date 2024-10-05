package org.javieraguerri;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Queue;

@Component
public class OrderQueue {
    private final Queue<Order> queue = new ArrayDeque<>();
    private final int MAX_QUEUE_SIZE;
    private final Object lock = new Object();
    private volatile boolean shutdown = false;

    public OrderQueue(@Value("${multithreading.maxQueueSize:4}") int maxQueueSize) {
        this.MAX_QUEUE_SIZE = maxQueueSize;
    }

    public boolean addOrder(OrderFactory orderFactory) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == MAX_QUEUE_SIZE && !shutdown) {
                System.out.println(Thread.currentThread().getName() + " found the queue full and goes to sleep!");
                lock.wait();
                System.out.println(Thread.currentThread().getName() + " woke up!");
            }
            if (shutdown) return false;
            Order order = orderFactory.produceOrder();
            queue.add(order);
            System.out.println(Thread.currentThread().getName() + " produced: " + order + " (Queue size: " + queue.size() + ")");
            lock.notifyAll();
            return true;
        }
    }

    public Order removeOrder() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty() && !shutdown) {
                System.out.println(Thread.currentThread().getName() + " found the queue empty and goes to sleep!");
                lock.wait();
                System.out.println(Thread.currentThread().getName() + " woke up!");
            }
            if (queue.isEmpty() && shutdown) return null;
            Order order = queue.poll();
            System.out.println(Thread.currentThread().getName() + " consumed: " + order + " (Queue size: " + queue.size() + ")");
            lock.notifyAll();
            return order;
        }
    }

    public void shutdown() {
        synchronized (lock) {
            shutdown = true;
            lock.notifyAll();
        }
    }

    public int currentSize() {
        synchronized (lock) {
            return queue.size();
        }
    }
}
