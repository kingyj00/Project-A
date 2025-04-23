package com.ll.P_A;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class PAApplication {

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);

		System.out.print("입력: ");
		String input = scanner.nextLine();

		System.out.println("입력값 : " + input);

		SpringApplication.run(PAApplication.class, args);
	}
}