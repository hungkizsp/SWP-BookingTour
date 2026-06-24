package com.tourbooking.booking.backend.scratch;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// @Component
@RequiredArgsConstructor
public class UnicodeDataFixer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        System.out.println("--- DANA UNICODE REPAIR START ---");
        try {
            // Sửa tên Tour "Sơn Trà" (S\u01A1n Tr\u00E0)
            // SQL Server cần tiền tố N'...' để nhận diện Unicode
            jdbcTemplate.execute("UPDATE Tours SET TourName = N'S\u01A1n Tr\u00E0' WHERE TourName LIKE 'S%n Tr%' OR TourName LIKE 'S?n Tr%'");
            jdbcTemplate.execute("UPDATE Tours SET Description = N'Kh\u00E1m ph\u00E1 linh h\u1ED3n c\u1EE7a \u0110\u00E0 N\u1EB5ng t\u1EA1i B\u00E1n \u0111\u1EA3o S\u01A1n Tr\u00E0.' WHERE TourName LIKE N'%S\u01A1n Tr\u00E0%'");
            
            System.out.println("Fixed Tour Names for Unicode.");
            System.out.println("--- DANA UNICODE REPAIR COMPLETED ---");
        } catch (Exception e) {
            System.err.println("Unicode repair failed: " + e.getMessage());
        }
    }
}
