package it.smartcommunitylab.playandgo.hsc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HSCApplication {

	public static void main(String[] args) {
		SpringApplication.run(HSCApplication.class, args);
	}

}
