package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "Bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "BookingID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class Booking extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", columnDefinition = "BIGINT")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ScheduleID", columnDefinition = "BIGINT")
    private TourSchedule schedule;

    @Column(name = "BookingDate")
    private LocalDateTime bookingDate;

    @Column(name = "NumberOfPeople")
    private Integer numberOfPeople;

    @Column(name = "TotalPrice", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "DiscountAmount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "DiscountCode", length = 50)
    private String discountCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private BookingStatus status = BookingStatus.PENDING;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (bookingDate == null) {
            bookingDate = LocalDateTime.now();
        }
    }

}
