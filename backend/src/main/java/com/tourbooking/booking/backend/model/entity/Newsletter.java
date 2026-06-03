package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Newsletters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "SubscriberID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class Newsletter extends Base {

    @Column(name = "Email", unique = true, length = 100)
    private String email;

    @Column(name = "SubscribedAt")
    private LocalDateTime subscribedAt;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
    }
}
