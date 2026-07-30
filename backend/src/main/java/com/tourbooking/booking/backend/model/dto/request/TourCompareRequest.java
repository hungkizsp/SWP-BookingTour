package com.tourbooking.booking.backend.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class TourCompareRequest {
    @NotNull
    @Size(min = 2, max = 4, message = "Please provide between 2 and 4 tour IDs to compare")
    private List<Long> tourIds;
}
