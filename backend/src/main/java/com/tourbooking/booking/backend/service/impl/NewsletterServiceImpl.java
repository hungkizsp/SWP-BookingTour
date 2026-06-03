package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.mapper.NewsletterMapper;
import com.tourbooking.booking.backend.model.dto.request.NewsletterRequest;
import com.tourbooking.booking.backend.model.dto.response.NewsletterResponse;
import com.tourbooking.booking.backend.model.entity.Newsletter;
import com.tourbooking.booking.backend.repository.NewsletterRepository;
import com.tourbooking.booking.backend.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsletterServiceImpl implements NewsletterService {

    private final NewsletterRepository newsletterRepo;

    @Override
    public List<NewsletterResponse> getAllNewsletters() {
        return newsletterRepo.findAll().stream()
                .map(NewsletterMapper::toResponse)
                .toList();
    }

    @Override
    public NewsletterResponse getNewsletterById(Long id) {
        Newsletter newsletter = newsletterRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWSLETTER_NOT_FOUND));
        return NewsletterMapper.toResponse(newsletter);
    }

    @Override
    @Transactional
    public NewsletterResponse createNewsletter(NewsletterRequest request) {
        String email = request == null ? null : request.getEmail();
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (newsletterRepo.existsByEmailIgnoreCase(email)) {
            // idempotent subscribe: return existing subscription instead of failing
            Newsletter existing = newsletterRepo.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new AppException(ErrorCode.NEWSLETTER_NOT_FOUND));
            return NewsletterMapper.toResponse(existing);
        }

        Newsletter newsletter = NewsletterMapper.toEntity(request);
        newsletter.setEmail(email);
        Newsletter savedNewsletter = newsletterRepo.save(newsletter);
        return NewsletterMapper.toResponse(savedNewsletter);
    }

    @Override
    @Transactional
    public NewsletterResponse updateNewsletter(Long id, NewsletterRequest request) {
        Newsletter existingNewsletter = newsletterRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NEWSLETTER_NOT_FOUND));
        NewsletterMapper.updateEntityFromRequest(existingNewsletter, request);
        Newsletter updatedNewsletter = newsletterRepo.save(existingNewsletter);
        return NewsletterMapper.toResponse(updatedNewsletter);
    }

    @Override
    @Transactional
    public void deleteNewsletter(Long id) {
        if (!newsletterRepo.existsById(id)) {
            throw new AppException(ErrorCode.NEWSLETTER_NOT_FOUND);
        }
        newsletterRepo.deleteById(id);
    }
}
