package com.tourbooking.booking.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "booking.passenger")
public class AgeCategoryConfig {
    
    /** Maximum age for an infant (exclusive). E.g., < 2 */
    private int infantMaxAge = 2;
    
    /** Maximum age for a child (exclusive). E.g., < 12 */
    private int childMaxAge = 12;

    /** Maximum allowed children + infants per adult */
    private int maxDependentsPerAdult = 2;
}
