package com.example.jewell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
		// Exclude Redis auto-configuration - we'll handle it manually via RedisCo
		// This prevents Spring Boot from auto-configuring Redis
		RedisAutoConfiguration.class,
		RedisRepositoriesAutoConfiguration.class
})
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}