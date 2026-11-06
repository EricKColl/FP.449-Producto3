package edu.uoc.biblioteca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Async y Scheduling: los avisos en tiempo real se envían en otro hilo y llevan un latido periódico (AvisosEnVivo)
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BibliotecaApplication {

	public static void main(String[] args) {
		SpringApplication.run(BibliotecaApplication.class, args);
	}

}
