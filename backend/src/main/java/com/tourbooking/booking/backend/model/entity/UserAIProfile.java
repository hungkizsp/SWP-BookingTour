package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserAIProfiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "ProfileID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class UserAIProfile extends Base {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", columnDefinition = "BIGINT", unique = true)
    private User user;

    @Column(name = "TravelStyle", length = 200)
    private String travelStyle;

    @Column(name = "FavoriteCategories", columnDefinition = "NVARCHAR(500)")
    private String favoriteCategories;

    @Column(name = "BudgetRange", length = 100)
    private String budgetRange;

    @Column(name = "PreferredDestinations", columnDefinition = "NVARCHAR(500)")
    private String preferredDestinations;

    @Column(name = "TravelFrequency", length = 50)
    private String travelFrequency;

    @Column(name = "FamilySize")
    private Integer familySize = 1;

    @Column(name = "LastAnalyzedAt")
    private LocalDateTime lastAnalyzedAt;
}
