package com.collection.benefitsjourney.config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {
public static void main(String[] args) {
BCryptPasswordEncoder encoder =
new BCryptPasswordEncoder();

    System.out.println(encoder.encode("0000"));
}

}