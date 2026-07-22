package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "TourChatGroupMembers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"GroupID", "UserID"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourChatGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GroupID", nullable = false)
    private TourChatGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "JoinedAt", updatable = false, nullable = false)
    private LocalDateTime joinedAt;
}
