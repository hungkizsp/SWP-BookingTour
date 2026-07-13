package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "UserNotifications")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "NotificationID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class UserNotification extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", columnDefinition = "BIGINT")
    private User user;

    @Column(name = "Title", length = 200)
    private String title;

    @Column(name = "Message", columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "Type", length = 50)
    private String type;

    @Column(name = "Link", length = 500)
    private String link;

    @Column(name = "IsRead", nullable = false)
    private boolean isRead = false;
}
