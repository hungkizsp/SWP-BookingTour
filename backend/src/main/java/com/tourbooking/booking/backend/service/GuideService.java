package com.tourbooking.booking.backend.service;

import com.tourbooking.booking.backend.model.dto.response.TourScheduleResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface GuideService {
    List<TourScheduleResponse> getAssignedTours(Long guideId);
    TourScheduleResponse getAssignedTourDetails(Long guideId, Long scheduleId);
    void updateTourProgress(Long guideId, Long scheduleId, String progress);
    void uploadTourPhotos(Long guideId, Long scheduleId, List<MultipartFile> photos);
    void submitTourReport(Long guideId, Long scheduleId, String reportContent);
}
