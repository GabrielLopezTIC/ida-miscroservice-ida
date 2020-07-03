package com.gabriel.core;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.gabriel.core.properties.FileStorageProperties;

@EnableConfigurationProperties({
	FileStorageProperties.class
})
@SpringBootApplication
public class MicroserviceIdaProcessApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(MicroserviceIdaProcessApplication.class, args);
	}
	
	@Override
	public void run(String... args) throws Exception {
	    // TODO Auto-generated method stub
	 
	}

}
