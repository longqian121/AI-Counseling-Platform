package org.example.aispringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiSpringbootApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiSpringbootApplication.class, args);
	}

}
