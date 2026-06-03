package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "DocumentID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class Document extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", referencedColumnName = "UserID", nullable = false)
    private User user;

    @Column(name = "FileUrl", length = 500)
    private String fileUrl;

    @Column(name = "Type", length = 100)
    private String type;

    @Column(name = "UploadedAt")
    private LocalDateTime uploadedAt;
}