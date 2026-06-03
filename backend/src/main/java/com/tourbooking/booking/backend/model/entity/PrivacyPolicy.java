package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PrivacyPolicies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "id", column = @Column(name = "PolicyID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class PrivacyPolicy extends Base {

    @Column(name = "Title", nullable = false, length = 255)
    private String title;

    @Column(name = "Content", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "IsActive")
    private Boolean isActive = true;
}
