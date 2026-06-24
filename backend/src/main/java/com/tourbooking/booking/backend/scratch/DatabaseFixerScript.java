package com.tourbooking.booking.backend.scratch;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// @Component
@RequiredArgsConstructor
public class DatabaseFixerScript implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        System.out.println("--- DANA DATABASE REPAIR START ---");
        try {
            // Liệt kê các cột đang có để debug
            jdbcTemplate.query("SELECT TOP 1 * FROM Users", rs -> {
                var metaData = rs.getMetaData();
                System.out.print("Current Columns in Users: ");
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    System.out.print(metaData.getColumnName(i) + ", ");
                }
                System.out.println();
            });

            // Sửa cột IsActive
            try { jdbcTemplate.execute("ALTER TABLE Users ADD IsActive BIT DEFAULT 1"); } catch (Exception e) {}
            // Sửa cột CurrentSessionId
            try { jdbcTemplate.execute("ALTER TABLE Users ADD CurrentSessionId NVARCHAR(64)"); } catch (Exception e) {}
            
            System.out.println("--- DANA DATABASE REPAIR COMPLETED ---");
        } catch (Exception e) {
            System.err.println("Database info check: " + e.getMessage());
        }
    }
}
