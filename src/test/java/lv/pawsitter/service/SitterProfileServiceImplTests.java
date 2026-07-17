package lv.pawsitter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.entity.SitterAvailability;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.InvalidSitterOperationException;
import lv.pawsitter.repository.SitterAvailabilityRepository;
import lv.pawsitter.repository.SitterProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SitterProfileServiceImplTests {

    private static final String SITTER_EMAIL = "sitter@example.com";

    @Mock
    private SitterProfileRepository sitterProfileRepository;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private SitterAvailabilityRepository sitterAvailabilityRepository;

    @Mock
    private Validator validator;

    private SitterProfileServiceImpl sitterProfileService;
    private SitterProfile sitterProfile;

    @BeforeEach
    void setUp() {
        sitterProfileService = new SitterProfileServiceImpl(
                sitterProfileRepository,
                imageStorageService,
                sitterAvailabilityRepository,
                validator
        );
        sitterProfile = sitterProfile(10L, SITTER_EMAIL);
    }

    @Test
    void addAvailabilityMergesOverlappingAndAdjacentRanges() {
        SitterAvailability firstRange = availability(100L, sitterProfile, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12));
        SitterAvailability secondRange = availability(101L, sitterProfile, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 18));
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId()))
                .thenReturn(List.of(firstRange, secondRange));

        sitterProfileService.addAvailability(
                SITTER_EMAIL,
                new SitterAvailabilityRequest(LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 14))
        );

        assertThat(firstRange.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(firstRange.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 18));
        verify(sitterAvailabilityRepository).deleteAll(List.of(secondRange));
        verify(sitterAvailabilityRepository).save(firstRange);
    }

    @Test
    void publishProfileRejectsOnlyExpiredAvailability() {
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(validator.validate(any())).thenReturn(Set.of());
        when(sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(
                sitterProfile.getId(),
                LocalDate.now()
        )).thenReturn(false);

        assertThatThrownBy(() -> sitterProfileService.publishProfile(SITTER_EMAIL))
                .isInstanceOf(InvalidSitterOperationException.class)
                .hasMessage("At least one current or future availability range is required");

        verify(sitterProfileRepository, never()).save(any());
    }

    @Test
    void publishProfilePublishesWhenCurrentOrFutureAvailabilityExists() {
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(validator.validate(any())).thenReturn(Set.of());
        when(sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(
                sitterProfile.getId(),
                LocalDate.now()
        )).thenReturn(true);

        sitterProfileService.publishProfile(SITTER_EMAIL);

        assertThat(sitterProfile.isPublished()).isTrue();
        verify(sitterProfileRepository).save(sitterProfile);
    }

    @Test
    void getPublishedSittersUnpublishesProfilesWithoutCurrentOrFutureAvailability() {
        SitterProfile stalePublishedProfile = sitterProfile(11L, "stale@example.com");
        stalePublishedProfile.setPublished(true);
        SitterProfile activePublishedProfile = sitterProfile(12L, "active@example.com");
        activePublishedProfile.setPublished(true);
        when(sitterProfileRepository.findByPublishedTrue()).thenReturn(List.of(stalePublishedProfile, activePublishedProfile));
        when(sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(stalePublishedProfile.getId(), LocalDate.now()))
                .thenReturn(false);
        when(sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(activePublishedProfile.getId(), LocalDate.now()))
                .thenReturn(true);

        List<SitterProfile> publishedSitters = sitterProfileService.getPublishedSitters();

        assertThat(stalePublishedProfile.isPublished()).isFalse();
        assertThat(publishedSitters).containsExactly(activePublishedProfile);
        verify(sitterProfileRepository).save(stalePublishedProfile);
        verify(sitterProfileRepository, never()).save(activePublishedProfile);
    }

    @Test
    void deleteAvailabilityUnpublishesWhenNoCurrentOrFutureAvailabilityRemains() {
        SitterAvailability availability = availability(100L, sitterProfile, LocalDate.now(), LocalDate.now().plusDays(2));
        sitterProfile.setPublished(true);
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(sitterAvailabilityRepository.findById(availability.getId())).thenReturn(Optional.of(availability));
        when(sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(sitterProfile.getId(), LocalDate.now()))
                .thenReturn(false);

        sitterProfileService.deleteAvailability(SITTER_EMAIL, availability.getId());

        assertThat(sitterProfile.isPublished()).isFalse();
        verify(sitterAvailabilityRepository).delete(availability);
        verify(sitterProfileRepository).save(sitterProfile);
    }

    @Test
    void getAvailabilityBySitterIdReturnsOnlyCurrentOrFutureRanges() {
        SitterAvailability currentRange = availability(100L, sitterProfile, LocalDate.now(), LocalDate.now().plusDays(2));
        when(sitterAvailabilityRepository.findBySitterProfileIdAndEndDateGreaterThanEqualOrderByStartDateAsc(
                sitterProfile.getId(),
                LocalDate.now()
        )).thenReturn(List.of(currentRange));

        List<SitterAvailability> ranges = sitterProfileService.getAvailabilityBySitterId(sitterProfile.getId());

        assertThat(ranges).containsExactly(currentRange);
    }

    @Test
    void addAvailabilityCreatesNewRangeWhenNothingTouches() {
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId())).thenReturn(List.of());

        sitterProfileService.addAvailability(
                SITTER_EMAIL,
                new SitterAvailabilityRequest(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12))
        );

        ArgumentCaptor<SitterAvailability> availabilityCaptor = ArgumentCaptor.forClass(SitterAvailability.class);
        verify(sitterAvailabilityRepository).save(availabilityCaptor.capture());

        SitterAvailability savedAvailability = availabilityCaptor.getValue();
        assertThat(savedAvailability.getSitterProfile()).isEqualTo(sitterProfile);
        assertThat(savedAvailability.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(savedAvailability.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        verify(sitterAvailabilityRepository, never()).deleteAll(any());
    }

    private static SitterProfile sitterProfile(Long id, String email) {
        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber("+37120000000");

        SitterProfile sitterProfile = new SitterProfile();
        sitterProfile.setId(id);
        sitterProfile.setUser(user);
        sitterProfile.setLocation("Riga");
        sitterProfile.setDescription("Experienced sitter");
        sitterProfile.setPricePerDay(BigDecimal.valueOf(25));
        return sitterProfile;
    }

    private static SitterAvailability availability(Long id, SitterProfile sitterProfile, LocalDate startDate, LocalDate endDate) {
        SitterAvailability availability = new SitterAvailability();
        availability.setId(id);
        availability.setSitterProfile(sitterProfile);
        availability.setStartDate(startDate);
        availability.setEndDate(endDate);
        return availability;
    }
}
