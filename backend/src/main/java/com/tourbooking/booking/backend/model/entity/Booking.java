package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.SuspensionActionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    /** Số chỗ thực tế chiếm trên schedule (ADULT + CHILD; INFANT không tính). */
    @Column(name = "OccupiedSlots")
    private Integer occupiedSlots;

    @Column(name = "TotalPrice", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "DiscountAmount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "DiscountCode", length = 50)
    private String discountCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DiscountID")
    private Discount discount;

    @Column(name = "LoyaltyPointsUsed")
    private Integer loyaltyPointsUsed = 0;

    @Column(name = "LoyaltyDiscountAmount", precision = 10, scale = 2)
    private BigDecimal loyaltyDiscountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "CancellationReason", length = 500)
    private String cancellationReason;

    /**
     * Tracks whether customer has responded to a suspension event affecting this booking.
     * NULL = not affected by any suspension. PENDING_CUSTOMER_ACTION = awaiting decision.
     * RESOLVED = customer already chose reschedule or refund.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "SuspensionActionStatus", length = 50)
    private SuspensionActionStatus suspensionActionStatus;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Passenger> passengers = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Review review;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (bookingDate == null) {
            bookingDate = LocalDateTime.now();
        }
    }

}
