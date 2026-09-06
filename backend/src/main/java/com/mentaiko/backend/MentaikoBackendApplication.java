package com.mentaiko.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = "com.mentaiko.backend.entity")
public class MentaikoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MentaikoBackendApplication.class, args);
	}

}
