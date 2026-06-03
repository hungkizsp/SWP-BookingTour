package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.entity.TourFaq;
import com.tourbooking.booking.backend.repository.TourFaqRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faqs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FaqController {

    private final TourFaqRepository faqRepository;

    public FaqController(TourFaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @GetMapping("/global")
    public List<TourFaq> getGlobalFaqs() {
        return faqRepository.findByTourIsNull();
    }

    @GetMapping("/tour/{tourId}")
    public List<TourFaq> getTourFaqs(@PathVariable Long tourId) {
        return faqRepository.findByTour_Id(tourId);
    }
}
