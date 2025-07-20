package edu.udg.tfg.SyncService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableFeignClients
@EnableWebSocket
@EnableScheduling
public class SyncServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyncServiceApplication.class, args);
	}

}
