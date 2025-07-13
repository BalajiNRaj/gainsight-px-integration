package org.example.gainsightapp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "org.example.gainsightapp")
@EntityScan(basePackages = "org.example.gainsightapp.model")
@EnableJpaRepositories(basePackages = "org.example.gainsightapp.repository")
@EnableScheduling
@EnableRetry
public class GainsightAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(GainsightAppApplication.class, args);
	}

}
