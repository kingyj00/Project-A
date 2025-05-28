package com.ll.P_A;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class PAApplication {

	private static final Logger log = LoggerFactory.getLogger(PAApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(PAApplication.class, args);
		log.info("HELLO"); // 로그에 출력
	}
}