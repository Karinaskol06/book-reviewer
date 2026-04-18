package com.project.bookreviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BookReviewerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookReviewerApplication.class, args);
    }

}
