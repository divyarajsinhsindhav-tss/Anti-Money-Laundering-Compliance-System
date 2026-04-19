package org.tss.tm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AmlTmServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AmlTmServerApplication.class, args);
	}

}
