package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.ChatEscalationStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ChatEscalations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "EscalationID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class ChatEscalation extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", columnDefinition = "BIGINT")
    private User customer;

    @Column(name = "GuestID", length = 50)
    private String guestId;

    @Column(name = "RequestNote", columnDefinition = "NVARCHAR(MAX)")
    private String requestNote;

    @Column(name = "MeetingPreference", length = 255)
    private String meetingPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    private ChatEscalationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedStaffID", columnDefinition = "BIGINT")
    private User assignedStaff;
}
