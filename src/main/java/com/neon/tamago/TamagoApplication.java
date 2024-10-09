package com.neon.tamago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class TamagoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TamagoApplication.class, args);
    }

}
