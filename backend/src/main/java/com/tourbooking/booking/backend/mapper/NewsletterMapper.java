package com.tourbooking.booking.backend.mapper;

import com.tourbooking.booking.backend.model.dto.request.NewsletterRequest;
import com.tourbooking.booking.backend.model.dto.response.NewsletterResponse;
import com.tourbooking.booking.backend.model.entity.Newsletter;

public class NewsletterMapper {

    public static NewsletterResponse toResponse(Newsletter newsletter) {
        if (newsletter == null)
            return null;
        NewsletterResponse response = new NewsletterResponse();
        response.setId(newsletter.getId());
        response.setEmail(newsletter.getEmail());
        response.setSubscribedAt(newsletter.getSubscribedAt());
        response.setSubscribed(true); // Since it exists in DB
        return response;
    }

    public static Newsletter toEntity(NewsletterRequest request) {
        if (request == null)
            return null;
        Newsletter newsletter = new Newsletter();
        updateEntityFromRequest(newsletter, request);
        return newsletter;
    }

    public static void updateEntityFromRequest(Newsletter newsletter, NewsletterRequest request) {
        if (request == null || newsletter == null)
            return;
        if (request.getEmail() != null) {
            newsletter.setEmail(request.getEmail());
        }
    }
}
