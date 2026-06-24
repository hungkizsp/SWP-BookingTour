package com.tourbooking.booking.backend.model.dto.request;

import lombok.Data;

@Data
public class ReviewRequest {

    private Long bookingId;

    private Integer rating;

    private String comment;

}
