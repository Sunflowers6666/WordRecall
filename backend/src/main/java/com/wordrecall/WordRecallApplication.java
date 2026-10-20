package com.wordrecall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.wordrecall.mapper")
public class WordRecallApplication {

    public static void main(String[] args) {
        SpringApplication.run(WordRecallApplication.class, args);
    }
}
