package lv.pawsitter.service;

import lombok.RequiredArgsConstructor;
import lv.pawsitter.dto.SitterAvailabilityRequest;
import lv.pawsitter.dto.SitterProfileUpdateDTO;
import lv.pawsitter.dto.SitterPublishDTO;
import lv.pawsitter.entity.Booking;
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
import java.util.List;
import java.util.Set;

import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;

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
        return sitterProfileRepository.findAll();
    }

    @Override
    public SitterProfile getSitterById(Long id)
    {
        return sitterProfileRepository.findById(id).orElseThrow(() -> new UserNotFoundException("Sitter profile not found"));
    }

    //Get profile By Email
    @Override
    public SitterProfile getProfileByUserEmail(String email)
    {
        return sitterProfileRepository.findByUserEmail(email).orElseThrow(() -> new UserNotFoundException("Sitter profile not found"));
    }

    //return only Published Sitters
    @Override
    @Transactional
    public List<SitterProfile> getPublishedSitters()
    {
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
                sitterProfile.setPublished(false);
                sitterProfileRepository.save(sitterProfile);
            }
        }

        return publishedSitters.stream()
                .filter(SitterProfile::isPublished)
                .toList();
    }

    //Update Profile
    @Override
    @Transactional
    public void updateProfile(String email, SitterProfileUpdateDTO dto)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        sitterProfile.setLocation(dto.location());
        sitterProfile.setDescription(dto.description());
        sitterProfile.setPricePerDay(dto.pricePerDay());

        sitterProfile.getUser().setPhoneNumber(dto.phoneNumber());

        if (dto.image() != null && !dto.image().isEmpty())
        {
            String imageUrl = imageStorageService.saveSitterImage(dto.image());
            String oldImageUrl = sitterProfile.getImageUrl();
            imageStorageService.deleteSitterImage(oldImageUrl);
            sitterProfile.setImageUrl(imageUrl);
        }

        sitterProfileRepository.save(sitterProfile);
    }

    //ad available dates to DB
    @Override
    @Transactional
    public void addAvailability(String email, SitterAvailabilityRequest request)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);
        LocalDate requestedStartDate = request.startDate();
        LocalDate requestedEndDate = request.endDate();
        LocalDate mergedStartDate = request.startDate();
        LocalDate mergedEndDate = request.endDate();

        List<SitterAvailability> availabilityRanges = sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId());


        //if this dates already exists throw err
        boolean alreadyCovered = availabilityRanges.stream()
                .anyMatch(availability ->
                        !requestedStartDate.isBefore(availability.getStartDate())
                                && !requestedEndDate.isAfter(availability.getEndDate())
                );

        if (alreadyCovered)
        {
            throw new InvalidSitterOperationException("These dates are already included in your availability");
        }

        List<SitterAvailability> rangesToMerge = availabilityRanges.stream()
                .filter(availability -> overlapsOrTouches(availability, requestedStartDate, requestedEndDate))
                .toList();

        SitterAvailability availability = rangesToMerge.isEmpty() ? new SitterAvailability() : rangesToMerge.getFirst();

        for (SitterAvailability range : rangesToMerge)
        {
            if (range.getStartDate().isBefore(mergedStartDate))
            {
                mergedStartDate = range.getStartDate();
            }

            if (range.getEndDate().isAfter(mergedEndDate))
            {
                mergedEndDate = range.getEndDate();
            }
        }

        if (rangesToMerge.size() > 1)
        {
            sitterAvailabilityRepository.deleteAll(rangesToMerge.subList(1, rangesToMerge.size()));
        }

        availability.setSitterProfile(sitterProfile);
        availability.setStartDate(mergedStartDate);
        availability.setEndDate(mergedEndDate);

        sitterAvailabilityRepository.save(availability);
    }

    //Get Available Dates From Db
    @Override
    @Transactional(readOnly = true)
    public List<SitterAvailability> getAvailability(String email)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        return sitterAvailabilityRepository.findBySitterProfileId(sitterProfile.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SitterAvailability> getAvailabilityBySitterId(Long sitterId)
    {
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
        SitterProfile sitterProfile = getProfileByUserEmail(email);

        SitterAvailability availability = sitterAvailabilityRepository.findById(availabilityId).orElseThrow(() -> new AvailabilityNotFoundException("Availability not found"));

        if (!availability.getSitterProfile().getId().equals(sitterProfile.getId()))
        {
            throw new AccessDeniedException("You cannot remove another sitter's availability");
        }

        sitterAvailabilityRepository.delete(availability);

        boolean hasAvailability = sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(
                sitterProfile.getId(),
                LocalDate.now()
        );

        //if it was last current/future range, set publish status to false
        if (!hasAvailability)
        {
            sitterProfile.setPublished(false);
            sitterProfileRepository.save(sitterProfile);
        }
    }

    //Try to publish and errors check using DTO (SitterPublishDTO)
    @Override
    @Transactional
    public void publishProfile(String email)
    {
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
            throw new InvalidSitterOperationException(violations.iterator().next().getMessage());
        }

        // Check if sitter has current or future availability
        boolean hasAvailability = sitterAvailabilityRepository.existsBySitterProfileIdAndEndDateGreaterThanEqual(
                sitterProfile.getId(),
                LocalDate.now()
        );

        if (!hasAvailability)
        {
            throw new InvalidSitterOperationException("At least one current or future availability range is required");
        }

        sitterProfile.setPublished(true);
        sitterProfileRepository.save(sitterProfile);
    }

    //Unpublish Profile
    @Override
    @Transactional
    public void unpublishProfile(String email)
    {
        SitterProfile sitterProfile = getProfileByUserEmail(email);
        sitterProfile.setPublished(false);
        sitterProfileRepository.save(sitterProfile);
    }

    //search
    @Override
    @Transactional(readOnly = true)
    public List<SitterProfile> findFullyAvailableSitters(LocalDate startDate, LocalDate endDate)
    {
        if (endDate.isBefore(startDate))
        {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        return sitterAvailabilityRepository.findFullyAvailableSitters(startDate, endDate);
    }

    //partial search
    @Override
    @Transactional(readOnly = true)
    public List<SitterProfile> findPartiallyAvailableSitters(LocalDate startDate, LocalDate endDate)
    {
        if (endDate.isBefore(startDate))
        {
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

        return sitters.stream()
                .filter(sitter -> !hasCity || cityMatches(sitter, city))
                .filter(sitter -> priceMatches(sitter, maxPrice))
                .toList();
    }


    private void validateSearchFilters(LocalDate startDate, LocalDate endDate, BigDecimal maxPrice)
    {
        boolean hasStartDate = startDate != null;
        boolean hasEndDate = endDate != null;

        if (hasStartDate != hasEndDate)
        {
            //just a note if sitter select only one DATE sitters will be return as empty list, so no sitters shown to focus that the was error in search condition!
            throw new IllegalArgumentException("Both start date and end date must be selected");
        }

        if (hasStartDate && startDate.isBefore(LocalDate.now()))
        {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        if (hasStartDate && endDate.isBefore(startDate))
        {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        //no need leading zero check like 00050000 browser and spring will deal with that(BigDecimal)
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) <= 0)
        {
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
}
