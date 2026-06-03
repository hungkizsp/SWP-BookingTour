package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "MessageID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class ChatMessages extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", columnDefinition = "BIGINT")
    private User user;

    @Column(name = "SenderType", length = 20)
    private String senderType;

    @Column(name = "Message", columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "GuestId", length = 50)
    private String guestId;

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
