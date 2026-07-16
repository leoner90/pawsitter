package lv.pawsitter.service;

import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.dto.SitterProfileUpdateDTO;
import lv.pawsitter.entity.SitterAvailability;
import lv.pawsitter.entity.SitterProfile;

import java.time.LocalDate;
import java.util.List;

public interface SitterProfileService
{
    List<SitterProfile> getAllSitters();
    SitterProfile getSitterById(Long id);
    SitterProfile getProfileByUserEmail(String email);
    List<SitterProfile> getPublishedSitters();
    List<SitterAvailability> getAvailability(String email);
    List<SitterAvailability> getAvailabilityBySitterId(Long sitterId);
    List<SitterProfile> findFullyAvailableSitters(LocalDate startDate, LocalDate endDate);
    List<SitterProfile> findPartiallyAvailableSitters(LocalDate startDate, LocalDate endDate);

    void updateProfile(String email, SitterProfileUpdateDTO dto);
    void addAvailability(String email, SitterAvailabilityRequest request);
    void deleteAvailability(String email, Long availabilityId);
    void publishProfile(String email);
    void unpublishProfile(String email);
}
