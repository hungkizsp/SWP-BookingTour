package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Reviews", uniqueConstraints = { @UniqueConstraint(columnNames = { "BookingID" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "ReviewID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class Review extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", columnDefinition = "BIGINT")
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID", columnDefinition = "BIGINT", unique = true)
    private Booking booking;

    @Column(name = "Rating")
    private Integer rating;

    @Column(name = "Comment", columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "ReviewDate")
    private LocalDateTime reviewDate;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (reviewDate == null) {
            reviewDate = LocalDateTime.now();
        }
    }
}
