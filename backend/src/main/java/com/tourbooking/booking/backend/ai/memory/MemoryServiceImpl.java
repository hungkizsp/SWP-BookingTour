package com.tourbooking.booking.backend.ai.memory;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.User;
import com.tourbooking.booking.backend.model.entity.UserAIProfile;
import com.tourbooking.booking.backend.repository.UserAIProfileRepository;
import com.tourbooking.booking.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final UserAIProfileRepository profileRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional(readOnly = true)
    public UserAIProfile getProfile(Long userId) {
        if (userId == null) return null;
        return profileRepo.findByUserId(userId).orElse(null);
    }

    @Override
    @Transactional
    public void updateAfterBooking(Long userId, Booking booking) {
        if (userId == null || booking == null) return;

        User user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        UserAIProfile profile = profileRepo.findByUserId(userId).orElseGet(() -> {
            UserAIProfile p = new UserAIProfile();
            p.setUser(user);
            p.setFamilySize(1);
            p.setTravelStyle("Linh hoạt");
            return p;
        });

        if (booking.getNumberOfPeople() != null && booking.getNumberOfPeople() > 0) {
            profile.setFamilySize(booking.getNumberOfPeople());
        }
        if (booking.getSchedule() != null && booking.getSchedule().getTour() != null) {
            var tour = booking.getSchedule().getTour();
            if (tour.getCategory() != null) {
                profile.setFavoriteCategories(tour.getCategory().getCategoryName());
            }
            if (tour.getEndLocation() != null) {
                profile.setPreferredDestinations(tour.getEndLocation());
            }
        }
        profile.setLastAnalyzedAt(LocalDateTime.now());
        profileRepo.save(profile);
        log.info("[MemoryService] Updated AI profile for user #{}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getProfileSummary(Long userId) {
        UserAIProfile profile = getProfile(userId);
        if (profile == null) {
            return "Khách hàng mới (chưa có hồ sơ sở thích cá nhân)";
        }
        return String.format("Phong cách: %s | Danh mục yêu thích: %s | Điểm đến ưa thích: %s | Quyến nhóm: %d người",
                profile.getTravelStyle() != null ? profile.getTravelStyle() : "N/A",
                profile.getFavoriteCategories() != null ? profile.getFavoriteCategories() : "Tất cả",
                profile.getPreferredDestinations() != null ? profile.getPreferredDestinations() : "N/A",
                profile.getFamilySize() != null ? profile.getFamilySize() : 1);
    }
}
