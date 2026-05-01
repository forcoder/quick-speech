package com.quickspeech.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;

@SpringBootApplication(scanBasePackages = "com.quickspeech",
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
    })
public class ApiApplication {

    public static void main(String[] args) {
        System.out.println("Starting Quick Speech API...");
        SpringApplication.run(ApiApplication.class, args);
        System.out.println("Quick Speech API started successfully!");
    }
}
