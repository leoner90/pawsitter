package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.dto.SitterProfileUpdateDTO;
import lv.pawsitter.dto.SitterPublishDTO;
import lv.pawsitter.entity.Booking;
import lv.pawsitter.entity.BookingStatus;
import lv.pawsitter.entity.SitterAvailability;
import lv.pawsitter.entity.SitterProfile;
import lv.pawsitter.exception.AvailabilityNotFoundException;
import lv.pawsitter.exception.BookingNotFoundException;
import lv.pawsitter.exception.InvalidSitterOperationException;
import lv.pawsitter.repository.BookingRepository;
import lv.pawsitter.repository.SitterAvailabilityRepository;
import lv.pawsitter.exception.UserNotFoundException;
import lv.pawsitter.repository.SitterProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;

@Slf4j
@Service
@RequiredArgsConstructor
public class SitterProfileServiceImpl implements SitterProfileService
{
    private final SitterProfileRepository sitterProfileRepository;
    private final ImageStorageService imageStorageService;
    private final SitterAvailabilityRepository sitterAvailabilityRepository;
    private final Validator validator;
    private final BookingRepository bookingRepository;

    @Override
    public List<SitterProfile> getAllSitters()
    {
        log.debug("Fetching all sitter profiles");
        return sitterProfileRepository.findAll();
    }

    @Override
    public SitterProfile getSitterById(Long id)
    {
        log.debug("Fetching sitter profile by id {}", id);
        return sitterProfileRepository.findById(id).orElseThrow(() -> {
            log.warn("Sitter profile {} not found", id);
            return new UserNotFoundException(id);
        });
    }

    //Get profile By Email
    @Override
    public SitterProfile getProfileByUserEmail(String email)
    {
        log.debug("Fetching sitter profile for email {}", email);
        return sitterProfileRepository.findByUserEmail(email).orElseThrow(() -> {
            log.warn("Sitter profile not found for email {}", email);
            return new UserNotFoundException(email);
        });
    }

    //return only Published Sitters
    @Override
    @Transactional
    public List<SitterProfile> getPublishedSitters()
    {
        log.debug("Fetching published sitters and pruning stale ones");
        List<SitterProfile> publishedSitters = sitterProfileRepository.findByPublishedTrue();
        LocalDate today = LocalDate.now();

        for (SitterProfile sitterProfile : publishedSitters)
        {
            boolean hasCurrentAvailability = sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(
                    sitterProfile.getId(),
                    today
            );

            if (!hasCurrentAvailability)
            {
                log.info("Sitter {} has no current/future availability, unpublishing", sitterProfile.getId());
                sitterProfile.setPublished(false);
                sitterProfileRepository.save(sitterProfile);
            }
        }

        List<SitterProfile> result = publishedSitters.stream()
                .filter(SitterProfile::isPublished)
                .toList();

        log.debug("Returning {} published sitters", result.size());
        return result;
    }

    //Update Profile
    @Override
    @Transactional
    public void updateProfile(String email, SitterProfileUpdateDTO dto)
    {
        log.info("Updating sitter profile for {}", email);
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        sitterProfile.setLocation(dto.location());
        sitterProfile.setDescription(dto.description());
        sitterProfile.setPricePerDay(dto.pricePerDay());

        sitterProfile.getUser().setPhoneNumber(dto.phoneNumber());

        if (dto.image() != null && !dto.image().isEmpty())
        {
            log.debug("Replacing profile image for sitter {}", sitterProfile.getId());
            String imageUrl = imageStorageService.saveSitterImage(dto.image());
            String oldImageUrl = sitterProfile.getImageUrl();
            imageStorageService.deleteSitterImage(oldImageUrl);
            sitterProfile.setImageUrl(imageUrl);
        }

        sitterProfileRepository.save(sitterProfile);
        log.info("Sitter profile updated for {}", email);
    }

