package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TourGroupMessages")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "MessageID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class TourGroupMessage extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ScheduleID", columnDefinition = "BIGINT")
    private TourSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SenderID", columnDefinition = "BIGINT")
    private User sender;

    /** Snapshot vai trò của sender tại thời điểm gửi (CUSTOMER / GUIDE), dùng để hiển thị nhãn đúng dù vai trò có đổi sau. */
    @Column(name = "SenderRole", length = 20)
    private String senderRole;

    @Column(name = "Message", columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "SentAt")
    private LocalDateTime sentAt;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}
