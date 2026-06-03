package com.tourbooking.booking.backend.service.impl;

import com.tourbooking.booking.backend.exception.AppException;
import com.tourbooking.booking.backend.exception.ErrorCode;
import com.tourbooking.booking.backend.model.entity.Document;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.repository.DocumentRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import com.tourbooking.booking.backend.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public Document upload(Long userId, String url, String type) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Document doc = new Document();

        doc.setUser(user);
        doc.setFileUrl(url);
        doc.setType(type);
        doc.setUploadedAt(LocalDateTime.now());

        return documentRepository.save(doc);
    }
}