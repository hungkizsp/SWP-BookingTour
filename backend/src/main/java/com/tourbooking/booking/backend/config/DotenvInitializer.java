package com.tourbooking.booking.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Nạp biến môi trường từ file .env (ưu tiên đường dẫn dự án) trước khi Spring đọc application.properties.
 */
public class DotenvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Path envPath = resolveEnvFile();
        if (envPath == null) {
            return;
        }

        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(envPath.getParent().toString())
                    .filename(envPath.getFileName().toString())
                    .ignoreIfMalformed()
                    .load();

            Map<String, Object> props = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String fromEnv = System.getenv(key);
                if (fromEnv != null && !fromEnv.isEmpty()) {
                    return;
                }
                if (System.getProperty(key) == null) {
                    System.setProperty(key, entry.getValue());
                    props.put(key, entry.getValue());
                }
            });

            if (!props.isEmpty()) {
                applicationContext.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("dotenvProperties", props));
            }
        } catch (Exception ignored) {
            // .env không đọc được — tiếp tục với biến OS
        }
    }

    public static Path resolveEnvFile() {
        Path[] candidates = new Path[] {
                Paths.get("D:", "GitHub", "SWP-BookingTour", ".env"),
                Paths.get(".env"),
                Paths.get("backend", ".env"),
                Paths.get("..", ".env")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }
}