    //ad available dates to DB
    @Override
    @Transactional
    public void addAvailability(String email, SitterAvailabilityRequest request)
    {
        log.info("Adding availability for sitter {} from {} to {}", email, request.startDate(), request.endDate());
        SitterProfile sitterProfile = getProfileByUserEmail(email);
        LocalDate requestedStartDate = request.startDate();
        LocalDate requestedEndDate = request.endDate();
        List<SitterAvailability> availabilityRanges = sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId());

        //If dates are already in Availability calendar
        boolean alreadyCovered = availabilityRanges.stream()
                .anyMatch(availability ->
                        !requestedStartDate.isBefore(availability.getStartDate())
                                && !requestedEndDate.isAfter(availability.getEndDate())
                );

        if (alreadyCovered)
        {
            log.warn("Add availability failed for sitter {} - dates already covered", sitterProfile.getId());
            throw new InvalidSitterOperationException("These dates are already included in your availability");
        }

        //are requested dates already booked or requested to book , then do not allow to add
        boolean overlapsActiveBooking =
                bookingRepository.existsOverlappingBooking(
                        sitterProfile.getId(),
                        requestedStartDate.atStartOfDay(),
                        requestedEndDate.atTime(LocalTime.MAX),
                        EnumSet.of(
                                BookingStatus.REQUESTED,
                                BookingStatus.ACCEPTED
                        )
                );

        if (overlapsActiveBooking)
        {
            log.warn("Add availability failed for sitter {} - dates overlap an active booking", sitterProfile.getId());
            throw new InvalidSitterOperationException("These dates overlap an active booking");
        }

