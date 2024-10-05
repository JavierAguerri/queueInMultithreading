# Order queue - multithreading
<p>The following code implements part of a purchase order system. It consists of a component that
puts the orders arriving in a queue, and another component takes the orders from the queue
and processes them. Each of these parts runs in a separate thread or process.</p>

<p>The queue has a maximum size, so if it is full, no more orders should be added until space is
freed. In the same way, no orders should be processed if the queue becomes empty. The
sleep() and wakeup() functions are used for thread synchronization, indicating when a thread
should pause or resume its activity.</p>

```
thread place_order (data) {
    while (true) {
        order = OrderFactory.produceOrder (data);
        if (self.queue.length () == MAX_QUEUE_SIZE) {
            // queue is full. do nothing until somebody wakes us up
            sleep ();
        }
        self.queue.insert (order);
        if (self.queue.size > 0) {
            // we have at least 1 element to process, notify the other component
            wakeup (process_order);
        }
    }
}

thread process_order () {
    while (true) {
        if (self.queue.length () == 0) {
            // queue is empty, block until there is at least 1 element
            sleep ();
        } 
        // if queue was full and we just made room, notify the producer
        if (self.queue.length () == MAX_QUEUE_SIZE - 1) {
            wakeup (place_order);
        }
        order = self.queue.pop ();
        do_actual_processing (order);
    }
}
```

<p>Assume that there are automated tests that take a string that describes the sequence of
operations of the two components: "place_order (data1), place_order (data2), process_order (), 
process_order (), ..." (or in its abbreviated version: "P, P, C, C, ... ").</p>

## Test cases
<p><i>Describe the most relevant test cases to be tested, and for each test case, indicate the input
that generates it (assume MAX_QUEUE_SIZE = 4). Use the abbreviated version “P,P,C,C…” to indicate the input.</i></p>

### Answer

```
Case 1. Processing from an empty queue
Input: "C, C, C"
Description: there is no processing if no items are in the queue.

Case 2. Force the upper boundary (queue full)
Input: "P, P, P, P, P, P ..."
Description: the placing thread does not proceed if the queue is full and remains blocked.

Case 3. Start with processing from an empty queue and then continue normally
Input: "C, P, P, C, P, P, C, C, P"
Description: it can handle attempts to process when the queue is empty and resume operations as new orders are added.

Case 4. Normal operation with balanced execution (no boundary limits reached)
Input: "P, P, P, C, P, C, C, P, C, P, P, C, P, C ..."
Description: it can handle a steady flow of placing and processing where limits are not hit.

Case 5. Gently hitting boundaries (without forcing them)
Input: "P, P, P, P, C, C, P, P, C, C, C, C, P, P, C, C, P, P, P, P, C, C, C, C ..."
Description: it can handle a steady flow of placing and processing where limits are hit and becomes full and empty intermittently, but boundaries are not forced.

Case 6. Burst of placing orders and burst of processing
Input: "P, P, P, P, P, P, P, P, C, C, C, C, C, C, C, C, ..."
Descrition: it can handle bursts of orders placed followed by bursts of processing

Case 7. Long execution with mixed operations
Input: "C, C, P, C, C, P, C, C, P, P, C, C, P, P, P, C, C, C, C, P"
Desciption: it can handle a long sequence of events without issues.

```

## Improvements
<p><i>Analyze the pseudo-code and identify if it contains any error(s). If there are no errors but
you want to propose improvements, detail them.</i></p>

### Answer

<p>First I would like to make an observation. In the provided pseudocode, the queue is referenced with
self.queue. I assume that self refers to the same shared instance, meaning both threads are trying to access the same queue.</p>

<p>Now let's discuss bugs and improvements:</p>
<ul>
<li>Inconsistent methods to check queue status: size vs length.</li>
<li>No synchronization strategy for common resources (queue), eventually this will cause data corruption, deadlocks and malfunctioning.</li>
<li>Use of conditionals instead of loops may cause spurious wakeups.</li>
<li>There could be a deadlock if process_order is delayed for some reason right after checking the queue length but before
executing sleep(), and in the meantime place_order fills the queue and does sleep().</li>
<li>Condition variables or synchronized blocks could be used for better synchronization.</li>
<li>process_order checks the queue length before it pops. In case place_order was waiting, this means process_order has to execute again
so the condition of the queue length becomes true. The missed wakeup introduces a delay and reduces the throughput.</li>
<li>There is no error handling. Upon exceptions the system may enter an invalid state or it may lose data. For example, if the queue 
is empty and place_order starts but it terminates abruptly right before waking up process_order, then the order will be lost.</li>
<li>Because of the infinite loops, there is no way to stop the system gracefully.</li>
</ul>
<p>In the current project there is an implemented solution which deals with the mentioned bugs and includes the improvements. 
Browse the repository and check it, but conceptually you can get an idea from the following pseudocode:</p>

```
thread place_order(data) {
    while (!shutdown) {
        order = OrderFactory.produceOrder(data);
        lock.acquire();
        try {
            while (self.queue.length() == MAX_QUEUE_SIZE) {
                conditionHasSpace.wait()
            }
            if (!shutdown) {
                self.queue.insert(order);
            }
            // we leave the notification outside the conditional to unlock process_order if it was locked
            conditionHasItems.notifyAll();  
        }
        finally {
            lock.release();
        }
    }
}

thread process_order() {
    while (true) {
        lock.acquire();
        try {
            while (self.queue.length() == 0) {
                if (shutdown) {
                    exit;
                }
                conditionHasItems.wait();
            }
            order = self.queue.pop();
            conditionHasSpace.notifyAll();
        }
        finally {
            lock.release();
        }
        do_actual_processing(order);
    }
}
```

## Multiple order producer and consumer
<p><i>If instead of having 1 order generator and 1 order processor, there were N and N, all sharing
the same queue, what new situations should it cover that the previous tests do not cover? 
Detail how you would implement the tests and describe a typical error.</i></p>

### Answer
<p>Possible issues related to deadlocks and race conditions are already covered by tests from section 1. 
Also those issues are mitigated by the improvements introduced in section 2.
However, with multiple producers and consumers, new challenges arise:</p>
<ul>
<li>Too many producers and/or consumers -> Implement limits -> test those limits</li>
<li>No way to stop the execution -> Implement shutdown mechanism -> test shutdown</li>
</ul>

<p>Here the list of additional test cases:</p>

```
Case 8. Add and remove producers and consumers beyond the limits.
Description: the system should handle this gracefully and perform as usual 

Case 9. Activate shutdowm repeatedly
Description: the system should shutdown gracefully as if shutdown were executed just once (shutdown operation should be idempotent)
```
<p>This project implements also all the tests listed both in section 1 and 3. In order to run them, follow these instructions:</p>
<ul>
<li>Install Java 17 and set $JAVA_HOME</li>
<li>Install maven 3.8.5</li>
<li>Open IDE (e.g. IntelliJ) and open project</li>
<li>Build project</li>
<li>Run all the tests</li>
</ul>

<p>Other testing considerations that are not implemented in this demo but could be part of a comprehensive test plan:</p>
<ul>
<li>Security testing: ensure threads, consumers and producers are unable to access data of other elements.</li>
<li>Endurance testing: monitor memory usage during long periods of time in order to detect memory leaks.</li>
<li>Stress testing: send a high number of requests to the system and monitor health status and response times.</li>
</ul>

