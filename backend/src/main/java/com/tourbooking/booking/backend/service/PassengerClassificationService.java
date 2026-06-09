package com.tourbooking.booking.backend.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tourbooking.booking.backend.exception.BadRequestException;
import com.tourbooking.booking.backend.model.dto.request.PassengerRequest;
import com.tourbooking.booking.backend.model.entity.enums.PassengerType;

import lombok.Getter;

@Service
public class PassengerClassificationService {

    private static final String AGE_MISMATCH_MESSAGE =
            "Độ tuổi hành khách không khớp với số lượng khai báo ban đầu, vui lòng kiểm tra lại!";

    public ClassificationResult classify(
            List<PassengerRequest> passengers,
            LocalDate tourStartDate,
            int declaredAdultCount,
            int declaredChildCount,
            int declaredInfantCount) {

        if (tourStartDate == null) {
            throw new BadRequestException("Ngày khởi hành tour không hợp lệ.");
        }

        int expectedPassengers = declaredAdultCount + declaredChildCount + declaredInfantCount;
        if (passengers == null || passengers.size() != expectedPassengers) {
            throw new BadRequestException(
                    "Số hành khách gửi lên (" + (passengers == null ? 0 : passengers.size())
                            + ") không khớp với tổng số người đặt (" + expectedPassengers + ").");
        }

        int realAdultCount = 0;
        int realChildCount = 0;
        int realInfantCount = 0;
        List<ClassifiedPassenger> classifiedPassengers = new ArrayList<>();

        for (PassengerRequest passengerRequest : passengers) {
            if (passengerRequest.getDateOfBirth() == null) {
                throw new BadRequestException("Ngày sinh hành khách không được để trống.");
            }

            PassengerType resolvedType = resolvePassengerType(passengerRequest.getDateOfBirth(), tourStartDate);
            classifiedPassengers.add(new ClassifiedPassenger(passengerRequest, resolvedType));

            switch (resolvedType) {
                case ADULT -> realAdultCount++;
                case CHILD -> realChildCount++;
                case INFANT -> realInfantCount++;
            }
        }

        if (realAdultCount != declaredAdultCount
                || realChildCount != declaredChildCount
                || realInfantCount != declaredInfantCount) {
            throw new BadRequestException(AGE_MISMATCH_MESSAGE);
        }

        if (realAdultCount < 1 && (realChildCount > 0 || realInfantCount > 0)) {
            throw new BadRequestException(
                    "Trẻ em hoặc em bé không được đi một mình, bắt buộc phải có ít nhất 1 người lớn đi kèm.");
        }

        return new ClassificationResult(
                realAdultCount,
                realChildCount,
                realInfantCount,
                classifiedPassengers);
    }

    /**
     * Tuổi tính theo ngày khởi hành tour (không dùng ngày đặt đơn).
     * >= 12: ADULT | 2-11: CHILD | &lt; 2: INFANT
     */
    public PassengerType resolvePassengerType(LocalDate dateOfBirth, LocalDate tourStartDate) {
        int age = Period.between(dateOfBirth, tourStartDate).getYears();
        if (age >= 12) {
            return PassengerType.ADULT;
        }
        if (age >= 2) {
            return PassengerType.CHILD;
        }
        return PassengerType.INFANT;
    }

    @Getter
    public static class ClassificationResult {
        private final int realAdultCount;
        private final int realChildCount;
        private final int realInfantCount;
        private final List<ClassifiedPassenger> passengers;

        public ClassificationResult(
                int realAdultCount,
                int realChildCount,
                int realInfantCount,
                List<ClassifiedPassenger> passengers) {
            this.realAdultCount = realAdultCount;
            this.realChildCount = realChildCount;
            this.realInfantCount = realInfantCount;
            this.passengers = passengers;
        }

        public int getSlotsToDeduct() {
            return realAdultCount + realChildCount;
        }

        public int getTotalPassengers() {
            return realAdultCount + realChildCount + realInfantCount;
        }
    }

    @Getter
    public static class ClassifiedPassenger {
        private final PassengerRequest request;
        private final PassengerType passengerType;

        public ClassifiedPassenger(PassengerRequest request, PassengerType passengerType) {
            this.request = request;
            this.passengerType = passengerType;
        }
    }
}
