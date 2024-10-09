package com.neon.tamago;

import com.neon.tamago.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TamagoApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

}
