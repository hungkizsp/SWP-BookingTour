package com.tourbooking.booking.backend.model.entity;

import com.tourbooking.booking.backend.model.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "UserID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class User extends Base {

    @Column(name = "FullName", length = 100)
    private String fullName;

    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "PasswordHash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", length = 20)
    private UserRole role;

    @Column(name = "AvatarURL", length = 255)
    private String avatarUrl;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "Address", length = 255)
    private String address;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    @Column(name = "CurrentSessionID", length = 64)
    private String currentSessionId;

    @Column(name = "Bio", columnDefinition = "NVARCHAR(MAX)")
    private String bio;

    @Column(name = "ExperienceYears")
    private Integer experienceYears;

    @Column(name = "DateOfBirth")
    private java.time.LocalDate dateOfBirth;

    @Column(name = "Gender", length = 20)
    private String gender;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Booking> bookings;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Review> reviews;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Wishlist> wishlist;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ChatMessages> chatMessages;
}
