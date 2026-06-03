package com.tourbooking.booking.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BookingApplication {

    public static void main(String[] args) {
        loadEnvFileIntoSystemProperties();
        SpringApplication.run(BookingApplication.class, args);
    }

    /**
     * Spring Boot không đọc file .env — nạp vào {@link System#setProperty} để khớp
     * {@code payos.client-id=${PAYOS_CLIENT_ID}} trong application.properties.
     * Ưu tiên biến môi trường OS đã có (production); chỉ bổ sung từ .env khi chưa
     * set.
     */
    static void loadEnvFileIntoSystemProperties() {
        Path[] candidates = new Path[] {
                Paths.get(".env"),
                Paths.get("backend", ".env"),
                Paths.get("..", ".env")
        };
        Path envPath = null;
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                envPath = p.toAbsolutePath().normalize();
                break;
            }
        }
        if (envPath == null) {
            return;
        }
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(envPath.getParent().toString())
                    .filename(envPath.getFileName().toString())
                    .ignoreIfMalformed()
                    .load();
            dotenv.entries().forEach(e -> {
                String key = e.getKey();
                String fromEnv = System.getenv(key);
                if (fromEnv != null && !fromEnv.isEmpty()) {
                    return;
                }
                if (System.getProperty(key) == null) {
                    System.setProperty(key, e.getValue());
                }
            });
        } catch (Exception ignored) {
            // .env không đọc được — vẫn chạy với biến OS / mặc định
        }
    }
}
