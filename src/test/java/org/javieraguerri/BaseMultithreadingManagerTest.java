package org.javieraguerri;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BaseMultithreadingManagerTest {

    @Autowired
    protected MultithreadingManager manager;

    protected static long getRandomDelay() {
        return 100L + (long) (Math.random() * 3901);
    }

}