        //Add new dates or merge with existing ones
        addOrMergeAvailability(sitterProfile, requestedStartDate, requestedEndDate);
        log.info("Availability added for sitter {}", sitterProfile.getId());
    }

    //Get Available Dates From Db
    @Override
    @Transactional(readOnly = true)
    public List<SitterAvailability> getAvailability(String email)
    {
        log.debug("Fetching availability for sitter {}", email);
        SitterProfile sitterProfile = getProfileByUserEmail(email);
        return sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SitterAvailability> getAvailabilityBySitterId(Long sitterId)
    {
        log.debug("Fetching current/future availability for sitter id {}", sitterId);
        return sitterAvailabilityRepository.findBySitterProfileIdAndEndDateGreaterThanEqualOrderByStartDateAsc(
                sitterId,
                LocalDate.now()
        );
    }

    //Remove Available date
    @Override
    @Transactional
    public void deleteAvailability(String email, Long availabilityId)
    {
        log.info("Sitter {} attempting to delete availability {}", email, availabilityId);
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        SitterAvailability availability = sitterAvailabilityRepository.findById(availabilityId).orElseThrow(() -> {
            log.warn("Availability {} not found", availabilityId);
            return new AvailabilityNotFoundException("Availability not found");
        });


        if (!availability.getSitterProfile().getId().equals(sitterProfile.getId()))
        {
            log.warn("Access denied - sitter {} attempted to delete availability {} belonging to another sitter", sitterProfile.getId(), availabilityId);
            throw new AccessDeniedException("You cannot remove another sitter's availability");
        }

        sitterAvailabilityRepository.delete(availability);
        log.info("Availability {} deleted for sitter {}", availabilityId, sitterProfile.getId());

        boolean hasAvailability = sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(
                sitterProfile.getId(),
                LocalDate.now()
        );

        //if it was last current/future range, set publish status to false
        if (!hasAvailability)
        {
            log.info("Sitter {} has no remaining availability after deletion, unpublishing profile", sitterProfile.getId());
            sitterProfile.setPublished(false);
            sitterProfileRepository.save(sitterProfile);
        }
    }

    //Try to publish and errors check using DTO (SitterPublishDTO)
    @Override
    @Transactional
    public void publishProfile(String email)
    {
        log.info("Sitter {} attempting to publish profile", email);
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        SitterPublishDTO publishDTO = new SitterPublishDTO(
                sitterProfile.getLocation(),
                sitterProfile.getDescription(),
                sitterProfile.getPricePerDay(),
                sitterProfile.getUser().getPhoneNumber()
        );

        //runs the DTO validation:
        Set<ConstraintViolation<SitterPublishDTO>> violations = validator.validate(publishDTO);

        //if errors
        if (!violations.isEmpty())
        {
            String violationMessage = violations.iterator().next().getMessage();
            log.warn("Publish failed for sitter {} - validation violation: {}", sitterProfile.getId(), violationMessage);
            throw new InvalidSitterOperationException(violations.iterator().next().getMessage());
        }

        // Check if sitter has current or future availability
        boolean hasAvailability = sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(
                sitterProfile.getId(),
                LocalDate.now()
        );

        if (!hasAvailability)
        {
            log.warn("Publish failed for sitter {} - no current or future availability", sitterProfile.getId());
            throw new InvalidSitterOperationException("At least one current or future availability range is required");
        }

        sitterProfile.setPublished(true);
        sitterProfileRepository.save(sitterProfile);
        log.info("Sitter {} profile published", sitterProfile.getId());
    }

    //Unpublish Profile
    @Override
    @Transactional
    public void unpublishProfile(String email)
    {
        log.info("Sitter {} unpublishing profile", email);
        SitterProfile sitterProfile = getProfileByUserEmail(email);
        sitterProfile.setPublished(false);
        sitterProfileRepository.save(sitterProfile);
    }

    //search
    @Override
    @Transactional(readOnly = true)
    public List<SitterProfile> findFullyAvailableSitters(LocalDate startDate, LocalDate endDate)
    {
        log.debug("Finding fully available sitters between {} and {}", startDate, endDate);
        if (endDate.isBefore(startDate))
        {
            log.warn("Fully-available search failed - end date {} before start date {}", endDate, startDate);
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        return sitterAvailabilityRepository.findFullyAvailableSitters(startDate, endDate);
    }

    //partial search
    @Override
    @Transactional(readOnly = true)
    public List<SitterProfile> findPartiallyAvailableSitters(LocalDate startDate, LocalDate endDate)
    {
        log.debug("Finding partially available sitters between {} and {}", startDate, endDate);
        if (endDate.isBefore(startDate))
        {
            log.warn("Partially-available search failed - end date {} before start date {}", endDate, startDate);
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        return sitterAvailabilityRepository.findPartiallyAvailableSitters(startDate, endDate);
    }

    private boolean overlapsOrTouches(SitterAvailability availability, LocalDate startDate, LocalDate endDate)
    {
        return !availability.getEndDate().isBefore(startDate.minusDays(1))
                && !availability.getStartDate().isAfter(endDate.plusDays(1));
    }

    //new search
    @Override
    @Transactional(readOnly = true)
    public List<SitterProfile> searchSitters(String city, LocalDate startDate, LocalDate endDate, BigDecimal maxPrice, boolean includePartial)
    {
        log.debug("Searching sitters - city={}, startDate={}, endDate={}, maxPrice={}, includePartial={}", city, startDate, endDate, maxPrice, includePartial);
        boolean hasCity = city != null && !city.isBlank();
        boolean hasStartDate = startDate != null;
        boolean hasEndDate = endDate != null;

        validateSearchFilters(startDate, endDate, maxPrice);

        List<SitterProfile> sitters;

        if (hasStartDate && hasEndDate)
        {
            sitters = includePartial ? findPartiallyAvailableSitters(startDate, endDate) : findFullyAvailableSitters(startDate, endDate);
        }
        else
        {
            sitters = getPublishedSitters();
        }

        List<SitterProfile> result = sitters.stream()
                .filter(sitter -> !hasCity || cityMatches(sitter, city))
                .filter(sitter -> priceMatches(sitter, maxPrice))
                .toList();

        log.debug("Sitter search returned {} results", result.size());
        return result;
    }

    private void validateSearchFilters(LocalDate startDate, LocalDate endDate, BigDecimal maxPrice)
    {
        boolean hasStartDate = startDate != null;
        boolean hasEndDate = endDate != null;

        if (hasStartDate != hasEndDate)
        {
            //just a note if sitter select only one DATE sitters will be return as empty list, so no sitters shown to focus that the was error in search condition!
            log.warn("Search filter validation failed - only one of start/end date supplied");
            throw new IllegalArgumentException("Both start date and end date must be selected");
        }

        if (hasStartDate && startDate.isBefore(LocalDate.now()))
        {
            log.warn("Search filter validation failed - start date {} is in the past", startDate);
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        if (hasStartDate && endDate.isBefore(startDate))
        {
            log.warn("Search filter validation failed - end date {} before start date {}", endDate, startDate);
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        //no need leading zero check like 00050000 browser and spring will deal with that(BigDecimal)
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) <= 0)
        {
            log.warn("Search filter validation failed - maxPrice {} is not greater than zero", maxPrice);
            throw new IllegalArgumentException("Maximum price must be greater than zero");
        }
    }

    private boolean cityMatches(SitterProfile sitter, String city)
    {
        return sitter.getLocation() != null && sitter.getLocation().equalsIgnoreCase(city.trim());
    }

    private boolean priceMatches(SitterProfile sitter, BigDecimal maxPrice)
    {
        if (maxPrice == null) {return true;}

        BigDecimal price = sitter.getPricePerDay();
        return price != null && price.compareTo(maxPrice) <= 0;
    }

    //Add new availability dates or merge with existing ones
    private void addOrMergeAvailability(SitterProfile sitterProfile, LocalDate requestedStartDate, LocalDate requestedEndDate)
    {
        //init dates
        LocalDate mergedStartDate = requestedStartDate;
        LocalDate mergedEndDate = requestedEndDate;

        //get already available dates from calendar
        List<SitterAvailability> availabilityRanges = sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId());

        //find are dates in calendar touching or overlap with new added ones return list if so( if ranges are new this filter will find nothing)
        List<SitterAvailability> rangesToMerge = availabilityRanges.stream()
                .filter(availability ->
                        overlapsOrTouches(availability, requestedStartDate, requestedEndDate))
                .toList();

        log.debug("Merging availability for sitter {} - {} range(s) touch/overlap {} to {}",
                sitterProfile.getId(), rangesToMerge.size(), requestedStartDate, requestedEndDate);

        // if there are no overlap dates -> create new SitterAvailability, else reuses the first existing range
        SitterAvailability availability = rangesToMerge.isEmpty() ? new SitterAvailability() : rangesToMerge.getFirst();

        for (SitterAvailability range : rangesToMerge)
        {
            //so if new date is before original -> it's become new start
            if (range.getStartDate().isBefore(mergedStartDate))
            {
                mergedStartDate = range.getStartDate();
            }

            //so if new date is after original end-> it's become new end
            if (range.getEndDate().isAfter(mergedEndDate))
            {
                mergedEndDate = range.getEndDate();
            }
        }

        //cleans up the extra database rows after merging
        if (rangesToMerge.size() > 1)
        {
            log.debug("Deleting {} redundant availability rows for sitter {} after merge", rangesToMerge.size() - 1, sitterProfile.getId());
            sitterAvailabilityRepository.deleteAll(rangesToMerge.subList(1, rangesToMerge.size()));
        }

        //Fills  final availability object with  correct sitter and merged dates, then save it.
        availability.setSitterProfile(sitterProfile);
        availability.setStartDate(mergedStartDate);
        availability.setEndDate(mergedEndDate);

        sitterAvailabilityRepository.save(availability);
        log.debug("Availability saved for sitter {} - merged range {} to {}", sitterProfile.getId(), mergedStartDate, mergedEndDate);
    }

    //basically Helper function to restore available dates if booking was canceled we call it from booking service
    @Override
    @Transactional
    public void restoreAvailability(SitterProfile sitterProfile, LocalDate startDate, LocalDate endDate)
    {
        log.info("Restoring availability for sitter {} from {} to {}", sitterProfile.getId(), startDate, endDate);
        addOrMergeAvailability(sitterProfile, startDate, endDate);
    }
}
