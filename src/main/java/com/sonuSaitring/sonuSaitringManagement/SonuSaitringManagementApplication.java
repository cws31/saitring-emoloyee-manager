package com.sonuSaitring.sonuSaitringManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SonuSaitringManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(SonuSaitringManagementApplication.class, args);
		System.out.println("MY HASH: "
				+ new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("Sonusaitring@31"));
	}

}
