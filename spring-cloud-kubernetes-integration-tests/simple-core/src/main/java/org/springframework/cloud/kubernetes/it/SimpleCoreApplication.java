package org.springframework.cloud.kubernetes.it;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SimpleCoreApplication {

	@GetMapping("/greeting")
	public Greeting home() {
		return new Greeting("Hello Spring Boot");
	}

	public static void main(String[] args) {
		SpringApplication.run(SimpleCoreApplication.class, args);
	}

	public static class Greeting {

		private final String message;

		public Greeting(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}

}
