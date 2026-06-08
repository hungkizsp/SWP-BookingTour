package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.model.entity.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "RefundRequests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "RefundID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class RefundRequest extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingID", columnDefinition = "BIGINT")
    private Booking booking;

    @Column(name = "Amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "Reason", columnDefinition = "NVARCHAR(MAX)")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 50)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "StaffNote", columnDefinition = "NVARCHAR(MAX)")
    private String staffNote;

    @Column(name = "ProcessedAt")
    private LocalDateTime processedAt;

    /**
     * Trạng thái booking TRƯỚC KHI khách bấm yêu cầu hoàn tiền (CONFIRMED hoặc SUCCESS).
     * Dùng để khôi phục chính xác khi Staff REJECT — không hardcode if/else.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "OriginalBookingStatus", length = 50)
    private BookingStatus originalBookingStatus;
}
