package com.tourbooking.booking.backend.controller;

import com.tourbooking.booking.backend.model.entity.PrivacyPolicy;
import com.tourbooking.booking.backend.repository.PrivacyPolicyRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/privacy-policies")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PrivacyPolicyController {

    private final PrivacyPolicyRepository privacyPolicyRepository;

    public PrivacyPolicyController(PrivacyPolicyRepository privacyPolicyRepository) {
        this.privacyPolicyRepository = privacyPolicyRepository;
    }

    @GetMapping
    public List<PrivacyPolicy> getAllActivePolicies() {
        return privacyPolicyRepository.findByIsActiveTrue();
    }
}
