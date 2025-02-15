package com.cdac.SpringCloudGateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

// @PropertySource({"classpath:/routes.yml"})
@SpringBootApplication
public class SpringCloudGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudGatewayApplication.class, args);
		System.out.println("\n***********************************************************************************************\n");
		System.out.println("                                  SPRING CLOUD GATEWAY SERVER STARTED                              ");
		System.out.println("\n***********************************************************************************************\n");
	}

}
