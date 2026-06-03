package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PaymentLogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "LogID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class PaymentLog extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PaymentID", columnDefinition = "BIGINT")
    private Payment payment;

    @Column(name = "LogMessage", columnDefinition = "NVARCHAR(MAX)")
    private String logMessage;
}
