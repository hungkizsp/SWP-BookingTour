package com.tourbooking.booking.backend.component;

import com.tourbooking.booking.backend.model.entity.Booking;
import com.tourbooking.booking.backend.model.entity.TourSchedule;
import com.tourbooking.booking.backend.model.entity.enums.BookingStatus;
import com.tourbooking.booking.backend.repository.BookingRepository;
import com.tourbooking.booking.backend.repository.TourScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingRepository bookingRepository;
    private final TourScheduleRepository tourScheduleRepository;

    @Scheduled(fixedRate = 60000) // Runs every 1 minute
    @Transactional
    public void releaseExpiredBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        log.debug("Running BookingScheduler to release PENDING online bookings created before {}", cutoff);

        List<Booking> expiredBookings = bookingRepository.findPendingPayosBookingsBefore(cutoff);

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Found {} expired PENDING online bookings to cancel", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // Change status to CANCELLED
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                // Return slots back to schedule
                TourSchedule schedule = booking.getSchedule();
                if (schedule != null) {
                    Integer slotsToRelease = booking.getOccupiedSlots() != null
                            ? booking.getOccupiedSlots()
                            : booking.getNumberOfPeople();
                    if (slotsToRelease != null && slotsToRelease > 0) {
                        tourScheduleRepository.findByIdWithLock(schedule.getId()).ifPresent(lockedSchedule -> {
                            lockedSchedule.setAvailableSlots(lockedSchedule.getAvailableSlots() + slotsToRelease);
                            tourScheduleRepository.save(lockedSchedule);
                            log.info("Cancelled booking #{} and returned {} slots to schedule #{}",
                                    booking.getId(), slotsToRelease, lockedSchedule.getId());
                        });
                    }
                }
            } catch (Exception e) {
                log.error("Error releasing slots for booking #{}", booking.getId(), e);
            }
        }
    }
}
