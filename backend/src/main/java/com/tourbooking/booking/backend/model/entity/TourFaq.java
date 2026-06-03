package com.tourbooking.booking.backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "TourFaqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "FaqID", nullable = false, unique = true, columnDefinition = "BIGINT"))
public class TourFaq extends Base {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TourID")
    private Tour tour; // Nullable for global FAQs

    @Column(name = "Question", columnDefinition = "NVARCHAR(500)", nullable = false)
    private String question;

    @Column(name = "Answer", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String answer;
}
