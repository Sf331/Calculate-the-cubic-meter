package com.kelifang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kelifang.**.mapper")
public class KelifangApplication {

    public static void main(String[] args) {
        SpringApplication.run(KelifangApplication.class, args);
    }
}
