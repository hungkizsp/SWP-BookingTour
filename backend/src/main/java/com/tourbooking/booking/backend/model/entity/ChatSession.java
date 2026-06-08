package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.ChatSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChatSessions")
@Getter
@Setter
@NoArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "SessionID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class ChatSession extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", columnDefinition = "BIGINT")
    private User user;

    @Column(name = "GuestId", length = 50)
    private String guestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 30)
    private ChatSessionStatus status;

    @Column(name = "LastMessageAt")
    private LocalDateTime lastMessageAt;

    /**
     * Staff đang phụ trách phiên này.
     * Set NULL khi phiên được giải phóng (auto-release) để staff khác có thể tiếp nhận.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedStaffID", columnDefinition = "BIGINT")
    private User assignedStaff;
}
