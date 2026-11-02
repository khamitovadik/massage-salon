package com.salon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MassageSalonApplication {

    public static void main(String[] args) {
        // Сервер на хостинге работает в UTC, а салон — в Алматы (UTC+5): без этого
        // LocalDateTime.now() отстаёт на 5 часов и слоты «прошли» не те
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Almaty"));
        SpringApplication.run(MassageSalonApplication.class, args);
    }
}
