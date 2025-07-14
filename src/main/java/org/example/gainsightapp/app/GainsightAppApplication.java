package org.example.gainsightapp.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "org.example.gainsightapp")
@EnableMongoRepositories(basePackages = "org.example.gainsightapp.repository")
@EnableScheduling
@EnableRetry
public class GainsightAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(GainsightAppApplication.class, args);
	}

}
