package com.tourbooking.booking.backend;

import com.tourbooking.booking.backend.config.DotenvInitializer;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@org.springframework.data.web.config.EnableSpringDataWebSupport(pageSerializationMode = org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
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
        Path envPath = DotenvInitializer.resolveEnvFile();
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
