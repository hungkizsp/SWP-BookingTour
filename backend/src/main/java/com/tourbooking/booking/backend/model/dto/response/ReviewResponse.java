package com.tourbooking.booking.backend.model.dto.response;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ReviewResponse {

    private Long reviewId;
    private Long userId;
    private String userName;
    private Long tourId;
    private String tourName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

}
