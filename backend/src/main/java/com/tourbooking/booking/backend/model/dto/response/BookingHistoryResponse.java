package com.tourbooking.booking.backend.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Response wrapper for booking history with statistics and pagination
 * Used in UC18: View Booking History
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistoryResponse {
    private List<BookingResponse> content;
    private BookingStatistics statistics;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    
    /**
     * Helper method to create response from Spring Data Page
     */
    public static BookingHistoryResponse from(Page<BookingResponse> page, BookingStatistics statistics) {
        return BookingHistoryResponse.builder()
                .content(page.getContent())
                .statistics(statistics)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
