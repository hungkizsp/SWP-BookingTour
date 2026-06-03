package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.entity.Document;

public interface DocumentService {
    Document upload(Long userId, String url, String type);
}