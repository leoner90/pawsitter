package lv.pawsitter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.dto.SitterProfileUpdateDTO;
import lv.pawsitter.entity.SitterAvailability;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.entity.User;
import lv.pawsitter.exception.AvailabilityNotFoundException;
import lv.pawsitter.exception.InvalidSitterOperationException;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.SitterAvailabilityRepository;
import lv.pawsitter.repository.SitterProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

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
    private BookingRepository bookingRepository;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @InjectMocks
    private SitterProfileServiceImpl sitterProfileService;
    private SitterProfile sitterProfile;

    @BeforeEach
    void setUp() {
        sitterProfile = sitterProfile(10L, SITTER_EMAIL);
    }

    @Test
    void getAllSittersReturnsRepositoryResults() {
        SitterProfile otherSitter = sitterProfile(11L, "other@example.com");
        when(sitterProfileRepository.findAll()).thenReturn(List.of(sitterProfile, otherSitter));

        List<SitterProfile> sitters = sitterProfileService.getAllSitters();

        assertThat(sitters).containsExactly(sitterProfile, otherSitter);
        verify(sitterProfileRepository).findAll();
    }

    @Test
    void getSitterByIdReturnsProfileWhenFound() {
        when(sitterProfileRepository.findById(sitterProfile.getId())).thenReturn(Optional.of(sitterProfile));

        SitterProfile result = sitterProfileService.getSitterById(sitterProfile.getId());

        assertThat(result).isEqualTo(sitterProfile);
    }

    @Test
    void getSitterByIdThrowsWhenProfileMissing() {
        when(sitterProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sitterProfileService.getSitterById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 is not found.");
    }

    @Test
    void getProfileByUserEmailReturnsProfileWhenFound() {
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));

        SitterProfile result = sitterProfileService.getProfileByUserEmail(SITTER_EMAIL);

        assertThat(result).isEqualTo(sitterProfile);
    }

    @Test
    void getProfileByUserEmailThrowsWhenProfileMissing() {
        when(sitterProfileRepository.findByUserEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sitterProfileService.getProfileByUserEmail("missing@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with email missing@example.com is not found.");
    }

    @Test
    void updateProfileUpdatesProfileAndUserFieldsWhenNoImageProvided() {
        SitterProfileUpdateDTO updateDto = updateDto(null);
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));

        sitterProfileService.updateProfile(SITTER_EMAIL, updateDto);

        assertThat(sitterProfile.getLocation()).isEqualTo("Jurmala");
        assertThat(sitterProfile.getDescription()).isEqualTo("Updated sitter description");
        assertThat(sitterProfile.getPricePerDay()).isEqualByComparingTo("35.00");
        assertThat(sitterProfile.getUser().getPhoneNumber()).isEqualTo("+37129999999");
        verify(sitterProfileRepository).save(sitterProfile);
        verify(imageStorageService, never()).saveSitterImage(any());
        verify(imageStorageService, never()).deleteSitterImage(any());
    }

    @Test
    void updateProfileReplacesImageWhenNewImageProvided() {
        MultipartFile newImage = new MockMultipartFile("image", "sitter.jpg", "image/jpeg", "content".getBytes());
        SitterProfileUpdateDTO updateDto = updateDto(newImage);
        sitterProfile.setImageUrl("/images/sittersImages/old.jpg");
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(imageStorageService.saveSitterImage(newImage)).thenReturn("/images/sittersImages/new.jpg");

        sitterProfileService.updateProfile(SITTER_EMAIL, updateDto);

        assertThat(sitterProfile.getImageUrl()).isEqualTo("/images/sittersImages/new.jpg");
        verify(imageStorageService).saveSitterImage(newImage);
        verify(imageStorageService).deleteSitterImage("/images/sittersImages/old.jpg");
        verify(sitterProfileRepository).save(sitterProfile);
    }

    @Test
    void updateProfileDoesNotTouchImageWhenImageIsEmpty() {
        MultipartFile emptyImage = new MockMultipartFile("image", new byte[0]);
        SitterProfileUpdateDTO updateDto = updateDto(emptyImage);
        sitterProfile.setImageUrl("/images/sittersImages/old.jpg");
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));

        sitterProfileService.updateProfile(SITTER_EMAIL, updateDto);

        assertThat(sitterProfile.getImageUrl()).isEqualTo("/images/sittersImages/old.jpg");
        verify(imageStorageService, never()).saveSitterImage(any());
        verify(imageStorageService, never()).deleteSitterImage(any());
        verify(sitterProfileRepository).save(sitterProfile);
    }

    @Test
    void updateProfileThrowsWhenProfileMissing() {
        when(sitterProfileRepository.findByUserEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sitterProfileService.updateProfile("missing@example.com", updateDto(null)))
                .isInstanceOf(UserNotFoundException.class);

        verify(sitterProfileRepository, never()).save(any());
    }

    @Test
    void updateProfilePropagatesImageStorageException() {
        MultipartFile badImage = new MockMultipartFile("image", "bad.gif", "image/gif", "content".getBytes());
        SitterProfileUpdateDTO updateDto = updateDto(badImage);
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(imageStorageService.saveSitterImage(badImage))
                .thenThrow(new IllegalArgumentException("Only JPEG and PNG images are allowed"));

        assertThatThrownBy(() -> sitterProfileService.updateProfile(SITTER_EMAIL, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JPEG and PNG");

        verify(sitterProfileRepository, never()).save(any());
    }

    @Test
    void getAvailabilityReturnsSitterAvailabilityRanges() {
        SitterAvailability firstRange = availability(100L, sitterProfile, LocalDate.now(), LocalDate.now().plusDays(2));
        SitterAvailability secondRange = availability(101L, sitterProfile, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId()))
                .thenReturn(List.of(firstRange, secondRange));

        List<SitterAvailability> availabilityRanges = sitterProfileService.getAvailability(SITTER_EMAIL);

        assertThat(availabilityRanges).containsExactly(firstRange, secondRange);
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

    @Test
    void deleteAvailabilityThrowsWhenAvailabilityMissing() {
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(sitterAvailabilityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sitterProfileService.deleteAvailability(SITTER_EMAIL, 999L))
                .isInstanceOf(AvailabilityNotFoundException.class)
                .hasMessage("Availability not found");

        verify(sitterAvailabilityRepository, never()).delete(any());
        verify(sitterProfileRepository, never()).save(any());
    }

    @Test
    void deleteAvailabilityRejectsAnotherSittersAvailability() {
        SitterProfile otherSitter = sitterProfile(11L, "other@example.com");
        SitterAvailability otherAvailability = availability(100L, otherSitter, LocalDate.now(), LocalDate.now().plusDays(2));
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(sitterAvailabilityRepository.findById(otherAvailability.getId())).thenReturn(Optional.of(otherAvailability));

        assertThatThrownBy(() -> sitterProfileService.deleteAvailability(SITTER_EMAIL, otherAvailability.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You cannot remove another sitter's availability");

        verify(sitterAvailabilityRepository, never()).delete(any());
        verify(sitterProfileRepository, never()).save(any());
    }

    @Test
    void deleteAvailabilityKeepsProfilePublishedWhenCurrentOrFutureAvailabilityRemains() {
        SitterAvailability availability = availability(100L, sitterProfile, LocalDate.now(), LocalDate.now().plusDays(2));
        sitterProfile.setPublished(true);
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(sitterAvailabilityRepository.findById(availability.getId())).thenReturn(Optional.of(availability));
        when(sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(sitterProfile.getId(), LocalDate.now()))
                .thenReturn(true);

        sitterProfileService.deleteAvailability(SITTER_EMAIL, availability.getId());

        assertThat(sitterProfile.isPublished()).isTrue();
        verify(sitterAvailabilityRepository).delete(availability);
        verify(sitterProfileRepository, never()).save(any());
    }

    @Test
    void publishProfileRejectsValidationErrors() {
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));
        when(constraintViolation.getMessage()).thenReturn("Location is required");
        when(validator.validate(any())).thenReturn(Set.of(constraintViolation));

        assertThatThrownBy(() -> sitterProfileService.publishProfile(SITTER_EMAIL))
                .isInstanceOf(InvalidSitterOperationException.class)
                .hasMessage("Location is required");

        verify(sitterAvailabilityRepository, never()).existsBySitterProfileIdAndEndDateGreaterThanEqual(any(), any());
        verify(sitterProfileRepository, never()).save(any());
    }

    @Test
    void unpublishProfileSetsPublishedFalse() {
        sitterProfile.setPublished(true);
        when(sitterProfileRepository.findByUserEmail(SITTER_EMAIL)).thenReturn(Optional.of(sitterProfile));

        sitterProfileService.unpublishProfile(SITTER_EMAIL);

        assertThat(sitterProfile.isPublished()).isFalse();
        verify(sitterProfileRepository).save(sitterProfile);
    }

    @Test
    void findFullyAvailableSittersReturnsRepositoryResults() {
        LocalDate startDate = LocalDate.of(2026, 8, 10);
        LocalDate endDate = LocalDate.of(2026, 8, 12);
        when(sitterAvailabilityRepository.findFullyAvailableSitters(startDate, endDate))
                .thenReturn(List.of(sitterProfile));

        List<SitterProfile> sitters = sitterProfileService.findFullyAvailableSitters(startDate, endDate);

        assertThat(sitters).containsExactly(sitterProfile);
    }

    @Test
    void findFullyAvailableSittersRejectsEndDateBeforeStartDate() {
        LocalDate startDate = LocalDate.of(2026, 8, 12);
        LocalDate endDate = LocalDate.of(2026, 8, 10);

        assertThatThrownBy(() -> sitterProfileService.findFullyAvailableSitters(startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");

        verify(sitterAvailabilityRepository, never()).findFullyAvailableSitters(any(), any());
    }

    @Test
    void findPartiallyAvailableSittersReturnsRepositoryResults() {
        LocalDate startDate = LocalDate.of(2026, 8, 10);
        LocalDate endDate = LocalDate.of(2026, 8, 12);
        when(sitterAvailabilityRepository.findPartiallyAvailableSitters(startDate, endDate))
                .thenReturn(List.of(sitterProfile));

        List<SitterProfile> sitters = sitterProfileService.findPartiallyAvailableSitters(startDate, endDate);

        assertThat(sitters).containsExactly(sitterProfile);
    }

    @Test
    void findPartiallyAvailableSittersRejectsEndDateBeforeStartDate() {
        LocalDate startDate = LocalDate.of(2026, 8, 12);
        LocalDate endDate = LocalDate.of(2026, 8, 10);

        assertThatThrownBy(() -> sitterProfileService.findPartiallyAvailableSitters(startDate, endDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");

        verify(sitterAvailabilityRepository, never()).findPartiallyAvailableSitters(any(), any());
    }

    private static SitterProfileUpdateDTO updateDto(MultipartFile image) {
        return new SitterProfileUpdateDTO(
                "Jurmala",
                "+37129999999",
                BigDecimal.valueOf(35),
                "Updated sitter description",
                image
        );
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
